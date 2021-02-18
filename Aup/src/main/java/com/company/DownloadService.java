package com.company;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import com.company.InfoTool;
public class DownloadService{

    /**
     * The logger of the class
     */
    private final Logger logger = Logger.getLogger(DownloadService.class.getName());

    // -----
    private long totalBytes;
    private boolean succeeded;
    private volatile boolean stopThread;
    private final ObjectProperty<URL> remoteResourceLocation = new SimpleObjectProperty<>();
    private final ObjectProperty<Path> pathToLocalResource = new SimpleObjectProperty<>();
    private Thread copyThread;
    private volatile FileChannel zip;
    public void startDload() throws InterruptedException, IOException {

                // Succeeded boolean
                succeeded = true;

                // URL and LocalFile
                //URL urlFile = new URL(java.net.URLDecoder.decode(urlString, "UTF-8"))
                File destinationFile = new File(pathToLocalResource.get().toString());

                //Update the message
        System.out.println("Connecting with Server");
                String failMessage;

                try {

                    // Open the connection and get totalBytes
                    URLConnection connection = remoteResourceLocation.get().openConnection();
                    totalBytes = Long.parseLong(connection.getHeaderField("Content-Length"));

                    // ------------------------------------------------ Copy the File to External Thread-------------------------------------------------------
                    copyThread = new Thread(() -> {
                        // Start File Copy
                        try {
                            zip = FileChannel.open(pathToLocalResource.get(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

                            zip.transferFrom(Channels.newChannel(connection.getInputStream()), 0, Long.MAX_VALUE);
                            // Files.copy(dl.openStream(), fl.toPath(),StandardCopyOption.REPLACE_EXISTING)

                        } catch (Exception ex) {
                            stopThread = true;
                            logger.log(Level.WARNING, "DownloadService failed", ex);
                        } finally {
                            try {
                                zip.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }

                        System.out.println("Copy Thread exited...");
                    });
                    // Set to Daemon
                    copyThread.setDaemon(true);
                    // Start the Thread
                    copyThread.start();
                    // ----------------------------------------------- End: Copy the File to External Thread-----------------------------------------------

                    // --------------------------------------------------Check the %100 Progress-------------------------------------------------------------
                    long outPutFileLength;
                    long previousLength = 0;
                    //actually it is millisecondsFailTime*50(cause Thread is sleeping for 50 milliseconds
                    int millisecondsFailTime = 40;
                    // While Loop
                    while ( ( outPutFileLength = destinationFile.length() ) < totalBytes && !stopThread) {

                        // Check the previous length
                        if (previousLength != outPutFileLength) {
                            previousLength = outPutFileLength;
                            millisecondsFailTime = 0;
                        } else
                            ++millisecondsFailTime;

                        // 2 Seconds passed without response
                        if (millisecondsFailTime == 40 || stopThread)
                            break;

                        // Update Progress
                        System.out.println("Downloading: [ " + InfoTool.getFileSizeEdited(totalBytes) + " ] Progress: [ " + ( outPutFileLength * 100 ) / totalBytes + " % ]");
                        System.out.println( ( outPutFileLength * 100 ) / totalBytes);
                        System.out.println(
                                "Current Bytes:" + outPutFileLength + " ,|, TotalBytes:" + totalBytes + " ,|, Current Progress: " + ( outPutFileLength * 100 ) / totalBytes + " %");

                        // Sleep
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                            logger.log(Level.WARNING, "", ex);
                        }
                    }

                    //Update to show 100%
                    System.out.println("Downloading: [ " + InfoTool.getFileSizeEdited(totalBytes) + " ] Progress: [ " + ( outPutFileLength * 100 ) / totalBytes + " % ]");
                    System.out.println( ( outPutFileLength * 100 ) / totalBytes);
                    System.out.println(
                            "Current Bytes:" + outPutFileLength + " ,|, TotalBytes:" + totalBytes + " ,|, Current Progress: " + ( outPutFileLength * 100 ) / totalBytes + " %");

                    // 2 Seconds passed without response
                    if (millisecondsFailTime == 40)
                        succeeded = false;
                    // ---------------------------------------------------End: Check the %100 Progress-----------------------------------------------------

                } catch (Exception ex) {
                    succeeded = false;
                    // Stop the External Thread which is updating the %100 progress
                    stopThread = true;
                    logger.log(Level.WARNING, "DownloadService failed", ex);
                    failMessage = ex.getMessage();
                }

                //-------------------------------------------------------Finally-------------------------------------------------------------------------------

                System.out.println("\nTrying to interrupt[shoot with an assault rifle] the copy Thread");

                // ---FORCE STOP COPY FILES
                if (!succeeded && copyThread != null && copyThread.isAlive()) {
                    zip.close();
                    copyThread.interrupt();
                    System.out.println("\nDone an interrupt to the copy Thread");

                    // Run a Looping checking if the copyThread has stopped...
                    while (copyThread.isAlive()) {
                        System.out.println("Copy Thread is still Alive,refusing to die.");
                        Thread.sleep(50);
                    }
                }

                //Check if failed && Update the message
                if (!succeeded)
                    System.out.println("Failed..." + ( InfoTool.isReachableByPing("www.google.com") ? "" : "No internet Connection" ) + " , please exit...");

                System.out
                        .println("\n ->Download Service exited:[Value=" + succeeded + "]" + " Copy Thread is Alive? " + ( copyThread == null ? "" : copyThread.isAlive() + "\n" ));

                //----------------------------------------------------- End: Finally-------------------------------------------------------------------------------
            }
    /**
     * Start the Download Service [[SuppressWarningsSpartan]]
     */
    public void startDownload(URL remoteResourceLocation , Path pathToLocalResource) throws InterruptedException, IOException {
        //!Running and Report null
        if (pathToLocalResource != null && remoteResourceLocation != null) {

            //Set
            this.setRemoteResourceLocation(remoteResourceLocation);
            this.setPathToLocalResource(pathToLocalResource);

            // setRemoteResourceLocation(new URL(java.net.URLDecoder.decode(remoteResourceLocation, "UTF-8")))
            //TotalBytes
            totalBytes = 0;

            startDload();
        } else
            logger.log(Level.INFO, "Please specify [Remote Resource Location] and [ Path to Local Resource ]");

    }
    //----------------------Getters--------------------------------------

    /**
     * @return The remoteResouceLocation
     */
    public final URL getRemoteResourceLocation() {
        return remoteResourceLocation.get();
    }

    /**
     * @return The PathToLocalResource
     */
    public final Path getPathToLocalResource() {
        return pathToLocalResource.get();

    }

    //----------------------Setters--------------------------------------

    /**
     * Set the remote resource location
     *
     * @param remoteResourceLocation
     */
    public final void setRemoteResourceLocation(URL remoteResourceLocation) {
        this.remoteResourceLocation.set(remoteResourceLocation);
    }

    /**
     * Set the path to the local resource
     *
     * @param pathToLocalResource
     */
    public final void setPathToLocalResource(Path pathToLocalResource) {
        this.pathToLocalResource.set(pathToLocalResource);
    }

    //----------------------Properties Getters--------------------------------------

    /**
     * @return remoteResourceLocation property
     */
    public ObjectProperty<URL> remoteResourceLocationProperty() {
        return remoteResourceLocation;
    }

    /**
     * @return pathToLocalResource property
     */
    public ObjectProperty<Path> pathToLocalResourceProperty() {
        return pathToLocalResource;
    }

}

