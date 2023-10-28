import java.net.*;
import java.util.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class client {
    private static Socket server_tcp = null;
    private static Socket cache_tcp = null;
    private static DatagramSocket client_snw = null;
    private static String dir = System.getProperty("user.dir");

    public static void check(Socket s) {
        try {
            if (s.isOutputShutdown())
                s.getOutputStream();
            if (s.isInputShutdown())
                s.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        if (args.length != 5) {
            System.out.println("Invalid args");
            return;
        }

        String server_ip = args[0].toString();
        int server_port = Integer.parseInt(args[1]);
        String cache_ip = args[2].toString();
        int cache_port = Integer.parseInt(args[3]);
        String protocol = args[4].toString();

        BufferedReader buf_reader = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        try {
            client_snw = new DatagramSocket();
            do {
                System.out.print("Enter command: ");
                command = buf_reader.readLine();
                String file_path = command.split(" ")[1];
                if (command.startsWith("put")) {
                    String dir = System.getProperty("user.dir");
                    if (protocol.equals("tcp")) {
                        File file = new File(dir + "/client_files/" + file_path);
                        if (!file.exists() || !file.isFile()) {
                            System.out.println("Invalid file:");
                            continue;
                        } else {
                            server_tcp = new Socket(server_ip, server_port);
                            tcp_transport.send_command(server_tcp, command, server_ip, server_port);

                            String msg = tcp_transport.receive_command(server_tcp);
                            tcp_transport.sendFile(server_tcp, dir + "/client_files/" + file_path);
                            System.out.println("msg: " + msg);
                        }

                    } else if (protocol.equals("snw")) {
                        File file = new File(dir + "/client_files/" + file_path);

                        if (!file.exists() || !file.isFile()) {
                            System.out.println("Invalid file:");
                            continue;
                        } else {
                            try {
                                long file_size = file.length();
                                server_tcp = new Socket(server_ip, server_port);
                                tcp_transport.send_command(server_tcp, command, server_ip, server_port);
                                tcp_transport.send_command(server_tcp, "LEN:" + file_size, server_ip, server_port);

                                ArrayList<byte[]> chunks = snw_transport.create_chunk(file, 1000);
                                client_snw.setSoTimeout(1000);
                                for (int i = 0; i < chunks.size(); i++) {
                                    byte[] sendData = chunks.get(i);
                                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                            InetAddress.getByName(server_ip), server_port);
                                    client_snw.send(sendPacket);
                                    System.out.println("Send: " + i);

                                    String ack = snw_transport.receive_command(client_snw,
                                            InetAddress.getByName(server_ip), server_port);
                                    System.out.println("ACK: " + ack + " receive: " + i);
                                }

                                System.out.println("Called this");
                                String fIN = snw_transport.receive_command(client_snw, InetAddress.getByName(server_ip),
                                        server_port);
                                System.out.println("Called3: " + fIN);

                                if (fIN.equals("FIN")) {
                                    String msg = tcp_transport.receive_command(server_tcp);
                                    System.out.println("msg: " + msg);
                                } else {
                                    System.out.println("FIN not received");
                                }
                            } catch (SocketTimeoutException e) {
                                System.out.println("Did not receive ACK. Terminating.");
                            }
                        }
                    } else {
                        if (server_tcp.isConnected()) {
                            server_tcp.close();
                        }

                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                } else if (command.startsWith("get")) {
                    if (protocol.equals("tcp")) {
                        cache_tcp = new Socket(cache_ip, cache_port);
                        tcp_transport.send_command(cache_tcp, command, cache_ip, cache_port);

                        String msg = tcp_transport.receive_command(cache_tcp);
                        System.out.println("msg : " + msg);
                        tcp_transport.receiveFile(cache_tcp, dir + "/client_files/" + file_path);
                        cache_tcp.close();
                    } else if (protocol.equals("snw")) {
                        cache_tcp = new Socket(cache_ip, cache_port);
                        
                        snw_transport.send_command(client_snw, InetAddress.getByName(cache_ip),
                        cache_port, command);
                        tcp_transport.send_command(cache_tcp, command, cache_ip, cache_port);

                        String msg = tcp_transport.receive_command(cache_tcp);
                        System.out.println("msg : " + msg);
                        long size = get_number(msg);
                        if (size < 0) {
                            System.out.println("Invalid file from cache.");
                            break;
                        }

                        ArrayList<byte[]> chunks = new ArrayList<>();
                        client_snw.setSoTimeout(1000);
                        while (true) {
                            byte[] r_buf = new byte[1000];
                            DatagramPacket dp = new DatagramPacket(r_buf, r_buf.length);
                            client_snw.receive(dp);
                            byte[] data = dp.getData();
                            int len = dp.getLength();
                            size -= len;
                            System.out.println("len: " + len + " size: " + size);

                            chunks.add(data);

                            InetAddress rcv_addr = dp.getAddress();
                            int rcv_port = dp.getPort();
                            snw_transport.send_command(client_snw, rcv_addr, rcv_port, "ACK");

                            if (len < 1000 || size < 1) {
                                System.out.println("Final Call");
                                snw_transport.send_command(client_snw, rcv_addr, rcv_port, "FIN");
                                snw_transport.write_file(chunks, new File(dir + "/client_files/" + file_path));
                                break;
                            }
                        }
                        System.out.println("Called finals");
                        String msgg = tcp_transport.receive_command(cache_tcp);
                        System.out.println("msgg: "+ msgg);
                        cache_tcp.close();
                    } else {
                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                } else {
                    System.out.println("Invalid command: ");
                    continue;
                }
            } while (!command.equals("quit"));
        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }
}
