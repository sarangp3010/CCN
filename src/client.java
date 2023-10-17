package src;
import java.net.*;

public class client {
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

        try {
            Socket server_s = new Socket(server_ip, server_port);
            Socket cache_s = new Socket(cache_ip, cache_port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
