import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage

public class DuplicateFileRemover {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Duplicate File Remover");

        Button selectSourceDirectoryButton = new Button("Select Source Directory");
        selectSourceDirectoryButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            String sourceDirectoryPath = selectedDirectory.getAbsolutePath();
            String reviewDirectoryPath = "/path/to/review/directory"; // Specify the review directory path here
            findAndMoveDuplicateFilesWithRetry(sourceDirectoryPath, reviewDirectoryPath);
        });

        VBox vBox = new VBox(selectSourceDirectoryButton);
        Scene scene = new Scene(vBox, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

    // Define a logger for logging messages
    private static final Logger logger = Logger.getLogger(DuplicateFileRemover.class.getName());

    // Main method to start the program
    public static void main(String[] args) {

        //source
        String sourceDirectoryPath = "/path/to/source/directory"; // Specify the source directory path here
        //review
        String reviewDirectoryPath = "/path/to/review/directory"; // Specify the review directory path here

        findAndMoveDuplicateFiles(sourceDirectoryPath, reviewDirectoryPath);
    }

    
    // Method to find and move duplicate files to the review directory with retry mechanism
public static void findAndMoveDuplicateFilesWithRetry(String sourceDirectoryPath, String reviewDirectoryPath) {
    // Maximum number of retry attempts
    int maxRetries = 3;
    // Current retry attempt count
    int retryCount = 0;

    // Flag to indicate if traversal and processing is successful
    boolean traversalSuccessful = false;

    while (!traversalSuccessful && retryCount < maxRetries) {
        try {
            // Attempt to traverse the directory and find duplicate files
            findAndMoveDuplicateFiles(sourceDirectoryPath, reviewDirectoryPath);
            // Set the flag to indicate successful traversal and processing
            traversalSuccessful = true;
        } catch (IOException e) {
            // Log error if there is an issue traversing the source directory
            logger.log(Level.SEVERE, "Error traversing directory: " + sourceDirectoryPath, e);
            // Increment the retry count
            retryCount++;
            // Log the retry attempt
            logger.log(Level.INFO, "Retrying traversal... Attempt " + retryCount + " out of " + maxRetries);
        }
    }

    // If traversal is still unsuccessful after maximum retries, log an error message
    if (!traversalSuccessful) {
        logger.log(Level.SEVERE, "Failed to traverse directory after " + maxRetries + " attempts.");
    }
}
    // Method to traverse files and process them concurrently
public static void traverseAndProcessFilesConcurrently(String sourceDirectoryPath, String reviewDirectoryPath)
        throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    try {
        Files.walk(Paths.get(sourceDirectoryPath))
                .filter(Files::isRegularFile)
                .forEach(file -> executorService.submit(() -> {
                    try {
                        String hash = getMD5Checksum(file);
                        // Add file processing logic here
                    } catch (IOException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }));
    } finally {
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }
}


    // Method to find and move duplicate files to the review directory
    public static void findAndMoveDuplicateFiles(String sourceDirectoryPath, String reviewDirectoryPath) {
        // Map to store MD5 hashes of files as keys and corresponding file paths as values
        Map<String, List<String>> filesMap = new HashMap<>();

        try {
            // Traverse through all files in the source directory and its subdirectories
            Files.walk(Paths.get(sourceDirectoryPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            // Compute MD5 hash of the file
                            String hash = getMD5Checksum(file);
                            // If the hash is not in the map, add it with an empty list
                            if (!filesMap.containsKey(hash)) {
                                filesMap.put(hash, new ArrayList<>());
                            }
                            // Add the file path to the list associated with the hash
                            filesMap.get(hash).add(file.toString());
                        } catch (IOException | NoSuchAlgorithmException e) {
                            // Log error if there is an issue processing the file
                            logger.log(Level.SEVERE, "Error processing file: " + file, e);
                        }
                    });

            // Iterate over the map to find and move duplicate files to the review directory
            for (String hash : filesMap.keySet()) {
                List<String> fileList = filesMap.get(hash);
                // If there are more than one file with the same hash, it's a duplicate
                if (fileList.size() > 1) {
                    // Log message indicating duplicate files are found
                    logger.info("Duplicate files found: ");
                    // Iterate over duplicate files and move them to the review directory
                    for (int i = 1; i < fileList.size(); i++) {
                        // Log the path of the duplicate file
                        logger.info(fileList.get(i));
                        try {
                            // Move the duplicate file to the review directory
                            Path sourceFilePath = Paths.get(fileList.get(i));
                            Path targetFilePath = Paths.get(reviewDirectoryPath, sourceFilePath.getFileName().toString());
                            Files.move(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                            // Log message indicating the file is moved to the review directory
                            logger.info("File moved to review directory: " + targetFilePath);
                        } catch (IOException e) {
                            // Log error if there is an issue moving the file
                            logger.log(Level.SEVERE, "Error moving file: " + fileList.get(i), e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Log error if there is an issue traversing the source directory
            logger.log(Level.SEVERE, "Error traversing directory: " + sourceDirectoryPath, e);
        }
        
    }
    

    // Method to compute MD5 hash of a file
    public static String getMD5Checksum(Path file) throws IOException, NoSuchAlgorithmException {
        try (InputStream fis = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024];
            MessageDigest md = MessageDigest.getInstance("MD5");
            int numBytesRead;
            while ((numBytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, numBytesRead);
            }
            byte[] digest = md.digest();
            
            StringBuilder result = new StringBuilder();
            for (byte b : digest) {
                
                result.append(String.format("%02x", b));
            }
            return result.toString();
            
        }
        
    }
    
}
