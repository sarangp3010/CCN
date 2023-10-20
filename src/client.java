import java.net.*;
import java.io.*;

public class client {
    private static Socket server_tcp = null;
    private static Socket cache_tcp = null;
    private static DatagramSocket server_udp = null;
    private static DatagramSocket cache_udp = null;

    // public client(String ip, int port) throws Exception {
    // server_tcp = new Socket(ip, port);
    // cache_tcp = new Socket(ip, port);
    // }

    private static void close() {
        try {
            server_tcp.close();
            cache_tcp.close();
            server_udp.close();
            cache_udp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        System.out.println("Hello Client ");
        System.out.print(" Server Ip: " + server_ip);
        System.out.print(" server_port: " + server_port);
        System.out.print(" cache_ip: " + cache_ip);
        System.out.print(" cache_port: " + cache_port);
        System.out.print(" protocol: " + protocol);

        BufferedReader buf_reader = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        try {
            do {
                System.out.print("Enter command: ");
                command = buf_reader.readLine();
                System.out.println("Command: " + command);
                if (command.startsWith("put")) {
                    if (protocol == "tcp") {

                    } else if (protocol == "snw") {

                    } else {
                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                } else if (command.equals("get")) {
                    cache_tcp = new Socket(cache_ip, cache_port);
                    server_tcp = new Socket(server_ip, server_port);
                    if (protocol == "tcp") {
                        tcp_transport.send_command(cache_tcp, command);
                        String msg = tcp_transport.receive_command(cache_tcp);
                        System.out.println("msg : " + msg);
                    } else if (protocol == "snw") {

                    } else {
                        System.out.println("From the Cleint Server");
                        System.out.println("Invalid protocol");
                    }
                }
            } while (!command.equals("quit"));
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
