import java.net.*;
import java.util.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class client {
    // initialization of the sockets that are required for the client implimentation
    // 1) to connect to the server using tcp = server_tcp
    // 2) to connect to the cache using tcp = cache_tcp
    // 3) for the client snw Datagram socket(UDP) = client_snw
    private static Socket server_tcp = null;
    private static Socket cache_tcp = null;
    private static DatagramSocket client_snw = null;
    private static String dir = System.getProperty("user.dir");

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
        if (args.length != 5) {
            System.out.println("Invalid args");
            return;
        }

        String server_ip = args[0].toString();
        int server_port = Integer.parseInt(args[1]);
        String cache_ip = args[2].toString();
        int cache_port = Integer.parseInt(args[3]);
        String protocol = args[4].toString();

        // By this buffer reader I collect the command that user give at the console.
        BufferedReader buf_reader = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        do {
            System.out.print("Enter command: ");
            command = buf_reader.readLine();
            String file_path = null;

            // By this exception if there's an eception I will return user to enter the command again.
            // The error could be anything here related to command only
            try {
                file_path = command.split(" ")[1];
            } catch (Exception e) {
                System.out.println("Error in getting file.");
                continue;
            }

            if (command.startsWith("put") || command.startsWith("PUT")) {
                System.out.println("Awaiting server response.");
                if (protocol.equals("tcp") || protocol.equals("TCP")) {
                    // Here it creates the file oject that is stored at the client_files.
                    File file = new File(dir + "/client_files/" + file_path);
                    // If any error in gettign file or invalid file simply return form that.
                    // otherwise continue the process
                    if (!file.exists() || !file.isFile()) {
                        System.out.println("Invalid file:");
                        continue;
                    } else {
                        // bind the ip and port to teh server_tcp to transfer command and file to
                        // server.
                        server_tcp = new Socket(server_ip, server_port);

                        // Here it simply call the send command methos and I pass the current server_Tcp
                        // and command.
                        // the additional arguments are if the Printwriter stream is closed I'll rebind
                        // it to the server
                        tcp_transport.send_command(server_tcp, command);

                        // receive msg from the serve to show to the user.
                        String msg = tcp_transport.receive_command(server_tcp);
                        tcp_transport.send_file(server_tcp, dir + "/client_files/" + file_path);
                        System.out.println("Server response: " + msg);

                        // I had faced an issue of open tcp port so that I closed the port.
                        server_tcp.close();
                    }
                } else if (protocol.equals("snw") || protocol.equals("SNW")) {
                    // create the file object of the path.
                    File file = new File(dir + "/client_files/" + file_path);

                    // return if file has error or not exist
                    if (!file.exists() || !file.isFile()) {
                        System.out.println("Invalid file:");
                        continue;
                    } else {
                        // otherwise
                        try {
                            // get the file size
                            long file_size = file.length();

                            // bind the server socket with it's ip and port.
                            server_tcp = new Socket(server_ip, server_port);

                            // bind the UDP socket.
                            client_snw = new DatagramSocket(server_tcp.getLocalPort());

                            // set 1ms timeout to the socket
                            client_snw.setSoTimeout(10000);
                            tcp_transport.send_command(server_tcp, command);
                            tcp_transport.send_command(server_tcp, "LEN:" + file_size);

                            int read_bytes = 0;

                            /**
                             * Now this below process reads the file packets over udp
                             * sends that packet to the destination.
                             * receive ACK for each packet and write that packets to the file
                             * untile it completely reads all the packets of that file.
                             */
                            byte[] buf = new byte[1000];
                            FileInputStream fIn = new FileInputStream(file);
                            while ((read_bytes = fIn.read(buf)) != -1) {
                                DatagramPacket sendPacket = new DatagramPacket(buf, read_bytes,
                                        InetAddress.getByName(server_ip), server_port);
                                client_snw.send(sendPacket);

                                String ack = snw_transport.receive_command(client_snw,
                                        InetAddress.getByName(server_ip), server_port);

                                if (ack == null || !ack.equals("ACK")) {
                                    System.out.println("Did not receive ACK. Terminating.");
                                    break;
                                }
                            }
                            fIn.close();

                            String fIN = snw_transport.receive_command(client_snw, InetAddress.getByName(server_ip),
                                    server_port);

                            if (fIN == null) {
                                System.out.println("FIN not received: Data transmission terminated prematurely.");
                                continue;
                            }

                            String msg = tcp_transport.receive_command(server_tcp);
                            if (fIN.equals("FIN"))
                                System.out.println("Server response: " + msg);
                            else
                                System.out.println("FIN not received: Data transmission terminated prematurely.");
                        } catch (SocketTimeoutException e) {
                            System.out.println("Data transmission terminated prematurely.");
                        }
                    }
                } else {
                    // else close the current open port.
                    server_tcp.close();
                    System.out.println("From the Cleint Server");
                    System.out.println("Invalid protocol");
                }
            } else if (command.startsWith("get") || command.startsWith("GET")) {
                // if command starts with get
                if (protocol.equals("tcp") || protocol.equals("TCP")) {
                    // bind cach tcp to send command andreceive file over tcp.
                    cache_tcp = new Socket(cache_ip, cache_port);

                    // send command received from the user.
                    tcp_transport.send_command(cache_tcp, command);

                    // get file and success/error msg from the server.
                    String msg = tcp_transport.receive_command(cache_tcp);
                    System.out.println("Server response: " + msg);
                    tcp_transport.receive_file(cache_tcp, dir + "/client_files/" + file_path);

                    // finally close cache tcp socket for the client.
                    cache_tcp.close();
                } else if (protocol.equals("snw") || protocol.equals("SNW")) {
                    try {
                        // bind the cache tcp and UDP to send commands and file respectively.
                        cache_tcp = new Socket(cache_ip, cache_port);
                        client_snw = new DatagramSocket(cache_tcp.getLocalPort());

                        // using tcp send command from teh user to cache.
                        tcp_transport.send_command(cache_tcp, command);

                        // receive msg from the cache like LEN:size
                        String msg = tcp_transport.receive_command(cache_tcp);

                        // retive size from msg.
                        long size = get_number(msg);

                        // it it's invalied return from here.
                        if (size < 0) {
                            System.out.println("Invalid file from server/cache.");
                            continue;
                        }

                        // create chunks list.
                        ArrayList<byte[]> chunks = new ArrayList<>();

                        // fOut will be store data to the given file.
                        FileOutputStream fOut = new FileOutputStream(new File(dir + "/client_files/" + file_path));
                        client_snw.setSoTimeout(10000);

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
                            client_snw.receive(dp);
                            int len = dp.getLength();
                            fOut.write(dp.getData(), 0, len);
                            fOut.flush();
                            size -= len;

                            chunks.add(dp.getData());

                            InetAddress rcv_addr = dp.getAddress();
                            int rcv_port = dp.getPort();
                            snw_transport.send_command(client_snw, rcv_addr, rcv_port, "ACK");

                            if (len < 1000 || size < 1) {
                                snw_transport.send_command(client_snw, rcv_addr, rcv_port, "FIN");
                                break;
                            }
                        }

                        // receive filan command over TCP. and printit.
                        String final_cmd = tcp_transport.receive_command(cache_tcp);
                        System.out.println("Server response: " + final_cmd);
                        cache_tcp.close();
                    } catch (SocketTimeoutException e) {
                        System.out.println("Data transmission terminated prematurely.");
                    } catch (Exception e) {
                        System.out.println("Something went wrong.");
                    }
                } else {
                    System.out.println("Invalid protocol");
                }
            } else {
                System.out.println("Invalid command: ");
                continue;
            }
        } while (!command.equals("quit"));
        System.out.println("Exiting program!");
    }
}
