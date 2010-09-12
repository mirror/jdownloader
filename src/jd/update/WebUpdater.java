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
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.nutils.zip.UnZip;
import jd.parser.Regex;
import jd.utils.JDUtilities;

import org.appwork.utils.event.Eventsender;

/**
 * Webupdater lädt pfad und hash infos von einem server und vergleicht sie mit
 * den lokalen versionen
 * 
 * @author JD-Team
 */
public class WebUpdater implements Serializable {

    public static final int                   DO_UPDATE_FAILED      = 40;
    public static final int                   DO_UPDATE_FILE        = 42;

    public static final int                   DO_UPDATE_SUCCESS     = 43;

    private static HashMap<String, File>      fileMap;
    private static final int                  NEW_FILE              = 0;
    public static HashMap<String, FileUpdate> PLUGIN_LIST           = null;
    private static final long                 serialVersionUID      = 1946622313175234371L;
    private static final int                  UPDATE_FILE           = 1;
    public static final String[]              UPDATE_MIRROR         = new String[] { "http://update0.jdownloader.org/", "http://update1.jdownloader.org/", "http://update2.jdownloader.org/", "http://update3.jdownloader.org/", };
    private static final String               UPDATE_ZIP_LOCAL_PATH = "tmp/update.zip";
    public static final String                PARAM_BRANCH          = "BRANCH";
    public static final String                BRANCHINUSE           = "BRANCHINUSE";

    public static String formatPathReadable(String localPath) {
        localPath = localPath.replace(".class", "-Plugin");
        localPath = localPath.replace(".jar", "-Module");
        localPath = localPath.replace("plugins/decrypter/.*", "Decrypter-Plugin");
        return localPath;
    }

    public static HashMap<String, File> getFileMap() {
        return WebUpdater.fileMap;
    }

    public static HashMap<String, FileUpdate> getPluginList() {
        if (WebUpdater.PLUGIN_LIST == null && JDUtilities.getResourceFile("tmp/hashlist.lst").exists()) {
            WebUpdater.PLUGIN_LIST = new HashMap<String, FileUpdate>();
            final WebUpdater updater = new WebUpdater();

            // if
            // (SubConfiguration.getConfig("WEBUPDATE").getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE,
            // false)) {
            updater.ignorePlugins(false);
            // }

            updater.parseFileList(JDUtilities.getResourceFile("tmp/hashlist.lst"), null, WebUpdater.PLUGIN_LIST);
        }

        return WebUpdater.PLUGIN_LIST;
    }

    public static boolean isBetaBranch(final String string) {
        return string.trim().startsWith("beta_");
    }

    public static void randomizeMirrors() {
        final ArrayList<String> mirrors = new ArrayList<String>();
        for (final String m : WebUpdater.UPDATE_MIRROR) {
            mirrors.add(m);
        }
        final int length = WebUpdater.UPDATE_MIRROR.length - 1;
        for (int i = 0; i <= length; i++) {
            WebUpdater.UPDATE_MIRROR[i] = mirrors.remove((int) (Math.random() * (length - i)));
        }
    }

    private final Browser                                        br;
    private String[]                                             branches;

    private transient Eventsender<MessageListener, MessageEvent> broadcaster;

    private Integer                                              errors        = 0;

    private boolean                                              ignorePlugins = true;

    private StringBuilder                                        logger;

    private boolean                                              OSFilter      = true;
    private JProgressBar                                         progressload  = null;
    public byte[]                                                sum;
    private File                                                 workingdir;

    private String                                               betaBranch;

    /**
     * if this field !=null, the updater uses this branch and ignores any other
     * branch settings
     */
    private String                                               branch;

    /**
     * @param path
     *            (Dir Pfad zum Updateserver)
     */
    public WebUpdater() {
        WebUpdater.randomizeMirrors();
        this.logger = new StringBuilder();
        this.br = new Browser();
        this.br.setReadTimeout(20 * 1000);
        this.br.setConnectTimeout(10 * 1000);
        this.errors = 0;
        this.initBroadcaster();
    }

