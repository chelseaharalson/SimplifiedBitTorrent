import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author chelsea metcalf
 */
public class Client {
    //static String textFileList = "fileNameListDOWNLOADED.txt";
    //static String FILE_TO_RECEIVE = "flowerimageDOWNLOADED.jpg";
    static int FILE_SIZE = 6022386;
    
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2) {
            System.err.println("Usage: java Client <host name> <port number>");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
 
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        try {
          sock = new Socket(hostName, portNumber);
          System.out.println("Connecting...");
          receive(sock);
        }
        finally {
          if (fos != null) fos.close();
          if (bos != null) bos.close();
          if (sock != null) sock.close();
        }
        
        //mergeFiles(fileList, new File("FileChunks/merge.jpg"));
    }
    
    public static void receive(Socket socket){
        try {
            DataInputStream dataIS = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            // read the number of files from the server
            int numOfFiles = dataIS.readInt();
            ArrayList<File>fileList = new ArrayList<File>(numOfFiles);
            System.out.println("Number of files to be received: " + numOfFiles);
            // read filenames and add files to arraylist
            for(int i = 0; i< numOfFiles; i++) {
                File file = new File(dataIS.readUTF());
                fileList.add(file);
            }
            int n = 0;
            byte[] buff = new byte[FILE_SIZE];

            // receive files
            for(int i = 0; i < fileList.size(); i++) {
                System.out.println("Receiving File: " + fileList.get(i).getName());
                FileOutputStream fos = new FileOutputStream("FileChunks/" + fileList.get(i).getName());
                while((n = dataIS.read(buff)) != -1 && n!=3 ) {
                  fos.write(buff,0,n);
                  fos.flush();
                }
                fos.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
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