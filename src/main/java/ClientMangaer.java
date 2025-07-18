import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class ClientMangaer implements Runnable {
    private final Socket clientSocket;
    private boolean keepAlive = true;

    public ClientMangaer(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            while (keepAlive) {
                handleRequest(in, out);
            }
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
        if (requestLine == null || requestLine.isEmpty()) {
            keepAlive = false;
            return;
        }

        System.out.println("Request Line: " + requestLine);

        // Read and store headers
        Map<String, String> headers = new HashMap<>();
        int contentLength = 0;
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            int sep = line.indexOf(":");
            if (sep != -1) {
                String key = line.substring(0, sep).trim().toLowerCase();
                String value = line.substring(sep + 1).trim();
                headers.put(key, value);
                if (key.equals("content-length")) {
                    contentLength = Integer.parseInt(value);
                }
            }
        }

        if (requestLine.startsWith("POST")) {
            String path = requestLine.split(" ")[1];
            if (path.startsWith("/files/")) {
                String targetFileName = path.substring("/files/".length()).trim();
                String fullFilePath = Main.map.get("dir") + targetFileName;
                Path pathToFile = Paths.get(fullFilePath);

                char[] body = new char[contentLength];
                int read = in.read(body, 0, contentLength);
                if (read != contentLength) {
                    respondWithBadRequest(out);
                    return;
                }

                Files.write(pathToFile, new String(body).getBytes());
                out.write("HTTP/1.1 201 Created\r\n\r\n");
                out.flush();
            } else {
                respondWithNotFound(out);
                out.flush();
            }

        } else if (requestLine.startsWith("GET")) {
            String path = requestLine.split(" ")[1];

            if (path.equals("/")) {
                respondWithOk(out);
            } else if (path.startsWith("/echo/")) {
                respondWithEcho(out, path.substring(6), headers);
            } else if (path.startsWith("/user-agent")) {
                respondWithUserAgent(out, headers);
            } else if (path.startsWith("/files/")) {
                respondWithFile(path, out);
            } else {
                respondWithNotFound(out);
            }

            out.flush();
        } else {
            respondWithBadRequest(out);
            out.flush();
        }
    }

    private void respondWithOk(BufferedWriter out) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n\r\n");
    }

    private void respondWithEcho(BufferedWriter out, String content, Map<String, String> headers) throws IOException {
        String acceptEncoding = headers.getOrDefault("accept-encoding", "");

        if (acceptEncoding.contains("gzip")) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteStream)) {
                gzipOut.write(content.getBytes());
            }
            byte[] compressedBytes = byteStream.toByteArray();

            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Encoding: gzip\r\n");
            out.write("Content-Type: text/plain\r\n");
            out.write("Content-Length: " + compressedBytes.length + "\r\n\r\n");
            out.flush();
            clientSocket.getOutputStream().write(compressedBytes);
            clientSocket.getOutputStream().flush();
        } else {
            out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "
                    + content.length() + "\r\n\r\n" + content);
        }
    }

    private void respondWithUserAgent(BufferedWriter out, Map<String, String> headers) throws IOException {
        String userAgent = headers.getOrDefault("user-agent", "");
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
        String filePath = Main.map.get("dir") + path.substring("/files/".length());
        File file = new File(filePath);
        if (file.exists()) {
            byte[] content = Files.readAllBytes(file.toPath());
            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Content-Type: application/octet-stream\r\n");
            out.write("Content-Length: " + content.length + "\r\n\r\n");
            out.flush();
            clientSocket.getOutputStream().write(content);
            clientSocket.getOutputStream().flush();
        } else {
            clientSocket.getOutputStream().write(
                    "HTTP/1.1 404 Not Found\r\n\r\n".getBytes()
            );
            clientSocket.getOutputStream().flush();
        }
    }
}