    public void cleanUp() {
    }

    private void errorWait() {
        try {
            this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FAILED, "Server Busy. Wait 10 Seconds"));
            Thread.sleep(10000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * löscht alles files aus files die nicht aktualisiert werden brauchen
     * 
     * @param files
     */
    public void filterAvailableUpdates(final ArrayList<FileUpdate> files) {
        // log(files.toString());

        for (final Iterator<FileUpdate> it = files.iterator(); it.hasNext();) {
            final FileUpdate file = it.next();
            if (new File(file.getLocalFile(), ".noupdate").exists()) {
                System.out.println("User excluded. " + file.getLocalPath());
                it.remove();
            } else {
                if (!file.exists()) {
                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.NEW_FILE, "New: " + WebUpdater.formatPathReadable(file.getLocalPath())));
                    continue;
                } else if (!file.equals()) {

                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.UPDATE_FILE, "Update: " + WebUpdater.formatPathReadable(file.getLocalPath())));

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

        final HashMap<String, FileUpdate> plugins = new HashMap<String, FileUpdate>();
        final ArrayList<FileUpdate> ret = new ArrayList<FileUpdate>();

        this.updateAvailableServers();
        this.loadUpdateList();

        this.parseFileList(WebUpdater.fileMap.get("hashlist.lst"), ret, plugins);

        return ret;
    }

    private ArrayList<Server> getAvailableServers() {
        if (Main.clone) { return Main.clonePrefix; }
        return SubConfiguration.getConfig("WEBUPDATE").getGenericProperty("SERVERLIST", new ArrayList<Server>());
    }

    public String getBetaBranch() {
        return this.betaBranch;
    }

