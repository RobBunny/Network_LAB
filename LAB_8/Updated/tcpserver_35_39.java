import java.io.*;
import java.net.*;
import java.util.*;

public class tcpserver_35_39 {

    public static void main(String[] args) {
        try {
            ServerSocket ss = new ServerSocket(1992);
            System.out.println("The server started on port 1992");
            Socket s = ss.accept();
            System.out.println("Client connected: " + s.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            Random rand = new Random();

            int N = Integer.parseInt(in.readLine());
            String mode = in.readLine();

            int timeoutRound = rand.nextInt(N - 2) + 3;
            int lossRound;
            do {
                lossRound = rand.nextInt(N - 2) + 3;
            } while (lossRound == timeoutRound);

            System.out.println("\nServer ready for " + N + " rounds. Mode: TCP " + mode);
            // System.out.println("Timeout will occur in Round: " + timeoutRound);
            // System.out.println("Packet loss (3 dup ACK) will occur in Round: " +
            // lossRound);

            out.println(timeoutRound);
            out.println(lossRound);

            String roundLine;
            while ((roundLine = in.readLine()) != null) {
                String packetsLine = in.readLine();
                if (packetsLine == null)
                    break;

                int roundNum = Integer.parseInt(roundLine);
                String[] packets = packetsLine.split(",");
                System.out.println("\n===== Round " + roundNum + " =====");
                System.out.println("Received packets: " + Arrays.toString(packets));

                if (roundNum == timeoutRound) {
                    System.out.println("[Server] Simulating TIMEOUT - not sending ACKs");

                    out.println("TIMEOUT_SIM");
                    out.println("END");
                    continue;
                }

                if (roundNum == lossRound && packets.length > 1) {
                    int lossIndex = rand.nextInt(packets.length - 1) + 1;
                    System.out.println("[Server] Simulating loss of packet: " + packets[lossIndex].trim());

                    for (int i = 0; i < packets.length; i++) {
                        if (i == lossIndex) {
                            String dupACK = "ACK:" + packets[i - 1].trim();

                            for (int j = 0; j < 3; j++) {
                                out.println(dupACK);
                                System.out.println("Sent " + dupACK + " (dup " + (j + 1) + ")");
                            }
                            out.println("END");
                            break;
                        } else {
                            String ack = "ACK:" + packets[i].trim();
                            out.println(ack);
                            System.out.println("Sent " + ack);
                        }
                    }
                    continue;
                }

                for (int i = 0; i < packets.length; i++) {
                    String ack = "ACK:" + packets[i].trim();
                    out.println(ack);
                    System.out.println("Sent " + ack);
                }
                out.println("END");
            }

            s.close();
            ss.close();
            System.out.println("\nServer closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}