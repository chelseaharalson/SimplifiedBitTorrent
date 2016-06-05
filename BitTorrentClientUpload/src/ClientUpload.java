import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chelsea metcalf
 */
public class ClientUpload extends Thread {

    static ArrayList<String> fileNeededList = new ArrayList<String>();
    static List<File> downloadedList = new ArrayList<File>();
    static String hostName = "";
    static String downloadNeighbor = "";
    static String uploadNeighbor = "";
    static int portNumber = 0;
    static String mode = "";

    public ClientUpload(String HostName, int PortNumber, String Mode) {
        hostName = HostName;
        portNumber = PortNumber;
        mode = Mode;
    }
    
    @Override
    public void run() {
        if (mode.equals("D")) {
            try {
                //System.out.println("DO DOWNLOAD");
                doDownload();
            } catch (IOException ex) {
                //ex.printStackTrace();
            } catch (InterruptedException ex) {
                //ex.printStackTrace();
            }
        }
        else {
            try {
                //System.out.println("DO UPLOAD");
                doUpload();
            } catch (IOException ex) {
                //ex.printStackTrace();
            } catch (InterruptedException ex) {
                
            }
        }
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 4) {
            System.err.println("Usage: java Client <host name> <host name> <host name> <port number>");
            System.exit(1);
        }

        String serverName = args[0];
        uploadNeighbor = args[1];
        downloadNeighbor = args[2];
        portNumber = Integer.parseInt(args[3]);
        
        // Initialize - get files from server
        //socketReceive(serverName, portNumber);

        try {
            new ClientUpload(uploadNeighbor, portNumber, "U").start();
            Thread.sleep(5000);
            //new ClientUpload(downloadNeighbor, portNumber, "D").start();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Press 'q' to quit:\t");
        String input = scanner.nextLine();
        if (input.equals("q")) {
            System.exit(0);
            //new Client(downloadNeighbor, portNumber, "D").start();
        }
    }
    
    public static void doDownload() throws IOException, InterruptedException {
        List<File> flist = new ArrayList<File>();
        flist.add(new File("fileNameList.txt"));
        boolean connected = false;
        while (connected == false) {
            try {
                Socket sock = null;
                sock = new Socket(downloadNeighbor, portNumber);
                connected = true;
                sendFILES(flist, sock);
            }
            catch (Exception e) {
                System.out.println("Trying to download connection... NOT FOUND");
                Thread.sleep(5000);
            }
        }
    }
    
