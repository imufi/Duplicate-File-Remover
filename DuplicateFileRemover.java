import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class DuplicateFileRemover extends Application {

    // Define a logger for logging messages
    private static final Logger logger = Logger.getLogger(DuplicateFileRemover.class.getName());

    // Main method to start the program
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Set the title of the primary stage
        primaryStage.setTitle("Duplicate File Remover");

        // Create a button to select the source directory
        Button selectSourceDirectoryButton = new Button("Select Source Directory");
        // Define the action to be performed when the button is clicked
        selectSourceDirectoryButton.setOnAction(e -> {
            // Create a directory chooser dialog
            DirectoryChooser directoryChooser = new DirectoryChooser();
            // Show the dialog and get the selected directory
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            // Get the absolute path of the selected source directory
            String sourceDirectoryPath = selectedDirectory.getAbsolutePath();
            // Specify the path of the review directory where duplicate files will be moved
            String reviewDirectoryPath = "/path/to/review/directory"; // Specify the review directory path here
            // Find and move duplicate files from the selected source directory to the review directory
            findAndMoveDuplicateFilesWithRetry(sourceDirectoryPath, reviewDirectoryPath);
        });

        // Create a vertical box layout to hold the button
        VBox vBox = new VBox(selectSourceDirectoryButton);
        // Create a scene with the vertical box layout
        Scene scene = new Scene(vBox, 400, 200);
        // Set the scene to the primary stage
        primaryStage.setScene(scene);
        // Display the primary stage
        primaryStage.show();
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

    // Creates a fixed thread pool with the number of threads equal to the number of available processors
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    try {
        // Walk through all files in the specified source directory path
        Files.walk(Paths.get(sourceDirectoryPath))
                // Filter to select only regular files (not directories)
                .filter(Files::isRegularFile)
                // For each regular file found, submit a task to the executor service
                .forEach(file -> executorService.submit(() -> {
                    try {
                        // Calculate the MD5 checksum for the file
                        String hash = getMD5Checksum(file);
                        // Add file processing logic here
                        // Note: You'll typically process the file in this section
                    } catch (IOException | NoSuchAlgorithmException e) {
                        // If there is an IOException or NoSuchAlgorithmException, wrap it in a RuntimeException and throw
                        throw new RuntimeException(e);
                    }
                }));
    } finally {
        // Shutdown the executor service, preventing new tasks from being submitted
        executorService.shutdown();
        // Wait for all tasks to complete or until interrupted
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
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
        // Open an input stream to read the file
        try (InputStream fis = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024]; // Create a buffer to read file data
            MessageDigest md = MessageDigest.getInstance("MD5"); // Create MD5 message digest
            int numBytesRead; //Create variable for number of bytes read
            while ((numBytesRead = fis.read(buffer)) != -1) {  // Read bytes from the FileInputStream (fis) into the buffer array
                md.update(buffer, 0, numBytesRead);  // Store the number of bytes read in the variable numBytesRead.
            }
            byte[] digest = md.digest();

            StringBuilder result = new StringBuilder(); // Build new StringBuilder
            for (byte b : digest) {

                result.append(String.format("%02x", b)); // Format result
            }
            return result.toString(); // Return result (cycle)

        }

    }
}

