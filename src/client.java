import java.net.*;
import java.io.*;

public class client {
    private static Socket server_tcp = null;
    private static Socket cache_tcp = null;
    private static DatagramSocket server_udp = null;
    private static DatagramSocket cache_udp = null;
    private static DatagramSocket client_udp = null;

    // public client(String ip, int port) throws Exception {
    // server_tcp = new Socket(ip, port);
    // cache_tcp = new Socket(ip, port);
    // }

    private static void close() {
        try {
            server_tcp.close();
            cache_tcp.close();
            server_udp.close();
            cache_udp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Invalid args");
            return;
        }

        String server_ip = args[0].toString();
        int server_port = Integer.parseInt(args[1]);
        String cache_ip = args[2].toString();
        int cache_port = Integer.parseInt(args[3]);
        String protocol = args[4].toString();

        System.out.println("Hello Client ");
        System.out.print(" Server Ip: " + server_ip);
        System.out.print(" server_port: " + server_port);
        System.out.print(" cache_ip: " + cache_ip);
        System.out.print(" cache_port: " + cache_port);
        System.out.print(" protocol: " + protocol);

        BufferedReader buf_reader = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        try {
            cache_tcp = new Socket(cache_ip, cache_port);
            client_udp = new DatagramSocket();
            do {
                System.out.print("Enter command: ");
                command = buf_reader.readLine();
                if (command.startsWith("put")) {
                    if (protocol.equals("tcp")) {

                    } else if (protocol.equals("snw")) {

                    } else {
                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                } else if (command.startsWith("get")) {
                    if (protocol.equals("tcp")) {
                        
                        PrintWriter out = new PrintWriter(cache_tcp.getOutputStream(), true);
                        out.println(command);

                        BufferedReader in = new BufferedReader(new InputStreamReader(cache_tcp.getInputStream()));
                        String msg = in.readLine();
                        System.out.println("msg : " + msg);
                    } else if (protocol.equals("snw")) {
                        
                        byte[] sendData;
                        sendData = command.getBytes();
                        DatagramPacket cache_udp_packets = new DatagramPacket(sendData, sendData.length,
                                InetAddress.getByName(cache_ip), cache_port);
                        client_udp.send(cache_udp_packets);

                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        client_udp.receive(receivePacket);

                        String receivedString = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println("Received from server: " + receivedString);
                    } else {
                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                }
            } while (!command.equals("quit"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
