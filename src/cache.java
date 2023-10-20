import java.io.IOException;
import java.net.*;
import java.util.*;

public class cache {
    private static ServerSocket server_tcp = null;
    private static DatagramSocket server_udp = null;
    ArrayList<String> cache_files = new ArrayList<>();

    // public cache(int port) throws Exception {
    //     server_udp = new DatagramSocket(port);
    // }

    // public cache(String ip, int port) throws Exception {
    //     server_tcp = new ServerSocket(port);
    // }

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
            server_tcp = new ServerSocket(port);
            if (protocol == "tcp") {
                
            } else if (protocol == "snw") {
    
            } else {
                System.out.println("From the Cache Server");
                System.out.println("Invalid protocol");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
