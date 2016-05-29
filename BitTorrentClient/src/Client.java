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
    static int FILE_SIZE = 6022386;
    static ArrayList<String> fileNeededList = new ArrayList<String>();
    static List<File> downloadedList = new ArrayList<File>();
    
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
          receiveFILES(sock);
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
                //byte[] buf = new byte[FILE_SIZE];
                // read all the files
                for (int i = 0; i < numOfFiles; i++) {
                    String filename = dis.readUTF();
                    long size = dis.readLong();
                    byte[] buff = new byte[(int)size];
                    File file = new File(filename);
                    //System.out.println("Added: " + filename);
                    downloadedList.add(file);
                    FileOutputStream fos = new FileOutputStream("FileChunks/" + filename);
                    long total = 0;
                    int count = 0;       
                    while ((total < size) && ((count = dis.read(buff, 0, (int) Math.min(buff.length, size - total))) > 0)) {
                        fos.write(buff, 0, count);
                        total += count;
                    }
                    fos.close();
                    System.out.println("Received File: " + filename + " (" + size + " bytes)");
                }
            }
            catch (EOFException e) {
                break;
                //dis.close();
                //e.printStackTrace();
            }
            //dis.close();
        }
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