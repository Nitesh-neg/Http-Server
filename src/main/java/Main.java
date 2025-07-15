import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static Map<String,String> map=new HashMap();
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        try{
             for(int i=0;i<args.length;i++)

             if(args[i].equals("--directory")){

                  map.put("dir",args[i+1]);
              }

        } catch (Exception e) {

           System.out.println("args erro " + e.getMessage());
        }

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept client connection
                new Thread(new ClientMangaer(clientSocket)).start(); // Handle in a new thread
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
