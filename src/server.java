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

        if (protocol == "tcp") {

        } else if (protocol == "snw") {

        } else {
            System.out.println("From the Server Server");
            System.out.println("Invalid protocol");
        }
        // try {
        // // ServerSocket ss = new ServerSocket(port);
        // System.out.print("Server listning on port " + port);

        // while (true) {
        // // Socket clientSocket = ss.accept();
        // // System.out.println("Client connected: " +
        // clientSocket.getInetAddress().getHostAddress());

        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }
}
