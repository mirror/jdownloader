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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JProgressBar;

import jd.config.CFGConfig;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.nutils.zip.UnZip;
import jd.parser.Regex;
import jd.utils.JDUtilities;

/**
 * @author JD-Team Webupdater lädt pfad und hash infos von einem server und
 *         vergleicht sie mit den lokalen versionen
 */
public class WebUpdater implements Serializable {

    private static final long serialVersionUID = 1946622313175234371L;
    private static final String UPDATE_ZIP_LOCAL_PATH = "tmp/update.zip";
    private static final String UPDATE_ZIP_URL = "http://update1.jdownloader.org/update.zip";
    private static String LISTPATH = "http://update1.jdownloader.org/server.list";
    private static String UPDATE_ZIP_HASH = "http://update1.jdownloader.org/update.md5";
    public static HashMap<String, FileUpdate> PLUGIN_LIST = null;

    private boolean ignorePlugins = true;

    private StringBuilder logger;

    private boolean OSFilter = true;

    private JProgressBar progressload = null;

    private Integer errors = 0;

    public byte[] sum;

    private Browser br;
    private File workingdir;

    public File getWorkingdir() {
        return workingdir;
    }

    public void setWorkingdir(File workingdir) {
        this.workingdir = workingdir;
    }

    private static HashMap<String, File> fileMap;

    public static HashMap<String, File> getFileMap() {
        return fileMap;
    }

    /**
     * @param path
     *            (Dir Pfad zum Updateserver)
     */
    public WebUpdater() {
        logger = new StringBuilder();
        this.br = new Browser();
        errors = 0;
    }

    public int getErrors() {
        synchronized (errors) {
            return errors;
        }
    }

