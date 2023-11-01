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
        if (s == null)
            return -1;
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(s);

        long r = -1;
        if (matcher.find()) {
            String numberStr = matcher.group();
            r = Long.parseLong(numberStr);
        }
        return r;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String server_ip = args[1].toString();
        int server_port = Integer.parseInt(args[2]);
        String protocol = args[3].toString();

        if (protocol.equals("tcp")) {
            cache_tcp = new ServerSocket(port);
        } else if (protocol.equals("snw")) {
            cache_tcp = new ServerSocket(port);
            cache_snw = new DatagramSocket(port);
        }

        while (true) {
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

                            try {
                                cache_snw.setSoTimeout(1000);
                                int bytesRead = 0;
                                byte[] buf = new byte[1000];
                                FileInputStream fIn = new FileInputStream(file);
                                while ((bytesRead = fIn.read(buf)) != -1) {
                                    DatagramPacket sendPacket = new DatagramPacket(buf, bytesRead,
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
                                fIn.close();

                                String fIN = snw_transport.receive_command(cache_snw, client_addr, client_port);
                                if (!fIN.equals("FIN")) {
                                    tcp_transport.send_command(cache_client_socket,
                                            "Did not receive ACK. Terminating.", null, -1);
                                    continue;
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
                            System.out.println("Server uploaded: "+ server_tcp.getPort() + "  local" + server_tcp.getLocalPort());
                            DatagramSocket cache_server_snw = new DatagramSocket(server_tcp.getLocalPort());
                            tcp_transport.send_command(server_tcp, command, server_ip, server_port);

                            System.out.println("Commanfd send to server: "+ command);
                            String msg = tcp_transport.receive_command(server_tcp);
                            long size = get_number(msg);
                            if (size < 0) {
                                System.out.println("Invalid file from cache.");
                                tcp_transport.send_command(cache_client_socket, "File Not Available either server or Cache", null, -1);
                                continue;
                            }

                            ArrayList<byte[]> chunks = new ArrayList<>();
                            cache_server_snw.setSoTimeout(1000);
                            try {
                                FileOutputStream fOut = new FileOutputStream(
                                        new File(dir + "/cache_files/" + file_name));
                                while (true) {
                                    byte[] r_buf = new byte[1000];
                                    DatagramPacket dp = new DatagramPacket(r_buf, r_buf.length);
                                    cache_server_snw.receive(dp);
                                    int len = dp.getLength();
                                    fOut.write(dp.getData(), 0, len);
                                    fOut.flush();
                                    size -= len;

                                    chunks.add(dp.getData());
                                    InetAddress rcv_addr = dp.getAddress();
                                    int rcv_port = dp.getPort();
                                    System.out.println("rcv_addr: "+ rcv_addr.getHostAddress());
                                    System.out.println("rcv_port: "+ rcv_port);
                                    snw_transport.send_command(cache_server_snw, rcv_addr, rcv_port, "ACK");

                                    if (len < 1000 || size < 1) {
                                        snw_transport.send_command(cache_server_snw, rcv_addr, rcv_port, "FIN");
                                        break;
                                    }
                                }

                                File file = new File(dir + "/cache_files/" + file_name);
                                if (!file.exists() || !file.isFile()) {
                                    System.out.println("Invalid file:");
                                }
                                long file_size = file.length();
                                tcp_transport.send_command(cache_client_socket, "LEN:" + file_size, null, -1);

                                cache_snw.setSoTimeout(1000);
                                int bytesRead = 0;
                                byte[] buf = new byte[1000];
                                FileInputStream fIn = new FileInputStream(file);
                                while ((bytesRead = fIn.read(buf)) != -1) {
                                    DatagramPacket sendPacket = new DatagramPacket(buf, bytesRead,
                                            client_addr, client_port);
                                    cache_snw.send(sendPacket);

                                    String ack = snw_transport.receive_command(cache_snw,
                                            client_addr, client_port);
                                    if (ack == null || !ack.equals("ACK")) {
                                        tcp_transport.send_command(cache_client_socket,
                                                "Did not receive ACK. Terminating.",
                                                null, -1);
                                        System.out.println("Did not receive ACK. Terminating.");
                                        fIn.close();
                                        continue;
                                    }
                                }
                                fIn.close();

                                String fIN = snw_transport.receive_command(cache_snw, client_addr, client_port);
                                if (!fIN.equals("FIN")) {
                                    tcp_transport.send_command(cache_client_socket,
                                            "Data transmission terminated prematurely.",
                                            null, -1);
                                    continue;
                                }
                                tcp_transport.send_command(cache_client_socket, "File Delivered from origin.",
                                        null, -1);
                            } catch (SocketTimeoutException e) {
                                System.out.println("Data transmission terminated prematurely.");
                                tcp_transport.send_command(cache_client_socket,
                                        "Data transmission terminated prematurely.",
                                        null, -1);
                            } catch (Exception e) {
                                System.out.println("Data transmission terminated prematurely.");
                                tcp_transport.send_command(cache_client_socket,
                                        "Data transmission terminated prematurely.",
                                        null, -1);
                            }
                        }
                    }
                }

            } else {
                System.out.println("caleed");
            }

        }

    }
}
