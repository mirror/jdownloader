package jd.utils;

import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import jd.config.CFGConfig;
import jd.http.Browser;
import jd.update.WebUpdater;

/**
 * Wie benutze ich diese Klasse.
 * 
 * Diese klasse sollte in einem woring directory ausgeführt werden das leer ist.
 * z.B. d:/jd_update Nach dem Start wird die aktuelle JD version vom bluehost
 * server geladen. anschließend fragt der Updater nach einem ordner in dem sich
 * die neuen files befinden. Die neuen files werden hochgeladen und auf crc
 * Fehler beim Upload geprüft. Anschließend wird eine neue hashliste erstellt
 * und auf unseren server geladen. Die DLC hashes werden ebenfalls aktualisiert.
 * 
 * Svn Ordner und files werden ignoriert und übersprungen.
 * 
 * Benötigte zugangsdaten: Bluehost update ftp password: jdservice.ath.cx ftp
 * logins.
 * 
 * @author coalado
 * 
 */
public class JDUpdater {
    private static Logger logger = JDUtilities.getLogger();
    private static CFGConfig CONFIG;

    public boolean secureUploadFolder(File file, File root, String test) throws FileNotFoundException, IOException, InterruptedException {
        if (root == null) root = file;
        if (!file.isDirectory()) return secureUploadFile(file, root, test);
        boolean ret = true;
        for (File f : file.listFiles()) {
            if (f.getName().contains("svn")) continue;
            if (!secureUploadFolder(f, root, test)) ret = false;
        }
        return ret;
    }

    private boolean secureUploadFile(File file, File root, String test) throws FileNotFoundException, IOException, InterruptedException {
        if (root == null) root = file;
        if (file.getName().contains("svn")) return true;
        if (file.isDirectory()) return secureUploadFolder(file, root, test);
        String cw = file.getParentFile().getAbsolutePath().replace(root.getAbsolutePath(), "");
        String def = ftp.getDir();
        ftp.mkdir(cw);
        ftp.cwdAdd(cw);
        if (cw.startsWith("/") || cw.startsWith("\\")) cw = cw.substring(1);
        File testFile = new File(((cw.length() > 0) ? (cw + "/") : "") + file.getName());
        String serverhash = JDUtilities.getLocalHash(testFile);
        String filename = file.getName() + ".tmp";
        String hash = JDUtilities.getLocalHash(file);

        if (serverhash != null && serverhash.equalsIgnoreCase(hash)) {
            ftp.cwd(def);
            System.out.println(file + " skipped");
            return true;
        }
        ftp.remove(filename);
        ftp.stor(new FileInputStream(file), filename);
        // testFile = new File(cw);
        if (cw.startsWith("/") || cw.startsWith("\\")) cw = cw.substring(1);
        Browser.downloadBinary(testFile.getAbsolutePath(), test + cw + "/" + filename);
        String hash2 = JDUtilities.getLocalHash(testFile);
        ftp.remove(file.getName());
        ftp.rename(ftp.getDir() + filename, ftp.getDir() + file.getName());
        ftp.cwd(def);
        if (!hash.equals(hash2)) {
            System.out.println(file + "  failed");
            return false;
        }
        // testFile.delete();
        // testFile.deleteOnExit();
        System.out.println(file + " successfull");
        return true;
    }

    public static void main(String args[]) throws Exception {
        CONFIG = CFGConfig.getConfig("JDUPDATER");
        new JDUpdater();
    }

    private File dir;
    private ArrayList<File> filelist;
    private SimpleFTP ftp;
    private String webRoot;
    private File workingdir;

    public JDUpdater() throws Exception {
        String wd = new File("").getAbsolutePath();
        JFileChooser fc = new JFileChooser();
        System.out.print("\r\nWorking dir auswählen. Das working directory darf außer JD installationsdaten nichts enthalten. Alles andere wird gelöscht!");
        fc.setApproveButtonText("Select as woringdir");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setSelectedFile((File) CONFIG.getProperty("LASTSELECTEDDIR1", new File(new File(wd).getParentFile(), "jd_update_dif")));
        if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            logger.severe("ABBRUCH");
            return;
        }

        workingdir = fc.getSelectedFile();
        if (workingdir == null) {
            logger.severe("ABBRUCH");
            return;

        }
      
        CONFIG.setProperty("LASTSELECTEDDIR1", workingdir);

        System.out.print(": " + workingdir.getAbsolutePath() + "\r\n");
        //
        if (!new File("").getAbsolutePath().equals(workingdir.getAbsolutePath())) {
            System.out.print("\r\n\r\n");
            System.err.println("JDUpdater muss in " + workingdir + " ausgeführt werden! Aktuell ausgeführt in: "+new File("").getAbsolutePath());
            return;
        }
        fc = new JFileChooser();
        System.out.print("\r\nUpdate Ordner auswählen. Der Updateordner muss alle aktualisierten files enthalten. Die Ordnerstruktur muss gegeben sein!");

