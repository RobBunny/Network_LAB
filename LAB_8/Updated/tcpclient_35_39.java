import java.io.*;
import java.net.*;
import java.util.*;

public class tcpclient_35_39 {

    private static final double ALPHA = 0.125;
    private static final double BETA = 0.25;

    private static double estimatedRTT = 100.0;
    private static double devRTT = 25.0;
    private static double timeoutInterval = 200.0;

    public static void main(String args[]) {
        try {
            Socket sck = new Socket("localhost", 1992);
            BufferedReader in = new BufferedReader(new InputStreamReader(sck.getInputStream()));
            PrintWriter out = new PrintWriter(sck.getOutputStream(), true);
            Scanner sc = new Scanner(System.in);

            System.out.println("Select TCP mode (TAHOE or RENO): ");
            String mode = sc.next().toUpperCase();

            System.out.println("Enter number of rounds: ");
            int N = sc.nextInt();

            out.println(N);
            out.println(mode);

            int timeoutRound = Integer.parseInt(in.readLine());
            int lossRound = Integer.parseInt(in.readLine());

            int cwnd = 1;
            int ssthresh = 8;
            int nextPkt = 1;

            System.out.println("\n============ TCP " + mode + " Mode ============");
            System.out.println("Initial EstimatedRTT: " + estimatedRTT + "ms");
            System.out.println("Initial DevRTT: " + devRTT + "ms");
            System.out.println("Initial TimeoutInterval: " + timeoutInterval + "ms");
            // System.out.println("Timeout Round: " + timeoutRound + ", Loss Round: " +
            // lossRound);

            String lastACK = "";
            int dupACKcount = 0;

            int round = 1;
            while (round <= N) {
                System.out.println("\n=============== Round " + round + " ===============");
                System.out.println("cwnd = " + cwnd + ", ssthresh = " + ssthresh);
                System.out.println("TimeoutInterval = " + String.format("%.2f", timeoutInterval) + "ms");

                sck.setSoTimeout((int) Math.max(Math.ceil(timeoutInterval), 100));

                List<String> packets = new ArrayList<>();
                int startPkt = nextPkt;
                for (int i = 0; i < cwnd; i++) {
                    packets.add("pkt" + nextPkt);
                    nextPkt++;
                }

                out.println(round);
                out.println(String.join(",", packets));
                System.out.println("Sent packets: " + String.join(", ", packets));

                long sendTime = System.currentTimeMillis();

                boolean fastRetransmit = false;
                boolean timeoutOccurred = false;
                List<String> receivedAcks = new ArrayList<>();

                try {
                    while (true) {
                        String ack = in.readLine();
                        if (ack == null || ack.equals("END"))
                            break;
                        if (ack.equals("TIMEOUT_SIM")) {
                            timeoutOccurred = true;
                            System.out.println("\n*** TIMEOUT SIMULATION from Server ***");
                            in.readLine();
                            break;
                        }
                        receivedAcks.add(ack);
                    }
                } catch (SocketTimeoutException e) {
                    timeoutOccurred = true;
                    System.out.println("\n*** TIMEOUT: No response within " +
                            String.format("%.2f", timeoutInterval) + "ms ***");

                    sck.setSoTimeout(50);
                    try {
                        while (in.ready())
                            in.readLine();
                    } catch (Exception ignored) {
                    }
                }

                long endTime = System.currentTimeMillis();
                long sampleRTT = endTime - sendTime;

                System.out.println("Received ACKs: " + receivedAcks);
                if (!receivedAcks.isEmpty()) {
                    System.out.println("Sample RTT: " + sampleRTT + "ms");
                }

                if (timeoutOccurred) {
                    ssthresh = Math.max(cwnd / 2, 1);
                    cwnd = 1;
                    nextPkt = startPkt;
                    dupACKcount = 0;
                    lastACK = "";
                    timeoutInterval = timeoutInterval * 2;
                    System.out.println("Timeout Action: ssthresh -> " + ssthresh + ", cwnd -> " + cwnd);
                    System.out.println("Exponential Backoff: TimeoutInterval -> " +
                            String.format("%.2f", timeoutInterval) + "ms");
                    System.out.println("Will retransmit from pkt" + startPkt);
                    round++;
                    continue;
                }

                if (!receivedAcks.isEmpty()) {
                    updateRTT(sampleRTT);
                    System.out.println("RTT Updated -> EstimatedRTT: " +
                            String.format("%.2f", estimatedRTT) + "ms, DevRTT: " +
                            String.format("%.2f", devRTT) + "ms, Timeout: " +
                            String.format("%.2f", timeoutInterval) + "ms");
                }

                dupACKcount = 0;
                for (int i = 0; i < receivedAcks.size(); i++) {
                    String pkt = receivedAcks.get(i).replace("ACK:", "");

                    if (pkt.equals(lastACK)) {
                        dupACKcount++;
                        System.out.println("Duplicate ACK #" + dupACKcount + " for " + pkt);

                        if (dupACKcount == 3) {
                            System.out.println("\n*** 3 DUPLICATE ACKs: Fast Retransmit ***");
                            fastRetransmit = true;
                            ssthresh = Math.max(cwnd / 2, 1);

                            if (mode.equals("RENO")) {
                                cwnd = ssthresh;
                                System.out.println("TCP RENO Fast Recovery: cwnd -> " + cwnd);
                            } else {
                                cwnd = 1;
                                System.out.println("TCP TAHOE Reset: cwnd -> " + cwnd);
                            }

                            int lostPktNum = Integer.parseInt(lastACK.replace("pkt", "")) + 1;
                            nextPkt = lostPktNum;
                            System.out.println("Will retransmit from pkt" + lostPktNum);
                            dupACKcount = 0;
                            break;
                        }
                    } else {
                        lastACK = pkt;
                        dupACKcount = 1;
                    }
                }

                if (!fastRetransmit && !timeoutOccurred) {
                    if (cwnd < ssthresh) {
                        cwnd = cwnd * 2;
                        System.out.println("Slow Start: cwnd -> " + cwnd);
                    } else {
                        cwnd = cwnd + 1;
                        System.out.println("Congestion Avoidance: cwnd -> " + cwnd);
                    }
                }

                round++;
            }

            sck.close();
            sc.close();
            System.out.println("\n============ Client Disconnected ============");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateRTT(long sampleRTT) {

        estimatedRTT = (1 - ALPHA) * estimatedRTT + ALPHA * sampleRTT;
        devRTT = (1 - BETA) * devRTT + BETA * Math.abs(sampleRTT - estimatedRTT);
        timeoutInterval = estimatedRTT + 4 * devRTT;

        timeoutInterval = Math.max(timeoutInterval, 100.0);
    }
}