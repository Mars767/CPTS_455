import java.net.*;
import java.io.*;

public class Server {
    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream input = null;
    private DataOutputStream output = null;
    
    public Server(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");

            System.out.println("Waiting for a client ...");
            socket = server.accept();
            System.out.println("Client accepted");

            input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            output = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try (BufferedReader serverInput = new BufferedReader(new InputStreamReader(System.in))) {
                    String messageToSend;
                    while (true) {
                        System.out.println("Enter a message or 'send file' to send a file:");
                        messageToSend = serverInput.readLine();

                        if (messageToSend.equals("send file")) {
                            sendFile();
                        } else {
                            output.writeUTF(messageToSend);
                            if (messageToSend.equalsIgnoreCase("End")) {
                                closeConnections();
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error sending message: " + e.getMessage());
                }
            }).start();
            
            String line = "";

            while (!line.equals("End")) {
                try {
                    line = input.readUTF();

                    if (line.equals("send file")) {
                        receiveFile();
                    } else {
                        System.out.println("Client: " + line);
                    }

                } catch (IOException i) {
                    System.out.println(i);
                    break;
                }
            }
            System.out.println("Closing connection");
            closeConnections();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    private void sendFile() {
        try {
            System.out.println("Enter file path:");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String filePath = reader.readLine();

            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.out.println("File not found.");
                return;
            }

            output.writeUTF("send file");
            output.writeUTF(file.getName());  
            output.writeLong(file.length()); 

            FileInputStream fileIn = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) > 0) {
                output.write(buffer, 0, bytesRead);
            }
            fileIn.close();
            System.out.println("File sent.");
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        }
    }

    private void receiveFile() {
        try {
            String fileName = input.readUTF();  
            long fileSize = input.readLong();  

            FileOutputStream fileOut = new FileOutputStream(fileName);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long remainingBytes = fileSize;

            while ((bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes))) > 0) {
                fileOut.write(buffer, 0, bytesRead);
                remainingBytes -= bytesRead;
            }
            fileOut.close();
            System.out.println("File received: " + fileName);
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    private void closeConnections() {
        try {
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();
            System.out.println("Connections closed.");
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) {
        Server server = new Server(6001);
    }
}
