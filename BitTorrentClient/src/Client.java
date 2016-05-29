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
    static String textFileList = "fileNameListDOWNLOADED.txt";
    static String FILE_TO_RECEIVE = "flowerimageDOWNLOADED.jpg";
    static int FILE_SIZE = 6022386;
    
    public static void main(String[] args) throws IOException, ClassNotFoundException {
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
          
          receive(sock);
          
          //List<File> fList = new ArrayList<File>();
          
          // receive files
          /*for (int i = 0; i < 43; i++) {
            byte[] byteArray  = new byte [FILE_SIZE];
            InputStream is = sock.getInputStream();
            fos = new FileOutputStream("name" + i);
            bos = new BufferedOutputStream(fos);
            bytesRead = is.read(byteArray,0,byteArray.length);
            current = bytesRead;

            do {
               bytesRead = is.read(byteArray, current, (byteArray.length-current));
               if(bytesRead >= 0) current += bytesRead;
            } while(bytesRead > -1);

            bos.write(byteArray, 0 , current);
            bos.flush();
            System.out.println("File " + "name"+i + " downloaded (" + current + " bytes read)");
          }*/
        }
        finally {
          //if (fos != null) fos.close();
          //if (bos != null) bos.close();
          //if (sock != null) sock.close();
        }
    }
    
    /*public static void saveFileChunk(int x, byte[] chunk) {
        try {
            FileOutputStream fso = new FileOutputStream("FileChunks/" + x, false);
            fso.write(chunk);
            fso.flush();
            fso.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
    
    public static void receive(Socket socket){
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            //read the number of files from the server
            int number = dis.readInt();
            ArrayList<File>files = new ArrayList<File>(number);
            System.out.println("Number of Files to be received: " +number);
            //read file names and add files to arraylist
            for(int i = 0; i< number;i++){
                File file = new File(dis.readUTF());
                files.add(file);
            }
            int n = 0;
            byte[] buf = new byte[FILE_SIZE];

            // receive files
            for(int i = 0; i < files.size(); i++){
                System.out.println("Receiving File: " + files.get(i).getName());
                FileOutputStream fos = new FileOutputStream("FileChunks/" + files.get(i).getName());
                while((n = dis.read(buf)) != -1 && n!=3 ) {
                  fos.write(buf,0,n);
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