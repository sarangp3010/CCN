import java.io.*;
import java.net.*;
import java.util.*;

public class tcp_transport {
    /**
     * 
     * @param socket
     * @param command
     * @param ip
     * @param port
     * 
     * To send the command from the outputstream of the given socket.
     * As this method is void it return nothing.
     */
    public static void send_command(Socket socket, String command, String ip, int port) {
        try {
            // From the output stream I send command using PrintWriter.
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param socket
     * @return
     * 
     * This method is simply reads the data of the outputstream of the given socket and return the readed string.
     */
    public static String receive_command(Socket socket) {
        try {
            // To read commands from Outputstream I created an BufferedReader input stream. 
            // Here we can create the simple inputstream.
            // Return the msg which get from the socket.
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = in.readLine();
            return msg;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 
     * @param socket
     * @param path
     * 
     * This method of tcp_transport will collect file and it trandfer to the outputstream of the socket.
     */
    public static void sendFile(Socket socket, String path) {
        try {
            // it create file object of the current filepath.
            File file = new File(path);

            // Create FieInput stream to collect the file data 
            FileInputStream fIn = new FileInputStream(file);

            // Data output stream which send the data to the socket
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // get the file_zise to create buffer array of that length.
            long file_size = 0;
            if (file.isFile()) {
                file_size = file.length();
            }
            byte[] buffer = new byte[(int) file_size];

            // I was confuse in collecting file data and send 
            // So that, for this, I took refrence from the https://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets

            int bytesRead = 0;
            while ((bytesRead = fIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            // finally close the FileInput object.
            fIn.close();

            // I faced an error when sending data so that by this code below I resolve that.
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param socket
     * @param path
     * 
     * This method receive the file data and write it to the given path.
     */
    public static void receiveFile(Socket socket, String path) {
        try {
            // Create an Inpurt and fileoutputstreams to get data from the socket and write data to file respectively.
            DataInputStream in = new DataInputStream(socket.getInputStream());
            FileOutputStream fOut = new FileOutputStream(path);

            // initialize buffer which can help to store the readed data from file
            int bytesRead = 0;
            byte[] buffer = new byte[1024];

        //  Here I write the buffer of size 1024 to the file.
            do {
                fOut.write(buffer, 0, bytesRead);
            } while ((bytesRead = in.read(buffer)) != -1);

            // finally close the Output stram as we write data to the file object.
            fOut.close();

            // I faced an error when sending data so that by this code below I resolve that.
            socket.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param al
     * @param directory
     * 
     * This is the void function which simply takes a arrayList and path/directory and fill that list with the available files in that directory.
     */
    public static void getAllFiles(ArrayList<String> al, String directory) {
        // create file object to collect all the files of the directory.
        File dir = new File(System.getProperty("user.dir") + "/" + directory);
        
        // This Returns an array of abstract pathnames denoting the files in the directory denoted by this abstract pathname.
        File[] files = dir.listFiles();

        // Iterate through the files and if is vaalid add it to teh arraylist.
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                String fileName = files[i].getName();
                al.add(fileName);
            }
        }
    }
}
