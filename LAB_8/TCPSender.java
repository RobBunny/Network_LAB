import java.io.*;
import java.net.*;
import java.util.*;

public class TCPSender {
    private String mode;
    private int cwnd;
    private int ssthresh;
    private int dupACKcount;
    private String lastACK;
    private String pendingRetransmit;
    private String lastRetransmittedPacket;
    private int packetCounter;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    public TCPSender() {
        this.cwnd = 1;
        this.ssthresh = 8;
        this.dupACKcount = 0;
        this.lastACK = "";
        this.packetCounter = 0;
    }
    
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
    
    private List<String> sendPackets(int numPackets) {
        List<String> packets = new ArrayList<>();
        for (int i = 0; i < numPackets; i++) {
            String packetId = "pkt" + packetCounter++;
            packets.add(packetId);
        }
        return packets;
    }
    
    private List<String> receiveACKs(int expectedCount) throws IOException {
        List<String> acks = new ArrayList<>();
        socket.setSoTimeout(2000);
        
        for (int i = 0; i < expectedCount; i++) {
            try {
                String ack = in.readLine();
                if (ack != null) {
                    acks.add(ack);
                    System.out.println("Received: " + ack);
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout waiting for ACK");
                break;
            }
        }
        
        socket.setSoTimeout(0);
        return acks;
    }
    
    private boolean processACKs(List<String> acks) {
        // Persist duplicate ACK counts across rounds.
        boolean fastRetransmitTriggered = false;

        for (String ack : acks) {
            if (ack == null || ack.equals("ACK:NA")) continue;

            // If this ACK acknowledges the packet we just retransmitted,
            // treat it as a fresh ACK and clear duplicate counting state.
            if (lastRetransmittedPacket != null && ack.equals("ACK:" + lastRetransmittedPacket)) {
                System.out.println("Received ACK for retransmitted packet: " + ack);
                dupACKcount = 0;
                lastACK = ack;
                lastRetransmittedPacket = null;
                // continue processing other ACKs in the batch
                continue;
            }

            if (ack.equals(lastACK)) {
                dupACKcount++;
            } else {
                lastACK = ack;
                dupACKcount = 0;
            }

            if (dupACKcount >= 3) {
                System.out.println("==> 3 Duplicate ACKs: Fast Retransmit triggered for " + ack);
                ssthresh = Math.max(cwnd / 2, 1);

                String missingPkt = ack.replace("ACK:", "");
                System.out.println("Scheduling retransmit of lost packet: " + missingPkt);
                // schedule a retransmit to be sent before sending new packets next round
                pendingRetransmit = missingPkt;

                if ("RENO".equals(mode)) {
                    cwnd = ssthresh;
                    System.out.println("TCP RENO Fast Recovery: cwnd -> " + cwnd);
                } else {
                    cwnd = 1;
                    System.out.println("TCP TAHOE Reset: cwnd -> " + cwnd);
                }

                // keep lastACK/dupACKcount until retransmit is handled
                fastRetransmitTriggered = true;
                break;
            }
        }

        return fastRetransmitTriggered;
    }

    
    private void updateCWnd(boolean fastRetransmitTriggered) {
        if (!fastRetransmitTriggered) {
            if (cwnd < ssthresh) {
                cwnd *= 2;
                System.out.println("Slow Start: cwnd -> " + cwnd);
            } else {
                cwnd += 1;
                System.out.println("Congestion Avoidance: cwnd -> " + cwnd);
            }
        }
    }
    
    public void runSimulation(int rounds) throws IOException, InterruptedException {
        System.out.println("Starting TCP " + mode + " simulation for " + rounds + " rounds");
        
        for (int roundNum = 1; roundNum <= rounds; roundNum++) {
            System.out.println("\nRound " + roundNum + ": cwnd = " + cwnd + ", ssthresh = " + ssthresh);
            // If there's a pending retransmission scheduled by fast retransmit,
            // send that first and skip sending a new burst this round.
            if (pendingRetransmit != null) {
                String toRetransmit = pendingRetransmit;
                pendingRetransmit = null;
                // record what we retransmitted so ACK for it can clear dup-ACK state
                lastRetransmittedPacket = toRetransmit;
                System.out.println("Retransmitting pending packet: " + toRetransmit);
                out.println(toRetransmit);
                out.flush();

                List<String> acks = receiveACKs(1);
                boolean fastRetransmit = processACKs(acks);
                updateCWnd(fastRetransmit);
                Thread.sleep(1000);
                continue;
            }

            int numPacketsToSend = Math.min(cwnd, 20);
            List<String> packets = sendPackets(numPacketsToSend);
            String packetStr = String.join(",", packets);
            System.out.println("Sent packets: " + packetStr);

            out.println(packetStr);

            List<String> acks = receiveACKs(packets.size());

            boolean fastRetransmit = processACKs(acks);

            updateCWnd(fastRetransmit);

            Thread.sleep(1000);
        }
        
        out.println("EXIT");
        socket.close();
        System.out.println("\nSimulation completed");
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== TCP Congestion Control Simulator ===");
        System.out.println("Choose TCP mode:");
        System.out.println("1. TCP Tahoe");
        System.out.println("2. TCP Reno");
        System.out.print("Enter your choice (1 or 2): ");
        
        int choice = scanner.nextInt();
        TCPSender sender = new TCPSender();
        
        if (choice == 1) {
            sender.setMode("TAHOE");
            System.out.println("\n=== TCP Tahoe Mode Selected ===");
        } else if (choice == 2) {
            sender.setMode("RENO");
            System.out.println("\n=== TCP Reno Mode Selected ===");
        } else {
            System.out.println("Invalid choice! Defaulting to TCP Tahoe.");
            sender.setMode("TAHOE");
        }
        
        try {
            System.out.print("Enter number of rounds: ");
            int rounds = scanner.nextInt();
            
            sender.connect("localhost", 5000);
            sender.runSimulation(rounds);
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Make sure TCPReceiver is running on port 5000");
        } finally {
            scanner.close();
        }
    }
}