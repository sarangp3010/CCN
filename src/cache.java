import java.io.*;
import java.net.*;
import java.util.*;

public class cache {
    private static ServerSocket cache_tcp = null;
    private static DatagramSocket cache_udp = null;
    private static ArrayList<String> cache_files = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String server_ip = args[1].toString();
        int server_port = Integer.parseInt(args[2]);
        String protocol = args[3].toString();

        System.out.println("port: " + port);
        System.out.println("protocol" + protocol);
        try {
            cache_tcp = new ServerSocket(port);
            cache_udp = new DatagramSocket(port);
            while (true) {
                try {
                    if (protocol.equals("tcp")) {
                        Socket serverClientSocket = cache_tcp.accept();
                        PrintWriter serverOut = (serverClientSocket != null)
                                ? new PrintWriter(serverClientSocket.getOutputStream(), true)
                                : null;
                        BufferedReader serverIn = (serverClientSocket != null)
                                ? new BufferedReader(new InputStreamReader(serverClientSocket.getInputStream()))
                                : null;

                        Socket server_tcp = new Socket(server_ip, server_port);
                        PrintWriter server_out = new PrintWriter(server_tcp.getOutputStream(), true);

                        BufferedReader server_in = new BufferedReader(
                                new InputStreamReader(server_tcp.getInputStream()));

                        String command = (serverIn != null) ? serverIn.readLine() : null;
                        if (command.startsWith("get")) {
                            String file_name = command.split(" ")[1];
                            if (cache_files.indexOf(file_name) != -1) {
                                serverOut.println("File Delivered from cache");
                            } else {
                                System.out.println("inner 1" + command);
                                try {
                                    server_out.println(command);
                                    cache_files.add(file_name);
                                    String server_response = server_in.readLine();
                                    serverOut.println(server_response);
                                } catch (Exception e) {
                                    System.out.println("err" + e);
                                }
                            }
                        }
                        serverIn.close();
                        server_in.close();
                    } else if (protocol.equals("snw")) {
                        byte[] cache_received_data = new byte[1024];
                        DatagramPacket cache_receive_udp_packet = new DatagramPacket(cache_received_data, cache_received_data.length);
                        cache_udp.receive(cache_receive_udp_packet);

                        String command = new String(cache_receive_udp_packet.getData(), 0,
                            cache_receive_udp_packet.getLength());
                        
                        System.out.println("Cache Received from cleint: " + command);

                        String file_name = command.split(" ")[1];
                        if (cache_files.indexOf(file_name) != -1) {

                        } else {

                        }

                        InetAddress client_addr = cache_receive_udp_packet.getAddress();
                        int client_port = cache_receive_udp_packet.getPort();

                        System.out.println("Client data : address: \n\n" + client_addr + " port : " + client_port);

                        // DatagramSocket server_udp = new DatagramSocket();
                        byte[] sendData = command.getBytes();
                        DatagramPacket server_udp_packets = new DatagramPacket(sendData, sendData.length,
                                InetAddress.getByName(server_ip), server_port);
                        cache_udp.send(server_udp_packets);
                        System.out.println("Called here");
// ------------------------ stop here ----------------------------
                        byte[] client_receive_data = new byte[1024];
                        DatagramPacket client_receive_packet = new DatagramPacket(client_receive_data, client_receive_data.length);
                        cache_udp.receive(client_receive_packet);
                        System.out.println("client_receive_data from server in cache: " + new String(client_receive_packet.getData(), 0, client_receive_packet.getLength()));

                        byte[] client_send_data = client_receive_data;
                        DatagramPacket client_send_packet = new DatagramPacket(client_send_data, client_send_data.length, client_addr, client_port);
                        cache_udp.send(client_send_packet);

                        String receivedString = new String(client_send_packet.getData(), 0, client_send_packet.getLength());
                        System.out.println("Cache Received from server: " + receivedString);
                    } else {
                        System.out.println("From the Cache Server");
                        System.out.println("Invalid protocol");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {

        }
    }
}
