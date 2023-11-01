import java.io.*;
import java.net.*;
import java.util.*;

public class tcp_transport {
    public static void send_command(Socket socket, String command, String ip, int port) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String receive_command(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = in.readLine();
            return msg;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendFile(Socket socket, String path) {
        try {
            File file = new File(path);
            FileInputStream fIn = new FileInputStream(file);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            long file_size = 0;
            if (file.isFile()) {
                file_size = file.length();
            }
            byte[] buffer = new byte[(int) file_size];
            int bytesRead = 0;
            while ((bytesRead = fIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            fIn.close();
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void receiveFile(Socket socket, String path) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            FileOutputStream fOut = new FileOutputStream(path);

            int bytesRead = 0;

            byte[] buffer = new byte[1024];

            do {
                fOut.write(buffer, 0, bytesRead);
            } while ((bytesRead = in.read(buffer)) != -1);

            fOut.close();
            socket.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getAllFiles(ArrayList<String> al, String directory) {
        File dir = new File(System.getProperty("user.dir") + "/" + directory);
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                String fileName = files[i].getName();
                al.add(fileName);
            }
        }
    }
}
