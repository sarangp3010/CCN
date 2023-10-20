import java.net.*;

public class cache {
    Socket server_tcp = null;
    DatagramSocket server_udp = null;

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String server_ip = args[1].toString();
        int server_port = Integer.parseInt(args[2]);
        String protocol = args[3].toString();

        if (protocol == "tcp") {

        } else if (protocol == "snw") {

        } else {
            System.out.println("From the Cache Server");
            System.out.println("Invalid protocol");
        }
        try {
            // ServerSocket cs = new ServerSocket(port);
            // System.out.print("Server listning on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
