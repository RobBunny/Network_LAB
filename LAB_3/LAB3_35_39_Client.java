import java.io.*;
import java.net.*;

public class LAB3_35_39_Client {
    Socket socket;
    DataInputStream dis;
    DataOutputStream dos;
    BufferedReader reader;

    public LAB3_35_39_Client() {
        try {
            socket = new Socket("localhost", 5000);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(System.in));
            startClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void startClient() {
        try {
            while (true) {
                String serverMessage = dis.readUTF();
                System.out.println(serverMessage);

                String userInput = reader.readLine();
                dos.writeUTF(userInput);

                if (userInput.equalsIgnoreCase("EXIT")) {
                    System.out.println("Session ended.");
                    break;
                }

                String response = dis.readUTF();
                System.out.println(response);
            }

            dis.close();
            dos.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new LAB3_35_39_Client();
    }
}