    public void resetErrors() {
        synchronized (errors) {
            errors = 0;
        }
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

            if (!file.exists()) {
                log("New file. " + file.getLocalPath());
                continue;
            }

            if (!file.equals()) {
                log("UPDATE AV. " + file.getLocalPath());
                continue;
            }
            it.remove();
        }

    }

    /**
     * Liest alle files vom server
     * 
     * @return Vector mit allen verfügbaren files
     * @throws UnsupportedEncodingException
     */
    public ArrayList<FileUpdate> getAvailableFiles() throws Exception {
        String source;

        HashMap<String, FileUpdate> plugins = new HashMap<String, FileUpdate>();
        ArrayList<FileUpdate> ret = new ArrayList<FileUpdate>();

        updateAvailableServers();
        loadUpdateList();
        source = JDIO.getLocalFile(fileMap.get("hashlist.lst"));

        String pattern = "[\r\n\\;]*([^=]+)\\=(.*?)\\;";

        if (source == null) {
            log("filelist nicht verfüpgbar");
            return ret;
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
                        if (System.getProperty("os.name").toLowerCase().indexOf(element) >= 0) {
                            correctOS = true;
                        }
                    }

                }
                if (this.OSFilter == true) {
                    if (!osFound || osFound && correctOS) {
                        ret.add(entry);
                    } else {
                        String url = entry.getRawUrl();
                        if (url == null) url = entry.getRelURL();
                        log("OS Filter: " + url);

                    }
                } else {
                    ret.add(entry);
                }
            }

        }
        this.sum = new byte[sum.size()];
        int ii = 0;
        for (int i = sum.size() - 1; i >= 0; i--) {
            this.sum[ii++] = sum.get(i);
        }

        WebUpdater.PLUGIN_LIST = plugins;

        return ret;
    }

    public boolean isIgnorePlugins() {
        return ignorePlugins;
    }

    public void setIgnorePlugins(boolean ignorePlugins) {
        this.ignorePlugins = ignorePlugins;
    }

    private void loadUpdateList() throws Exception {
        String serverHash = br.getPage(UPDATE_ZIP_HASH + "?t=" + System.currentTimeMillis()).trim();
        String localHash = JDHash.getMD5(JDUtilities.getResourceFile(UPDATE_ZIP_LOCAL_PATH));
        if (!serverHash.equalsIgnoreCase(localHash)) {
            Browser.download(JDUtilities.getResourceFile(UPDATE_ZIP_LOCAL_PATH), UPDATE_ZIP_URL + "?t=" + System.currentTimeMillis());
        }
        UnZip u = new UnZip(JDUtilities.getResourceFile(UPDATE_ZIP_LOCAL_PATH), JDUtilities.getResourceFile("tmp/"));

        File[] efiles = u.extract();
        fileMap = new HashMap<String, File>();
        for (File f : efiles) {
            fileMap.put(f.getName().toLowerCase(), f);
        }

    }

    private ArrayList<Server> updateAvailableServers() {
        try {
            log("Update Downloadmirrors");
            br.getPage(LISTPATH + "?t=" + System.currentTimeMillis());
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
                log("Updateserver: " + s);
            }
            if (servers.size() > 0) {
                WebUpdater.getConfig("WEBUPDATE").setProperty("SERVERLIST", servers);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getAvailableServers();

    }

    @SuppressWarnings("unchecked")
    private ArrayList<Server> getAvailableServers() {
        return (ArrayList<Server>) WebUpdater.getConfig("WEBUPDATE").getProperty("SERVERLIST");
    }

    public StringBuilder getLogger() {
        return logger;
    }

    public boolean getOSFilter() {
        return this.OSFilter;
    }

    public void ignorePlugins(boolean b) {
        this.ignorePlugins = b;
    }

    public void log(String buf) {
        System.out.println(buf);
        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        if (logger != null) {
            logger.append(df.format(dt) + ":" + buf + System.getProperty("line.separator"));
        }
    }

    public void setDownloadProgress(JProgressBar progresslist) {
        progressload = progresslist;
    }

    public void setLogger(StringBuilder log) {
        logger = log;
    }

    public void setOSFilter(boolean filter) {
        this.OSFilter = filter;
    }

    public void updateFile(Vector<String> file) throws IOException {

        String[] tmp = file.elementAt(0).split("\\?");
        log("Webupdater: download " + tmp[1] + " to " + JDUtilities.getResourceFile(tmp[0]).getAbsolutePath());
        Browser.download(JDUtilities.getResourceFile(tmp[0]), tmp[0]);

    }

    /**
     * Updated alle files in files
     * 
     * @param files
     * @throws IOException
     */
    public void updateFiles(ArrayList<FileUpdate> files) throws IOException {

        if (progressload != null) {
            progressload.setMaximum(files.size());
        }
        int i = 0;
        for (FileUpdate file : files) {
            try {
                log("Update file: " + file.getLocalPath());

                if (updateUpdatefIle(file)) {

                    log(file.toString());
                    log("Successfull\r\n");

                } else {
                    log(file.toString());
                    log("Failed\r\n");
                    if (progressload != null) progressload.setForeground(Color.RED);

                }
            } catch (Exception e) {
                e.printStackTrace();
                log(e.getLocalizedMessage());
                log(file.toString());
                log("Failed\r\n");
                if (progressload != null) progressload.setForeground(Color.RED);
            }

            i++;

            if (progressload != null) {
                progressload.setValue(i);
            }
        }
        if (progressload != null) {
            progressload.setValue(100);
        }
    }

    public boolean updateUpdatefIle(FileUpdate file) throws IOException {

        file.reset(getAvailableServers());
        return file.update();

    }

    /**
     * Funktion übertragt alle werte aus den alten Config files in die datenbank
     * 
     * @param string
     * @return
     */
    public static SubConfiguration getConfig(String string) {
        SubConfiguration guiConfig = JDUtilities.getSubConfig(string);
        CFGConfig gui = CFGConfig.getConfig(string);
        if (gui.getProperties().size() != 0) {
            guiConfig.getProperties().putAll(gui.getProperties());
            gui.getProperties().clear();
            gui.save();
        }
        guiConfig.save();
        return guiConfig;
    }

}