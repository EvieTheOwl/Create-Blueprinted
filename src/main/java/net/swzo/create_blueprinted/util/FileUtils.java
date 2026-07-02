package net.swzo.create_blueprinted.util;

import net.swzo.create_blueprinted.CreateBlueprinted;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class FileUtils {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp" );

    public static Path saveImage(File directory, String fileName, String extension, byte[] imageByteArray) throws IOException, IllegalArgumentException {
        String fileNameAndExt = fileName + "." + extension;
        String errorPrefix = "Failed to write image: " + fileNameAndExt + ". ";

        if (imageByteArray == null || imageByteArray.length == 0)
            throw new IllegalArgumentException(errorPrefix + "Image byte array cannot be null or empty.");
        if (fileName == null || fileName.isBlank())
            throw new IllegalArgumentException(errorPrefix + "File name cannot be null or blank");
        if (!IMAGE_EXTENSIONS.contains(extension))
            throw new IllegalArgumentException(errorPrefix + "Unrecognised image file extension");
        if (extension.contains("."))
            extension = extension.replace(".", "");

        File outputFile = new File(directory, fileName + "." + extension);
        File tempFile = new File(directory, fileName + ".tmp");

        try {
            Files.write(tempFile.toPath(), imageByteArray);
            if (!tempFile.renameTo(outputFile))
                throw new IOException("Failed to rename temporary image file: " + tempFile.getAbsolutePath());

            return outputFile.toPath();
        } catch (IOException e) {
            if (!tempFile.delete())
                CreateBlueprinted.LOGGER.error("Failed to delete temporary image file: {}", tempFile.getAbsolutePath());
            throw e;
        }
    }
}
