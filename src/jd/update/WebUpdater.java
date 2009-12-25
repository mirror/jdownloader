//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.update;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JProgressBar;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.event.JDBroadcaster;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.nutils.zip.UnZip;
import jd.parser.Regex;
import jd.utils.JDUtilities;

/**
 * Webupdater lädt pfad und hash infos von einem server und vergleicht sie mit
 * den lokalen versionen
 * 
 * @author JD-Team
 */
public class WebUpdater implements Serializable {

    public static final int DO_UPDATE_FAILED = 40;
    public static final int DO_UPDATE_FILE = 42;

    public static final int DO_UPDATE_SUCCESS = 43;

    private static HashMap<String, File> fileMap;
    private static final int NEW_FILE = 0;
    public static HashMap<String, FileUpdate> PLUGIN_LIST = null;
    private static final long serialVersionUID = 1946622313175234371L;
    private static final int UPDATE_FILE = 1;
    public static final String[] UPDATE_MIRROR = new String[] { "http://update0.jdownloader.org/", "http://update0.jdownloader.org/", "http://update1.jdownloader.org/", "http://update2.jdownloader.org/", };
    private static final String UPDATE_ZIP_LOCAL_PATH = "tmp/update.zip";
    public static final String PARAM_BRANCH = "BRANCH";
    public static final String BRANCHINUSE = "BRANCHINUSE";

    public static HashMap<String, File> getFileMap() {
        return fileMap;
    }

    public static HashMap<String, FileUpdate> getPluginList() {
        if (PLUGIN_LIST == null && JDUtilities.getResourceFile("tmp/hashlist.lst").exists()) {
            PLUGIN_LIST = new HashMap<String, FileUpdate>();
            final WebUpdater updater = new WebUpdater();

            // if
            // (SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE,
            // false)) {
            updater.ignorePlugins(false);
            // }

            updater.parseFileList(JDUtilities.getResourceFile("tmp/hashlist.lst"), null, PLUGIN_LIST);
        }

        return PLUGIN_LIST;
    }

    public static void randomizeMirrors() {
        final ArrayList<String> mirrors = new ArrayList<String>();
        for (String m : UPDATE_MIRROR) {
            mirrors.add(m);
        }
        final int length = UPDATE_MIRROR.length - 1;
        for (int i = 0; i <= length; i++) {
            UPDATE_MIRROR[i] = mirrors.remove((int) (Math.random() * (length - i)));
        }
    }

    private Browser br;

    private String[] branches;
    private transient JDBroadcaster<MessageListener, MessageEvent> broadcaster;
    private Integer errors = 0;

    private boolean ignorePlugins = true;

    private StringBuilder logger;

    private boolean OSFilter = true;

    private JProgressBar progressload = null;

    public byte[] sum;
    private File workingdir;
    private String betaBranch;
    /**
     * if this field !=null, the updater uses this branch and ignores any other
     * branch settings
     */
    private String branch;

    /**
     * @param path
     *            (Dir Pfad zum Updateserver)
     */
    public WebUpdater() {
        randomizeMirrors();
        logger = new StringBuilder();
        this.br = new Browser();
        br.setReadTimeout(20 * 1000);
        br.setConnectTimeout(10 * 1000);
        errors = 0;
        initBroadcaster();
    }

    /**
     * löscht alles files aus files die nicht aktualisiert werden brauchen
     * 
     * @param files
     */
    public void filterAvailableUpdates(ArrayList<FileUpdate> files) {
        // log(files.toString());

        for (Iterator<FileUpdate> it = files.iterator(); it.hasNext();) {
            FileUpdate file = it.next();
            if (new File(file.getLocalFile(), ".noupdate").exists()) {
                System.out.println("User excluded. " + file.getLocalPath());
                it.remove();
            } else {
                if (!file.exists()) {
                    broadcaster.fireEvent(new MessageEvent(this, NEW_FILE, "New: " + WebUpdater.formatPathReadable(file.getLocalPath())));
                    continue;
                } else if (!file.equals()) {

                    broadcaster.fireEvent(new MessageEvent(this, UPDATE_FILE, "Update: " + WebUpdater.formatPathReadable(file.getLocalPath())));

                    continue;
                } else {
                    it.remove();
                }
            }

        }

    }

    /**
     * Liest alle files vom server
     * 
     * @return Vector mit allen verfügbaren files
     * @throws UnsupportedEncodingException
     */
    public ArrayList<FileUpdate> getAvailableFiles() throws Exception {

        HashMap<String, FileUpdate> plugins = new HashMap<String, FileUpdate>();
        ArrayList<FileUpdate> ret = new ArrayList<FileUpdate>();

        updateAvailableServers();
        loadUpdateList();

        parseFileList(fileMap.get("hashlist.lst"), ret, plugins);

        return ret;
    }

