package com.company;
/*
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Duration;
import com.company.ActionTool;
import com.company.NotificationType;

/**
 * This class is used to import an XR3Player database (as .zip folder)
 *
 * @author SuperGoliath
 *
 */
public class ExportZipService{

    /** The logger. */
    private final Logger logger = Logger.getLogger(getClass().getName());

    /** The given ZIP file */
    private String zipFile;

    /** The output folder */
    private String destinationFolder;

    /** The exception. */
    private String exception;
    /**
     * This Services initialises and external Thread to Export a ZIP folder to a
     * Destination Folder
     *
     * @param zipFolder
     *        The absolute path of the ZIP folder
     * @param destinationFolder
     *        The absolutePath of the destination folder
     */
    public void exportZip(String zipFolder , String destinationFolder) {

        //-----
        this.zipFile = zipFolder;
        this.destinationFolder = destinationFolder;
        try{startExport();}
        catch (ZipException e)
        {System.out.println("Version Not found");
        System.exit(0);}
    }
    public void startExport() throws ZipException {

                //---------------------Move on Importing the Database-----------------------------------------------

                // get the zip file content
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {

                    // create output directory is not exists
                    File folder = new File(destinationFolder);
                    if (!folder.exists())
                        folder.mkdir();

                    // get the zipped file list entry
                    ZipEntry ze = zis.getNextEntry();

                    // Count entries
                    ZipFile zip = new ZipFile(zipFile);
                    double counter = 0 , total = zip.size();

                    //Start
                    for (byte[] buffer = new byte[1024]; ze != null;) {

                        String fileName = ze.getName();
                        File newFile = new File(destinationFolder + File.separator + fileName);

                        // Refresh the dataLabel text
                        System.out.println("Exporting: [ " + newFile.getName() + " ]");

                        // create all non exists folders else you will hit FileNotFoundException for compressed folder
                        new File(newFile.getParent()).mkdirs();

                        //Create File OutputStream
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {

                            // Copy byte by byte
                            int len;
                            while ( ( len = zis.read(buffer) ) > 0)
                                fos.write(buffer, 0, len);

                        } catch (IOException ex) {
                            exception = ex.getMessage();
                            logger.log(Level.WARNING, "", ex);
                        }

                        //Get next entry
                        ze = zis.getNextEntry();

                        //Update the progress
                        System.out.println((++counter / total)*100);
                    }

                    zis.closeEntry();
                    zis.close();
                    zip.close();

                } catch (IOException ex) {
                    exception = ex.getMessage();
                    logger.log(Level.WARNING, "", ex);
                }
            }

        }
