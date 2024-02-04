package Proxy;

import java.io.BufferedWriter;
import java.io.*;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;




public class ProxyServer {

    //cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
    Map<String, String> cache;

    ServerSocket proxySocket;

    String logFileName = "proxy.log";

    public static void main(String[] args) {
        new ProxyServer().startServer(Integer.parseInt(args[0]));
    }

    private final ExecutorService executorService;

    public ProxyServer() {
        // Initialize the ExecutorService
        executorService = Executors.newCachedThreadPool();

        // Initialize the cache map
        cache = new ConcurrentHashMap<>();

        // Read cache from disk
        try (BufferedReader reader = new BufferedReader(new FileReader("cache.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    cache.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading cache from file: " + e.getMessage());
        }

        // Log the size of the cache and some sample entries after loading
        System.out.println("Cache loaded. Size: " + cache.size());
        cache.entrySet().stream().limit(20).forEach(entry ->
                System.out.println("Cache entry: URL = " + entry.getKey() + ", File = " + entry.getValue())
        );

        System.out.println("Instance hashcode in ProxyServer: " + this.hashCode());
        System.out.println("Cache size in ProxyServer: " + cache.size());


        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down the proxy server...");

            try {
                if (proxySocket != null && !proxySocket.isClosed()) {
                    proxySocket.close();
                    System.out.println("Proxy socket closed.");
                }
            } catch (IOException e) {
                System.err.println("Error closing proxy socket: " + e.getMessage());
            }

            shutdownServer();
        }));
    }

    private void shutdownServer() {
        try {
            File cacheFile = new File("cache.txt");
            Map<String, String> existingCache = new ConcurrentHashMap<>();

            // Read existing cache from disk into existingCache
            if (cacheFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length == 2) {
                            existingCache.put(parts[0], parts[1]);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading existing cache from file: " + e.getMessage());
                }
            }

            // Write new or updated entries to cache file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile, true))) { // true for appending
                for (Map.Entry<String, String> entry : cache.entrySet()) {
                    if (!entry.getValue().equals(existingCache.get(entry.getKey()))) {
                        writer.write(entry.getKey() + "\t" + entry.getValue());
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error writing cache to file: " + e.getMessage());
            }

            System.out.println("Shutting down the ExecutorService...");
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate");
                }
            }

            // Close the server socket
            if (proxySocket != null && !proxySocket.isClosed()) {
                proxySocket.close();
                System.out.println("Proxy socket closed.");
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error closing proxy socket: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error shutting down the server: " + e.getMessage());
        }
    }


    void startServer(int proxyPort) {

//        cache = new ConcurrentHashMap<>();

        // create the directory to store cached files.
        File cacheDir = new File("cached");
        if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
            boolean isDirCreated = cacheDir.mkdirs();
            if (!isDirCreated) {
                System.err.println("Failed to create directory for cache. Please check permissions or disk space.");
                return;
            }
        }

        // Create a thread pool to handle multiple requests
        ExecutorService executorService = Executors.newCachedThreadPool();

        try {
            proxySocket = new ServerSocket(proxyPort);
            System.out.println("Proxy Server is listening on port: " + proxyPort);

            while (!proxySocket.isClosed()) {
                try {
                    Socket clientSocket = proxySocket.accept();
                    RequestHandler requestHandler = new RequestHandler(clientSocket, this);
                    executorService.execute(requestHandler);
                } catch (SocketException se) {
                    System.out.println("Server socket closed.");
                }
            }
        } catch (IOException e) {
            System.out.println("Error in creating or accepting socket");
            e.printStackTrace();
        } finally {
            // shutdown the executor service when done
            executorService.shutdown();
        }

    }

    public String getCache(String hashcode) {
        return cache.get(hashcode);
    }

    public void putCache(String hashcode, String fileName) {
        cache.put(hashcode, fileName);
    }

    // Define a lock to avoid concurrent write to log file
    private final Lock logFileLock = new ReentrantLock();

    public synchronized void writeLog(String info) {

        // Lock to avoid concurrent write to log file
        logFileLock.lock();
        try {
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            String logEntry = timeStamp + " " + info + "\n";

            Files.write(Paths.get(logFileName), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Error writing to log file");
            e.printStackTrace();
        } finally {
            logFileLock.unlock(); // Release the lock
        }
    }

}
