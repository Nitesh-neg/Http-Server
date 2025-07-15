import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {

    System.out.println("Logs from your program will appear here!");

    
    try {
      ServerSocket serverSocket = new ServerSocket(4221);
    
      serverSocket.setReuseAddress(true);
    
      Socket clientSocket = serverSocket.accept(); // Wait for connection from client.


      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

      String requestLine = in.readLine();
      System.out.println("Request Line: " + requestLine);

      if (requestLine != null && requestLine.startsWith("GET")) {
                    // Extract path from: GET /path HTTP/1.1
            String[] parts = requestLine.split(" ");
            String path = parts[1];

            if (path.equals("/")) {
                  out.write("HTTP/1.1 200 OK\r\n\r\n");
            }else if (path.startsWith("/echo/")) {
                 String [] echo_break = path.split("/");
                 String content = echo_break[2];
                 out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + content.length() + "\r\n\r\n"+ content);
            }
            else {
                  out.write("HTTP/1.1 404 Not Found\r\n\r\n");
                }
            } else {
                  // not valid request
                  out.write("HTTP/1.1 400 Bad Request\r\n\r\n");
                }

                out.flush();
                clientSocket.close(); // Close connection after responding
            } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

