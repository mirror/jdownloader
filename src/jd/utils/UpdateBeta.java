package jd.utils;

// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Runner3.java

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JOptionPane;


public class UpdateBeta {
    private static Logger logger = JDUtilities.getLogger();
    private ArrayList<File> filelist;
    private File dir;

    public UpdateBeta() {
        dir = new File("D:\\jd_gc_update_beta\\bin");
        filelist = new ArrayList<File>();
        scanDir(dir);
        logger.info("");
        StringBuffer sb = new StringBuffer();
        for (File file : filelist) {
            String sub = file.toString().substring(dir.toString().length() + 1).replaceAll("\\\\", "/");
            sb.append("$" + sub + "?" + "http://jdownloader.googlecode.com/svn/trunk/" + sub + "=\""+JDUtilities.getLocalHash(file)+"\";\r\n");
        }
        logger.info(sb+"");
        
        upload(sb+"");
    }

    private void scanDir(File dir) {
        dir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".svn")) return true;
                if (new File(dir, name).isDirectory()) {
                    scanDir(new File(dir, name));
                } else {
                    filelist.add(new File(dir, name));
                }
                return true;
            }

        });
    }
private void upload(String list){
    
    try {
        
        logger.info("connect to ftp");
        SimpleFTP ftp = new SimpleFTP();
        
        // Connect to an FTP server on port 21.
        ftp.connect("jdservice.ath.cx", 21, JOptionPane.showInputDialog("USER"), JOptionPane.showInputDialog("PASS"));
        
        // Set binary mode.
        ftp.bin();
        
        // Change to a new working directory on the FTP server.
        ftp.cwd("/httpdocs/update/beta/");
        
        // Upload some files.
        //ftp.
        //ftp.stor(new File("webcam.jpg"));
        //ftp.stor(new File("comicbot-latest.png"));
       
        // You can also upload from an InputStream, e.g.
        //ftp.stor(new FileInputStream(new File("test.png")), "test.png");
        logger.info("write list.php");
        JDUtilities.writeLocalFile(JDUtilities.getResourceFile("list.php"),list);
        ftp.remove("list.php");
        ftp.stor(JDUtilities.getResourceFile("list.php"));
        ftp.remove("updatemessage.html");
        logger.info("write updatemessage (changelog)");
        ftp.stor(new File(dir,"updatemessage.html"));
        ftp.cwd("/httpdocs/dlcryptbeta/configs/");
        ftp.remove("jdtc2o.php");
        
       if(! ftp.rename("/httpdocs/dlcryptbeta/configs/jdtc2.php","/httpdocs/dlcryptbeta/configs/jdtc2o.php")){
           
         logger.severe("rename to jdtc2o failed");
       }
       
       StringBuffer sb= new StringBuffer();
       sb.append("<?php\r\n");
       sb.append("$jd='"+JDUtilities.getLocalHash(new File(dir,"JDownloader.jar"))+"';\r\n");
       sb.append("$jdc='"+JDUtilities.getLocalHash(new File(dir,"JDownloaderContainer.jar"))+"';\r\n");
       sb.append("$jdp='"+JDUtilities.getLocalHash(new File(dir,"JDownloaderPlugins.jar"))+"';\r\n");
       sb.append("?>");
       
       JDUtilities.writeLocalFile(JDUtilities.getResourceFile("jdtc2.php"),sb+"");
       ftp.stor(JDUtilities.getResourceFile("jdtc2.php"));
        
        // Quit from the FTP server.
        ftp.disconnect();
        logger.info("update ok");
    }
    catch (IOException e) {
       e.printStackTrace();
    }
    
}
    public static void main(String args[]) {

        new UpdateBeta();

        // MiniLogDialog mld = new MiniLogDialog(new JFrame(), "String message",
        // Thread.currentThread(), true, true);
        // String tmp[] = new String[args.length - 1];
        // for(int i = 1; i < args.length; i++)
        // tmp[i - 1] = args[i];
        //
        // runCommand(args[0], tmp, null);
    }

}
