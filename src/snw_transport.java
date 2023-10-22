import java.io.*;
import java.net.*;

public class snw_transport {
    public static void send_command(DatagramSocket socket, InetAddress addr, int port, String command) {
        try {
            byte[] data = command.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String receive_command(DatagramSocket socket) {
        try {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            return new String(packet.getData(), 0, packet.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendFile(DatagramSocket socket, InetAddress addr, int port, String path) {
        try {
            File file = new File(path);
            FileInputStream fIn = new FileInputStream(file);
            DatagramPacket send;
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fIn.read(buffer)) != -1) {
                send = new DatagramPacket(buffer, bytesRead, addr, port);
                socket.send(send);
            }

            send = new DatagramPacket(new byte[0], 0, addr, port);
            socket.send(send);

            fIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void receiveFile(DatagramSocket socket, String path) {
        try {
            FileOutputStream fIn = new FileOutputStream(path);
            DatagramPacket receive;
            byte[] buffer = new byte[1024];

            while (true) {
                receive = new DatagramPacket(buffer, buffer.length);
                socket.receive(receive);

                if (receive.getLength() == 0) {
                    break;
                }

                fIn.write(buffer, 0, receive.getLength());
            }

            fIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
