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
            do {
                System.out.print("Enter command: ");
                command = buf_reader.readLine();
                String file_path = null;
                try {
                    file_path = command.split(" ")[1];    
                } catch (Exception e) {
                    System.out.println("Error in getting file.");
                    continue;
                }
                
                if (command.startsWith("put")) {
                    System.out.println("Awaiting server response.");
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
                            System.out.println("Server response: " + msg);
                            server_tcp.close();
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
                                client_snw = new DatagramSocket(server_tcp.getLocalPort());
                                client_snw.setSoTimeout(1000);
                                tcp_transport.send_command(server_tcp, command, server_ip, server_port);
                                tcp_transport.send_command(server_tcp, "LEN:" + file_size, server_ip, server_port);

                                // ArrayList<byte[]> chunks = snw_transport.create_chunk(file, 1000);
                                int bytesRead = 0;
                                byte[] buf = new byte[1000];
                                FileInputStream fIn = new FileInputStream(file);
                                while ((bytesRead = fIn.read(buf)) != -1) {
                                    DatagramPacket sendPacket = new DatagramPacket(buf, bytesRead,
                                            InetAddress.getByName(server_ip), server_port);
                                    client_snw.send(sendPacket);

                                    String ack = snw_transport.receive_command(client_snw,
                                            InetAddress.getByName(server_ip), server_port);

                                    if (!ack.equals("ACK")) {
                                        System.out.println("Did not receive ACK. Terminating.");
                                        break;
                                    }
                                }
                                fIn.close();

                                String fIN = snw_transport.receive_command(client_snw, InetAddress.getByName(server_ip),
                                        server_port);

                                System.out.println("Fin: " + fIN);
                                String msg = tcp_transport.receive_command(server_tcp);
                                if (fIN.equals("FIN"))
                                    System.out.println("Server response: " + msg);
                                else
                                    System.out.println("FIN not received");
                            } catch (SocketTimeoutException e) {
                                System.out.println("Data transmission terminated prematurely.");
                            }
                        }
                    } else {
                        server_tcp.close();
                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                } else if (command.startsWith("get")) {
                    if (protocol.equals("tcp")) {
                        cache_tcp = new Socket(cache_ip, cache_port);
                        tcp_transport.send_command(cache_tcp, command, cache_ip, cache_port);

                        String msg = tcp_transport.receive_command(cache_tcp);
                        System.out.println("Server response: " + msg);
                        tcp_transport.receiveFile(cache_tcp, dir + "/client_files/" + file_path);
                        cache_tcp.close();
                    } else if (protocol.equals("snw")) {
                        try {
                            cache_tcp = new Socket(cache_ip, cache_port);
                            client_snw = new DatagramSocket(cache_tcp.getLocalPort());
                            tcp_transport.send_command(cache_tcp, command, cache_ip, cache_port);

                            String msg = tcp_transport.receive_command(cache_tcp);
                            long size = get_number(msg);
                            if (size < 0) {
                                System.out.println("Invalid file from cache.");
                                break;
                            }

                            ArrayList<byte[]> chunks = new ArrayList<>();
                            FileOutputStream fOut = new FileOutputStream(new File(dir + "/client_files/" + file_path));
                            client_snw.setSoTimeout(1000);
                            while (true) {
                                byte[] r_buf = new byte[1000];
                                DatagramPacket dp = new DatagramPacket(r_buf, r_buf.length);
                                client_snw.receive(dp);
                                int len = dp.getLength();
                                fOut.write(dp.getData(), 0, len);
                                fOut.flush();
                                size -= len;

                                chunks.add(dp.getData());

                                InetAddress rcv_addr = dp.getAddress();
                                int rcv_port = dp.getPort();
                                snw_transport.send_command(client_snw, rcv_addr, rcv_port, "ACK");

                                if (len < 1000 || size < 1) {
                                    snw_transport.send_command(client_snw, rcv_addr, rcv_port, "FIN");
                                    break;
                                }
                            }

                            String final_cmd = tcp_transport.receive_command(cache_tcp);
                            System.out.println("Server response: " + final_cmd);
                            cache_tcp.close();
                        } catch (SocketTimeoutException e) {
                            System.out.println("Data transmission terminated prematurely.");
                        } catch (Exception e) {
                            System.out.println("Something went wrong.");
                        }
                    } else {
                        System.out.println("Invalid protocol");
                    }
                } else {
                    System.out.println("Invalid command: ");
                    continue;
                }
            } while (!command.equals("quit"));
            System.out.println("Exiting program!");
        } catch (Exception e) {
            System.out.println("Something went wrong.");
            e.printStackTrace();
        }
    }
}
