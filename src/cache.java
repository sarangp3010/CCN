import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cache {
    private static ServerSocket cache_tcp = null;
    private static DatagramSocket cache_snw = null;
    private static ArrayList<String> cache_files = new ArrayList<>();
    private static String dir = System.getProperty("user.dir");

    private static long get_number(String s) {
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
        if (args.length != 4) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String server_ip = args[1].toString();
        int server_port = Integer.parseInt(args[2]);
        String protocol = args[3].toString();

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
                        BufferedReader client_in = new BufferedReader(
                                new InputStreamReader(cache_client_socket.getInputStream()));

                        String command = client_in.readLine();

                        InetAddress client_addr = cache_client_socket.getInetAddress();
                        int client_port = cache_client_socket.getPort();
                        String file_name = command.split(" ")[1];

                        cache_files.clear();
                        if (command != null) {
                            if (command.startsWith("get")) {
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

                                            String ack = snw_transport.receive_command(cache_snw,
                                                    client_addr, client_port);
                                            if (!ack.equals("ACK")) {
                                                System.out.println("Did not receive ACK. Terminating.");
                                                tcp_transport.send_command(cache_client_socket,
                                                        "Did not receive ACK. Terminating.",
                                                        null, -1);
                                                break;
                                            }
                                        }

                                        String fIN = snw_transport.receive_command(cache_snw, client_addr, client_port);
                                        if (!fIN.equals("FIN")) {
                                            tcp_transport.send_command(cache_client_socket,
                                                    "Did not receive ACK. Terminating.", null, -1);
                                            break;
                                        }

                                        tcp_transport.send_command(cache_client_socket, "File Delivered from cache.",
                                                null, -1);
                                    } catch (SocketTimeoutException e) {
                                        System.out.println("Data transmission terminated prematurely.");
                                        tcp_transport.send_command(cache_client_socket,
                                                "Data transmission terminated prematurely.",
                                                null, -1);
                                    } catch (Exception e) {
                                        System.out.println("Something went wrong.");
                                        tcp_transport.send_command(cache_client_socket,
                                                "Data transmission terminated prematurely.",
                                                null, -1);
                                    }
                                } else {
                                    Socket server_tcp = new Socket(server_ip, server_port);
                                    DatagramSocket cache_server_snw = new DatagramSocket(server_tcp.getLocalPort());
                                    tcp_transport.send_command(server_tcp, command, server_ip, server_port);

                                    String msg = tcp_transport.receive_command(server_tcp);
                                    long size = get_number(msg);
                                    if (size < 0) {
                                        System.out.println("Invalid file from cache.");
                                        break;
                                    }

                                    ArrayList<byte[]> chunks = new ArrayList<>();
                                    cache_server_snw.setSoTimeout(1000);
                                    FileOutputStream fOut = new FileOutputStream(
                                            new File(dir + "/cache_files/" + file_name));
                                    while (true) {
                                        byte[] r_buf = new byte[1000];
                                        DatagramPacket dp = new DatagramPacket(r_buf, r_buf.length);
                                        cache_server_snw.receive(dp);
                                        int len = dp.getLength();
                                        fOut.write(dp.getData(), 0, len);
                                        size -= len;

                                        chunks.add(dp.getData());
                                        InetAddress rcv_addr = dp.getAddress();
                                        int rcv_port = dp.getPort();
                                        snw_transport.send_command(cache_server_snw, rcv_addr, rcv_port, "ACK");

                                        if (len < 1000 || size < 1) {
                                            snw_transport.send_command(cache_server_snw, rcv_addr, rcv_port, "FIN");
                                            // snw_transport.write_file(chunks,
                                            // new File(dir + "/cache_files/" + file_name));
                                            break;
                                        }
                                    }

                                    File file = new File(dir + "/cache_files/" + file_name);
                                    if (!file.exists() || !file.isFile()) {
                                        System.out.println("Invalid file:");
                                        throw new Exception("Cache dosen't have a file.");
                                    }
                                    long file_size = file.length();
                                    tcp_transport.send_command(cache_client_socket, "LEN:" + file_size, null, -1);

                                    ArrayList<byte[]> cache_chunks = snw_transport.create_chunk(file, 1000);
                                    try {
                                        cache_snw.setSoTimeout(1000);
                                        for (int i = 0; i < cache_chunks.size(); i++) {
                                            byte[] sendData = cache_chunks.get(i);
                                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                                    client_addr, client_port);
                                            cache_snw.send(sendPacket);

                                            String ack = snw_transport.receive_command(cache_snw,
                                                    client_addr, client_port);
                                            if (!ack.equals("ACK")) {
                                                System.out.println("Did not receive ACK. Terminating.");
                                                tcp_transport.send_command(cache_client_socket,
                                                        "File Delivered from origin.",
                                                        null, -1);
                                                break;
                                            }
                                        }
                                        String fIN = snw_transport.receive_command(cache_snw, client_addr, client_port);
                                        if (fIN.equals("FIN")) {
                                            System.out.println("");
                                            break;
                                        }
                                        tcp_transport.send_command(cache_client_socket, "File Delivered from origin.",
                                                null, -1);
                                    } catch (SocketTimeoutException e) {
                                        System.out.println("Data transmission terminated prematurely.");
                                        tcp_transport.send_command(cache_client_socket,
                                                "Data transmission terminated prematurely.",
                                                null, -1);
                                    } catch (Exception e) {
                                        tcp_transport.send_command(cache_client_socket,
                                                "Data transmission terminated prematurely.",
                                                null, -1);
                                    }
                                }
                            }
                        }

                    } else {
                    }
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {

        }
    }
}
