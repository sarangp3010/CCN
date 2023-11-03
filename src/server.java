import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class server {
    // First, Initialize the sockets needed to the server.
    // server_tcp -> server's own tcp
    // server_udp -> server's own udp
    private static ServerSocket server_tcp = null;
    private static DatagramSocket server_udp = null;

    // This arraylist handle all the files in the folder server_files.
    private static ArrayList<String> server_files = new ArrayList<>();
    private static String dir = System.getProperty("user.dir");

    // Same as client
    // This helper method give the number form tthe string like LEN:33223 -> 33223
    // I used the pattern and matcher to handle it using the regex
    private static long get_number(String s) {
        if (s == null)
            return -1;
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(s);

        long r = -1;
        if (matcher.find()) {
            String numberStr = matcher.group();
            r = Long.parseLong(numberStr);
        }
        return r;
    }

    public static void main(String[] args) throws Exception {
        // if arguments are less than 2 simply return msg
        if (args.length != 2) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String protocol = args[1].toString();

        // Create both tcp and udp ports and set timeout to the udp port
        server_tcp = new ServerSocket(port);
        server_udp = new DatagramSocket(port);
        server_udp.setSoTimeout(1000);
        while (true) {
            if (protocol.equals("tcp") || protocol.equals("TCP")) {
                // Accept the string from the socket.
                Socket cache_client_socket = server_tcp.accept();

                // Create BufferedReader to read the data from the socket.
                BufferedReader in = new BufferedReader(new InputStreamReader(cache_client_socket.getInputStream()));
                String command = in.readLine();
                if (command != null) {
                    // get filename from teh command
                    String file_name = command.split(" ")[1];
                    if (command.startsWith("get") || command.startsWith("GET")) {
                        // clear all teh server_files
                        server_files.clear();

                        // By this again refresh the file list of teh server_file.
                        tcp_transport.get_directory_files(server_files, "server_files");
                        if (server_files.indexOf(file_name) != -1) {
                            // if we have a file in server_files just send that to the cache.
                            tcp_transport.send_file(cache_client_socket, dir + "/server_files/" + file_name);
                        } else {
                            // else send command that filenot found.
                            tcp_transport.send_command(cache_client_socket, "File Not Found in origin");
                        }
                    } else if (command.startsWith("put") || command.startsWith("PUT")) {
                        // for the put just send the command that "File Successfully uploaded" to
                        // client.
                        tcp_transport.send_command(cache_client_socket, "File Successfully uploaded");
                        tcp_transport.receive_file(cache_client_socket, dir + "/server_files/" + file_name);
                    } else {
                        System.out.println("From server: Invalid command");
                    }
                }
                cache_client_socket.close();
                in.close();
            } else if (protocol.equals("snw") || protocol.equals("SNW")) {
                // open server_tcp to accept the string/commands from either client or cache.
                Socket server_client_socket = server_tcp.accept();

                // BufferedReader to read the bytes through the socket.
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(server_client_socket.getInputStream()));
                String command = in.readLine();

                // collect the ip and port for the later use
                InetAddress cache_addr = server_client_socket.getInetAddress();
                int cache_port = server_client_socket.getPort();
                if (command.startsWith("get") || command.startsWith("GET")) {
                    // Split the file_name from the command
                    String file_name = command.split(" ")[1];

                    // clear files list and re assign it so that it someone manually deleted file it
                    // will get detect before read it from the server_files.
                    server_files.clear();
                    // call method to get all teh filenames as a arraylist.
                    tcp_transport.get_directory_files(server_files, "server_files");

                    // if we dont' have a in server_files folder.
                    if (server_files.indexOf(file_name) != -1) {
                        // create a new file object.
                        File file = new File(dir + "/server_files/" + file_name);

                        // it it's invalid return or terminate process.
                        if (!file.exists() || !file.isFile()) {
                            System.out.println("Invalid file:");
                            continue;
                        }

                        // get length and transfer msg like LEN:file_length to the soket
                        long file_size = file.length();
                        tcp_transport.send_command(server_client_socket, "LEN:" + file_size);
                        String cn = snw_transport.receive_command(server_udp, cache_addr, cache_port);

                        /**
                         * Now this below process reads the file packets over udp
                         * sends that packet to the destination.
                         * receive ACK for each packet and write that packets to the file
                         * untile it completely reads all the packets of that file.
                         */

                        /**
                         * I faced the error while sending the packets and receive at the other end so
                         * the below stackoverflow artical helped me to resolve this.
                         * https://stackoverflow.com/questions/23177351/udp-client-server-file-transfer
                         */
                        try {
                            int read_bytes = 0;
                            byte[] buf = new byte[1000];
                            FileInputStream fIn = new FileInputStream(file);
                            while ((read_bytes = fIn.read(buf)) != -1) {
                                DatagramPacket dp = new DatagramPacket(buf, read_bytes,
                                        cache_addr, cache_port);
                                server_udp.send(dp);

                                String ack = snw_transport.receive_command(server_udp,
                                        cache_addr, cache_port);
                                if (ack == null || !ack.equals("ACK")) {
                                    tcp_transport.send_command(server_client_socket,
                                            "Did not receive ACK. Terminating.");
                                    System.out.println("Did not receive ACK. Terminating.");
                                    fIn.close();
                                    break;
                                }
                            }
                            fIn.close();

                            // Finally, here read FIN msg if it's not FIN then it terminated and return
                            // error msg to the sender socket.
                            String fIN = snw_transport.receive_command(server_udp, cache_addr, cache_port);
                            if (fIN == null || !fIN.equals("FIN")) {
                                tcp_transport.send_command(server_client_socket,
                                        "Data transmission terminated prematurely.");
                                continue;
                            } else {
                                tcp_transport.send_command(server_client_socket,
                                        "Data transmission terminated prematurely.");
                            }

                        } catch (SocketTimeoutException e) {
                            tcp_transport.send_command(server_client_socket,
                                    "Data transmission terminated prematurely.");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (command.startsWith("put") || command.startsWith("PUT")) {
                    // it it's put then get the file_name.
                    String file_name = command.split(" ")[1];
                    String msg = in.readLine();

                    // after read from the input stream get the size of the file from msg: LEN:size.
                    long size = get_number(msg);

                    // if it's invalie or <0 return invalid file.
                    if (size < 0) {
                        tcp_transport.send_command(server_client_socket, "Invalid File Size.");
                        continue;
                    }

                    // create chunks arraylist/
                    ArrayList<byte[]> chunks = new ArrayList<>();

                    try {
                        // Create FileOutputStream to write the buffer to the file/
                        FileOutputStream fOut = new FileOutputStream(new File(dir + "/server_files/" + file_name));

                        /**
                         * This below process reads packets from the socket
                         * Then, write it to the file
                         * Then, collect the data and size and reduce size from the actual size as we
                         * have received the data.
                         * now from the packets collet the metadata like port and ip.
                         * and send ACK and FIN msg accordingly.
                         * Finally, if all completed then trasnfer msg to client like: "File
                         * successfully uploaded."
                         */
                        while (true) {
                            byte[] r_buf = new byte[1000];
                            DatagramPacket dp = new DatagramPacket(r_buf, r_buf.length);
                            server_udp.receive(dp);
                            int len = dp.getLength();
                            fOut.write(dp.getData(), 0, len);
                            fOut.flush();
                            size -= len;

                            chunks.add(dp.getData());

                            InetAddress rcv_addr = dp.getAddress();
                            int rcv_port = dp.getPort();
                            snw_transport.send_command(server_udp, rcv_addr, rcv_port, "ACK");

                            if (len < 1000 || size < 1) {
                                snw_transport.send_command(server_udp, rcv_addr, rcv_port, "FIN");
                                break;
                            }
                        }
                        tcp_transport.send_command(server_client_socket, "File successfully uploaded.");

                    } catch (SocketTimeoutException e) {
                        tcp_transport.send_command(server_client_socket, "File successfully uploaded.");
                        e.printStackTrace();
                    }
                }
                server_client_socket.close();
            } else {
                System.out.println("From the Server Server");
                System.out.println("Invalid protocol");
            }
        }

    }
}
