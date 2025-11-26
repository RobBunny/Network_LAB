import java.io.*;
import java.net.*;
import java.util.*;

public class TCPServer_35_39 {
    private static final int PORT=5000;
    private static final double LOSS_PROBABILITY=0.15;
    private static final double TIMEOUT_PROBABILITY=0.15;
    
    public void start() {
        try (ServerSocket serverSocket =new ServerSocket(PORT)) {
            System.out.println("The server started on port "+PORT);
            
            while (true) {
                try (Socket clientSocket=serverSocket.accept();
                     BufferedReader in=new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out=new PrintWriter(clientSocket.getOutputStream(), true)) {
                    
                    System.out.println("Client connected: "+clientSocket.getInetAddress());
                    
                    String mode = in.readLine();
                    System.out.println("Mode: TCP "+mode);
                    
                    String packetLine;
                    while ((packetLine=in.readLine())!=null){
                        String[] packets=packetLine.split(",");
                        
                        if (packets.length==0) continue;

                        boolean simulateTimeout=Math.random() < TIMEOUT_PROBABILITY;
                        
                        if (simulateTimeout){
                            System.out.println("[Server] Simulating TIMEOUT - not sending any ACKs");
                            continue;
                        }
                        
                        int lossIndex=-1;
                      
                        
                        for (int i=0;i<packets.length;i++) {
                            String packet=packets[i].trim();
                            
                            if(i==lossIndex){
                                if(i==0){
                                    for(int j=0;j<3;j++){
                                        out.println("ACK:NA");
                                    }
                                } else{
                                    String prevPacket = packets[i - 1].trim();
                                    for (int j=0;j<3;j++) {
                                        out.println("ACK:"+ prevPacket);
                                    }
                                }
                            } else{
                                out.println("ACK:"+packet);
                            }
                        }
                    }
                    System.out.println("Client disconnected.");
                    
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
            
        } catch (IOException e){
            System.err.println("Server error: "+e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== TCP Tahoe/Reno Server ===");
        System.out.println("Loss Probability: " + (LOSS_PROBABILITY * 100) + "%");
        System.out.println("Timeout Probability: " + (TIMEOUT_PROBABILITY * 100) + "%");
        TCPServer_35_39 server = new TCPServer_35_39();
        server.start();
    }
}