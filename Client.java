import java.io.*;
import java.net.*;

public class Client {
    private Socket socket = null;
    private BufferedReader input = null;
    private DataOutputStream out = null;
    private DataInputStream serverInput = null;
    private boolean isRunning = true; 

    public Client(String address, int port) {
        try {
            socket = new Socket(address, port);
            System.out.println("Connected");

            input = new BufferedReader(new InputStreamReader(System.in));
            out = new DataOutputStream(socket.getOutputStream());
            serverInput = new DataInputStream(socket.getInputStream());

            new Thread(() -> {
                String messageFromServer;
                try {
                    while (isRunning) {
                        messageFromServer = serverInput.readUTF();
                        if (messageFromServer.equalsIgnoreCase("End")) {
                            System.out.println("Server ended the connection.");
                            isRunning = false; 
                            break;
                        } else if (messageFromServer.equals("send file")) {
                            receiveFile();
                        } else {
                            System.out.println("Server: " + messageFromServer);
                        }
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        System.out.println("Error receiving message: " + e.getMessage());
                    }
                } finally {
                    closeConnections();
                }
            }).start();

            String line = "";
            while (isRunning && !line.equals("End")) {
                try {
                    System.out.println("Enter a message or 'send file' to send a file:");
                    line = input.readLine();

                    if (line.equals("send file")) {
                        sendFile();
                    } else {
                        out.writeUTF(line);
                        if (line.equalsIgnoreCase("End")) {
                            isRunning = false; 
                        }
                    }
                } catch (IOException i) {
                    if (isRunning) {
                        System.out.println("Error sending message: " + i.getMessage());
                    }
                    break;
                }
            }
        } catch (UnknownHostException u) {
            System.out.println("Unknown host: " + u.getMessage());
        } catch (IOException i) {
            System.out.println("Connection error: " + i.getMessage());
        } finally {
            closeConnections();
        }
    }

    private void sendFile() {
        try {
            System.out.println("Enter file path:");
            String filePath = input.readLine();

            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.out.println("File not found.");
                return;
            }

            out.writeUTF("send file");
            out.writeUTF(file.getName());  
            out.writeLong(file.length()); 

            FileInputStream fileIn = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            fileIn.close();
            System.out.println("File sent.");
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        }
    }

    private void receiveFile() {
        try {
            String fileName = serverInput.readUTF();  
            long fileSize = serverInput.readLong();  

            FileOutputStream fileOut = new FileOutputStream(fileName);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long remainingBytes = fileSize;

            while ((bytesRead = serverInput.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes))) > 0) {
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
            isRunning = false; 
            if (input != null) input.close();
            if (out != null) out.close();
            if (serverInput != null) serverInput.close();
            if (socket != null) socket.close();
            System.out.println("Connections closed.");
        } catch (IOException i) {
            System.out.println("Error closing connections: " + i.getMessage());
        }
    }

    public static void main(String args[]) {
        new Client("127.0.0.1", 6001);
    }
}
