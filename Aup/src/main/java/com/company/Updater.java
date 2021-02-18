package com.company;
import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import java.io.FileReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Updater {
    private File updateFolder = new File(InfoTool.getBasePathForClass(Main.class));

    /**
     * Download update as a ZIP Folder , this is the prefix name of the ZIP
     * folder
     */
    private static String foldersNamePrefix;
    /**
     * Update to download
     */
    private static int update;
    /**
     * The name of the application you want to update
     */
    private String applicationName;
    //================Services================
    DownloadService downloadService;
    ExportZipService exportZipService;
    //=============================================
    public void startUpdate() throws Exception {

        //Parse Arguments -> I want one parameter -> for example [45] which is the update i want

        long version = fetchSystemVersion();
        if(version==-1)
        {
            System.out.println("Failed To Fetch Version.PLease Check JSON File");
            System.exit(0);
        }
        update=(int) version;
        System.out.println("Getting Update Version "+version);
        System.out.println("Updater Starting");
        prepareForUpdate("Sape");
        updateJson(100);
        System.out.println(updateFolder);
    }
    /**
     * Prepare for the Update
     *
     * @param applicationName
     */
    public void prepareForUpdate(String applicationName) {
        this.applicationName = applicationName;
        //FoldersNamePrefix
        foldersNamePrefix = updateFolder.getAbsolutePath() + File.separator + applicationName + " Update Package " + update;
        //Check the Permissions
        System.out.println("Checking for Permissions");
        if (checkPermissions()) {
            // downloadMode.getProgressLabel().setText("Checking permissions");

            try {
                downloadUpdate("https://github.com/goxr3plus/XR3Player/releases/download/V3." + update + "/XR3Player.Update." + update + ".zip");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("You do not have the Read/Write Permissions.PLease Grant Permissions and Try Again");
            ActionTool.showNotification("Permission Denied[FATAL ERROR]",
                    "Application has no permission to write inside this folder:\n [ " + updateFolder.getAbsolutePath()
                            + " ]\n -> I am working to find a solution for this error\n -> You can download " + applicationName + " manually :) ]",
                    Duration.minutes(1), NotificationType.ERROR);
        }
    }
    /**
     * In order to update this application must have READ,WRITE AND CREATE
     * permissions on the current folder
     */
    public boolean checkPermissions() {

        //Check for permission to Create
        try {
            File sample = new File(updateFolder.getAbsolutePath() + File.separator + "empty123123124122354345436.txt");
            /*
             * Create and delete a dummy file in order to check file
             * permissions. Maybe there is a safer way for this check.
             */
            sample.createNewFile();
            sample.delete();
        } catch (IOException e) {
            //Error message shown to user. Operation is aborted
            return false;
        }

        //Also check for Read and Write Permissions
        return updateFolder.canRead() && updateFolder.canWrite();
    }

    /**
     * Try to download the Update
     */
    private void downloadUpdate(String downloadURL) {

        if (InfoTool.isReachableByPing("www.google.com")) {

            //Download it
            try {
                //Delete the ZIP Folder
                deleteZipFolder();

                //Create the downloadService
                downloadService = new DownloadService();
                System.out.println("Starting Download");
                //Start
                downloadService.startDownload(new URL(downloadURL), Paths.get(foldersNamePrefix + ".zip"));
            } catch (MalformedURLException e) {
                System.out.println("Cannot Find Version");
                System.exit(0);
                //e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Cannot Find Version");
               // e.printStackTrace();
                System.exit(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            //Update
            System.out.println("NO internet Connection, Please Connect to the Internet And Try Again");
            //Delete the ZIP Folder
            deleteZipFolder();

            System.exit(0);
        }
        exportUpdate();
        packageUpdate();

    }
    /**
     * Exports the Update ZIP Folder
     */
    private void exportUpdate() {
        //Create the ExportZipService
        exportZipService = new ExportZipService();
        System.out.println("Extracting the Update");
        exportZipService.exportZip(foldersNamePrefix + ".zip", updateFolder.getAbsolutePath());
    }
    /**
     * After the exporting has been done, delete the old update files and
     * add the new ones
     */
    private void packageUpdate() {
        System.out.println("Deleting old files");
        //Delete the ZIP Folder
       deleteZipFolder();
       System.out.println("Saving Changes");
       updateJson(update);
        //Start the Launcher
        System.out.println("Starting the Installer");
        restartApplication(applicationName);
    }
    /**
     * Calling this method to start the main Application which is The installer
     */
    public static void restartApplication(String appName) {

        // Restart XR3Player
        new Thread(() -> {
            String path = InfoTool.getBasePathForClass(Main.class);
            // Set this as the path of the file to be launched.
            String[] applicationPath = {new File(path + appName + ".jar").getAbsolutePath()};
            System.out.println(Arrays.toString(applicationPath));

            //Show message that application is restarting
            Platform.runLater(() -> ActionTool.showNotification("Starting " + appName,
                    "Application Path:[ " + applicationPath[0] + " ]\n\tIf this takes more than [20] seconds either the computer is slow or it has failed....", Duration.seconds(25),
                    NotificationType.INFORMATION));

            try {

                //Delete the ZIP Folder
                deleteZipFolder();

                //------------Wait until Application is created
                File applicationFile = new File(applicationPath[0]);
                while (!applicationFile.exists()) {
                    Thread.sleep(50);
                    System.out.println("Waiting " + appName + " Jar to be created...");
                }

                System.out.println(appName + " Path is : " + applicationPath[0]);

                //Create a process builder
                ProcessBuilder builder = new ProcessBuilder("java", "-jar", applicationPath[0], !"Sape".equals(appName) ? "" : String.valueOf(update));
                builder.redirectErrorStream(true);
                Process process = builder.start();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                // Wait n seconds
                PauseTransition pause = new PauseTransition(Duration.seconds(10));
                pause.setOnFinished(f -> Platform.runLater(() -> ActionTool.showNotification("Starting " + appName + " failed",
                        "\nApplication Path: [ " + applicationPath[0] + " ]\n\tTry to do it manually...", Duration.seconds(10), NotificationType.ERROR)));
                pause.play();
                System.exit(0);

            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.INFO, null, ex);

                // Show failed message
                Platform.runLater(() -> Platform.runLater(() -> ActionTool.showNotification("Starting " + appName + " failed",
                        "\nApplication Path: [ " + applicationPath[0] + " ]\n\tTry to do it manually...", Duration.seconds(10), NotificationType.ERROR)));

            }
        }, "Start Application Thread").start();
    }
    /**
     * Delete the ZIP folder from the update
     *
     * @return True if deleted , false if not
     */
    public static boolean deleteZipFolder() {
        return new File(foldersNamePrefix + ".zip").delete();
    }
    public String CurrDirectory() {

            String path = System.getProperty("user.dir");
            return path;
    }
    public long fetchSystemVersion()
    {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(updateFolder.getAbsolutePath()+"\\Version.json"))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
            JSONObject o=(JSONObject) obj;
            reader.close();

            return (long) o.get("version");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }
    private void updateJson(int vernum) {
        JSONParser jsonParser = new JSONParser();
        try (FileWriter file = new FileWriter(updateFolder.getAbsolutePath()+"\\Version.json")) {
            JSONObject o=new JSONObject();
            o.put("version",vernum);
            file.write(o.toJSONString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        }
}
