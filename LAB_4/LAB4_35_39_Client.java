import java.io.*;
import java.net.*;
import java.util.*;

public class LAB4_35_39_Client {
    public static void main(String[] args) {
        String serverIP = "localhost";
        int port = 5000;

        try (Socket socket = new Socket(serverIP, port)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in);

            while (true) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("quit):")) break;
                }


                String fileName = scanner.nextLine();
                bw.write(fileName + "\n");
                bw.flush();

                if (fileName.equalsIgnoreCase("exit")) {
                    System.out.println("Disconnected from server.");
                    break;
                }

                String response = br.readLine();
                if ("FOUND".equalsIgnoreCase(response)) {
                    String fileInfo = br.readLine();
                    if (fileInfo.startsWith("FILE_INFO:")) {
                        String[] parts = fileInfo.split(":");
                        System.out.println("Server confirmed: " + parts[1] + " (" + parts[2] + " bytes)");
                    }

                    long fileSize = dis.readLong();
                    FileOutputStream fos = new FileOutputStream("downloaded_" + fileName);

                    byte[] buffer = new byte[4096];
                    long remaining = fileSize;

                    while (remaining > 0) {
                        int bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (bytesRead == -1) break;
                        fos.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }

                    fos.close();
                    System.out.println("Downloaded: " + fileName + " (" + fileSize + " bytes)");
                } else {
                    System.out.println("File not found on server.");
                }
            }

            br.close();
            bw.close();
            dis.close();
            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
