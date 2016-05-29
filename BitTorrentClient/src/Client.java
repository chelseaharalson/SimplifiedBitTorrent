import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author chelsea metcalf
 */
public class Client {
    //static String textFileList = "fileNameListDOWNLOADED.txt";
    //static String FILE_TO_RECEIVE = "flowerimageDOWNLOADED.jpg";
    static int FILE_SIZE = 6022386;
    static ArrayList<String> fileNeededList = new ArrayList<String>();
    static ArrayList<File> downloadedList = new ArrayList<File>();
    
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
          fileNeededList = neededList("fileNameList.txt");
          //System.out.println(fileNeededList);
          //receive(sock);
          receiveFILES(sock);
        }
        finally {
          if (fos != null) fos.close();
          if (bos != null) bos.close();
          if (sock != null) sock.close();
        }
        
        //mergeFiles(fileList, new File("FileChunks/merge.jpg"));
    }
    
    public static void receiveFILES(Socket socket) throws IOException {
        while (true) {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            try {
                // get number of files being received
                int numFiles = dis.readInt();
                //byte[] buf = new byte[FILE_SIZE];
                // read all files
                for (int i = 0; i < numFiles; i++) {
                    String filename = dis.readUTF();
                    //System.out.println("Receiving " + filename);
                    long size = dis.readLong();
                    byte[] buf = new byte[(int)size];
                    //File file = new File("FileChunks/" + filename);               
                    FileOutputStream fos = new FileOutputStream("FileChunks/" + filename);
                    long total = 0;
                    int count = 0;       
                    while ((total < size) && ((count = dis.read(buf, 0, (int) Math.min(buf.length, size - total))) > 0)) {
                        fos.write(buf, 0, count);
                        total += count;
                    }
                    fos.close();
                    System.out.println("Received File: " + filename + " (" + size + " bytes)");
                }
            }
            catch (EOFException e) {
                //e.printStackTrace();
            }
            //dis.close();
        }
    }
    
    public static void receive(Socket socket){
        try {
            DataInputStream dataIS = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dataOS = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            // read the number of files from the server
            int numOfFiles = dataIS.readInt();
            downloadedList = new ArrayList<File>(numOfFiles);
            
            /*int n = 0;
            byte[] buff = new byte[FILE_SIZE];
            n = dataIS.read(buff, 0, (int)Math.min(buff.length, fileSize));
            while ( (fileSize > 0) && (n != -1) ) {
              FOS.write(buff,0,n);
              fileSize -= n;
            }
            FOS.close();*/
            
            
            //ArrayList<File>fileList = new ArrayList<File>(numOfFiles);
            System.out.println("Number of files to be received: " + numOfFiles);
            // read filenames and add files to arraylist
            for (int i = 0; i < numOfFiles; i++) {
                File file = new File(dataIS.readUTF());
                downloadedList.add(file);
            }
            
            for (int i = 0; i < numOfFiles; i++) {
                long fileSize = dataIS.readLong();
                System.out.println("FILESIZE: " + fileSize);
            }

            int n = 0;
            byte[] buff = new byte[FILE_SIZE];

            // receive files
            for (int i = 0; i < downloadedList.size(); i++) {
                long fileSizeFromServer = downloadedList.get(i).length();
                System.out.println("FILE SIZE FROM SERVER: " + fileSizeFromServer);
                System.out.println("Receiving File: " + downloadedList.get(i).getName());
                FileOutputStream fos = new FileOutputStream("FileChunks/" + downloadedList.get(i).getName());
                System.out.println(n);
                while ((n = dataIS.read(buff)) != -1 && n != 3 ) {
                  fos.write(buff, 0, n);
                  //System.out.println("n: " + n);
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