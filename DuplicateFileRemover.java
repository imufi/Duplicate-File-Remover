import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;

public class DuplicateFileRemover {

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
