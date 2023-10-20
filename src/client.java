import java.net.*;
import java.io.*;

public class client {
    Socket server_tcp = null;
    Socket cache_tcp = null;
    DatagramSocket server_udp = null;
    DatagramSocket cache_udp = null;

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

        if (protocol == "tcp") {

        } else if (protocol == "snw") {

        } else {
            System.out.println("From the Cleint Server");
            System.out.println("Invalid protocol");
        }
        // try {
        // // Socket server_s = new Socket(server_ip, server_port);
        // // Socket cache_s = new Socket(cache_ip, cache_port);

        // // BufferedReader server_reader = new BufferedReader(new
        // InputStreamReader(server_s.getInputStream()));
        // // BufferedReader cache_reader = new BufferedReader(new
        // InputStreamReader(cache_s.getInputStream()));

        // // PrintWriter server_writer = new PrintWriter(server_s.getOutputStream(),
        // true);
        // // PrintWriter cache_writer = new PrintWriter(cache_s.getOutputStream(),
        // true);

        // BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // String command = "";
        // do {
        // System.out.print("Enter command: ");
        // command = reader.readLine();
        // System.out.println("Command: " + command);
        // if (command.startsWith("put")) {
        // System.out.println("Invalid command. Please try again.");
        // } else if (!command.equals("quit")) {
        // System.out.println("Invalid command. Please try again.");
        // }
        // } while (!command.equals("quit"));
        // // server_s.close();
        // // cache_s.close();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

    }
}
