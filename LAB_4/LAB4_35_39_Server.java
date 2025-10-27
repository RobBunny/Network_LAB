import java.io.*;
import java.net.*;

class ClientHandler extends Thread {
    private Socket socket;
    private DataOutputStream dos;

    ClientHandler(Socket socket, DataOutputStream dos) {
        this.socket = socket;
        this.dos = dos;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            File folder = new File("Upload_files");
            if (!folder.exists()) {
                folder.mkdir();
            }

            File[] files = folder.listFiles();
            writer.write("\nAvailable files on server:\n");
            if (files != null && files.length > 0) {
                for (File f : files) {
                    if (f.isFile()) {
                        writer.write(" - " + f.getName() + " (" + f.length() + " bytes)\n");
                    }
                }
            } else {
                writer.write(" (No files available)\n");
            }
            writer.write("\nEnter the file name you want to download (or type 'exit' to quit):\n");
            writer.flush();

            while (true) {
                String fileName = reader.readLine();
                if (fileName == null) break;
                if (fileName.equalsIgnoreCase("exit")) {
                    System.out.println("Client disconnected.");
                    break;
                }

                System.out.println("Client requested file: " + fileName);
                File file = new File(folder, fileName);

                if (file.exists() && file.isFile()) {
                    writer.write("FOUND\n");
                    writer.flush();

                    writer.write("FILE_INFO:" + fileName + ":" + file.length() + "\n");
                    writer.flush();

                    long fileSize = file.length();
                    dos.writeLong(fileSize);

                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                    }
                    fis.close();

                    System.out.println("File " + fileName + " (" + fileSize + " bytes) sent to client.");
                } else {
                    writer.write("NOT_FOUND\n");
                    writer.flush();
                    System.out.println("File " + fileName + " not found.");
                }

                writer.write("\nEnter another file name (or type 'exit' to quit):\n");
                writer.flush();
            }

            reader.close();
            writer.close();
            dos.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class LAB4_35_39_Server {
    public static void main(String[] args) {
        int port = 5000;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("File server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                new ClientHandler(socket, dos).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
