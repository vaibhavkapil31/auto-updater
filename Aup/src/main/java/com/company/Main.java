package com.company;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.company.DownloadService;
import com.company.ExportZipService;
import com.company.ActionTool;
import com.company.InfoTool;
import com.company.NotificationType;
import com.company.Updater;

public class Main{
    public static void main(String[] args) throws Exception {
        Updater updaterObject=new Updater();
        updaterObject.startUpdate();
        System.out.println("Everything Done");
        System.exit(0);
    }

}

