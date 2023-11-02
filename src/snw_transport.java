import java.io.*;
import java.net.*;
import java.util.*;

public class snw_transport {
    /**
     * 
     * @param socket
     * @param addr
     * @param port
     * @param command
     * 
     *                This method is used when snd the command through the Datagram
     *                socket
     *                Commands like ACK and LEN and FIN messages.
     */
    public static void send_command(DatagramSocket socket, InetAddress addr, int port, String command) {
        try {
            // it simply create the bytes array of the msg and then create teh datagram
            // packet(UDP) using ip and port.
            byte[] data = command.getBytes();

            DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
            socket.send(packet);
        } catch (Exception e) {
        }
    }

    /**
     * 
     * @param socket
     * @param addr
     * @param port
     * 
     *         This method receive command packets and return it as a string.
     */
    public static String receive_command(DatagramSocket socket, InetAddress addr, int port) {
        try {
            // create the data buffer to store the recived packets.
            byte[] data = new byte[1024];
            DatagramPacket packet;

            // Her eI faced the issue so that when the addr and port is there I create it as
            // a send packet or else it'll be works as a receiveing packet.
            if (addr == null && port == -1) {
                packet = new DatagramPacket(data, data.length);
            } else {
                packet = new DatagramPacket(data, data.length, addr, port);
            }

            // receive packet
            socket.receive(packet);
            // return it as a string.
            return new String(packet.getData(), 0, packet.getLength());
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 
     * @param path
     * @return
     * 
     *         This will create file and return it length from the given path
     */
    public static long get_file_length(String path) {
        File file = new File(path);
        long len = 0;
        if (file.isFile() && file.isFile()) {
            len = file.length();
        }
        return len;
    }

    /**
     * 
     * @param file
     * @param chunk_size
     * @return
     * 
     *         Frm the file it create the chunks of bytes and return it as a
     *         arraylist.
     */
    public static ArrayList<byte[]> create_chunk(File file, int chunk_size) {
        ArrayList<byte[]> chunks = new ArrayList<>();
        FileInputStream fIn;
        try {
            fIn = new FileInputStream(file);
            byte[] buffer = new byte[chunk_size];

            while (fIn.read(buffer) != -1) {
                chunks.add(buffer);
            }
            fIn.close();
        } catch (Exception e) {
        }
        return chunks;
    }

    /**
     * 
     * @param chunks
     * @param file
     * 
     *               Write the arraylist chunks to the file.
     */
    public static void write_file(ArrayList<byte[]> chunks, File file) {
        try {
            FileOutputStream fOut = new FileOutputStream(file);

            for (int i = 0; i < chunks.size(); i++) {
                byte[] buf = chunks.get(i);
                fOut.write(buf, 0, buf.length);
                fOut.flush();
            }
            fOut.close();
        } catch (Exception e) {
        }
    }
}
