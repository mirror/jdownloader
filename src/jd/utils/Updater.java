//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.config.CFGConfig;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.http.Browser;
import jd.http.Encoding;
import jd.nutils.JDHash;
import jd.nutils.SimpleFTP;
import jd.nutils.io.JDIO;
import jd.nutils.svn.Subversion;
import jd.nutils.zip.Zip;
import jd.parser.Regex;
import jd.update.FileUpdate;
import jd.update.WebUpdater;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;

public class Updater {
    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Updater upd = new Updater();
        // WebUpdater.getConfig("WEBUPDATE").setProperty("BRANCH","bin" );
        System.out.println("STATUS: Webupdate");
        upd.webupdate();
        // System.out.println("STATUS: Webupdate ende");
        // System.out.println("STATUS: Scan local");
        // upd.lockUpdate();
        upd.removeFileOverhead();

        System.out.println("STATUS: move plugins");
        upd.movePlugins(getCFG("plugins_dir"));
        upd.moveJars(getCFG("dist_dir"));
        // // // System.out.println("STATUS: FINISHED");
        upd.cleanUp();
        String id = upd.createBranch(System.currentTimeMillis() + "_testbin");

        ArrayList<File> list = upd.getFileList();
        // //
        upd.upload(list);
        // //
        upd.merge();
        upd.checkHashes();
        upd.clone0(upd.branch);
        upd.clone2(upd.branch);
        // upd.clonebluehost2();
        upd.uploadHashList();
        // upd.spread(list);

