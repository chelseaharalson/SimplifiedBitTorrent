import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.List;

/**
 *
 * @author chelsea metcalf
 */
public class Client {
    static String FILE_TO_RECEIVE = "flowerimageDOWNLOADED.jpg";
    static int FILE_SIZE = 6022386;
    
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java Client <host name> <port number>");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
 
        int bytesRead;
        int current = 0;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        try {
          sock = new Socket(hostName, portNumber);
          System.out.println("Connecting...");

          // receive file
          byte [] mybytearray  = new byte [FILE_SIZE];
          InputStream is = sock.getInputStream();
          fos = new FileOutputStream(FILE_TO_RECEIVE);
          bos = new BufferedOutputStream(fos);
          bytesRead = is.read(mybytearray,0,mybytearray.length);
          current = bytesRead;

          do {
             bytesRead = is.read(mybytearray, current, (mybytearray.length-current));
             if(bytesRead >= 0) current += bytesRead;
          } while(bytesRead > -1);

          bos.write(mybytearray, 0 , current);
          bos.flush();
          System.out.println("File " + FILE_TO_RECEIVE
              + " downloaded (" + current + " bytes read)");
        }
        finally {
          if (fos != null) fos.close();
          if (bos != null) bos.close();
          if (sock != null) sock.close();
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