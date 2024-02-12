import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public class DuplicateFileRemover {

    public static void main(String[] args) {
        String directoryPath = "/path/to/your/directory"; // Specify the directory path here
        findAndDeleteDuplicateFiles(directoryPath);
    }

    // Method to find and delete duplicate files in the specified directory
    public static void findAndDeleteDuplicateFiles(String directoryPath) {
        // Map to store MD5 hashes of files as keys and corresponding file paths as values
        Map<String, List<String>> filesMap = new HashMap<>();

        try {
            // Traverse through all files in the directory and its subdirectories
            Files.walk(Paths.get(directoryPath))
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
                            e.printStackTrace();
                        }
                    });

            // Iterate over the map to find and delete duplicate files
            for (String hash : filesMap.keySet()) {
                List<String> fileList = filesMap.get(hash);
                // If there are more than one file with the same hash, it's a duplicate
                if (fileList.size() > 1) {
                    System.out.println("Duplicate files found: ");
                    // Iterate over duplicate files and delete them
                    for (int i = 1; i < fileList.size(); i++) {
                        System.out.println(fileList.get(i));
                        try {
                            // Delete the duplicate file
                            Files.deleteIfExists(Paths.get(fileList.get(i)));
                            System.out.println("File deleted: " + fileList.get(i));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