    public static void doUpload() throws IOException, InterruptedException {
        try {
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            ServerSocket serverSocket = null;
            Socket sock = null;
            try {
                serverSocket = new ServerSocket(portNumber);
                System.out.println("Waiting...");
                try {
                        sock = serverSocket.accept();
                        System.out.println("Accepted connection: " + sock);
                        while (true) {
                            DataInputStream dis = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
                            try {
                                // get number of files being received
                                int numOfFiles = dis.readInt();
                                // read all the files
                                for (int i = 0; i < numOfFiles; i++) {
                                    String filename = dis.readUTF();
                                    long size = dis.readLong();
                                    saveFile(filename, size, dis);
                                }
                            }
                            catch (EOFException e) {
                                break;
                            }
                        }
                        Thread.sleep(2000);
                        List<File> flist = new ArrayList<File>();
                        flist.add(new File("test.txt"));
                        Socket sendSocket = null;
                        //portNumber++;
                        System.out.println("ADDED TEST.TXT TO SEND ON " + portNumber + "    " + downloadNeighbor);
                        sendSocket = new Socket(downloadNeighbor, portNumber);
                        sendFILES(flist, sendSocket);
                    }
                finally {
                    if (bis != null) bis.close();
                    if (sock != null) sock.close();
                }
            }
            finally {
                if (serverSocket != null) serverSocket.close();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static void socketReceive(String hostName, int portNumber) throws IOException {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        
        boolean connected = false;
        try {
            while (!connected) {
                try {
                    sock = new Socket(hostName, portNumber);
                    connected = true;
                    System.out.println("Connecting...");

                    receiveFILES(sock);
                    fileNeededList = neededList("fileNameList.txt");
                    //System.out.println(fileNeededList);
                    //System.out.println("DOWNLOADED LIST: " + downloadedList);
                    for (int i = 0; i < fileNeededList.size(); i++) {
                        for (int j = 0; j < downloadedList.size(); j++) {
                            if (fileNeededList.get(i).equals(downloadedList.get(j).getName())) {
                                //System.out.println(fileNeededList.get(i));
                                fileNeededList.remove(i);
                            }
                        }
                    }
                    //System.out.println(fileNeededList.size());
                    if (fileNeededList.size() == 0) {
                        mergeFiles(downloadedList, new File("merge.jpg"));
                    }
                }
                catch (Exception e) {
                    System.out.println("Connection failed");
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        finally {
          if (fos != null) fos.close();
          if (bos != null) bos.close();
          if (sock != null) sock.close();
        }
    }
    
    public static void receiveFILES(Socket socket) throws IOException {
        while (true) {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            try {
                // get number of files being received
                int numOfFiles = dis.readInt();
                // read all the files
                for (int i = 0; i < numOfFiles; i++) {
                    String filename = dis.readUTF();
                    long size = dis.readLong();
                    //byte[] buff = new byte[(int)size];
                    File file = new File(filename);
                    //System.out.println("Added: " + filename);
                    if (file.getName().equals("fileNameList.txt")) {
                        saveFile(filename, size, dis);
                    }
                    else {
                        downloadedList.add(file);
                        saveFile("FileChunks/"+filename, size, dis);
                    }
                }
            }
            catch (EOFException e) {
                break;
            }
        }
    }
    
    public static void saveFile(String filename, long size, DataInputStream dis) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        long total = 0;
        int count = 0;
        byte[] buff = new byte[(int)size];
        while ((total < size) && ((count = dis.read(buff, 0, (int) Math.min(buff.length, size - total))) > 0)) {
            fos.write(buff, 0, count);
            total += count;
        }
        fos.close();
        System.out.println("Received File: " + filename + " (" + size + " bytes)");
    }
    
    public static void sendFILES(List<File> files, Socket socket) throws IOException {
        FileInputStream fis = null;
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        long fileSize;
        dos.writeInt(files.size());

        // send every file in list
        for (File file : files) {
            int bytesRead = 0;
            fis = new FileInputStream(file);
            // send filename                        
            dos.writeUTF(file.getName());
            // send filesize (bytes)
            dos.writeLong(fileSize = file.length());
            //System.out.println("File Size: " + fileSize);
            // send file 
            byte[] buff = new byte [(int)file.length()];
            try {
                while ((bytesRead = fis.read(buff)) != -1) {
                    dos.write(buff, 0, bytesRead);
                    System.out.println("Sending file: " + file.getName() + " (" + buff.length + " bytes)");
                }
                dos.flush();
            } catch (IOException e) {
                System.out.println("IO Error!!");
            }
            // close the filestream
            fis.close();
        }
        System.out.println("Finished sending files");
        dos.close();
        socket.close();
    }
    
    public static void mergeFiles(List<File> files, File into) throws IOException {
        try (BufferedOutputStream mergingStream = new BufferedOutputStream(new FileOutputStream(into))) {
            String fi = "";
            for (File f : files) {
                fi = "FileChunks/" + f.getName();
                File joinedFile = new File(fi);
                //System.out.println(joinedFile);
                Files.copy(joinedFile.toPath(), mergingStream);
            }
        }
    }
    
    public static ArrayList<String> neededList(String fileName) throws FileNotFoundException {
        Scanner s = new Scanner(new File(fileName));
        ArrayList<String> fList = new ArrayList<String>();
        while (s.hasNextLine()) {
            fList.add(s.nextLine());
        }
        s.close();
        return fList;
    }
}