        System.exit(0);
    }

    private void cleanUp() {
        String[] outdated = Regex.getLines(JDIO.getLocalFile(new File(this.updateDir, "outdated.dat")));

        for (String path : outdated) {
            if (new File(this.workingDir, path).exists()) {
                new File(this.workingDir, path).delete();
                System.err.println(" CLEAN UP: " + new File(this.workingDir, path).getAbsolutePath());
            }
            if (new File(this.updateDir, path).exists()) {
                new File(this.updateDir, path).delete();
                System.err.println(" CLEAN UP: " + new File(this.updateDir, path).getAbsolutePath());
            }
        }
    }

    private String branch;

    private String createBranch(String id) throws IOException {
        this.branch = id;

        String ret = new Browser().getPage(UPDATE_SERVER + "createBranch.php?pass=" + getCFG("updateHashPW") + "&parent=" + latestBranch + "&branch=" + id);
        System.out.println(ret);
        return id;
    }

    private File pluginsDir;

    private WebUpdater webupdater;
    private ArrayList<FileUpdate> remoteFileList;
    private File workingDir;
    private JFrame frame;
    private static String UPDATE_SUB_DIR = "exclude_jd_update";
    private static String UPDATE_SUB_SRC = "exclude_jd_src";
    private File updateDir;
    private File svn;
    private static String UPDATE_SERVER = "http://update1.jdownloader.org/";
    private File jars;

    private boolean uploadaddons;

    private String latestBranch;

    public Updater() throws IOException, SVNException {
        workingDir = new File(".").getCanonicalFile();

        updateDir = new File(workingDir, UPDATE_SUB_DIR);
        svn = new File(workingDir, UPDATE_SUB_SRC);
        svn.mkdirs();
        updateDir.mkdirs();
        initGUI();

    }

    /**
     * gets a config entry (string) asks if not available
     * 
     * @param key
     * @return
     */
    public static String getCFG(String key) {
        CFGConfig cfg = CFGConfig.getConfig("LOCALCONFIG");
        String ret = cfg.getStringProperty(key);
        if (ret == null) {
            JFrame frame = new JFrame();
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            ret = JOptionPane.showInputDialog(frame, "get Config: " + key);
            cfg.setProperty(key, ret);
            cfg.save();
        }
        return ret;
    }

    private void clonebluehost2() throws IOException {
        HashMap<String, String> map = createHashList(this.workingDir);
        Browser br = new Browser();
        br.forceDebug(true);

        map.put("pass", getCFG("updateHashPW"));

        br.postPage("http://jdupdate.bluehost.to/clone.php?pass=" + getCFG("updateHashPW"), map);
        System.out.println(br + "");
        if (!br.containsHTML("<b>fail</b>") && !br.containsHTML("<b>Warning</b>") && !br.containsHTML("<b>Error</b>")) {
            System.out.println("CLONE update2 OK");
            return;
        }

        JOptionPane.showConfirmDialog(frame, "MD5 ERROR!!!! See log");

    }

    private void moveJars(String string) throws IOException {
        jars = new File(string);
        copyDirectory(new File(jars, "libs"), new File(this.updateDir, "libs"));
        copyFile(new File(jars, "JDownloader.jar"), new File(updateDir, "JDownloader.jar"));
        copyFile(new File(jars.getParentFile(), "ressourcen\\outdated.dat"), new File(updateDir, "outdated.dat"));
        copyDirectory(new File(jars.getParentFile(), "ressourcen\\pluginressourcen\\128__JDTray"), this.updateDir);
        copyDirectory(new File(jars.getParentFile(), "ressourcen\\pluginressourcen\\127__JDHJMerge"), this.updateDir);
        copyDirectory(new File(jars.getParentFile(), "ressourcen\\pluginressourcen\\123__JDUnrar"), this.updateDir);
        copyDirectory(new File(jars.getParentFile(), "ressourcen\\pluginressourcen\\106__JDShutdown"), this.updateDir);
        copyDirectory(new File(jars.getParentFile(), "ressourcen\\pluginressourcen\\100__JDChat"), this.updateDir);

        copyDirectory(new File(jars.getParentFile(), "ressourcen\\jd"), new File(this.updateDir, "jd"));
        copyDirectory(new File(jars.getParentFile(), "ressourcen\\tools"), new File(this.updateDir, "tools"));
        copyDirectory(new File(jars.getParentFile(), "ressourcen\\libs"), new File(this.updateDir, "libs"));

    }

    private void clone2(String branch) throws IOException {
        HashMap<String, String> map = createHashList(this.workingDir);
        Browser br = new Browser();
        br.forceDebug(true);

        map.put("pass", getCFG("updateHashPW"));

        br.postPage("http://update2.jdownloader.org/clone.php?pass=" + getCFG("updateHashPW") + "&branch=" + branch, map);
        System.out.println(br + "");
        map = map;
        if (!br.containsHTML("<b>fail</b>") && !br.containsHTML("<b>Warning</b>") && !br.containsHTML("<b>Error</b>")) {
            System.out.println("CLONE update2 OK");
            return;
        }

        JOptionPane.showConfirmDialog(frame, "MD5 ERROR!!!! See log");

    }

    private void clone0(String branch) throws IOException {
        HashMap<String, String> map = createHashList(this.workingDir);
        Browser br = new Browser();
        br.forceDebug(true);

        map.put("pass", getCFG("updateHashPW"));

        br.postPage("http://update0.jdownloader.org/clone.php?pass=" + getCFG("updateHashPW") + "&branch=" + branch, map);
        System.out.println(br + "");
        map = map;
        if (!br.containsHTML("<b>fail</b>") && !br.containsHTML("<b>Warning</b>") && !br.containsHTML("<b>Error</b>")) {
            System.out.println("CLONE update0 OK");
            return;
        }

        JOptionPane.showConfirmDialog(frame, "MD5 ERROR!!!! See log");

    }

    // private void lockUpdate() throws IOException {
    // Browser br = new Browser();
    // br.forceDebug(true);
    // System.out.println(br.getPage("http://update0.jdownloader.org/lock.php?pass="
    // + getCFG("server_pass")));
    // System.out.println(br.getPage("http://update1.jdownloader.org/lock.php?pass="
    // + getCFG("server_pass")));
    // System.out.println(br.getPage("http://update2.jdownloader.org/lock.php?pass="
    // + getCFG("server_pass")));
    //
    // }

    /**
     * TODO spreadinglist wird online nicht verabeitet
     * 
     * @param list
     * @throws IOException
     */
    private void spread(ArrayList<File> list) throws IOException {
        System.out.println("Spread now");
        Browser br = new Browser();
        br.forceDebug(true);
        br.setReadTimeout(10 * 60 * 1000);
        try {
            System.out.println(br.getPage("http://update1.jdownloader.org/spread.php?pass=" + getCFG("server_pass") + "&server=update4ex.jdownloader.org"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println(br.getPage("http://update1.jdownloader.org/spread.php?pass=" + getCFG("server_pass") + "&server=r22593.ovh.net"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println(br.getPage("http://update1.jdownloader.org/spread.php?pass=" + getCFG("server_pass") + "&server=78.143.20.67"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Spread ok");
    }

    private void checkHashes() throws IOException {
        while (true) {
            HashMap<String, String> map = createHashList(this.workingDir);
            Browser br = new Browser();
            br.forceDebug(true);

            map.put("pass", getCFG("updateHashPW"));

            br.postPage("http://update1.jdownloader.org/checkHashes.php?pass=" + getCFG("updateHashPW") + "&branch=" + branch, map);
            System.out.println(br + "");
            if (br.containsHTML("success") && !br.containsHTML("<b>Warning</b>") && !br.containsHTML("<b>Error</b>")) break;

            JOptionPane.showConfirmDialog(frame, "MD5 ERROR!!!! See log");
        }
    }

    private void uploadHashList() throws Exception {
        while (true) {

            HashMap<String, String> map = createHashList(this.workingDir);
            Browser br = new Browser();
            br.forceDebug(true);
            StringBuilder serverList = new StringBuilder();

            serverList.append("-1:http://update0.jdownloader.org/" + branch + "/\r\n");
            serverList.append("-1:http://update4ex.jdownloader.org/" + branch + "/\r\n");
            serverList.append("-1:http://jdupdate.bluehost.to/" + branch + "/\r\n");
            // serverList.append("-1:http://update1.jdownloader.org/"+branch+"/\r\n");
            serverList.append("-1:http://update2.jdownloader.org/" + branch + "/\r\n");
            System.out.println(br.postPage("http://update1.jdownloader.org/unlock.php?pass=" + getCFG("updateHashPW") + "&branch=" + branch, "server=" + serverList.toString()));
            map.put("pass", getCFG("updateHashPW"));
            // map = map;
            String addonlist = createAddonList();
            map.put("addonlist", Encoding.urlEncode(addonlist));
            br.postPage("http://update1.jdownloader.org/updateHashList.php?pass=" + getCFG("updateHashPW") + "&branch=" + branch, map);
            System.out.println(br + "");
            br = br;
            if (br.containsHTML("success") && !br.containsHTML("<b>Warning</b>") && !br.containsHTML("<b>Error</b>")) break;

            JOptionPane.showConfirmDialog(frame, "MD5 ERROR!!!! See log");
        }
    }

    private String createAddonList() throws Exception {
        File file = new File(getCFG("addon_dir"));
        StringBuilder sb = new StringBuilder();
        uploadaddons = JOptionPane.showConfirmDialog(frame, "Upload addons?") == JOptionPane.OK_OPTION;

        if (!uploadaddons) {

            if (new File(this.workingDir, "addonlist.lst").exists()) {
                return JDIO.getLocalFile(new File(this.workingDir, "addonlist.lst"));
            } else {
                return "";
            }

        }
        sb.append("<packages>\r\n");
        for (File addon : file.listFiles()) {

            if (new File(addon, "info.txt").exists()) {

                Properties info = new Properties();
                FileInputStream stream;
                try {
                    stream = new FileInputStream(new File(addon, "info.txt"));
                    info.load(stream);
                    stream.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (info.getProperty("disabled") == null || !info.getProperty("disabled").equalsIgnoreCase("false")) {
                    addAddon(sb, addon, info);
                }
            }
        }

        sb.append("</packages>");
        JDIO.writeLocalFile(new File(this.workingDir, "addonlist.lst"), sb.toString());
        System.out.println(sb);
        return sb.toString();
    }

    private void addAddon(StringBuilder sb, File addon, Properties info) throws Exception {

        File[] files = addon.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".svn") || name.endsWith("info.txt")) { return false; }
                return true;

            }

        });
        Subversion svn = new Subversion("https://www.syncom.org/svn/jdownloader/");
        SVNDirEntry svnInfo = svn.getRepository().info("/trunk/ressourcen/pluginressourcen/" + addon.getName(), svn.latestRevision());

        File des = File.createTempFile("test", ".jdu");
        des = new File(des.getParentFile(), svnInfo.getRevision() + "_" + info.getProperty("filename"));
        Zip zip = new Zip(files, des);
        zip.setExcludeFilter(Pattern.compile("\\.svn", Pattern.CASE_INSENSITIVE));
        zip.zip();
        SimpleFTP.uploadtoFolderSecure("update2.jdownloader.org", 2121, getCFG("update2_ftp_user"), getCFG("update2_ftp_pass"), "/http/addons", des);
        sb.append("    <package>\r\n");
        for (Iterator<Entry<Object, Object>> it = info.entrySet().iterator(); it.hasNext();) {
            Entry<Object, Object> next = it.next();
            if (next.getKey().toString().equalsIgnoreCase("version")) {
                next.setValue(svnInfo.getRevision());

                sb.append("          <lastAuthor>" + svnInfo.getAuthor() + "</lastAuthor>\r\n");
                sb.append("          <lastChangeDate>" + svnInfo.getDate() + "</lastChangeDate>\r\n");

            }

            if (next.getKey().toString().equalsIgnoreCase("url")) {
                next.setValue("http://update2.jdownloader.org/addons/" + des.getName() + "?.zip");
                Browser br = new Browser();
                br.openGetConnection("http://update2.jdownloader.org/addons/" + des.getName());

                sb.append("          <filesize>" + br.getRequest().getContentLength() + "</filesize>\r\n");
                sb.append("          <md5>" + JDHash.getMD5(des) + "</md5>\r\n");
                sb.append("          <responseCode>" + br.getRequest().getHttpConnection().getResponseCode() + "</responseCode>\r\n");
                br.getRequest().getHttpConnection().disconnect();
            }
            if (next.getKey().toString().equalsIgnoreCase("light-url")) {
                next.setValue("http://update2.jdownloader.org/addons/" + des.getName() + "?.zip");
            }
            sb.append("          <" + next.getKey() + ">" + next.getValue() + "</" + next.getKey() + ">\r\n");
        }
        sb.append("    </package>\r\n");
    }

    /**
     * 
     * 
     * @param dir
     * @return
     */
    private HashMap<String, String> createHashList(File dir) {
        HashMap<String, String> map = new HashMap<String, String>();
        ArrayList<File> list = getLocalFileList(dir, false);

        for (File f : list) {
            String path = f.getAbsolutePath().replace(dir.getAbsolutePath(), "");

            path = path.replace("\\", "/");
            if (path.trim().length() == 0 || f.isDirectory()) {

                continue;
            }

            String hash = JDHash.getMD5(f);
            int i = 1;
            String nH = hash;
            while (map.containsKey(nH)) {
                nH = hash + "_" + i;
                i++;

            }
            map.put(nH, path);
        }

        return map;
    }

    private void merge() throws IOException {
        this.copyDirectory(this.updateDir, this.workingDir);

    }

    /**
     * Uploads this filelist. Hash is checked after upload.
     * 
     * @param list
     * @throws IOException
     */
    private boolean upload(ArrayList<File> list) throws IOException {

        SimpleFTP.uploadSecure("update1.jdownloader.org", 2121, getCFG("update1_ftp_user"), getCFG("update1_ftp_pass"), "/http/" + this.branch, updateDir, list.toArray(new File[] {}));

        return true;

    }

    private JFrame getFrame() {
        return frame;
    }

    /**
     * Gets the diuf between lokal copy and updatelist
     * 
     * @return
     */
    private ArrayList<File> getFileList() {

        System.out.println("Demerge updatelist");
        ArrayList<File> listUpdate = this.getLocalFileList(this.updateDir, true);
        listUpdate.remove(0);
        ArrayList<File> listLocal = this.getLocalFileList(this.workingDir, false);
        main: for (Iterator<File> it = listUpdate.iterator(); it.hasNext();) {
            File file = it.next();
            String newHash = JDHash.getMD5(file);
            String newFile = file.getAbsolutePath().replace(updateDir.getAbsolutePath(), "");
            newFile = newFile.replace("\\", "/");
            if (newFile.trim().length() == 0) continue;
          

            File localFile = new File(workingDir, newFile);
            String localHash = JDHash.getMD5(localFile);
            if (localHash != null && localHash.equalsIgnoreCase(newHash)) {
                it.remove();
                continue main;
            }

            System.out.println("Update: " + localFile);
        }
        System.out.println("Demerge updatelist finished: " + listUpdate.size() + " files");
        return listUpdate;
    }

    /** Copies host and decryptplugins from svn dir to updatelist */
    private void movePlugins(String cfg) throws IOException {
        if (cfg == null) return;
        pluginsDir = new File(cfg);
        File file;
        JDIO.removeDirectoryOrFile(file = new File(this.updateDir, "jd/plugins/host"));
        copyDirectory(new File(pluginsDir, "host"), file);
        System.out.println("Updated BIN->" + file);
        JDIO.removeDirectoryOrFile(file = new File(this.updateDir, "jd/plugins/decrypt"));
        copyDirectory(new File(pluginsDir, "decrypt"), file);
        System.out.println("Updated BIN->" + file);

        JDIO.removeDirectoryOrFile(file = new File(this.updateDir, "jd/dynamics"));
        copyDirectory(new File(pluginsDir.getParentFile(), "dynamics"), file);
        System.out.println("Updated BIN->" + file);

    }

    /**
     * Exports svn ressources dir
     * 
     * @throws SVNException
     * @throws IOException
     */
    private void updateSource() throws SVNException, IOException {
        Subversion sv = new Subversion("https://www.syncom.org/svn/jdownloader/trunk/ressourcen/");
        sv.export(svn);
        moveSrcToDif("jd", "jd");
        moveSrcToDif("tools", "tools");
        moveSrcToDif("libs", "libs");
        // moveSrcToDif("jd/captcha", "jd/captcha");
    }

    /**
     * Copies directory a to b
     * 
     * @param srcPath
     * @param dstPath
     * @throws IOException
     */
    public void copyDirectory(File srcPath, File dstPath) throws IOException {
        if (srcPath.getAbsolutePath().contains(".svn")) return;

        if (srcPath.isDirectory()) {
            if (!dstPath.exists()) {
                System.out.println("Create Dir" + dstPath);
                dstPath.mkdir();
            }
            String files[] = srcPath.list();
            for (int i = 0; i < files.length; i++) {
                copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
            }
        } else {
            copyFile(srcPath, dstPath);

        }

    }

    private void copyFile(File srcPath, File dstPath) throws IOException {
        String hashd = JDHash.getMD5(dstPath);
        String hashs = JDHash.getMD5(srcPath);

        if (srcPath.getAbsolutePath().contains(".svn")) return;

        if (!srcPath.exists()) {
            System.out.println("File or directory does not exist.");
            System.exit(0);
        } else {
            if (hashs.equalsIgnoreCase(hashd)) return;
            if (dstPath.exists()) {
                dstPath.delete();
            }
            InputStream in = new FileInputStream(srcPath);
            dstPath.getParentFile().mkdirs();
            dstPath.createNewFile();
            OutputStream out = new FileOutputStream(dstPath);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            System.out.println("        Copy File " + srcPath + " -> " + dstPath);
        }

    }

    /**
     * Copies ressources from svn t update
     * 
     * @param string
     * @param string2
     * @throws IOException
     */
    private void moveSrcToDif(String string, String string2) throws IOException {
        File file;
        JDIO.removeDirectoryOrFile(file = new File(this.updateDir, string2));

        copyDirectory(new File(this.svn, string), file);
        System.out.println("Updated SVN->" + file);

    }

    /**
     * Removes files that are in working dir, but not in hashlist
     * 
     * @throws Exception
     */
    private void removeFileOverhead() throws Exception {
        ArrayList<File> localFiles = getLocalFileList(workingDir, false);
        StringBuilder sb = new StringBuilder();
        ArrayList<String> remRequested = new ArrayList<String>();
        // clear folders in updaedir
        File tmp = new File(this.updateDir, "/tmp/");
        JDIO.removeDirectoryOrFile(tmp);
        tmp.mkdirs();
        tmp = new File(this.updateDir, "/config/");
        JDIO.removeDirectoryOrFile(tmp);
        tmp.mkdirs();
        tmp = new File(this.updateDir, "/backup/");
        JDIO.removeDirectoryOrFile(tmp);
        tmp.mkdirs();
        // clear folders in local dir
        tmp = new File(this.workingDir, "/tmp/");
        JDIO.removeDirectoryOrFile(tmp);
        tmp.mkdirs();

        tmp = new File(this.workingDir, "/config/");
        JDIO.removeDirectoryOrFile(tmp);
        tmp.mkdirs();
        tmp = new File(this.workingDir, "/backup/");
        JDIO.removeDirectoryOrFile(tmp);
        tmp.mkdirs();
        int i = 0;
        for (File f : localFiles) {
            if (!f.isDirectory() && !containsFile(f) && !f.getAbsolutePath().equalsIgnoreCase(workingDir.getAbsolutePath())) {
                sb.append(f.getAbsolutePath() + "\r\n");
                remRequested.add(f.getAbsolutePath());
                i++;
            }

        }
        if (true) {
            String removeFiles = TextAreaDialog.showDialog(frame, "Files to remove", "These " + i + " files were found localy, but not in the remotehashlist. The will be removed if you don't delete them.", sb + "");
            if (removeFiles != null) {
                for (String line : Regex.getLines(removeFiles)) {
                    File del = new File(line.trim());
                    if (del.exists()) {
                        System.out.println("Delete " + del.getAbsolutePath());

                        while (!JDIO.removeDirectoryOrFile(del)) {
                            JOptionPane.showConfirmDialog(frame, "COuld not delete " + del.getAbsolutePath());

                        }
                    }
                }
            }
        }
        /**
         * rest move
         * 
         */
        for (String path : remRequested) {
            File f = new File(path);
            if (f.exists()) {
                String newPath = path.replace(workingDir.getAbsolutePath(), this.updateDir.getAbsolutePath());
                File newFile = new File(newPath);
                if (newFile.exists() && newFile.lastModified() >= f.lastModified()) {
                    System.out.println("Removed " + path + "(newer file in " + updateDir.getAbsolutePath());
                    f.delete();
                } else if (newFile.exists()) {
                    System.out.println("Rename " + path + "-->" + newPath + "(newer file in " + workingDir.getAbsolutePath());
                    newFile.delete();
                    f.renameTo(newFile);
                } else {
                    System.out.println("Move " + path + "->" + newFile.getAbsolutePath());
                    f.renameTo(newFile);
                }
            }

        }
    }

    private void initGUI() {
        this.frame = new JFrame();
        frame.setTitle("Updater");
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
    }

    /**
     * Updates the current working dir. sync with online bin
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void webupdate() {
        try {
            webupdater = new WebUpdater();
            webupdater.setIgnorePlugins(false);
            webupdater.setWorkingdir(workingDir);
            webupdater.setOSFilter(false);
            remoteFileList = webupdater.getAvailableFiles();
            latestBranch = webupdater.getBranch();
            ArrayList<FileUpdate> update = (ArrayList<FileUpdate>) remoteFileList.clone();
            webupdater.filterAvailableUpdates(update);
            System.out.println("UPdate: " + update);
            webupdater.updateFiles(update);
        } catch (Exception e) {
            JDLogger.exception(e);
            remoteFileList = new ArrayList<FileUpdate>();
        }

    }

    /** checks if file f is oart of the hashlist */
    private boolean containsFile(File f) {

        for (FileUpdate fu : remoteFileList) {
            String remote = fu.getLocalFile().getAbsolutePath();
            String local = f.getAbsolutePath();
            if (f.isDirectory()) {
                if (remote.startsWith(local)) { return true; }
            } else {
                if (remote.equals(local)) { return true; }
            }

        }
        return false;
    }

    /**
     * Scans a folder rec. filters addons.lst, src and update_doif folder.
     * 
     * @param dir
     * @param noFilter
     * @return
     */
    private ArrayList<File> getLocalFileList(File dir, boolean noFilter) {
        ArrayList<File> ret = new ArrayList<File>();
        if (noFilter || (!dir.getAbsolutePath().contains("addonlist.lst") && !dir.getAbsolutePath().contains(UPDATE_SUB_SRC) && !dir.getAbsolutePath().contains(UPDATE_SUB_DIR))) {
            ret.add(dir);
        } else {
            return ret;
        }
        for (File f : dir.listFiles()) {
            // if(f.getAbsolutePath().contains("jd\\captcha")){
            // f=f;
            // }
            if (f.isDirectory()) {
                ret.addAll(getLocalFileList(f, noFilter));
            } else {
                if (noFilter || (!f.getAbsolutePath().contains("addonlist.lst") && !f.getAbsolutePath().contains(UPDATE_SUB_SRC) && !f.getAbsolutePath().contains(UPDATE_SUB_DIR))) {
                    ret.add(f);
                }
            }
        }
        return ret;
    }

}
