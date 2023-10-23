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
            server_udp = new DatagramSocket(port);
            while (true) {
                if (protocol.equals("tcp")) {
                    Socket client_socket = server_tcp.accept();
                    System.out.println("Client connected: " + client_socket.getInetAddress().getHostAddress());

                    BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
                    String command = in.readLine();
                    System.out.println(" command ::  " + command);
                    if (command != null) {
                        String file_name = command.split(" ")[1];
                        if (command.startsWith("get")) {
                            if (server_files.indexOf(file_name) != -1) {
                                tcp_transport.send_command(client_socket, "File Deliver from Origin");
                            } else {
                                server_files.add(file_name);
                                tcp_transport.send_command(client_socket, "File Not Found in origin");
                            }
                        } else if (command.startsWith("put")) {
                            String dir = System.getProperty("user.dir");
                            System.out.println("Dir: " + dir);
                            tcp_transport.receiveFile(client_socket, "1" + file_name);
                            System.out.println("Here1");
                            server_files.add(file_name);
                            System.out.println("Here2");
                            tcp_transport.send_command(client_socket, "File Successfully uploaded");
                        } else {
                            System.out.println("From server: Invalid command");
                        }
                    }
                } else if (protocol.equals("snw")) {

                    byte[] receiveData = new byte[1024];
                    DatagramPacket server_receive_udp_packet = new DatagramPacket(receiveData, receiveData.length);
                    server_udp.receive(server_receive_udp_packet);
                    String command = new String(server_receive_udp_packet.getData(), 0,
                            server_receive_udp_packet.getLength());
                    System.out.println("receive at server: " + command);
                    if (command.startsWith("get")) {
                        String file_name = command.split(" ")[1];
                        System.out.println("Filename: " + file_name);
                        InetAddress cache_addr = server_receive_udp_packet.getAddress();
                        int cache_port = server_receive_udp_packet.getPort();
                        System.out.println("Cache data : address: \n\n" + cache_addr + " port : " + cache_port);
                        if (server_files.indexOf(file_name) != -1) {
                            byte[] sendData = "File Deliver from Origin".getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, cache_addr,
                                    cache_port);
                            server_udp.send(sendPacket);
                        } else {
                            server_files.add(file_name);
                            byte[] sendData = "File Not Found in origin".getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, cache_addr,
                                    cache_port);
                            server_udp.send(sendPacket);
                        }
                    } else if (command.startsWith("put")) {

                    } else {
                        System.out.println("From server: Invalid command");
                    }
                } else {
                    System.out.println("From the Server Server");
                    System.out.println("Invalid protocol");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
