package com.revnomix.revseed.integration.utility;



import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import com.revnomix.revseed.integration.exception.RevseedException;

@Configuration
public class FileService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public File moveFileToDirectory(File file, File directory) {
        Assert.notNull(file, "File cannot be null");
        Assert.notNull(directory, "Directory cannot be null");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File movedFile = new File(directory, file.getName());
        if (movedFile.exists()) {
            movedFile.delete();
        }

        logger.info("Moving File: " + file + " to Directory: " + directory);

        try {
        	FileUtils.copyFileToDirectory(file, directory, true);
        	FileUtils.deleteQuietly(file);
        	
        } catch (IOException ioe) {
            throw new RevseedException("Unable to copy file: " + file + " to directory: " + directory, ioe);
        }

        return new File(directory, file.getName());
    }

    public File copyFileToDirectory(File file, File directory) {
        Assert.notNull(file, "File cannot be null");
        Assert.notNull(directory, "Directory cannot be null");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File copyFile = new File(directory, file.getName());
        if (copyFile.exists()) {
            copyFile.delete();
        }

        logger.info("Copying File: " + file + " to Directory: " + directory);

        try {
            FileUtils.copyFileToDirectory(file, directory, true);
        } catch (IOException ioe) {
            throw new RevseedException("Unable to copy file: " + file + " to directory: " + directory, ioe);
        }

        return new File(directory, file.getName());
    }

    public boolean deleteDirectory(File directory) {
        logger.info("Deleting Directory: " + directory);

        try {
            FileUtils.deleteDirectory(directory);
            return true;
        } catch (IOException e) {
            logger.warn("An error occurred while trying to delete the directory : " + directory.getAbsolutePath(), e);
            return false;
        }
    }

    public void uploadFile(MultipartFile multipartFile, File directory) {
        BufferedOutputStream stream = null;
        try {
            // Read the file to a temp location
            File tempFileLocation = new File(FileUtils.getTempDirectory(), multipartFile.getOriginalFilename());
            byte[] bytes = multipartFile.getBytes();
            stream = new BufferedOutputStream(new FileOutputStream(tempFileLocation));
            stream.write(bytes);
            stream.close();
            // Once the file is uploaded, move it to the directory
            FileUtils.moveFileToDirectory(tempFileLocation, directory, true);
        } catch (Exception e) {
            throw new RevseedException("Unable to upload file: " + multipartFile.getName(), e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
