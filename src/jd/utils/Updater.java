package jd.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.config.CFGConfig;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.nutils.svn.Subversion;
import jd.parser.Regex;
import jd.update.FileUpdate;
import jd.update.WebUpdater;

import org.tmatesoft.svn.core.SVNException;

public class Updater {

    private File pluginsDir;

    private WebUpdater webupdater;
    private ArrayList<FileUpdate> remoteFileList;
    private File workingDir;
    private JFrame frame;
    private static String UPDATE_SUB_DIR = "exclude_jd_update";
    private static String UPDATE_SUB_SRC = "exclude_jd_src";
    private File updateDir;
    private File svn;

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

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Updater upd = new Updater();
        System.out.println("STATUS: Webupdate");
        upd.webupdate();
        System.out.println("STATUS: Webupdate ende");
        System.out.println("STATUS: Scan local");
        upd.removeFileOverhead();
        if (JOptionPane.showConfirmDialog(upd.getFrame(), "SVN UPdate") == JOptionPane.OK_OPTION) {
            System.out.println("STATUS: update svn");
            upd.updateSource();
        }
        System.out.println("STATUS: move plugins");
        upd.movePlugins(getCFG("plugins_dir"));
        System.out.println("STATUS: FINISHED");
        ArrayList<File> list = upd.getFileList();

        upd.upload(list);

        upd.merge();
        upd.uploadHashList();
        upd.spread(list);
        System.exit(0);
    }

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
        System.out.println(br.getPage("http://update1.jdownloader.org/spread.php?pass=" + getCFG("server_pass")));

        System.out.println("Spread ok");
    }

    private void uploadHashList() throws IOException {
        while (true) {
            HashMap<String, String> map = createHashList(this.workingDir);
            Browser br = new Browser();
            br.forceDebug(true);

            map.put("pass", getCFG("updateHashPW"));
            File file = new File(this.workingDir, "addonlist.lst");

            String addonlist = JDIO.getLocalFile(file);
            map.put("addonlist", Encoding.urlEncode(addonlist));
            br.postPage("http://update1.jdownloader.org/updateHashList.php", map);
            System.out.println(br + "");
            if (br.containsHTML("success") && !br.containsHTML("<b>Warning</b>") && !br.containsHTML("<b>Error</b>")) break;

            JOptionPane.showConfirmDialog(frame, "MD5 ERROR!!!! See log");
        }
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
        Browser br = new Browser();
        System.out.println("Starting upload: " + list.size() + " files");
        br.forceDebug(true);
        PostFormDataRequest request = (PostFormDataRequest) br.createPostFormDataRequest("http://update1.jdownloader.org/upload.php");
        int i = 0;
        request.addFormData(new FormData("fileNum", list.size() + ""));
        request.addFormData(new FormData("pass", getCFG("server_pass") + ""));
        for (File f : list) {
            if (i > 10) {
                URLConnectionAdapter con = br.openRequestConnection(request);
                String res = br.loadConnection(con);
                if (res.contains("hash failed") || res.contains("Forbidden") || res.contains("Warning</b>") || res.contains("Error</b>")) {
                    System.err.println("Error uploading: " + res);
                    return false;
                } else {
                    System.out.println(res);

                }

                request = (PostFormDataRequest) br.createPostFormDataRequest("http://update1.jdownloader.org/upload.php");
                i = 0;
                request.addFormData(new FormData("fileNum", list.size() + ""));
                request.addFormData(new FormData("pass", getCFG("server_pass") + ""));
            }
            String newFile = f.getAbsolutePath().replace(updateDir.getAbsolutePath(), "");
            newFile = newFile.replace("\\", "/");
            if (newFile.trim().length() == 0) continue;
            i++;
            if (f.isDirectory()) {

                request.addFormData(new FormData("path_" + i, newFile));

            } else {
                request.addFormData(new FormData("file_" + i, f.getName(), f));
                request.addFormData(new FormData("path_" + i, newFile));
                request.addFormData(new FormData("hash_" + i, JDHash.getMD5(f)));
            }
        }

        URLConnectionAdapter con = br.openRequestConnection(request);
        String res = br.loadConnection(con);
        if (res.contains("hash failed") || res.contains("Forbidden") || res.contains("Warning</b>") || res.contains("Error</b>")) {
            System.err.println("Error uploading: " + res);
            return false;
        } else {
            System.out.println(res);

        }
        System.out.println("Succeded hashes ok");
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
        ArrayList<File> listUpdate = this.getLocalFileList(this.updateDir, true);
        listUpdate.remove(0);
        ArrayList<File> listLocal = this.getLocalFileList(this.workingDir, false);
        main: for (Iterator<File> it = listUpdate.iterator(); it.hasNext();) {
            File file = it.next();
            String newHash = JDHash.getMD5(file);
            String newFile = file.getAbsolutePath().replace(updateDir.getAbsolutePath(), "");
            newFile = newFile.replace("\\", "/");
            if (newFile.trim().length() == 0) continue;

            for (File rf : listLocal) {
                String localFile = rf.getAbsolutePath().replace(workingDir.getAbsolutePath(), "");
                localFile = localFile.replace("\\", "/");
                if (localFile.trim().length() == 0) continue;
                if (localFile.equalsIgnoreCase(newFile)) {
                    String localHash = JDHash.getMD5(rf);
                    if (localHash == newHash || localHash.equalsIgnoreCase(newHash)) {
                        it.remove();
                        continue main;
                    } else {
                        System.out.println("Update: " + rf);
                    }
                }
            }
        }
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
        moveSrcToDif("jd/languages", "jd/languages");
        moveSrcToDif("jd/captcha", "jd/captcha");
    }

    /**
     * Copies directory a to b
     * 
     * @param srcPath
     * @param dstPath
     * @throws IOException
     */
    public void copyDirectory(File srcPath, File dstPath) throws IOException {

        if (srcPath.isDirectory()) {
            if (!dstPath.exists()) {
                dstPath.mkdir();
            }
            String files[] = srcPath.list();
            for (int i = 0; i < files.length; i++) {
                copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
            }
        } else {
            if (!srcPath.exists()) {
                System.out.println("File or directory does not exist.");
                System.exit(0);
            } else {
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
            }
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

            ArrayList<FileUpdate> update = (ArrayList<FileUpdate>) remoteFileList.clone();
            webupdater.filterAvailableUpdates(update);
            System.out.println("UPdate: " + update);
            webupdater.updateFiles(update);
        } catch (Exception e) {
            e.printStackTrace();
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
