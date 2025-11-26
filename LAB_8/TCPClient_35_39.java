import java.io.*;
import java.net.*;
import java.util.*;

public class TCPClient_35_39 {
    private static final String SERVER_HOST="10.33.2.198";
    private static final int SERVER_PORT=5000;
    private static final int MAX_ROUNDS=15;
    private static final int TIMEOUT_MS=2000;
    
    private int cwnd=1;
    private int ssthresh=8;
    private int dupACKcount=0;
    private String lastACK ="";
    private String mode;
    private int packetCounter=0;
    
    public TCPClient_35_39(String mode) {
        this.mode =mode.toUpperCase();
    }
    
    public void start(){
        try (Socket socket=new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out=new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            socket.setSoTimeout(TIMEOUT_MS);
            
            System.out.println("== TCP "+mode +" Mode ==");
            
            out.println(mode);
            
            for (int round=1;round<=MAX_ROUNDS;round++){
                System.out.println("\nRound "+round+": cwnd = " +cwnd+", ssthresh = "+ssthresh);
                
                List<String> packets=new ArrayList<>();
                for (int i=0;i<cwnd;i++){
                    packets.add("pkt"+packetCounter);
                    packetCounter++;
                }

                System.out.println("Sent packets: "+String.join(", ",packets));
                out.println(String.join(",",packets));
                
                boolean fastRetransmitTriggered=false;
                boolean timeoutOccurred=false;
                dupACKcount =0;
                lastACK="";
                
                for(int i=0;i<packets.size();i++){
                    try {
                        String ack=in.readLine();
                        System.out.println("Received: "+ack);
                        
                        if (ack.equals(lastACK)) {
                            dupACKcount++;
                            
                            if(dupACKcount==3){
                                System.out.println("==> 3 Duplicate ACKs: Fast Retransmit triggered.");
                                fastRetransmitTriggered=true;
                                
                                ssthresh=Math.max(cwnd/2,1);
                                
                                if (mode.equals("RENO")) {
                                    cwnd=ssthresh;
                                    System.out.println("TCP RENO Fast Recovery: cwnd -> " + cwnd);
                                } else { 
                                    cwnd=1;
                                    System.out.println("TCP TAHOE Reset: cwnd -> " + cwnd);
                                }
                                
                                dupACKcount=0;
                                break;
                            }
                        } else {
                            dupACKcount=1;
                            lastACK=ack;
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("==> TIMEOUT: No ACK received within " + TIMEOUT_MS + "ms");
                        timeoutOccurred=true;
                        
                        ssthresh=Math.max(cwnd/2,1);
                        cwnd=1;
                        System.out.println("Timeout Recovery(Both TAHOE & RENO): ssthresh -> " + ssthresh + ", cwnd -> " + cwnd);
                        
                        break;
                    }
                }
                
                if (!fastRetransmitTriggered && !timeoutOccurred) {
                    if(cwnd<ssthresh){
                        cwnd=cwnd*2;
                        System.out.println("Slow Start: cwnd -> " + cwnd);
                    }else{
                        cwnd=cwnd+1;
                        System.out.println("Congestion Avoidance: cwnd -> " + cwnd);
                    }
                }
            }
            
            System.out.println("\nClient finished.");
            
        } catch (IOException e){
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Select TCP Mode:");
        System.out.println("1. TAHOE");
        System.out.println("2. RENO");
        System.out.print("Enter choice (1 or 2): ");
        
        int choice=scanner.nextInt();
        String mode=(choice==1)?"TAHOE":"RENO";
        
        TCPClient_35_39 client=new TCPClient_35_39(mode);
        client.start();
        
        scanner.close();
    }
}