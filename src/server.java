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
        if (args.length != 2) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String protocol = args[1].toString();

        try {
            server_tcp = new ServerSocket(port);
            server_udp = new DatagramSocket(port);
            while (true) {
                if (protocol.equals("tcp")) {
                    Socket cache_client_socket = server_tcp.accept();

                    BufferedReader in = new BufferedReader(new InputStreamReader(cache_client_socket.getInputStream()));
                    String command = in.readLine();
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

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(server_client_socket.getInputStream()));
                    String command = in.readLine();

                    InetAddress cache_addr = server_client_socket.getInetAddress();
                    int cache_port = server_client_socket.getPort();
                    if (command.startsWith("get")) {
                        String file_name = command.split(" ")[1];
                        server_files.clear();
                        tcp_transport.getAllFiles(server_files, "server_files");

                        if (server_files.indexOf(file_name) != -1) {
                            File file = new File(dir + "/server_files/" + file_name);
                            if (!file.exists() || !file.isFile()) {
                                System.out.println("Invalid file:");
                                continue;
                            }
                            long file_size = file.length();
                            tcp_transport.send_command(server_client_socket, "LEN:" + file_size, null, -1);

                            try {
                                int bytesRead = 0;
                                byte[] buf = new byte[1000];
                                FileInputStream fIn = new FileInputStream(file);
                                while ((bytesRead = fIn.read(buf)) != -1) {
                                    DatagramPacket sendPacket = new DatagramPacket(buf, bytesRead,
                                            cache_addr, cache_port);
                                    server_udp.send(sendPacket);

                                    String ack = snw_transport.receive_command(server_udp,
                                            cache_addr, cache_port);
                                    if (!ack.equals("ACK")) {
                                        tcp_transport.send_command(server_client_socket,
                                                "Did not receive ACK. Terminating.",
                                                null, -1);
                                        System.out.println("Did not receive ACK. Terminating.");
                                        fIn.close();
                                        continue;
                                    }
                                }
                                fIn.close();

                                String fIN = snw_transport.receive_command(server_udp, cache_addr, cache_port);
                                if (!fIN.equals("FIN")) {
                                    tcp_transport.send_command(server_client_socket,
                                            "Data transmission terminated prematurely.",
                                            null, -1);
                                    continue;
                                }

                                tcp_transport.send_command(server_client_socket, "File Delivered from server.",
                                        null, -1);
                            } catch (Exception e) {

                            }

                        } else {
                        }
                    } else if (command.startsWith("put")) {
                        String file_name = command.split(" ")[1];
                        String msg = in.readLine();
                        long size = get_number(msg);
                        if (size < 0) {
                            tcp_transport.send_command(server_client_socket, "Invalid File Size.", null, -1);
                            continue;
                        }

                        ArrayList<byte[]> chunks = new ArrayList<>();
                        server_udp.setSoTimeout(1000);
                        FileOutputStream fOut = new FileOutputStream(new File(dir + "/server_files/" + file_name));
                        while (true) {
                            byte[] r_buf = new byte[1000];
                            DatagramPacket dp = new DatagramPacket(r_buf, r_buf.length);
                            server_udp.receive(dp);
                            int len = dp.getLength();
                            fOut.write(dp.getData(), 0, len);
                            fOut.flush();
                            size -= len;

                            chunks.add(dp.getData());

                            InetAddress rcv_addr = dp.getAddress();
                            int rcv_port = dp.getPort();
                            snw_transport.send_command(server_udp, rcv_addr, rcv_port, "ACK");

                            if (len < 1000 || size < 1) {
                                snw_transport.send_command(server_udp, rcv_addr, rcv_port, "FIN");
                                break;
                            }
                        }
                        tcp_transport.send_command(server_client_socket, "File successfully uploaded.", null, -1);
                        fOut.close();
                        server_client_socket.close();
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
