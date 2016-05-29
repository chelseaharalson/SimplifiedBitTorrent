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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.*;

/**
 *
 * @author chelsea metcalf
 */
public class FileOwner {
    
    static List<File> fileList = new ArrayList<File>();
    static FileInputStream fis = null;
    static BufferedInputStream bis = null;
    static OutputStream os = null;
    //static String FILE_TO_SEND = "FileChunks/flowerimage.jpg";
    //static String textFile = "fileNameList.txt";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java Server <port number>");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
        try {
            splitFile(new File("FileChunks/flowerimage.jpg"));
            createTextFileList(fileList);
            //System.out.println(fileList);
            //mergeFiles(fileList, new File("FileChunks/merge.jpg"));
        } catch (IOException ex) {
            Logger.getLogger(FileOwner.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        ServerSocket serverSocket = null;
        Socket sock = null;
        try {
          serverSocket = new ServerSocket(portNumber);
          while (true) {
            System.out.println("Waiting...");
            try {
              sock = serverSocket.accept();
              System.out.println("Accepted connection: " + sock);
              
              sendFiles(fileList, sock);
            }
            finally {
              if (bis != null) bis.close();
              if (os != null) os.close();
              if (sock!=null) sock.close();
            }
          }
        }
        finally {
          if (serverSocket != null) serverSocket.close();
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
                File newFile = new File(f.getParent(), name + "." + String.format("%03d", partCounter++));
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, temp); // temp is the chunk size
                    if (!fileList.contains(newFile)) {
                        fileList.add(newFile);
                    }
                    System.out.println("Total # of pieces file is divided into: " + (partCounter-1));
                }
            }
        }
    }
    
    public static void createTextFileList(List<File> fList) throws FileNotFoundException, IOException {
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
    }
    
    public static void sendFiles(List<File> files, Socket socket) {
        try {
            DataOutputStream dataOS = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            System.out.println(files.size());
            // write the number of files to the server
            dataOS.writeInt(files.size());
            dataOS.flush();

            // write filenames 
            for(int i = 0 ; i < files.size(); i++) {
                dataOS.writeUTF(files.get(i).getName());
                dataOS.flush();
            }

            int n = 0;
            byte[] buff = new byte[4092];
            byte[] finished = new byte[3];
            String str = "done";
            finished = str.getBytes();
            for(int i = 0; i < files.size(); i++) {
                System.out.println("Sending File: " + files.get(i).getName());
                FileInputStream fis = new FileInputStream(files.get(i));
                while((n = fis.read(buff)) != -1) {
                    dataOS.write(buff,0,n);
                    dataOS.flush();
                }              
                dataOS.write(finished,0,3);
                dataOS.flush();
            }
            dataOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }	
    }
    
    /*public static void mergeFiles(List<File> files, File into) throws IOException {
        try (BufferedOutputStream mergingStream = new BufferedOutputStream(new FileOutputStream(into))) {
            for (File f : files) {
                Files.copy(f.toPath(), mergingStream);
            }
        }
    }*/

}