        fc.setApproveButtonText("Select as update dir");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setSelectedFile((File) CONFIG.getProperty("LASTSELECTEDDIR2", new File(new File(wd).getParentFile(), "jd_update_dif")));
        if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            logger.severe("ABBRUCH");
            return;
        }
        dir = fc.getSelectedFile();
        if (dir == null) {
            logger.severe("ABBRUCH");
            return;

        }
        CONFIG.setProperty("LASTSELECTEDDIR2", dir);
        CONFIG.save();
        System.out.print(": " + dir.getAbsolutePath() + "\r\n");
        webRoot = "http://78.143.20.68/update/jd/";
        System.out.println("Webroot updateserver: " + webRoot);

        System.out.println("Hashlist laden");
        WebUpdater updater = new WebUpdater(null);
        updater.setOSFilter(false);
        updater.ignorePlugins(false);
        Vector<Vector<String>> files = null;
        try {
            files = updater.getAvailableFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<File> localfiles = getLocalFileList(workingdir);
        HashMap<String, String> webupdaterfiles = new HashMap<String, String>();
        if (files == null) files = new Vector<Vector<String>>();
        for (int i = 0; i < files.size(); i++) {
            String path = files.get(i).get(0).split("\\?")[0];
            File f = new File(this.workingdir, path);
            webupdaterfiles.put(f.getAbsolutePath(), files.get(i).get(1));
        }

        System.out.println("Working dir überprüfen");
        for (File f : localfiles) {
            if (!webupdaterfiles.containsKey(f.getAbsolutePath())) {
                if (!f.isDirectory()) {
                    int answer = JOptionPane.showConfirmDialog(null, "Datei " + f.getAbsolutePath() + " ist im Workingdir, aber nicht in der Updatelist. entfernen?");
                    if (answer == JOptionPane.CANCEL_OPTION) break;
                    if (answer == JOptionPane.OK_OPTION) {
                        if (!f.delete()) {
                            logger.severe("Datei " + f + " konnte nicht entfernt werden. ABBRUCH");
                            return;
                        }
                    }
                }

            }

        }
        System.out.println("Workingdir aktualisieren");
        // boolean success = false;
        if (files != null) {
            updater.filterAvailableUpdates(files);
            updater.updateFiles(files);
        }
        System.out.println("Aktualisierung fertig. Frage nach Dateien zum entfernen");
        while (true) {
            fc = new JFileChooser();
            System.out.println("Sollen Dateien entfernt werden? Falls ja bitte auswählen. Falls nein-->Abbrechen");
            fc.setApproveButtonText("Select file to DELETE");
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setSelectedFile(this.workingdir);
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                break;
            }
            File[] filesToRemove = fc.getSelectedFiles();
            if (filesToRemove == null) {
                break;
            }

            for (File f : filesToRemove) {
                if (JOptionPane.showConfirmDialog(null, "Datei " + f.getAbsolutePath() + " wirklich entfernen?") == JOptionPane.OK_OPTION) {
                    if (!JDUtilities.removeDirectoryOrFile(f)) {
                        logger.severe("Datei " + f + " konnte nicht entfernt werden. ABBRUCH");
                    } else {
                        System.out.println("Datei/Ordner entfernt: " + f);
                    }
                    return;
                }

            }
        }

        if (!update()) {
            logger.severe("UPDATE FAILED");
            return;
        }
        filelist = new ArrayList<File>();
        scanDir(new File(wd));
        logger.info("");
        StringBuffer sb = new StringBuffer();

        for (File file : filelist) {
            String sub = file.toString().substring(new File(wd).toString().length() + 1).replaceAll("\\\\", "/");
            if (sub.startsWith("config")) continue;

            sb.append("$" + sub + "?" + webRoot + sub + "=\"" + JDUtilities.getLocalHash(file) + "\";\r\n");
        }
        logger.info(sb + "");
        upload(sb + "");

    }

    private ArrayList<File> getLocalFileList(File dir) {
        ArrayList<File> ret = new ArrayList<File>();
        ret.add(dir);
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                ret.addAll(getLocalFileList(f));
            } else {
                ret.add(f);
            }
        }
        return ret;
    }

    private boolean update() {
        ftp = new SimpleFTP();
        try {
            ftp.connect("78.143.20.68", 1200, "jd", JOptionPane.showInputDialog("Bluehost Updateserver Passwort"));
            ftp.bin();
            ftp.cwd("/update/jd");
            return secureUploadFolder(dir, null, webRoot);
        } catch (HeadlessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void scanDir(File scan) {
        scan.list(new FilenameFilter() {
            public boolean accept(File scan, String name) {
                if (name.endsWith(".svn")) return true;
                if (new File(scan, name).isDirectory()) {
                    scanDir(new File(scan, name));
                } else {
                    filelist.add(new File(scan, name));
                }
                return true;
            }
        });
    }

    private void upload(String list) throws Exception {
        try {
            logger.info("connect to ftp");
            SimpleFTP ftp = new SimpleFTP();
            ftp.connect("87.118.121.244", 21, JOptionPane.showInputDialog("USER jdownloader.org"), JOptionPane.showInputDialog("PASS jdownloader.org"));
            ftp.bin();
            ftp.cwd("/httpdocs/service/update/jd/");
            logger.info("write list.php");
            JDUtilities.writeLocalFile(JDUtilities.getResourceFile("list.php"), list);
            ftp.remove("list.php");
            ftp.stor(JDUtilities.getResourceFile("list.php"));
            ftp.remove("updatemessage.html");
            logger.info("write updatemessage (changelog)");
            ftp.stor(new File("updatemessage.html"));
            // Quit from the FTP server.
            ftp.disconnect();
            logger.info("update ok");

            new Browser().getPage("http://service.jdownloader.org/update/jd/updatelist.php");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
