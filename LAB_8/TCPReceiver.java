import java.io.*;
import java.net.*;
import java.util.*;

public class TCPReceiver {
    private ServerSocket serverSocket;
    private int port;

    public TCPReceiver(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Receiver started on port " + port);
            System.out.println("Waiting for sender to connect...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Random random;

    private int lastInOrderSeq = -1;
    private Set<Integer> receivedPackets = new HashSet<>();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String clientMessage;
            int round = 0;

            while ((clientMessage = in.readLine()) != null) {
                if (clientMessage.equals("EXIT")) break;

                round++;
                System.out.println("\n--- Round " + round + " ---");
                System.out.println("Received packets: " + clientMessage);

                String[] packets = clientMessage.split(",");

                // Choose random packet to lose (25% chance)
                int lossIndex = -1;
                if (random.nextDouble() < 0.25 && packets.length > 0) {
                    lossIndex = random.nextInt(packets.length);
                    System.out.println("Simulating LOSS of " + packets[lossIndex]);
                }

                for (int i = 0; i < packets.length; i++) {
                    int seq = Integer.parseInt(packets[i].substring(3));

                    if (i == lossIndex) {
                        // LOST packet → send DUPLICATE ACKs
                        String missingAck = "ACK:pkt" + (lastInOrderSeq + 1);
                        System.out.println("Lost pkt" + seq + " → Sending DUP ACK for missing: " + missingAck);
                        out.println(missingAck);
                        continue;
                    }

                    // Mark packet as received
                    receivedPackets.add(seq);

                    // Update cumulative ACK pointer
                    while (receivedPackets.contains(lastInOrderSeq + 1)) {
                        lastInOrderSeq++;
                    }

                    String ackToSend = "ACK:pkt" + (lastInOrderSeq + 1);
                    System.out.println("Sending: " + ackToSend);
                    out.println(ackToSend);
                }
            }

            clientSocket.close();
            System.out.println("Sender disconnected.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


    public static void main(String[] args) {
        new TCPReceiver(5000).start();
    }
}
