import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author chelsea metcalf
 */
public class ClientUpload extends Thread {

    static ArrayList<String> fileNeededList = new ArrayList<String>();
    static List<File> downloadedList = new ArrayList<File>();
    static ArrayList<String> uploadList = new ArrayList<String>();
    static List<File> uploadFileList = new ArrayList<File>();
    static String hostName = "";
    static String downloadNeighbor = "";
    static String uploadNeighbor = "";
    static int serverPortNumber = 0;
    static int uploadPortNumber = 0;
    static int downloadPortNumber = 0;
    static int myPortNumber = 0;
    static String mode = "";
    static boolean done = false;

    public ClientUpload(String HostName, int PortNumber, String Mode) {
        hostName = HostName;
        serverPortNumber = PortNumber;
        mode = Mode;
    }
    
    public ClientUpload(int PortNumber, String Mode) {
        serverPortNumber = PortNumber;
        mode = Mode;
    }
    
    @Override
    public void run() {
        if (mode.equals("D")) {
            try {
                sendFileList();
            } catch (IOException | InterruptedException ex) {
                //ex.printStackTrace();
            }
        }
        else if (mode.equals("L")) {
            try {
                while (true) {
                    waitForFiles();
                }
            } catch (IOException | InterruptedException ex) {
                //ex.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        if (args.length != 7) {
            System.err.println("Usage: java Client <host name> <server port number> <upload port number> <download port number>");
            System.exit(1);
        }

        String serverName = args[0];
        uploadNeighbor = args[1];
        downloadNeighbor = args[2];
        serverPortNumber = Integer.parseInt(args[3]);
        uploadPortNumber = Integer.parseInt(args[4]);
        downloadPortNumber = Integer.parseInt(args[5]);
        myPortNumber = Integer.parseInt(args[6]);
        
        // Initialize - get files from server
        initialPull(serverName, serverPortNumber);
        
        Thread.sleep(5000);
        new ClientUpload(myPortNumber, "L").start();
        System.out.println("DONE VALUE: " + done);
        //while (!done) {
            Thread.sleep(5000);
            new ClientUpload(downloadNeighbor, downloadPortNumber, "D").start();
        //}
    }
    
    public static void waitForFiles() throws IOException, InterruptedException {
        try {
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            ServerSocket serverSocket = null;
            Socket sock = null;
            try {
                serverSocket = new ServerSocket(myPortNumber);
                System.out.println("Waiting on port " + myPortNumber);
                try {
                    sock = serverSocket.accept();
                    System.out.println("Accepted connection: " + sock);
                    receiveFILES(sock);
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
    
    public static void sendFileList() throws IOException, InterruptedException {
        List<File> flist = new ArrayList<File>();
        convertArrayToFile(fileNeededList, "uploadFileList.txt");
        flist.add(new File("uploadFileList.txt"));
        boolean connected = false;
        // TO DO
        while (connected == false) {
            try {
                Socket sock = null;
                sock = new Socket(downloadNeighbor, downloadPortNumber);
                connected = true;
                sendFILES(flist, sock);
            }
            catch (Exception e) {
                System.out.println("Trying to download connection... NOT FOUND on port number " + downloadPortNumber);
                Thread.sleep(5000);
                //e.printStackTrace();
            }
        }
    }
    
    public static void initialPull(String hostName, int portNumber) throws IOException {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        
        boolean connected = false;
        try {
            while (!connected) {
                try {
                    sock = new Socket(hostName, portNumber);
                    connected = true;
                    System.out.println("Connecting on port " + portNumber);
                    receiveFILES(sock);
                    String fname = "summary-"+myPortNumber+".txt";
                    convertListToFile(downloadedList, fname);
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
    
    public static void receiveFILES(Socket socket) throws IOException, InterruptedException {
        File fileChunksDir = new File("FileChunks-"+myPortNumber);
        if (!fileChunksDir.exists()) {
            fileChunksDir.mkdir();
        }
        
        //while (true) {
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
                        fileNeededList = convertFileToArray("fileNameList.txt");
                    }
                    else if (file.getName().equals("uploadFileList.txt")) {
                        saveFile(filename, size, dis);
                        uploadList.clear();
                        uploadList = convertFileToArray(filename);
                        for (int j = 0; j < uploadList.size(); j++) {
                            for (int k = 0; k < downloadedList.size(); k++) {
                                if (uploadList.get(j).equals(downloadedList.get(k).getName())) {
                                    System.out.println("Peer-" + myPortNumber + " has this file: " + "FileChunks-"+myPortNumber+"/" + downloadedList.get(k).getName() + " and will send to Peer-" + downloadPortNumber);
                                    uploadFileList.add(new File("FileChunks-"+myPortNumber+"/" + downloadedList.get(k).getName()));
                                }
                            }
                        }
                        try {
                            Socket sock = null;
                            sock = new Socket(downloadNeighbor, downloadPortNumber);
                            sendFILES(uploadFileList, sock);
                        }
                        catch (Exception e) {
                            System.out.println("Trying to download connection... NOT FOUND on port number " + downloadPortNumber);
                            Thread.sleep(5000);
                            e.printStackTrace();
                        }
                    }
                    else {
                        downloadedList.add(file);
                        fileNeededList.remove(file.getName());
                        saveFile("FileChunks-"+myPortNumber+"/"+filename, size, dis);
                    }
                }
            }
            catch (EOFException e) {
                //break;
            }

            if (fileNeededList.size() == 0) {
                System.out.println("Merging files...");
                done = true;
                Collections.sort(downloadedList);
                String fname = "merge-"+myPortNumber+".jpg";
                mergeFiles(downloadedList, new File(fname));
            }
        //}
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
                    System.out.println("Sending file: " + file.getName() + " (" + buff.length + " bytes) on port number " + socket.getPort());
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
                fi = "FileChunks-"+myPortNumber+"/" + f.getName();
                File joinedFile = new File(fi);
                //System.out.println(joinedFile);
                Files.copy(joinedFile.toPath(), mergingStream);
            }
        }
    }
    
    public static ArrayList<String> convertFileToArray(String fileName) throws FileNotFoundException {
        Scanner s = new Scanner(new File(fileName));
        ArrayList<String> fList = new ArrayList<String>();
        while (s.hasNextLine()) {
            fList.add(s.nextLine());
        }
        s.close();
        return fList;
    }
    
    public static void convertArrayToFile(ArrayList<String> flistArr, String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        for (int i = 0; i < flistArr.size(); i++) {
            writer.write(flistArr.get(i) + "\n");
        }
        writer.close();
    }
    
    public static void convertListToFile(List<File> flist, String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        for (int i = 0; i < flist.size(); i++) {
            writer.write(flist.get(i) + "\n");
        }
        writer.close();
    }
}