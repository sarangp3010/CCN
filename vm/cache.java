import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cache {
    // Initialize the requires tcp and udp sockets
    private static ServerSocket cache_tcp = null;
    private static DatagramSocket cache_snw = null;

    // this cache_files list will help to ahndle all the files of "cache_files"
    // folder.
    private static ArrayList<String> cache_files = new ArrayList<>();
    private static String dir = System.getProperty("user.dir");

    // Same as client.java
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
        if (args.length != 4) {
            System.out.println("Invalid args");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String server_ip = args[1].toString();
        int server_port = Integer.parseInt(args[2]);
        String protocol = args[3].toString();

        // bind the ports based on the protocol.
        // So, here I bind only required ports to the sockets.
        if (protocol.equals("tcp")) {
            cache_tcp = new ServerSocket(port);
        } else if (protocol.equals("snw")) {
            cache_tcp = new ServerSocket(port);
            cache_snw = new DatagramSocket(port);
        }

        while (true) {
            if (protocol.equals("tcp")) {
                // start accepting commands from client.
                Socket server_client_socket = cache_tcp.accept();

                // BufferedReader to read that command from teh Input stream.
                BufferedReader server_in = new BufferedReader(
                        new InputStreamReader(server_client_socket.getInputStream()));

                String command = server_in.readLine();
                if (command.startsWith("get")) {
                    // get file_name from the command.
                    String file_name = command.split(" ")[1];

                    // first clear and then modify with teh updated current available files in
                    // directory cache_files.
                    cache_files.clear();
                    tcp_transport.get_directory_files(cache_files, "cache_files");

                    // if have a file in cache
                    if (cache_files.indexOf(file_name) != -1) {
                        // send file and command to the client.
                        tcp_transport.send_command(server_client_socket, "File Delivered from cache");
                        tcp_transport.send_file(server_client_socket, dir + "/cache_files/" + file_name);
                    } else {
                        // otherwise

                        // to send command get file from the server create new server socket.
                        Socket server_tcp = new Socket(server_ip, server_port);

                        // send command to the server.
                        tcp_transport.send_command(server_tcp, command);

                        // recieive the file and store it into the cache_files
                        tcp_transport.receive_file(server_tcp, dir + "/cache_files/" + file_name);

                        // send msg "File Delivered from origin" to the client
                        tcp_transport.send_command(server_client_socket, "File Delivered from origin");

                        // send file to the client.
                        tcp_transport.send_file(server_client_socket, dir + "/cache_files/" + file_name);
                    }
                }
                server_client_socket.close();
            } else if (protocol.equals("snw")) {
                // accept connections.
                Socket cache_client_socket = cache_tcp.accept();

                // BufferedReader will be created to get the commands through tcp.
                BufferedReader client_in = new BufferedReader(
                        new InputStreamReader(cache_client_socket.getInputStream()));

                String command = client_in.readLine();

                // collect metadata like ip and port from the command received.
                InetAddress client_addr = cache_client_socket.getInetAddress();
                int client_port = cache_client_socket.getPort();
                String file_name = command.split(" ")[1];

                cache_files.clear();
                if (command != null) {
                    if (command.startsWith("get")) {
                        // refresh available files in cache_files directory.
                        tcp_transport.get_directory_files(cache_files, "cache_files");

                        // if file not available in cache_files.
                        if (cache_files.indexOf(file_name) != -1) {
                            // create file object
                            File file = new File(dir + "/cache_files/" + file_name);

                            // check for the valid file.
                            if (!file.exists() || !file.isFile()) {
                                System.out.println("Invalid file:");
                                continue;
                            }

                            // get the file size
                            long file_size = file.length();

                            // and send it to the socket like LEN:size
                            tcp_transport.send_command(cache_client_socket, "LEN:" + file_size);

                            /**
                             * Now this below process reads the file packets over udp
                             * sends that packet to the destination.
                             * receive ACK for each packet and write that packets to the file
                             * until it completely reads all the packets of that file.
                             */
                            try {
                                cache_snw.setSoTimeout(1000);
                                int read_bytes = 0;
                                byte[] buf = new byte[1000];
                                FileInputStream fIn = new FileInputStream(file);
                                while ((read_bytes = fIn.read(buf)) != -1) {
                                    DatagramPacket sendPacket = new DatagramPacket(buf, read_bytes,
                                            client_addr, client_port);
                                    cache_snw.send(sendPacket);

                                    String ack = snw_transport.receive_command(cache_snw,
                                            client_addr, client_port);
                                    if (ack == null || !ack.equals("ACK")) {
                                        System.out.println("Did not receive ACK. Terminating.");
                                        tcp_transport.send_command(cache_client_socket,
                                                "Did not receive ACK. Terminating.");
                                        break;
                                    }
                                }
                                // close the FileInputStream as we already store the data.
                                fIn.close();

                                // receive the FIN msg from the cache.
                                String fIN = snw_transport.receive_command(cache_snw, client_addr, client_port);
                                if (!fIN.equals("FIN")) {
                                    tcp_transport.send_command(cache_client_socket,
                                            "Did not receive ACK. Terminating.");
                                    continue;
                                }

                                // send final command
                                tcp_transport.send_command(cache_client_socket, "File Delivered from cache.");
                                // if error appears inbetween show it and send it to the client.
                            } catch (SocketTimeoutException e) {
                                System.out.println("Data transmission terminated prematurely.");
                                tcp_transport.send_command(cache_client_socket,
                                        "Data transmission terminated prematurely.");
                            } catch (Exception e) {
                                System.out.println("Something went wrong.");
                                tcp_transport.send_command(cache_client_socket,
                                        "Data transmission terminated prematurely.");
                            }
                        } else {
                            // create new server_tcp to send and receive commands and file.
                            Socket server_tcp = new Socket(server_ip, server_port);

                            // create new UDP socket.
                            DatagramSocket cache_server_snw = new DatagramSocket(server_tcp.getLocalPort());

                            // send command over tcp
                            tcp_transport.send_command(server_tcp, command);

                            // receive teh msg like LEN:size
                            String msg = tcp_transport.receive_command(server_tcp);

                            // check for the valid file size.
                            long size = get_number(msg);
                            if (size < 0) {
                                System.out.println("Invalid file from cache.");
                                tcp_transport.send_command(cache_client_socket,
                                        "File Not Available either server or Cache");
                                continue;
                            }

                            // set chunks and set timeout
                            ArrayList<byte[]> chunks = new ArrayList<>();
                            cache_server_snw.setSoTimeout(1000);

                            /**
                             * This try catch mainly first receive file from the server and then send this
                             * file to the client.
                             */
                            try {
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
                                FileOutputStream fOut = new FileOutputStream(
                                        new File(dir + "/cache_files/" + file_name));
                                while (true) {
                                    byte[] r_buf = new byte[1000];
                                    DatagramPacket dp = new DatagramPacket(r_buf, r_buf.length);
                                    cache_server_snw.receive(dp);
                                    int len = dp.getLength();
                                    fOut.write(dp.getData(), 0, len);
                                    fOut.flush();
                                    size -= len;

                                    chunks.add(dp.getData());
                                    InetAddress rcv_addr = dp.getAddress();
                                    int rcv_port = dp.getPort();
                                    snw_transport.send_command(cache_server_snw, rcv_addr, rcv_port, "ACK");

                                    if (len < 1000 || size < 1) {
                                        snw_transport.send_command(cache_server_snw, rcv_addr, rcv_port, "FIN");
                                        break;
                                    }
                                }

                                // Now we have a file in cache_files so send it to the client.
                                File file = new File(dir + "/cache_files/" + file_name);
                                if (!file.exists() || !file.isFile()) {
                                    System.out.println("Invalid file:");
                                }
                                long file_size = file.length();
                                tcp_transport.send_command(cache_client_socket, "LEN:" + file_size);

                                cache_snw.setSoTimeout(1000);

                                /**
                                 * Now this below process reads the file packets over udp
                                 * sends that packet to the destination.
                                 * receive ACK for each packet and write that packets to the file
                                 * untile it completely reads all the packets of that file.
                                 */
                                int read_bytes = 0;
                                byte[] buf = new byte[1000];
                                FileInputStream fIn = new FileInputStream(file);
                                while ((read_bytes = fIn.read(buf)) != -1) {
                                    DatagramPacket sendPacket = new DatagramPacket(buf, read_bytes,
                                            client_addr, client_port);
                                    cache_snw.send(sendPacket);

                                    String ack = snw_transport.receive_command(cache_snw,
                                            client_addr, client_port);
                                    if (ack == null || !ack.equals("ACK")) {
                                        tcp_transport.send_command(cache_client_socket,
                                                "Did not receive ACK. Terminating.");
                                        System.out.println("Did not receive ACK. Terminating.");
                                        fIn.close();
                                        continue;
                                    }
                                }
                                fIn.close();

                                String fIN = snw_transport.receive_command(cache_snw, client_addr, client_port);
                                if (!fIN.equals("FIN")) {
                                    tcp_transport.send_command(cache_client_socket,
                                            "Data transmission terminated prematurely.");
                                    continue;
                                }
                                tcp_transport.send_command(cache_client_socket, "File Delivered from origin.");
                            } catch (SocketTimeoutException e) {
                                // When timeout appears this block invokes.
                                System.out.println("Data transmission terminated prematurely.");
                                tcp_transport.send_command(cache_client_socket,
                                        "Data transmission terminated prematurely.");
                            } catch (Exception e) {
                                // for any other exception appears this block invokes.
                                System.out.println("Data transmission terminated prematurely.");
                                tcp_transport.send_command(cache_client_socket,
                                        "Data transmission terminated prematurely.");
                            }
                        }
                    }
                }
            }
        }
    }
}
