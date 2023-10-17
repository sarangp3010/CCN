package src;
import java.net.*;

public class server {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String protocol = args[1].toString();
        System.out.println("Hello Server" + port + protocol);

        try {
            ServerSocket ss = new ServerSocket(port);
            System.out.print("Server listning on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
