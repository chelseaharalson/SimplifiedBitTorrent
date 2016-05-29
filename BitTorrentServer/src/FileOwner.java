import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
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
    static String FILE_TO_SEND = "FileChunks/flowerimage.jpg";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java Server <port number>");
            System.exit(1);
        }
        int portNumber = Integer.parseInt(args[0]);
        /*try {
            splitFile(new File("FileChunks/flowerimage.jpg"));
            System.out.println(fileList);
            mergeFiles(fileList, new File("FileChunks/merge.jpg"));
        } catch (IOException ex) {
            Logger.getLogger(FileOwner.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
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
              // send file
              File myFile = new File (FILE_TO_SEND);
              byte [] mybytearray  = new byte [(int)myFile.length()];
              fis = new FileInputStream(myFile);
              bis = new BufferedInputStream(fis);
              bis.read(mybytearray,0,mybytearray.length);
              os = sock.getOutputStream();
              System.out.println("Sending " + FILE_TO_SEND + "(" + mybytearray.length + " bytes)");
              os.write(mybytearray,0,mybytearray.length);
              os.flush();
              System.out.println("Done.");
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
                    System.out.println("Total # of pieces file is divided into: " + partCounter);
                }
            }
        }
    }
    
    public static void mergeFiles(List<File> files, File into) throws IOException {
        try (BufferedOutputStream mergingStream = new BufferedOutputStream(new FileOutputStream(into))) {
            for (File f : files) {
                Files.copy(f.toPath(), mergingStream);
            }
        }
    }

}
