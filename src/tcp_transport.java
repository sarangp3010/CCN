import java.io.*;
import java.net.*;

public class tcp_transport {
    public static void send_command(Socket socket, String command) {
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
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendFile(Socket socket, String path, String destination) {
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
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void receiveFile(Socket socket, String path, String source) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            FileOutputStream fOut = new FileOutputStream(path);

            int bytesRead = 0;

            byte[] buffer = new byte[1024];
            File f = new File(source);
            long file_size = f.length();
            while (file_size > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, file_size))) != -1) {
                fOut.write(buffer, 0, bytesRead);
                file_size -= bytesRead;
            }

            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
