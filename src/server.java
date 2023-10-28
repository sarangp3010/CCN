import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class server {
    private static ServerSocket server_tcp = null;
    private static DatagramSocket server_udp = null;
    private static ArrayList<String> server_files = new ArrayList<>();
    private static String dir = System.getProperty("user.dir");

    private long get_number(String s) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(s);

        long r = -1;
        if (matcher.find()) {
            String numberStr = matcher.group();
            r = Long.parseLong(numberStr);
        }
        return r;
    }

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
                    System.out.println("Client connected: " + " ip: " + ip + " post: " + p);

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
                    Socket server_client_socket = server_tcp.accept();
                    String ip = server_client_socket.getInetAddress().getHostAddress();
                    int p = server_client_socket.getPort();
                    System.out.println("Client connected: " + "ip: " + ip + " port: " + p);

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(server_client_socket.getInputStream()));
                    String command = in.readLine();
                    // byte[] receiveData = new byte[1024];
                    // DatagramPacket server_receive_udp_packet = new DatagramPacket(receiveData,
                    // receiveData.length);
                    // server_udp.receive(server_receive_udp_packet);
                    // String command = new String(server_receive_udp_packet.getData(), 0,
                    // server_receive_udp_packet.getLength());
                    System.out.println("receive at server: " + command);
                    if (command.startsWith("get")) {
                        String file_name = command.split(" ")[1];
                        System.out.println("Filename: " + file_name);
                        String msg = in.readLine();
                        System.out.println("MSG: " + msg);
                        // InetAddress cache_addr = server_receive_udp_packet.getAddress();
                        // int cache_port = server_receive_udp_packet.getPort();
                        // System.out.println("Cache data : address: \n\n" + cache_addr + " port : " +
                        // cache_port);
                        // if (server_files.indexOf(file_name) != -1) {
                        // byte[] sendData = "File Deliver from Origin".getBytes();
                        // DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        // cache_addr,
                        // cache_port);
                        // server_udp.send(sendPacket);
                        // } else {
                        // server_files.add(file_name);
                        // byte[] sendData = "File Not Found in origin".getBytes();
                        // DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        // cache_addr,
                        // cache_port);
                        // server_udp.send(sendPacket);
                        // }
                    } else if (command.startsWith("put")) {
                        String file_name = command.split(" ")[1];
                        System.out.println("Filename: " + file_name);
                        String msg = in.readLine();
                        long size = 0;
                        System.out.println("MSG: " + msg);
                        Pattern pattern = Pattern.compile("\\d+");
                        Matcher matcher = pattern.matcher(msg);

                        if (matcher.find()) {
                            String numberStr = matcher.group();
                            size = Long.parseLong(numberStr);
                        }

                        byte[] buf = new byte[1000];
                        ArrayList<byte[]> chunks = new ArrayList<>();
                        System.out.println("ip: " + ip);
                        System.out.println("p: " + p);
                        System.out.println("size: " + size);
                        FileOutputStream fOut = new FileOutputStream(file_name);
                        // server_udp.setSoTimeout(100);
                        while (size > 0) {
                            DatagramPacket dp = new DatagramPacket(buf, buf.length);
                            server_udp.receive(dp);
                            byte[] data = dp.getData();
                            int len = dp.getLength();
                            size -= len;
                            System.out.println("len: " + len + "  size: " + size);
                            fOut.write(data, 0, len);

                            // Send ACK to the client
                            InetAddress clientAddress = dp.getAddress();
                            int clientPort = dp.getPort();
                            String ack = "ACK";
                            byte[] ackData = ack.getBytes();
                            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress,
                                    clientPort);
                            server_udp.send(ackPacket);

                            // Last packet received
                            if (len < 1000 || size < 1) {
                                break;
                            }
                        }
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
