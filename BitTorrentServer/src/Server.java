import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.net.*;

/**
 *
 * @author chelsea metcalf
 */

public class Server extends Thread {
    
    public static class ReadyForClient {
        private boolean ready = true;
        
        public synchronized void setValue(boolean val) {
            ready = val;
        }
        
        public synchronized boolean read() {
            return ready;
        }
    }

    static List<File> fileList = new ArrayList<File>();
    static List<File> partialFileList = new ArrayList<File>();
    static int filePosition = 1;
    static int numOfFiles = 0;
    static FileInputStream fis = null;
    static BufferedInputStream bis = null;
    static OutputStream os = null;
    int pNumber = 0;
    final static ReadyForClient readyforClient = new ReadyForClient();
    
    public Server(int portNumber) {
        pNumber = portNumber;
    }
    
    @Override
    public void run() {
        try {
            createFileList("image2.jpg");
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            OutputStream os = null;
            ServerSocket serverSocket = null;
            Socket sock = null;
            try {
                serverSocket = new ServerSocket(pNumber);
                //while (true) {
                System.out.println("Waiting...");
                try {
                    sock = serverSocket.accept();
                    System.out.println("Accepted connection: " + sock);
                    readyforClient.setValue(true);
                    /*try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }*/
                    File oFile = createTextFileList(fileList);
                    partialFileList.add(oFile);
                    //sendFILES(fileList, sock);
                    sendFILES(partialFileList, sock);
                }
                finally {
                    if (bis != null) bis.close();
                    if (os != null) os.close();
                    if (sock != null) sock.close();
                }
                //}
            }
            finally {
                if (serverSocket != null) serverSocket.close();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java Server <port number> <numOfFiles>");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
        numOfFiles = Integer.parseInt(args[1]);
        
        try {
            splitFile(new File("image2.jpg"));
            //File oFile = createTextFileList(fileList);
            //System.out.println("Added: " + oFile.getName());
            //fileList.add(oFile);
            //partialFileList.add(oFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
                
        readyforClient.setValue(true);
        while (true) {
            if (readyforClient.read()) {
                try {
                    Thread.sleep(500);
                    //Thread.sleep(6000);
                    //System.out.println(partialFileList);
                    new Server(portNumber).start(); 
                    //System.out.println("READY FOR CLIENT!! " + readyforClient.read());
                    readyforClient.setValue(false); 
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    public static void splitFile(File f) throws IOException {
        int partCounter = 1;
        int fileSize = 100 * 1024; // 100 kb
        byte[] buffer = new byte[fileSize];
        
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))) {
            String name = f.getName();
            int temp = 0;
            
            while ((temp = bis.read(buffer)) > 0) {
                // write each chunk of data into a separate file with different numbers in the name
                //File newFile = new File(f.getParent(), name + "." + String.format("%03d", partCounter++));
                File newFile = new File("FileChunks/" + name + "." + String.format("%03d", partCounter++));
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, temp); // temp is the chunk size
                    if (!fileList.contains(newFile)) {
                        //System.out.println("Adding: " + newFile);
                        fileList.add(newFile);
                    }
                    System.out.println("Total # of pieces file is divided into: " + (partCounter-1));
                }
            }
        }
    }
    
    public static void createFileList(String fileName) {
        int partCounter = 1;
        String fName = "";
        partialFileList.clear();
        for (File f : fileList) {
            fName = fileName + "." + String.format("%03d", partCounter++);
            if (f.getName().equals(fName) && partCounter >= filePosition && partCounter <= filePosition+numOfFiles) {
                //System.out.println("f.getName(): " + f.getName() + " fileName: " + fName);
                partialFileList.add(f);
            }
        }
        filePosition = filePosition + numOfFiles + 1;
    }
    
    public static File createTextFileList(List<File> fList) throws FileNotFoundException, IOException {
        BufferedWriter writer = null;
        File outputFile = new File("fileNameList.txt");
        writer = new BufferedWriter(new FileWriter(outputFile));
        String flist = "";
        for (int i = 0; i < fList.size(); i++) {
            flist = flist + fList.get(i).getName() + "\n";
        }
        flist = flist.trim();
        writer.write(flist);
        writer.close();
        return outputFile;
    }
    
    public static void sendFILES(List<File> files, Socket socket) throws IOException {
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        long fileSize;
        dos.writeInt(files.size());

        // send every file in list
        for (int i = 0; i < files.size(); i++) {
        //for (File file : files) {
            int bytesRead = 0;
            fis = new FileInputStream(files.get(i));
            // send filename                        
            dos.writeUTF(files.get(i).getName());
            // send filesize (bytes)
            dos.writeLong(fileSize = files.get(i).length());
            //System.out.println("File Size: " + fileSize);
            // send file 
            byte[] buff = new byte [(int)files.get(i).length()];
            try {
                while ((bytesRead = fis.read(buff)) != -1) {
                    dos.write(buff, 0, bytesRead);
                    System.out.println("Sending file: " + files.get(i).getName() + " (" + buff.length + " bytes)");
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
}