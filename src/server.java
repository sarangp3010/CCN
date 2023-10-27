import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static ServerSocket server_tcp = null;
    private static DatagramSocket server_udp = null;
    private static ArrayList<String> server_files = new ArrayList<>();
    private static String dir = System.getProperty("user.dir");

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
                    Socket cache_client_socket = server_tcp.accept();
                    String ip = cache_client_socket.getInetAddress().getHostAddress();
                    int p = cache_client_socket.getPort();
                    System.out.println("Client connected: " + cache_client_socket.getInetAddress().getHostAddress());

                    BufferedReader in = new BufferedReader(new InputStreamReader(cache_client_socket.getInputStream()));
                    String command = in.readLine();
                    System.out.println(" command ::  " + command);
                    if (command != null) {
                        String file_name = command.split(" ")[1];
                        if (command.startsWith("get")) {
                            server_files.clear();
                            tcp_transport.getAllFiles(server_files, "server_files");
                            if (server_files.indexOf(file_name) != -1) {
                                tcp_transport.sendFile(cache_client_socket, dir + "/server_files/" + file_name);
                            } else {
                                tcp_transport.send_command(cache_client_socket, "File Not Found in origin", null, -1);
                            }
                        } else if (command.startsWith("put")) {
                            tcp_transport.send_command(cache_client_socket, "File Successfully uploaded", null, -1);
                            tcp_transport.receiveFile(cache_client_socket, dir + "/server_files/" + file_name);
                        } else {
                            System.out.println("From server: Invalid command");
                        }
                    }
                    cache_client_socket.close();
                    in.close();
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
