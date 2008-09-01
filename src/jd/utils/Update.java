package jd.utils;


import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jd.gui.skins.simple.components.JDFileChooser;
import jd.http.Browser;
import jd.update.WebUpdater;
/**
 * Wie benutze ich diese Klasse.
 * 
 * Diese klasse sollte in einem woring directory ausgeführt werden das leer ist. z.B. d:/jd_update
 * Nach dem Start wird die aktuelle JD version vom bluehost server geladen. anschließend fragt der Updater nach einem ordner in dem sich die neuen files befinden.
 * Die neuen files werden hochgeladen und auf crc Fehler beim Upload geprüft. Anschließend wird eine neue hashliste erstellt und auf unseren server geladen. 
 * Die DLC hashes werden ebenfalls aktualisiert.
 * 
 * Svn Ordner und files werden ignoriert und übersprungen.
 * 
 * Benötigte zugangsdaten:
 * Bluehost update ftp password:
 * jdservice.ath.cx ftp logins.
 * 
 * @author coalado
 *
 */
public class Update {
    private static Logger logger = JDUtilities.getLogger();
    public boolean secureUploadFolder(File file, File root, String test) throws FileNotFoundException, IOException, InterruptedException {
        if (root == null) root = file;
        if (!file.isDirectory()) {
            return secureUploadFile(file, root, test);
        }
        boolean ret = true;
        for (File f : file.listFiles()) {
            if(f.getName().contains("svn"))continue;
            if (secureUploadFolder(f, root, test)) {
//                System.out.println("Upload " + f + " successfull");
            }
            else {
//                System.out.println("Upload " + f + " failed");
                ret = false;
            }
        }
        return ret;
    }
    private boolean secureUploadFile(File file, File root, String test) throws FileNotFoundException, IOException, InterruptedException {
        if (root == null) root = file;
        if(file.getName().contains("svn"))return true;
        if (file.isDirectory()) {
            return secureUploadFolder(file, root, test);
        }
        String cw = file.getParentFile().getAbsolutePath().replace(root.getAbsolutePath(), "");
        String def = ftp.getDir();
        ftp.mkdir(cw);
        ftp.cwdAdd(cw);
        if (cw.startsWith("/") || cw.startsWith("\\")) cw = cw.substring(1);
        File testFile=new File(((cw.length()>0)?(cw+"/"):"")+file.getName());
        String path=testFile.getAbsolutePath();
       String serverhash=JDUtilities.getLocalHash(testFile);
        String filename = file.getName() + ".tmp";
        String hash = JDUtilities.getLocalHash(file);
        
      
        if(serverhash!=null&&serverhash.equalsIgnoreCase(hash)){
            ftp.cwd(def);
            System.out.println(file+" skipped");
            return true;
        }
        ftp.remove(filename);
        ftp.stor(new FileInputStream(file), filename);
        //testFile = new File(cw);
        if (cw.startsWith("/") || cw.startsWith("\\")) cw = cw.substring(1);
        Browser.downloadBinary(testFile.getAbsolutePath(), test + cw + "/" + filename);
        String hash2 = JDUtilities.getLocalHash(testFile);
        ftp.remove(file.getName());
        ftp.rename(ftp.getDir() + filename, ftp.getDir() + file.getName());
        ftp.cwd(def);
        if (!hash.equals(hash2)) {
            System.out.println(file+"  failed");
            return false;
        }
       // testFile.delete();
        //testFile.deleteOnExit();
        System.out.println(file+" successfull");
        return true;
    }
    public static void main(String args[]) {
        new Update();
        // MiniLogDialog mld = new MiniLogDialog(new JFrame(), "String message",
        // Thread.currentThread(), true, true);
        // String tmp[] = new String[args.length - 1];
        // for(int i = 1; i < args.length; i++)
        // tmp[i - 1] = args[i];
        //
        // runCommand(args[0], tmp, null);
    }
    private File            dir;
    private ArrayList<File> filelist;
    private SimpleFTP       ftp;
    private String webRoot;
    private File workingdir;
    public Update() {
        String wd=new File("").getAbsolutePath();
       
        JDFileChooser fc = new JDFileChooser();
        fc.setApproveButtonText("Select Folder with updates");
        fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
       fc.setSelectedFile(new File("D:\\jd_update_changes"));
        fc.showOpenDialog(null);
         dir = fc.getSelectedFile();
       if(dir==null)return;
        webRoot="http://78.143.20.67/update/jd/";
 
     WebUpdater updater = new WebUpdater(null);
     Vector<Vector<String>> files = updater.getAvailableFiles();

     // boolean success = false;
     if (files != null) {


         updater.filterAvailableUpdates(files);
        
         updater.updateFiles(files);
     }
     
   if(JOptionPane.showConfirmDialog(null, "DOWNLOAD OK. update now from "+dir+"?")!=JOptionPane.OK_OPTION)return;
        if(!update()){
            logger.severe("UPDATE FAILED");
            return;
        }
        filelist = new ArrayList<File>();
        scanDir(new File(wd));
        logger.info("");
        StringBuffer sb = new StringBuffer();
        for (File file : filelist) {
            String sub = file.toString().substring(new File(wd).toString().length() + 1).replaceAll("\\\\", "/");
            if(sub.startsWith("config"))continue;
            sb.append("$" + sub + "?" + webRoot + sub + "=\"" + JDUtilities.getLocalHash(file) + "\";\r\n");
        }
        logger.info(sb + "");
        upload(sb + "");
    }
    private boolean update() {
        ftp = new SimpleFTP();
        // Connect to an FTP server on port 21.
        try {
            ftp.connect("jd.bluehost.to", 1200, "jd", JOptionPane.showInputDialog("PASS BH"));
            // Set binary mode.
            ftp.bin();
            // Change to a new working directory on the FTP server.
            ftp.cwd("/update/jd");
           return  secureUploadFolder(dir, null, webRoot);
        }
        catch (HeadlessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    private void scanDir(File scan) {
        scan.list(new FilenameFilter() {
            public boolean accept(File scan, String name) {
                if (name.endsWith(".svn")) {
                    return true;
                }
                if (new File(scan, name).isDirectory()) {
                    scanDir(new File(scan, name));
                }
                else {
                    filelist.add(new File(scan, name));
                }
                return true;
            }
        });
    }
    private void upload(String list) {
        try {
            logger.info("connect to ftp");
            SimpleFTP ftp = new SimpleFTP();
            // Connect to an FTP server on port 21.
            ftp.connect("jdservice.ath.cx", 21, JOptionPane.showInputDialog("USER JDSERVICE"), JOptionPane.showInputDialog("PASS JDSERVICE"));
            // Set binary mode.
            ftp.bin();
            // Change to a new working directory on the FTP server.
            ftp.cwd("/httpdocs/update/jd/");
            // Upload some files.
            // ftp.
            // ftp.stor(new File("webcam.jpg"));
            // ftp.stor(new File("comicbot-latest.png"));
            // You can also upload from an InputStream, e.g.
            // ftp.stor(new FileInputStream(new File("test.png")), "test.png");
            logger.info("write list.php");
            JDUtilities.writeLocalFile(JDUtilities.getResourceFile("list.php"), list);
            ftp.remove("list.php");
            ftp.stor(JDUtilities.getResourceFile("list.php"));
            ftp.remove("updatemessage.html");
            logger.info("write updatemessage (changelog)");
            ftp.stor(new File("updatemessage.html"));
            ftp.cwd("/httpdocs/dlcrypt/configs/");
            ftp.remove("jdtc2o.php");
            if (!ftp.rename("/httpdocs/dlcrypt/configs/jdtc2.php", "/httpdocs/dlcrypt/configs/jdtc2o.php")) {
                logger.severe("rename to jdtc2o failed");
            }
            StringBuffer sb = new StringBuffer();
            sb.append("<?php\r\n");
            sb.append("$jd='" + JDUtilities.getLocalHash(new File("JDownloader.jar")) + "';\r\n");
            sb.append("$jdc='" + JDUtilities.getLocalHash(new File("JDownloaderContainer.jar")) + "';\r\n");
            sb.append("$jdp='" + JDUtilities.getLocalHash(new File("JDownloaderPlugins.jar")) + "';\r\n");
            sb.append("?>");
            JDUtilities.writeLocalFile(JDUtilities.getResourceFile("jdtc2.php"), sb + "");
            ftp.stor(JDUtilities.getResourceFile("jdtc2.php"));
            // Quit from the FTP server.
            ftp.disconnect();
            logger.info("update ok");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