    private ArrayList<Server> getAvailableServers() {
        if (Main.clone) return Main.clonePrefix;
        return SubConfiguration.getConfig("WEBUPDATE").getGenericProperty("SERVERLIST", new ArrayList<Server>());
    }

    /**
     * Returns the current branch
     * 
     * @return
     */
    public String getBranch() {
        try {
            if (branch != null) return branch;
            String latestBranch = getLatestBranch();

            String ret = SubConfiguration.getConfig("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH);

            if (ret == null) ret = latestBranch;

            SubConfiguration.getConfig("WEBUPDATE").setProperty(WebUpdater.BRANCHINUSE, ret);
            SubConfiguration.getConfig("WEBUPDATE").save();
            return ret;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * loads branches.lst from a random (0-2) updateserver.
     * 
     * @return
     */
    private String[] getBranches() {
        ArrayList<String> mirrors = new ArrayList<String>();
        for (String m : UPDATE_MIRROR)
            mirrors.add(m);

        for (int i = 0; i < UPDATE_MIRROR.length; i++) {
            String serv = mirrors.remove((int) (Math.random() * (UPDATE_MIRROR.length - 1 - i)));
            try {

                br.getPage(serv + "branches.lst");
                if (br.getRequest().getHttpConnection().isOK()) {
                    String[] bs = Regex.getLines(br.toString());

                    ArrayList<String> ret = new ArrayList<String>();
                    this.branches = new String[bs.length];
                    for (int ii = 0; ii < bs.length; ii++) {

                        if (isBetaBranch(bs[ii]) && betaBranch == null) {
                            betaBranch = bs[ii];
                        } else if (!isBetaBranch(bs[ii])) {
                            ret.add(bs[ii]);
                        }

                    }
                    branches = ret.toArray(new String[] {});
                    System.out.println("Found branches on " + serv + ":\r\n" + br);

                    String savedBranch = SubConfiguration.getConfig("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH);

                    if (branches.length > 0 && savedBranch != null && isBetaBranch(savedBranch)) {

                        if (betaBranch == null || !savedBranch.equals(betaBranch)) {
                            SubConfiguration.getConfig("WEBUPDATE").setProperty(WebUpdater.PARAM_BRANCH, branches[0]);
                            SubConfiguration.getConfig("WEBUPDATE").save();
                            JDLogger.getLogger().severe("RESETTED BRANCH; SINCE BETA branch " + savedBranch + " is not available any more");
                        }

                    }

                    return branches;
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorWait();
            }
            System.err.println("No branches found on " + serv);
        }
        branches = new String[] {};
        return branches;
    }

    public static boolean isBetaBranch(String string) {
        return string.trim().startsWith("beta_");
    }

    public String getBetaBranch() {
        return betaBranch;
    }

    public JDBroadcaster<MessageListener, MessageEvent> getBroadcaster() {
        return broadcaster;
    }

    public int getErrors() {
        synchronized (errors) {
            return errors;
        }
    }

    /**
     * loads branches.lst and returns the latest branch
     * 
     * @return
     */
    private synchronized String getLatestBranch() {
        if (branches == null) {
            this.getBranches();
        }
        if (branches == null || branches.length == 0) return null;

        return branches[0];
    }

    public String getListPath(int trycount) {
        if (getBranch() == null) return null;
        return UPDATE_MIRROR[trycount % UPDATE_MIRROR.length] + getBranch() + "_server.list";
    }

    public StringBuilder getLogger() {
        return logger;
    }

    public boolean getOSFilter() {
        return this.OSFilter;
    }

    public File getWorkingdir() {
        return workingdir;
    }

    private String getZipMD5(int trycount) {

        return UPDATE_MIRROR[trycount % UPDATE_MIRROR.length] + getBranch() + "_update.md5";
    }

    private String getZipUrl(int trycount) {
        return UPDATE_MIRROR[trycount % UPDATE_MIRROR.length] + getBranch() + "_update.zip";
    }

    /**
     * By defauklt, the webupdater does NOT Update plugins
     * (hoster,decrypter,...) set this flag to false if you want to do a full
     * update
     * 
     * @param b
     */
    public void ignorePlugins(boolean b) {
        this.ignorePlugins = b;
    }

    private void initBroadcaster() {
        this.broadcaster = new JDBroadcaster<MessageListener, MessageEvent>() {

            @Override
            protected void fireEvent(MessageListener listener, MessageEvent event) {
                listener.onMessage(event);

            }

        };
    }

    public boolean isIgnorePlugins() {
        return ignorePlugins;
    }

    private void loadUpdateList() throws Exception {
        for (int trycount = 0; trycount < 10; trycount++) {
            try {
                String path = getZipMD5(trycount);
                if (path == null) continue;
                String serverHash = br.getPage(path + "?t=" + System.currentTimeMillis()).trim();
                String localHash = JDHash.getMD5(JDUtilities.getResourceFile(UPDATE_ZIP_LOCAL_PATH));
                if (!serverHash.equalsIgnoreCase(localHash)) {
                    path = getZipUrl(trycount);
                    if (path == null) continue;
                    Browser.download(JDUtilities.getResourceFile(UPDATE_ZIP_LOCAL_PATH), path + "?t=" + System.currentTimeMillis());
                }
                UnZip u = new UnZip(JDUtilities.getResourceFile(UPDATE_ZIP_LOCAL_PATH), JDUtilities.getResourceFile("tmp/"));

                File[] efiles = u.extract();
                fileMap = new HashMap<String, File>();
                for (File f : efiles) {
                    fileMap.put(f.getName().toLowerCase(), f);
                }
                return;
            } catch (Exception e) {
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                continue;
            }
        }
        throw new Exception("could not load Updatelist");
    }

    private void parseFileList(File file, ArrayList<FileUpdate> ret, HashMap<String, FileUpdate> plugins) {
        String source;
        source = JDIO.readFileToString(file);

        String pattern = "[\r\n\\;]*([^=]+)\\=(.*?)\\;";

        if (source == null) {
            System.out.println("filelist nicht verfüpgbar");
            return;
        }
        FileUpdate entry;

        String[] os = new String[] { "windows", "mac", "linux" };
        String[][] matches = new Regex(source, pattern).getMatches();
        ArrayList<Byte> sum = new ArrayList<Byte>();
        for (String[] m : matches) {

            if (this.workingdir != null) {
                entry = new FileUpdate(m[0], m[1], workingdir);
            } else {
                entry = new FileUpdate(m[0], m[1]);
            }

            entry.getBroadcaster().addAllListener(broadcaster.getListener());

            sum.add((byte) entry.getRemoteHash().charAt(0));

            if (entry.getLocalPath().endsWith(".class")) {
                plugins.put(entry.getLocalPath(), entry);
            }

            if (!entry.getLocalPath().endsWith(".class") || !this.ignorePlugins) {
                boolean osFound = false;
                boolean correctOS = false;
                for (String element : os) {
                    String url = entry.getRawUrl();
                    if (url == null) url = entry.getRelURL();
                    if (url.toLowerCase().indexOf(element) >= 0) {
                        osFound = true;
                        if (OSDetector.getOSString().toLowerCase().indexOf(element) >= 0) {
                            correctOS = true;
                        }
                    }

                }
                if (this.OSFilter == true) {
                    if (!osFound || osFound && correctOS) {
                        if (ret != null) ret.add(entry);
                    } else {
                        String url = entry.getRawUrl();
                        if (url == null) url = entry.getRelURL();
                        System.out.println("OS Filter: " + url);

                    }
                } else {
                    if (ret != null) ret.add(entry);
                }
            }

        }
        this.sum = new byte[sum.size()];
        int ii = 0;
        for (int i = sum.size() - 1; i >= 0; i--) {
            this.sum[ii++] = sum.get(i);
        }

        WebUpdater.PLUGIN_LIST = plugins;

    }

    public void resetErrors() {
        synchronized (errors) {
            errors = 0;
        }
    }

    public void setDownloadProgress(JProgressBar progresslist) {
        progressload = progresslist;
    }

    public void setIgnorePlugins(boolean ignorePlugins) {
        this.ignorePlugins = ignorePlugins;
    }

    public void setLogger(StringBuilder log) {
        logger = log;
    }

    public void setOSFilter(boolean filter) {
        this.OSFilter = filter;
    }

    public void setWorkingdir(File workingdir) {
        this.workingdir = workingdir;
    }

    @Override
    public String toString() {
        return "Updater";
    }

    private ArrayList<Server> updateAvailableServers() {

        boolean fnf = true;
        for (int trycount = 0; trycount < 10; trycount++) {
            try {
                broadcaster.fireEvent(new MessageEvent(this, 0, "Update Downloadmirrors"));

                String path = getListPath(trycount);
                if (path == null) continue;
                br.getPage(path + "?t=" + System.currentTimeMillis());
                if (br.getRequest().getHttpConnection().getResponseCode() != 404l) {
                    fnf = false;

                }
                if (br.getRequest().getHttpConnection().getResponseCode() != 200l) {
                    errorWait();
                    continue;
                }
                int total = 0;
                ArrayList<Server> servers = new ArrayList<Server>();
                Server serv;
                boolean auto = false;
                for (String[] match : br.getRegex("(\\-?\\d+)\\:([^\r^\n]*)").getMatches()) {
                    servers.add(serv = new Server(Integer.parseInt(match[0]), match[1].trim()));
                    if (serv.getPercent() < 0) auto = true;
                    total += serv.getPercent();
                }
                for (Server s : servers) {
                    if (auto) {
                        s.setPercent(-1);
                    } else {
                        s.setPercent((s.getPercent() * 100) / total);
                    }
                    broadcaster.fireEvent(new MessageEvent(this, 0, "Updateserver: " + s));

                }
                if (servers.size() > 0) {
                    SubConfiguration.getConfig("WEBUPDATE").setProperty("SERVERLIST", servers);
                }
                return getAvailableServers();
            } catch (Exception e) {
                e.printStackTrace();
                errorWait();
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                continue;
            }
        }
        if (fnf && SubConfiguration.getConfig("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH) != null) {
            System.err.println("Branch " + SubConfiguration.getConfig("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH) + " is not available any more. Reset to default");
            SubConfiguration.getConfig("WEBUPDATE").setProperty(WebUpdater.PARAM_BRANCH, null);
            SubConfiguration.getConfig("WEBUPDATE").save();
            return updateAvailableServers();
        }
        return getAvailableServers();
    }

    private void errorWait() {
        try {
            broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FAILED, "Server Busy. Wait 10 Seconds"));
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void updateFile(Vector<String> file) throws IOException {

        String[] tmp = file.elementAt(0).split("\\?");

        broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FILE, String.format("Download %s to %s", WebUpdater.formatPathReadable(tmp[1]), WebUpdater.formatPathReadable(JDUtilities.getResourceFile(tmp[0]).getAbsolutePath()))));

        Browser.download(JDUtilities.getResourceFile(tmp[0]), tmp[0]);

    }

    /**
     * Updated alle files in files
     * 
     * @param files
     * @param prg
     *            TODO
     * @throws IOException
     */
    public void updateFiles(ArrayList<FileUpdate> files, ProgressController prg) throws IOException {

        if (progressload != null) {
            progressload.setMaximum(files.size());
        }

        int i = 0;
        if (prg != null) prg.addToMax(files.size());
        for (FileUpdate file : files) {
            try {
                broadcaster.fireEvent(new MessageEvent(this, 0, String.format("Update %s", WebUpdater.formatPathReadable(file.getLocalPath()))));
                if (updateUpdatefile(file)) {
                    broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_SUCCESS, WebUpdater.formatPathReadable(file.toString())));
                    broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_SUCCESS, "Successfull"));
                } else {
                    broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FAILED, WebUpdater.formatPathReadable(file.toString())));
                    broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FAILED, "Failed"));
                    if (progressload != null) progressload.setForeground(Color.RED);
                    if (prg != null) prg.setColor(Color.RED);
                }

            } catch (Exception e) {
                e.printStackTrace();
                broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FAILED, e.getLocalizedMessage()));
                broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FAILED, WebUpdater.formatPathReadable(file.toString())));
                broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FAILED, "Failed"));
                if (progressload != null) progressload.setForeground(Color.RED);
                if (prg != null) prg.setColor(Color.RED);
            }

            i++;

            if (progressload != null) {
                progressload.setValue(i);
            }

            if (prg != null) prg.increase(1);
        }
        if (progressload != null) {
            progressload.setValue(100);
        }
    }

    public static String formatPathReadable(String localPath) {
        localPath = localPath.replace(".class", "-Plugin");
        localPath = localPath.replace(".jar", "-Module");
        localPath = localPath.replace("plugins/decrypter/.*", "Decrypter-Plugin");
        return localPath;
    }

    public boolean updateUpdatefile(FileUpdate file) {
        if (file.update(getAvailableServers())) {
            if (file.getLocalTmpFile().getName().endsWith(".extract")) {
                UnZip u = new UnZip(file.getLocalTmpFile(), file.getLocalTmpFile().getParentFile());
                u.setOverwrite(false);
                File[] efiles;
                try {
                    efiles = u.extract();
                    broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_SUCCESS, "Extracted " + file.getLocalTmpFile().getName() + ": " + efiles.length + " files"));
                } catch (Exception e) {
                    broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FAILED, e.getLocalizedMessage()));
                    broadcaster.fireEvent(new MessageEvent(this, DO_UPDATE_FAILED, "Extracting " + file.getLocalTmpFile().getAbsolutePath() + " failed"));
                    e.printStackTrace();
                    file.getLocalTmpFile().delete();
                    file.getLocalTmpFile().deleteOnExit();
                    errors++;
                    return false;
                }
            }

            return true;
        }
        errors++;
        return false;
    }

    public void cleanUp() {
    }

    /**
     * sets the branch to use. This overwrites the webupdater settings. This
     * means that the updater uses this branch and ignores anything else
     * 
     * @param branchtoUse
     */
    public void setBranch(String branchtoUse) {
        this.branch = branchtoUse;

    }

    /**
     * Return the internal browser object
     * 
     * @return
     */
    public Browser getBrowser() {
        // TODO Auto-generated method stub
        return br;
    }

}