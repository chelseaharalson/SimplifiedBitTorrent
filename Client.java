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
public class Client extends Thread {
    
    public static class ClientDone {
        private boolean done = true;
        
        public synchronized void setValue(boolean val) {
            done = val;
        }
        
        public synchronized boolean read() {
            return done;
        }
    }

    static ArrayList<String> fileNeededList = new ArrayList<String>();
    static List<File> downloadedList = new ArrayList<File>();
    static ArrayList<String> uploadList = new ArrayList<String>();
    static List<File> uploadFileList = new ArrayList<File>();
    static String hostName = "";
    static String serverName = "";
    static int serverPortNumber = 0;
    static int uploadPortNumber = 0;
    static int downloadPortNumber = 0;
    static int myPortNumber = 0;
    static String mode = "";
    final static ClientDone done = new ClientDone();

    public Client(String HostName, int PortNumber, String Mode) {
        hostName = HostName;
        serverPortNumber = PortNumber;
        mode = Mode;
    }
    
    public Client(int PortNumber, String Mode) {
        serverPortNumber = PortNumber;
        mode = Mode;
    }
    
    @Override
    public void run() {
        if (mode.equals("D")) {
            try {
                while (done.read() == false) {
                    Thread.sleep(5000);
                    sendFileList();
                }
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
        if (args.length != 5) {
            System.err.println("Usage: java Client <host name> <server port number> <upload port number> <download port number> <my port number>");
            System.exit(1);
        }

        serverName = args[0];
        serverPortNumber = Integer.parseInt(args[1]);
        uploadPortNumber = Integer.parseInt(args[2]);
        downloadPortNumber = Integer.parseInt(args[3]);
        myPortNumber = Integer.parseInt(args[4]);
        
        File dir = new File("Peer-" + myPortNumber);
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        // Initialize - get files from server
        initialPull(serverName, serverPortNumber);
        
        done.setValue(false);
        
        Thread.sleep(5000);
        new Client(myPortNumber, "L").start();
        Thread.sleep(5000);
        new Client(serverName, downloadPortNumber, "D").start(); 
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
        String folderName = "Peer-" + myPortNumber + "/";
        flist.add(new File(folderName+"uploadFileList-"+myPortNumber+".txt"));
        try {
            Socket sock = null;
            sock = new Socket(serverName, downloadPortNumber);
            sendFILES(flist, sock);
            Thread.sleep(2000);
        }
        catch (Exception e) {
            System.out.println("Trying to download connection... not found on port number " + downloadPortNumber);
            Thread.sleep(5000);
            //e.printStackTrace();
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
                    String folderName = "Peer-" + myPortNumber + "/";
                    convertListToFile(downloadedList, folderName+"summary.txt");
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
        
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        try {
            String folderName = "Peer-" + myPortNumber + "/";
            // get number of files being received
            int numOfFiles = dis.readInt();
            // read all the files
            for (int i = 0; i < numOfFiles; i++) {
                String filename = dis.readUTF();
                long size = dis.readLong();
                File file = new File(filename);
                if (file.getName().equals("fileNameList.txt")) {
                    saveFile(filename, size, dis);
                    fileNeededList = convertFileToArray("fileNameList.txt");
                }
                else if (file.getName().equals("uploadFileList-"+uploadPortNumber+".txt")) {
                    saveFile(folderName+filename, size, dis);
                    uploadList.clear();
                    uploadFileList.clear();
                    uploadList = convertFileToArray(folderName+filename);
                    for (int j = 0; j < uploadList.size(); j++) {
                        for (int k = 0; k < downloadedList.size(); k++) {
                            if (uploadList.get(j).equals(downloadedList.get(k).getName())) {
                                System.out.println("Peer-" + myPortNumber + " has this file: " + "FileChunks-"+myPortNumber+"/" + downloadedList.get(k).getName() + " and will send to Peer-" + uploadPortNumber);
                                uploadFileList.add(new File("FileChunks-"+myPortNumber+"/" + downloadedList.get(k).getName()));
                            }
                        }
                    }
                    try {
                        Socket sock = null;
                        sock = new Socket(serverName, uploadPortNumber);
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
            convertArrayToFile(fileNeededList, folderName+"uploadFileList-"+myPortNumber+".txt");
        }
        catch (EOFException e) {
            e.printStackTrace();
        }

        if (fileNeededList.size() == 0 && done.read() == false) {
            System.out.println("Merging files...");
            Collections.sort(downloadedList);
            String folderName = "Peer-" + myPortNumber + "/";
            mergeFiles(downloadedList, new File(folderName+"merge.jpg"));
            done.setValue(true);
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
                    System.out.println("Sending file: " + file.getName() + " (" + buff.length + " bytes) on port number " + socket.getPort());
                }
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
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