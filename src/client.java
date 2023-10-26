import java.net.*;
import java.io.*;

public class client {
    private static Socket server_tcp = null;
    private static Socket cache_tcp = null;
    private static DatagramSocket client_udp = null;

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

        // System.out.println("Hello Client ");
        // System.out.print(" Server Ip: " + server_ip);
        // System.out.print(" server_port: " + server_port);
        // System.out.print(" cache_ip: " + cache_ip);
        // System.out.print(" cache_port: " + cache_port);
        // System.out.print(" protocol: " + protocol);

        BufferedReader buf_reader = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        try {
            client_udp = new DatagramSocket();
            do {
                System.out.print("Enter command: ");
                command = buf_reader.readLine();
                if (command.startsWith("put")) {
                    String dir = System.getProperty("user.dir");
                    if (protocol.equals("tcp")) {
                        String file_path = command.split(" ")[1];
                        File file = new File(dir + "/client_files/" + file_path);
                        if (!file.exists() || !file.isFile()) {
                            System.out.println("Invalid file:");
                            continue;
                        } else {
                            server_tcp = new Socket(server_ip, server_port);
                            tcp_transport.send_command(server_tcp, command);

                            tcp_transport.sendFile(server_tcp, dir + "/client_files/" + file_path, dir + "/server_files/" + file_path);

                            String msg = tcp_transport.receive_command(server_tcp);

                            System.out.println("msg: " + msg);
                            server_tcp.close();
                        }

                    } else if (protocol.equals("snw")) {

                    } else {
                        if (server_tcp != null) {
                            server_tcp.close();
                        }
                        
                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                } else if (command.startsWith("get")) {
                    if (protocol.equals("tcp")) {
                        cache_tcp = new Socket(cache_ip, cache_port);
                        tcp_transport.send_command(cache_tcp, command);

                        String msg = tcp_transport.receive_command(cache_tcp);
                        System.out.println("msg : " + msg);
                    } else if (protocol.equals("snw")) {
                        snw_transport.send_command(client_udp, InetAddress.getByName(cache_ip), cache_port, command);

                        String msg = snw_transport.receive_command(client_udp);
                        System.out.println("Received from server: " + msg);
                    } else {
                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                } else {
                    System.out.println("Invalid command: ");
                    continue;
                }
            } while (!command.equals("quit"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
