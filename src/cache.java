import java.io.*;
import java.net.*;
import java.util.*;

public class cache {
    private static ServerSocket cache_tcp = null;
    private static DatagramSocket cache_snw = null;
    private static ArrayList<String> cache_files = new ArrayList<>();
    private static String dir = System.getProperty("user.dir");

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
        System.out.println("protocol: " + protocol);
        try {
            if (protocol.equals("tcp")) {
                cache_tcp = new ServerSocket(port);
            } else if (protocol.equals("snw")) {
                cache_tcp = new ServerSocket(port);
                cache_snw = new DatagramSocket(port);
            }

            while (true) {
                try {
                    if (protocol.equals("tcp")) {
                        Socket server_client_socket = cache_tcp.accept();
                        BufferedReader server_in = new BufferedReader(
                                new InputStreamReader(server_client_socket.getInputStream()));

                        String command = server_in.readLine();
                        System.out.println("Command: " + command);
                        if (command.startsWith("get")) {
                            String file_name = command.split(" ")[1];
                            cache_files.clear();
                            tcp_transport.getAllFiles(cache_files, "cache_files");
                            if (cache_files.indexOf(file_name) != -1) {
                                tcp_transport.send_command(server_client_socket, "File Delivered from cache", null, -1);
                                tcp_transport.sendFile(server_client_socket, dir + "/cache_files/" + file_name);
                            } else {
                                Socket server_tcp = new Socket(server_ip, server_port);
                                tcp_transport.send_command(server_tcp, command, server_ip, server_port);

                                tcp_transport.receiveFile(server_tcp, dir + "/cache_files/" + file_name);

                                tcp_transport.send_command(server_client_socket, "File Delivered from origin", null,
                                        -1);
                                tcp_transport.sendFile(server_client_socket, dir + "/cache_files/" + file_name);
                            }
                        }
                        server_client_socket.close();
                    } else if (protocol.equals("snw")) {
                        Socket cache_client_socket = cache_tcp.accept();
                        byte[] readCommand = new byte[1024];
                        DatagramPacket cache_receive_udp_packet = new DatagramPacket(readCommand,
                                readCommand.length);
                        cache_snw.receive(cache_receive_udp_packet);

                        String command = new String(cache_receive_udp_packet.getData(), 0,
                                cache_receive_udp_packet.getLength());
                        System.out.println("command: " + command);

                        InetAddress client_addr = cache_receive_udp_packet.getAddress();
                        int client_port = cache_receive_udp_packet.getPort();

                        String file_name = command.split(" ")[1];

                        if (command != null) {
                            if (command.startsWith("get")) {
                                cache_files.clear();
                                tcp_transport.getAllFiles(cache_files, "cache_files");
                                if (cache_files.indexOf(file_name) != -1) {
                                    File file = new File(dir + "/cache_files/" + file_name);
                                    if (!file.exists() || !file.isFile()) {
                                        System.out.println("Invalid file:");
                                        continue;
                                    }
                                    long file_size = file.length();
                                    tcp_transport.send_command(cache_client_socket, "LEN:" + file_size, null, -1);

                                    ArrayList<byte[]> chunks = snw_transport.create_chunk(file, 1000);
                                    try {
                                        cache_snw.setSoTimeout(1000);
                                        for (int i = 0; i < chunks.size(); i++) {
                                            byte[] sendData = chunks.get(i);
                                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                                    client_addr, client_port);
                                            cache_snw.send(sendPacket);
                                            System.out.println("Send: " + i);

                                            String ack = snw_transport.receive_command(cache_snw,
                                                    client_addr, client_port);
                                            
                                        }

                                        String fIN = snw_transport.receive_command(cache_snw, client_addr, client_port);
                                        
                                        tcp_transport.send_command(cache_client_socket, "File Delivered from cache.",
                                                null, -1);
                                    } catch (Exception e) {

                                    }

                                } else {
                                    // snw_transport.send_command(cache_snw, InetAddress.getByName(server_ip),
                                    // server_port,
                                    // command);

                                    // String client_receive = snw_transport.receive_command(cache_snw);
                                    // System.out.println("client_receive: " + client_receive);

                                    // snw_transport.send_command(cache_snw, client_addr, client_port,
                                    // client_receive);
                                }
                            }
                        }

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
