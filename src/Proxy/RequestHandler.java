package Proxy;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
//import java.nio.charset.StandardCharsets;


// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {


    Socket clientSocket;

    InputStream inFromClient;

    OutputStream outToClient;

    byte[] request = new byte[1024];


    private final ProxyServer server;


    public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {


        this.clientSocket = clientSocket;


        this.server = proxyServer;

        System.out.println("Instance hashcode in RequestHandler: " + server.hashCode());
        System.out.println("Cache size in RequestHandler: " + server.cache.size());

        try {
            clientSocket.setSoTimeout(5000);
            inFromClient = clientSocket.getInputStream();
            outToClient = clientSocket.getOutputStream();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override

    public void run() {

        try {
            // Read client's request
            int bytesRead = inFromClient.read(request);
            if (bytesRead > 0) {
                String requestData = new String(request, StandardCharsets.UTF_8);
//                System.out.println("Request: " + requestData);

                // Logg all requests
//                server.writeLog(clientSocket.getInetAddress().getHostAddress() + " " + requestData.split("\n")[0]);

                // Only process GET requests
                if (requestData.startsWith("GET")) {
                    System.out.println("Request: " + requestData);
                    String url = requestData.split("\n")[0].split(" ")[1];

                    // Log GET request
                    server.writeLog(clientSocket.getInetAddress().getHostAddress() + " GET " + url);

                    String fileName = server.getCache(url);
                    // print out the cache file name
                    System.out.println("Cache file name for: " + url + " is " + fileName);
                    if (fileName != null) {
                        System.out.println("Cache found: Serving " + url + " from cache.");
                        server.writeLog("Cache found for URL: " + url);

                        sendCachedInfoToClient(fileName);
                    } else {
                        System.out.println("Cache not found: Fetching " + url + " from the web server.");
                        server.writeLog("Cache not found for URL: " + url);

                        proxyServertoClient(request);
                    }
                } else {
                    System.out.println("Received a non-GET request: Ignoring.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private void proxyServertoClient(byte[] clientRequest) {

        FileOutputStream fileWriter = null;
        Socket toWebServerSocket = null;
        InputStream inFromServer;
        OutputStream outToServer;

        // Create Buffered output stream to write to cached copy of file
//        String fileName = "cached/" + generateRandomFileName() + ".dat";

        // to handle binary content, byte is used
        byte[] serverReply = new byte[4096];

        try {
            // Create a socket to connect to the web server (default port 80)
            String url = new String(clientRequest).split(" ")[1];
            String host = new URL(url).getHost();
            toWebServerSocket = new Socket(host, 80);
            inFromServer = toWebServerSocket.getInputStream();
            outToServer = toWebServerSocket.getOutputStream();

            // Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
            outToServer.write(clientRequest);
            outToServer.flush();

            // Generate a hash of the URL for the cache filename
            String urlHash = generateHashFromUrl(url);
            String fileName = "cached/" + urlHash + ".dat";

            // Prepare to cache the response
            fileWriter = new FileOutputStream(fileName);

            // Use a while loop to read all responses from web server and send back to client
            int bytesRead;
            while ((bytesRead = inFromServer.read(serverReply)) != -1) {
                outToClient.write(serverReply, 0, bytesRead);
                outToClient.flush();

                // Write the web server's response to a cache file
                fileWriter.write(serverReply, 0, bytesRead);
            }

            // Put the request URL and cache file name to the cache Map
            server.putCache(url, fileName);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // (5) close file, and sockets.
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
                if (toWebServerSocket != null) {
                    toWebServerSocket.close();
                }
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private String generateHashFromUrl(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null; // Handle this appropriately, perhaps with a fallback
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    // Sends the cached content stored in the cache file to the client
    private void sendCachedInfoToClient(String fileName) {
        File file = new File(fileName);

        // Check if the cache file exists
        if (!file.exists()) {
            System.err.println("Cache file does not exist: " + file.getAbsolutePath());
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            // Get the size of the file
            long fileSize = file.length();
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Read in the file and send it to the client
            while ((bytesRead = fis.read(buffer)) != -1) {
                outToClient.write(buffer, 0, bytesRead);
                outToClient.flush();
            }

            System.out.println("Sent cached file to client: " + fileName);

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException while sending cached file to client: " + file.getAbsolutePath());
            e.printStackTrace();
        } finally {
            // Close the client socket
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("IOException while closing client socket");
                e.printStackTrace();
            }
        }
    }



    // Generates a random file name
    public String generateRandomFileName() {

        String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        SecureRandom RANDOM = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10; ++i) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

}