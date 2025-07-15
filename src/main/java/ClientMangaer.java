import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientMangaer implements Runnable {
    private final Socket clientSocket;

    public ClientMangaer(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            handleRequest(in, out);
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleRequest(BufferedReader in, BufferedWriter out) throws IOException {
        String requestLine = in.readLine();
        System.out.println("Request Line: " + requestLine);

        // if (requestLine == null || !requestLine.startsWith("GET")) {
        //     respondWithBadRequest(out);
        //     return;
             if( requestLine.startsWith("POST")){

                String path = requestLine.split(" ")[1];
                if(path.startsWith("/files/")){

                    String targetFileName = path.substring("/files/".length()).trim();
                    String fullFilePath = Main.map.get("dir") + targetFileName;
                    Path path_to_file = Paths.get(fullFilePath);
                    int contentLength = 0;
                    String line;

                    while (!(line = in.readLine()).isEmpty()) {
                            if (line.toLowerCase().startsWith("content-length:")) {
                                contentLength = Integer.parseInt(line.split(":")[1].trim());
                            }
                    }

                    char[] body = new char[contentLength];
                    in.read(body, 0, contentLength); 
                    Files.write(path_to_file, new String(body).getBytes());
                    out.write("HTTP/1.1 201 Created\r\n\r\n");
                    out.flush();
                }
        }else if(requestLine.startsWith("GET")){

                String path = requestLine.split(" ")[1];

                if (path.equals("/")) {
                    respondWithOk(out);
                } else if (path.startsWith("/echo/")) {
                    respondWithEcho(in,out, path.substring(6));
                } else if (path.startsWith("/user-agent")) {
                    respondWithUserAgent(in, out);
                } else if(path.startsWith("/files")){
                    respondWithFile(path,out);
                }
                else {
                    respondWithNotFound(out);
                }

                out.flush();
            }else{
                respondWithBadRequest(out);
                out.flush();
            }
        }

    private void respondWithOk(BufferedWriter out) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n\r\n");
    }

    private void respondWithEcho(BufferedReader in,BufferedWriter out, String content) throws IOException {

        String line;
        String content_Encoding = "";
        while (!(line = in.readLine()).isEmpty()) {
            if(line.startsWith("Accept-Encoding:")){
                content_Encoding = line.split(":")[1].trim();             
            }
        }
        if(content_Encoding.equals("gzip")){
            out.write("HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Type: text/plain\r\nContent-Length: "
                  + content.length() + "\r\n\r\n" + content);
            out.flush();
        }else{
            out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "
                  + content.length() + "\r\n\r\n" + content);
            out.flush();
        }
        
    }

    private void respondWithUserAgent(BufferedReader in, BufferedWriter out) throws IOException {
        String line;
        String userAgent = "";
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("User-Agent:")) {
                userAgent = line.substring("User-Agent:".length()).trim();
                break;
            }
        }

        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "
                  + userAgent.length() + "\r\n\r\n" + userAgent);
    }

    private void respondWithNotFound(BufferedWriter out) throws IOException {
        out.write("HTTP/1.1 404 Not Found\r\n\r\n");
    }

    private void respondWithBadRequest(BufferedWriter out) throws IOException {
        out.write("HTTP/1.1 400 Bad Request\r\n\r\n");
    }
    
    private void respondWithFile(String path, BufferedWriter out) throws IOException {
        String filePath = Main.map.get("dir") + path.substring(6);
        File file = new File(filePath);
          if (file.exists()) {
            byte[] content = Files.readAllBytes(file.toPath());
            out.write(
                ("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                 content.length + "\r\n\r\n" + new String(content)));
                    out.flush();
          } else {
            clientSocket.getOutputStream().write(
                "HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                out.flush();
          }
    }      
 }
