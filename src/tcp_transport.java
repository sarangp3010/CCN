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

    public static void sendFile(Socket socket, String path) {
        try {
            File file = new File(path);
            FileInputStream fIn = new FileInputStream(file);
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            fIn.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void receiveFile(Socket socket, String path) {
        try {
            InputStream in = socket.getInputStream();
            FileOutputStream fOut = new FileOutputStream(path);

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                fOut.write(buffer, 0, bytesRead);
            }

            fOut.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
