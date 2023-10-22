import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static ServerSocket server_tcp = null;
    private static DatagramSocket server_udp = null;
    private static ArrayList<String> server_files = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String protocol = args[1].toString();
        System.out.println("Hello Server" + port + protocol);

        try {
            server_tcp = new ServerSocket(port);

            while (true) {
                Socket client_socket = server_tcp.accept();
                PrintWriter out = new PrintWriter(client_socket.getOutputStream(), true);

                BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
                String command = in.readLine();
                System.out.println(" command ::  " + command);
                if (command != null) {
                    if (protocol.equals("tcp")) {
                        if (command.startsWith("get")) {
                            String file_name = command.split(" ")[1];
                            if (server_files.indexOf(file_name) != -1) {
                                out.println("File Deliver from Origin");
                            } else {
                                server_files.add(file_name);
                                out.println("File Not Found in origin");
                            }
                        } else if (command.startsWith("put")) {

                        } else {
                            System.out.println("From server: Invalid command");
                        }
                    } else if (protocol.equals("snw")) {
                        server_udp = new DatagramSocket(port);
                        // DatagramPacket udp_packet = new DatagramPacket(new byte[1024], 1024);
                        // DatagramSocket udp_out = (udp_packet != null) ? new DatagramSocket() : null;
                        // InetAddress addr = (udp_packet != null) ? udp_packet.getAddress() : null;
                        // int clientPort = (udp_packet != null) ? udp_packet.getPort() : -1;

                        // System.out.println("From server: addr: " + addr);
                        // System.out.println("From server: clientPort: " + clientPort);
                    } else {
                        System.out.println("From the Server Server");
                        System.out.println("Invalid protocol");
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
