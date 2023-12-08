package com.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Ignore this class. It's a hackjob of a client to automate testing. This one searches for files that FileClient1 made
 */

public class FileClient2 {


    private static final String BASE_URL = "http://localhost:8080/api/host_info";
    private static final String SHARED_FILES_DIRECTORY = "./springboot/src/main/java/momSharedFiles";
    private static final String LOCAL_FILES_DIRECTORY = "./springboot/src/main/java/momLocalFiles";
    private static String clientUsername = "mom";
    private static String clientIpAddress;
    private static ServerSocket serverSocket;
    private static int clientPort;

    public static void main(String[] args) throws IOException {
        serverSocket = new ServerSocket(0);
        clientIpAddress = InetAddress.getLocalHost().getHostAddress();
        clientPort = serverSocket.getLocalPort();

        // Start the file transfer server in a separate thread
        new Thread(() -> startFileTransferServer()).start();

        // Fake user input for searching files
        fakeSearchFile("notpresent");
        fakeSearchFile("2.txt");
        fakeSearchFile("1.txt");
    }

    // Other methods remain unchanged

    private static void fakeSearchFile(String filename) {
        System.out.println("\nSearching for file: " + filename);

        String url = BASE_URL + "/searchFile";
        String queryParams = String.format("?filename=%s&clientUsername=%s", filename, clientUsername);
        String fullUrl = url + queryParams;

        try {
            String response = sendGetRequest(fullUrl);

            if ("true".equalsIgnoreCase(response)) {
                System.out.println("The file \"" + filename + "\" is available to Mom.");

                // Pretend Mom confirms the download
                System.out.println("Mom confirms the download.");
                initiateFileDownload(filename);
            } else {
                System.out.println("File \"" + filename + "\" not found for Mom.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initiateFileDownload(String filename) {
        try {
            // Retrieve Client-B information from the server
            HostInfo hostInfo = confirmDownload(filename);

            // Connect to Client-B
            try (Socket fileOwnerSocket = new Socket(hostInfo.getHostIp(), hostInfo.getHostPort());
                 DataInputStream fileOwnerInputStream = new DataInputStream(fileOwnerSocket.getInputStream());
                 DataOutputStream fileOwnerOutputStream = new DataOutputStream(fileOwnerSocket.getOutputStream())
            ) {
                // Request by file name
                fileOwnerOutputStream.writeUTF(filename);

                // Receive  from Client-B
                int fileSize = fileOwnerInputStream.readInt();
                byte[] fileContent = new byte[fileSize];
                fileOwnerInputStream.readFully(fileContent);

                saveFileLocally(filename, fileContent);

                System.out.println("File download complete.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveFileLocally(String filename, byte[] fileContent) {
        try {
            Path directoryPath = Path.of(LOCAL_FILES_DIRECTORY);
            if (Files.notExists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // path where file will be saved
            Path targetPath = Path.of(LOCAL_FILES_DIRECTORY, filename);

            Files.write(targetPath, fileContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("File received and saved to: " + targetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startFileTransferServer() {
        try {
            int fileTransferPort = serverSocket.getLocalPort();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\nAccepted connection from: " + clientSocket.getInetAddress());

                // Handle the file transfer request in a new thread
                new Thread(() -> handleFileTransferRequest(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleFileTransferRequest(Socket clientSocket) {
        try (
                DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            // receive file name request
            String filename = dataInputStream.readUTF();
            System.out.println("Incoming file transfer request for: " + filename);

            Path filePath = Path.of(SHARED_FILES_DIRECTORY, filename);

            if (Files.exists(filePath)) {
                // Send the file content to the requesting client
                sendFile(dataOutputStream, filePath);
                System.out.println("File transfer complete. Choose your option 1-4:");
            } else {
                System.out.println("File not found in sharedFiles directory.");
                // Send empty string if file not found
                dataOutputStream.writeUTF("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(DataOutputStream dataOutputStream, Path filePath) {
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            dataOutputStream.writeInt(fileContent.length);

            // Send the file content to the client
            dataOutputStream.write(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFile() {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the file name to delete: ");
            String filenameToDelete = scanner.nextLine();

            String url = BASE_URL + "/removeFile/" + filenameToDelete + "?clientUsername=" + clientUsername;

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("File deleted successfully.");
            } else if (response.statusCode() == 404) {
                System.out.println("File not found.");
            } else if (response.statusCode() == 403) {
                System.out.println("You don't have permission to delete this file.");
            } else {
                System.out.println("Unexpected response: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void getClientUsername() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        //no spaces allowed in usernames
        clientUsername = scanner.nextLine().replaceAll("\\s+","");
    }

    private static void registerFile() {
        try {
            String url = BASE_URL + "/registerFile";

            // Prompt the user for the file name
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the file name: ");
            String filename = scanner.nextLine();
            // Check if the file exists
            if (!fileExists(filename)) {
                System.out.println("File not found in sharedFiles directory. " +
                        "Shared files must be located in sharedFilesDirectory");
                return;
            }

            // Prompt the user for a list of usernames to share the file with
            System.out.print("Enter usernames to share the file with (comma-separated): ");
            String sharedWithInput = scanner.nextLine();
            String payload = getString(sharedWithInput, filename);
            System.out.println(payload);
            sendPostRequest(url, payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean fileExists(String filename) {
        // Check if the file exists in the ./sharedFiles directory
        Path filePath = Path.of(SHARED_FILES_DIRECTORY, filename);
        return Files.exists(filePath);
    }

    private static String getString(String sharedWithInput, String filename) {
        List<String> sharedWith = Arrays.asList(sharedWithInput.split(","));

        String formattedSharedWith = sharedWith.stream()
                .map(username -> "\"" + username + "\"")
                .collect(Collectors.joining(", "));

        // Create the file registration payload
        String payload = String.format("{\n" +
                "  \"username\": \"%s\",\n" +
                "  \"filename\": \"%s\",\n" +
                "  \"sharedWith\": [%s],\n" + // Use square brackets for the array
                "  \"hostIp\": \"%s\",\n" +
                "  \"hostPort\": %d\n" +
                "}", clientUsername, filename, formattedSharedWith, clientIpAddress, clientPort);

        return payload;
    }

    private static void searchFile() {
        try {
            String url = BASE_URL + "/searchFile";

            // Prompt the user for the file name to search
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the file name to search: ");
            String filename = scanner.nextLine();

            String queryParams = String.format("?filename=%s&clientUsername=%s", filename, clientUsername);
            String fullUrl = url + queryParams;

            String response = sendGetRequest(fullUrl);

            if ("true".equalsIgnoreCase(response)) {
                System.out.println("The file is available to you.");

                // Ask the user if they want to confirm the download
                System.out.print("Do you want to confirm the download? (yes/no): ");
                String confirmResponse = scanner.nextLine().toLowerCase();

                if ("yes".equals(confirmResponse)) {
                    initiateFileDownload(filename);
                } else {
                    System.out.println("File not downloaded");
                }
            } else {
                System.out.println("File not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HostInfo confirmDownload(String filename) {
        try {
            String url = BASE_URL + "/confirmDownload";
            String queryParams = String.format("?filename=%s&clientUsername=%s", filename, clientUsername);
            String fullUrl = url + queryParams;

            String response = sendGetRequest(fullUrl);

            if (response != null) {
                // Parse the JSON response into HostInfo
                ObjectMapper objectMapper = new ObjectMapper();
                HostInfo hostInfo = objectMapper.readValue(response, HostInfo.class);

                return hostInfo;
            } else {
                System.out.println("Error confirming download. Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void sendPostRequest(String url, String payload) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("POST Response Code: " + response.statusCode());
        System.out.println("POST Response Body: " + response.body());
    }

    private static String sendGetRequest(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("GET Response: " + response.statusCode() + " " + response.body());

        return response.body();
    }
}