    /**
     * Returns the current branch
     * 
     * @return
     */
    public String getBranch() {
        try {
            if (this.branch != null) { return this.branch; }
            final String latestBranch = this.getLatestBranch();

            String ret = SubConfiguration.getConfig("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH);

            if (ret == null) {
                ret = latestBranch;
            }

            SubConfiguration.getConfig("WEBUPDATE").setProperty(WebUpdater.BRANCHINUSE, ret);
            SubConfiguration.getConfig("WEBUPDATE").save();
            return ret;
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * loads branches.lst from a random (0-2) updateserver.
     * 
     * @return
     */
    private String[] getBranches() {
        final ArrayList<String> mirrors = new ArrayList<String>();
        for (final String m : WebUpdater.UPDATE_MIRROR) {
            mirrors.add(m);
        }

        for (int i = 0; i < WebUpdater.UPDATE_MIRROR.length; i++) {
            final String serv = mirrors.remove((int) (Math.random() * (WebUpdater.UPDATE_MIRROR.length - 1 - i)));
            try {

                this.br.getPage(serv + "branches.lst");
                if (this.br.getRequest().getHttpConnection().isOK()) {
                    final String[] bs = org.appwork.utils.Regex.getLines(this.br.toString());

                    final ArrayList<String> ret = new ArrayList<String>();
                    this.branches = new String[bs.length];
                    for (int ii = 0; ii < bs.length; ii++) {

                        if (WebUpdater.isBetaBranch(bs[ii]) && this.betaBranch == null) {
                            this.betaBranch = bs[ii];
                        } else if (!WebUpdater.isBetaBranch(bs[ii])) {
                            ret.add(bs[ii]);
                        }

                    }
                    this.branches = ret.toArray(new String[] {});
                    System.out.println("Found branches on " + serv + ":\r\n" + this.br);

                    final String savedBranch = SubConfiguration.getConfig("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH);

                    if (this.branches.length > 0 && savedBranch != null && WebUpdater.isBetaBranch(savedBranch)) {

                        if (this.betaBranch == null || !savedBranch.equals(this.betaBranch)) {
                            SubConfiguration.getConfig("WEBUPDATE").setProperty(WebUpdater.PARAM_BRANCH, this.branches[0]);
                            SubConfiguration.getConfig("WEBUPDATE").save();
                            JDLogger.getLogger().severe("RESETTED BRANCH; SINCE BETA branch " + savedBranch + " is not available any more");
                        }

                    }

                    return this.branches;
                }
            } catch (final Exception e) {
                e.printStackTrace();
                this.errorWait();
            }
            System.err.println("No branches found on " + serv);
        }
        return this.branches = new String[] {};
    }

    public Eventsender<MessageListener, MessageEvent> getBroadcaster() {
        return this.broadcaster;
    }

    /**
     * Return the internal browser object
     * 
     * @return
     */
    public Browser getBrowser() {
        // TODO Auto-generated method stub
        return this.br;
    }

    public int getErrors() {
        synchronized (this.errors) {
            return this.errors;
        }
    }

    /**
     * loads branches.lst and returns the latest branch
     * 
     * @return
     */
    private synchronized String getLatestBranch() {
        if (this.branches == null) {
            this.getBranches();
        }
        if (this.branches == null || this.branches.length == 0) { return null; }

        return this.branches[0];
    }

    public String getListPath(final int trycount) {
        if (this.getBranch() == null) { return null; }
        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + this.getBranch() + "_server.list";
    }

    public StringBuilder getLogger() {
        return this.logger;
    }

    public boolean getOSFilter() {
        return this.OSFilter;
    }

    public File getWorkingdir() {
        return this.workingdir;
    }

    private String getZipMD5(final int trycount) {

        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + this.getBranch() + "_update.md5";
    }

    private String getZipUrl(final int trycount) {
        return WebUpdater.UPDATE_MIRROR[trycount % WebUpdater.UPDATE_MIRROR.length] + this.getBranch() + "_update.zip";
    }

    /**
     * By defauklt, the webupdater does NOT Update plugins
     * (hoster,decrypter,...) set this flag to false if you want to do a full
     * update
     * 
     * @param b
     */
    public void ignorePlugins(final boolean b) {
        this.ignorePlugins = b;
    }

    private void initBroadcaster() {
        this.broadcaster = new Eventsender<MessageListener, MessageEvent>() {

            @Override
            protected void fireEvent(final MessageListener listener, final MessageEvent event) {
                listener.onMessage(event);

            }

        };
    }

    public boolean isIgnorePlugins() {
        return this.ignorePlugins;
    }

    private void loadUpdateList() throws Exception {
        for (int trycount = 0; trycount < 10; trycount++) {
            try {
                String path = this.getZipMD5(trycount);
                if (path == null) {
                    continue;
                }
                final String serverHash = this.br.getPage(path + "?t=" + System.currentTimeMillis()).trim();
                final String localHash = JDHash.getMD5(JDUtilities.getResourceFile(WebUpdater.UPDATE_ZIP_LOCAL_PATH));
                if (!serverHash.equalsIgnoreCase(localHash)) {
                    path = this.getZipUrl(trycount);
                    if (path == null) {
                        continue;
                    }
                    Browser.download(JDUtilities.getResourceFile(WebUpdater.UPDATE_ZIP_LOCAL_PATH), path + "?t=" + System.currentTimeMillis());
                }
                final UnZip u = new UnZip(JDUtilities.getResourceFile(WebUpdater.UPDATE_ZIP_LOCAL_PATH), JDUtilities.getResourceFile("tmp/"));

                final File[] efiles = u.extract();
                WebUpdater.fileMap = new HashMap<String, File>();
                for (final File f : efiles) {
                    WebUpdater.fileMap.put(f.getName().toLowerCase(), f);
                }
                return;
            } catch (final Exception e) {
            }
            try {
                Thread.sleep(250);
            } catch (final InterruptedException e) {
                continue;
            }
        }
        throw new Exception("could not load Updatelist");
    }

    private void parseFileList(final File file, final ArrayList<FileUpdate> ret, final HashMap<String, FileUpdate> plugins) {
        String source;
        source = JDIO.readFileToString(file);

        final String pattern = "[\r\n\\;]*([^=]+)\\=(.*?)\\;";

        if (source == null) {
            System.out.println("filelist nicht verfüpgbar");
            return;
        }
        FileUpdate entry;

        final String[] os = new String[] { "windows", "mac", "linux" };
        final String[][] matches = new Regex(source, pattern).getMatches();
        final ArrayList<Byte> sum = new ArrayList<Byte>();
        for (final String[] m : matches) {

            if (this.workingdir != null) {
                entry = new FileUpdate(m[0], m[1], this.workingdir);
            } else {
                entry = new FileUpdate(m[0], m[1]);
            }

            entry.getBroadcaster().addAllListener(this.broadcaster.getListener());

            sum.add((byte) entry.getRemoteHash().charAt(0));

            if (entry.getLocalPath().endsWith(".class")) {
                plugins.put(entry.getLocalPath(), entry);
            }

            if (!entry.getLocalPath().endsWith(".class") || !this.ignorePlugins) {
                boolean osFound = false;
                boolean correctOS = false;
                for (final String element : os) {
                    String url = entry.getRawUrl();
                    if (url == null) {
                        url = entry.getRelURL();
                    }
                    if (url.toLowerCase().indexOf(element) >= 0) {
                        osFound = true;
                        if (System.getProperty("os.name").toLowerCase().indexOf(element) >= 0) {
                            correctOS = true;
                        }
                    }

                }
                if (this.OSFilter == true) {
                    if (!osFound || osFound && correctOS) {
                        if (ret != null) {
                            ret.add(entry);
                        }
                    } else {
                        String url = entry.getRawUrl();
                        if (url == null) {
                            url = entry.getRelURL();
                        }
                        System.out.println("OS Filter: " + url);

                    }
                } else {
                    if (ret != null) {
                        ret.add(entry);
                    }
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
        synchronized (this.errors) {
            this.errors = 0;
        }
    }

    /**
     * sets the branch to use. This overwrites the webupdater settings. This
     * means that the updater uses this branch and ignores anything else
     * 
     * @param branchtoUse
     */
    public void setBranch(final String branchtoUse) {
        this.branch = branchtoUse;

    }

    public void setDownloadProgress(final JProgressBar progresslist) {
        this.progressload = progresslist;
    }

    public void setIgnorePlugins(final boolean ignorePlugins) {
        this.ignorePlugins = ignorePlugins;
    }

    public void setLogger(final StringBuilder log) {
        this.logger = log;
    }

    public void setOSFilter(final boolean filter) {
        this.OSFilter = filter;
    }

    public void setWorkingdir(final File workingdir) {
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
                this.broadcaster.fireEvent(new MessageEvent(this, 0, "Update Downloadmirrors"));

                final String path = this.getListPath(trycount);
                if (path == null) {
                    continue;
                }
                this.br.getPage(path + "?t=" + System.currentTimeMillis());
                if (this.br.getRequest().getHttpConnection().getResponseCode() == 404l) {
                    /*
                     * if branchname is not available on any server then its no
                     * longer valid
                     */
                    fnf = false;
                }
                if (this.br.getRequest().getHttpConnection().getResponseCode() != 200l) {
                    this.errorWait();
                    continue;
                }
                int total = 0;
                final ArrayList<Server> servers = new ArrayList<Server>();
                Server serv;
                boolean auto = false;
                for (final String[] match : this.br.getRegex("(\\-?\\d+)\\:([^\r^\n]*)").getMatches()) {
                    servers.add(serv = new Server(Integer.parseInt(match[0]), match[1].trim()));
                    if (serv.getPercent() < 0) {
                        auto = true;
                    }
                    total += serv.getPercent();
                }
                for (final Server s : servers) {
                    if (auto) {
                        s.setPercent(-1);
                    } else {
                        s.setPercent(s.getPercent() * 100 / total);
                    }
                    this.broadcaster.fireEvent(new MessageEvent(this, 0, "Updateserver: " + s));

                }
                if (servers.size() > 0) {
                    SubConfiguration.getConfig("WEBUPDATE").setProperty("SERVERLIST", servers);
                }
                return this.getAvailableServers();
            } catch (final Exception e) {
                e.printStackTrace();
                this.errorWait();
            }
            try {
                Thread.sleep(250);
            } catch (final InterruptedException e) {
                continue;
            }
        }
        if (fnf && SubConfiguration.getConfig("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH) != null) {
            System.err.println("Branch " + SubConfiguration.getConfig("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH) + " is not available any more. Reset to default");
            SubConfiguration.getConfig("WEBUPDATE").setProperty(WebUpdater.PARAM_BRANCH, null);
            SubConfiguration.getConfig("WEBUPDATE").save();
            return this.updateAvailableServers();
        }
        return this.getAvailableServers();
    }

    public void updateFile(final Vector<String> file) throws IOException {

        final String[] tmp = file.elementAt(0).split("\\?");

        this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FILE, String.format("Download %s to %s", WebUpdater.formatPathReadable(tmp[1]), WebUpdater.formatPathReadable(JDUtilities.getResourceFile(tmp[0]).getAbsolutePath()))));

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
    public void updateFiles(final ArrayList<FileUpdate> files, final ProgressController prg) throws IOException {

        if (this.progressload != null) {
            this.progressload.setMaximum(files.size());
        }

        int i = 0;
        if (prg != null) {
            prg.addToMax(files.size());
        }
        for (final FileUpdate file : files) {
            try {
                this.broadcaster.fireEvent(new MessageEvent(this, 0, String.format("Update %s", WebUpdater.formatPathReadable(file.getLocalPath()))));
                if (this.updateUpdatefile(file)) {
                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_SUCCESS, WebUpdater.formatPathReadable(file.toString())));
                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_SUCCESS, "Successfull"));
                } else {
                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FAILED, WebUpdater.formatPathReadable(file.toString())));
                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FAILED, "Failed"));
                    if (this.progressload != null) {
                        this.progressload.setForeground(Color.RED);
                    }
                    if (prg != null) {
                        prg.setColor(Color.RED);
                    }
                }

            } catch (final Exception e) {
                e.printStackTrace();
                this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FAILED, e.getLocalizedMessage()));
                this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FAILED, WebUpdater.formatPathReadable(file.toString())));
                this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FAILED, "Failed"));
                if (this.progressload != null) {
                    this.progressload.setForeground(Color.RED);
                }
                if (prg != null) {
                    prg.setColor(Color.RED);
                }
            }

            i++;

            if (this.progressload != null) {
                this.progressload.setValue(i);
            }

            if (prg != null) {
                prg.increase(1);
            }
        }
        if (this.progressload != null) {
            this.progressload.setValue(100);
        }
    }

    public boolean updateUpdatefile(final FileUpdate file) {
        if (file.update(this.getAvailableServers())) {
            if (file.getLocalTmpFile().getName().endsWith(".extract")) {
                final UnZip u = new UnZip(file.getLocalTmpFile(), file.getLocalTmpFile().getParentFile());
                u.setOverwrite(false);
                File[] efiles;
                try {
                    efiles = u.extract();
                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_SUCCESS, "Extracted " + file.getLocalTmpFile().getName() + ": " + efiles.length + " files"));
                } catch (final Exception e) {
                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FAILED, e.getLocalizedMessage()));
                    this.broadcaster.fireEvent(new MessageEvent(this, WebUpdater.DO_UPDATE_FAILED, "Extracting " + file.getLocalTmpFile().getAbsolutePath() + " failed"));
                    e.printStackTrace();
                    file.getLocalTmpFile().delete();
                    file.getLocalTmpFile().deleteOnExit();
                    this.errors++;
                    return false;
                }
            }

            return true;
        }
        this.errors++;
        return false;
    }

}