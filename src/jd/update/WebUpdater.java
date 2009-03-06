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

    // private static String primaryUpdatePrefix;
    // private static String primaryUpdatePrefixfromServer = null;
    // private static String secondaryUpdatePrefix;
    // private static String secondaryUpdatePrefixfromServer = null;
    // private boolean useUpdatePrefixFromServer = true;
    //
    // private Integer switchtosecondary = 0;

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
        // setUrls("http://212.117.163.148/update/");
        // setprimaryUpdatePrefix("http://78.143.20.68/update/jd/");
        // setsecondaryUpdatePrefix("http://212.117.163.148/update/jd/");
        // switchtosecondary = 0;
        this.br = new Browser();
        errors = 0;

    }

    // /**
    // * Lädt fileurl nach filepath herunter
    // *
    // * @param filepath
    // * @param fileurl
    // * @return true/False
    // */
    // public boolean downloadBinary(String filepath, String fileurl, String
    // Hash) {
    // String finalurl = fileurl;
    // boolean useprefixes = false;
    // boolean primaryfirst = true;
    // boolean ret = false;
    // synchronized (switchtosecondary) {
    // if (switchtosecondary > 10) primaryfirst = false;
    // }
    // String localhash;
    // for (int i = 1; i < 5; i++) {
    // try {
    // /* wurde komplette url oder nur relativ angegeben? */
    // try {
    // new URL(fileurl);
    // } catch (Exception e1) {
    // /* primary update server */
    // if (primaryfirst) {
    // finalurl = this.getprimaryUpdatePrefix() + fileurl;
    // } else {
    // finalurl = this.getsecondaryUpdatePrefix() + fileurl;
    // }
    // useprefixes = true;
    // }
    // /* von absolut oder primary laden */
    // ret = downloadBinaryIntern(filepath, finalurl);
    //
    // /* hashcheck 1 */
    // if (Hash != null) {
    // localhash = getLocalHash(new File(filepath));
    // if (localhash != null && localhash.equalsIgnoreCase(Hash)) {
    // if (useprefixes) {
    // synchronized (switchtosecondary) {
    // if (primaryfirst) switchtosecondary--;
    // }
    // }
    // return true;
    // }
    // }
    // /* falls von absolut geladen wurde, dann hier stop */
    // if (!useprefixes) {
    // if (ret) return true;
    // try {
    // Thread.sleep(250);
    // } catch (InterruptedException e) {
    // }
    // log("Fehler beim laden von " + finalurl + "Retry " + i);
    // continue;
    // }
    // synchronized (switchtosecondary) {
    // if (primaryfirst) switchtosecondary++;
    // }
    // /* secondary update server */
    // if (!primaryfirst) {
    // finalurl = this.getprimaryUpdatePrefix() + fileurl;
    // } else {
    // finalurl = this.getsecondaryUpdatePrefix() + fileurl;
    // }
    // ret = downloadBinaryIntern(filepath, finalurl);
    // if (Hash != null) {
    // localhash = getLocalHash(new File(filepath));
    // if (localhash != null && localhash.equalsIgnoreCase(Hash)) {
    // if (useprefixes) {
    // synchronized (switchtosecondary) {
    // if (!primaryfirst) switchtosecondary = 0;
    // }
    // }
    // return true;
    // }
    // }
    // } catch (Exception e2) {
    // log("Fehler beim laden von " + finalurl);
    // }
    // try {
    // Thread.sleep(250);
    // } catch (InterruptedException e) {
    // }
    // log("Fehler beim laden von " + finalurl + "Retry " + i);
    // }
    // synchronized (errors) {
    // errors++;
    // }
    // return false;
    // }

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

    // public boolean downloadBinaryIntern(String filepath, String fileurl) {
    //
    // try {
    // log("downloading... You must NOT close the window!");
    // fileurl = urlEncode(fileurl.replaceAll("\\\\", "/"));
    // String org = filepath;
    // File file = new File(filepath + ".tmp");
    // if (file.exists() && file.isFile()) {
    // if (!file.delete()) {
    // log("Konnte Datei nicht löschen " + file);
    // return false;
    // }
    //
    // }
    //
    // if (file.getParentFile() != null && !file.getParentFile().exists()) {
    // file.getParentFile().mkdirs();
    // }
    // file.createNewFile();
    //
    // BufferedOutputStream output = new BufferedOutputStream(new
    // FileOutputStream(file, true));
    // fileurl = URLDecoder.decode(fileurl, "UTF-8");
    // fileurl += (fileurl.contains("?") ? "&" : "?") +
    // System.currentTimeMillis();
    // URL url = new URL(fileurl);
    // URLConnection con = url.openConnection();
    // con.setReadTimeout(20000);
    // con.setConnectTimeout(20000);
    // con.setRequestProperty("User-Agent",
    // "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/2008111317 Ubuntu/8.04 (hardy) Firefox/3.0.4"
    // );
    // if (WebUpdater.getConfig("WEBUPDATE").getBooleanProperty("USE_PROXY",
    // false)) {
    // String user =
    // WebUpdater.getConfig("WEBUPDATE").getStringProperty("PROXY_USER", "");
    // String pass =
    // WebUpdater.getConfig("WEBUPDATE").getStringProperty("PROXY_PASS", "");
    //
    // con.setRequestProperty("Proxy-Authorization", "Basic " +
    // Base64Encode(user + ":" + pass));
    //
    // }
    //
    // if (WebUpdater.getConfig("WEBUPDATE").getBooleanProperty("USE_SOCKS",
    // false)) {
    //
    // String user =
    // WebUpdater.getConfig("WEBUPDATE").getStringProperty("PROXY_USER_SOCKS",
    // "");
    // String pass =
    // WebUpdater.getConfig("WEBUPDATE").getStringProperty("PROXY_PASS_SOCKS",
    // "");
    //
    // con.setRequestProperty("Proxy-Authorization", "Basic " +
    // Base64Encode(user + ":" + pass));
    //
    // }
    // BufferedInputStream input = new
    // BufferedInputStream(con.getInputStream());
    //
    // byte[] b = new byte[1024];
    // int len;
    // while ((len = input.read(b)) != -1) {
    // output.write(b, 0, len);
    // }
    // output.close();
    // input.close();
    //
    // log("Download ok...rename " + file.getName() + " to " + new
    // File(org).getName());
    // if (new File(org).exists() && new File(org).isFile()) {
    // if (!new File(org).delete()) {
    // log("Could not delete file " + org);
    // return false;
    // }
    //
    // }
    //
    // file.renameTo(new File(org));
    //
    // return true;
    // } catch (FileNotFoundException e) {
    // e.printStackTrace();
    // return false;
    //
    // } catch (MalformedURLException e) {
    // e.printStackTrace();
    // return false;
    //
    // } catch (Exception e) {
    // e.printStackTrace();
    // return false;
    //
    // }
    //
    // }

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

    // public void filterAvailableUpdates(Vector<Vector<String>> files, File
    // dir) {
    // // log(files.toString());
    // String akt;
    // String hash;
    // try {
    // for (int i = files.size() - 1; i >= 0; i--) {
    // String[] tmp = files.elementAt(i).elementAt(0).split("\\?");
    //
    // akt = new File(dir, tmp[0]).getAbsolutePath();
    //
    // if (!new File(akt).exists()) {
    // log("New file. " + files.elementAt(i) + " - " + akt);
    // continue;
    // }
    // hash = JDHash.getMD5(new File(akt));
    //
    // if (!hash.equalsIgnoreCase(files.elementAt(i).elementAt(1))) {
    // log("UPDATE AV. " + files.elementAt(i) + " - " + hash);
    // continue;
    // }
    //
    // files.removeElementAt(i);
    // }
    // } catch (Exception e) {
    // log(e.getLocalizedMessage());
    // }
    //
    // }

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

            if (entry.getLocalPath().endsWith(".class")) {
                plugins.put(entry.getLocalPath(), entry);
            }

            if (!entry.getLocalPath().endsWith(".class") || !this.ignorePlugins) {
                boolean osFound = false;
                boolean correctOS = false;
                for (String element : os) {
                    String url=entry.getRawUrl();
                    if(url==null)url=entry.getRelURL();
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
                        log("OS Filter: " + entry.getRawUrl());

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
        String serverHash = br.getPage(UPDATE_ZIP_HASH).trim();
        String localHash = JDHash.getMD5(JDUtilities.getResourceFile(UPDATE_ZIP_LOCAL_PATH));
        if (!serverHash.equalsIgnoreCase(localHash)) {
            Browser.download(JDUtilities.getResourceFile(UPDATE_ZIP_LOCAL_PATH), UPDATE_ZIP_URL);
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
            br.getPage(LISTPATH);
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

    //
    // public void useUpdatePrefixFromServer(boolean b) {
    // useUpdatePrefixFromServer = b;
    // }
    //
    // public String getprimaryUpdatePrefix() {
    // if (useUpdatePrefixFromServer && primaryUpdatePrefixfromServer != null)
    // return primaryUpdatePrefixfromServer;
    // return primaryUpdatePrefix;
    // }
    //
    // public String getsecondaryUpdatePrefix() {
    // if (useUpdatePrefixFromServer && secondaryUpdatePrefixfromServer != null)
    // return secondaryUpdatePrefixfromServer;
    // return secondaryUpdatePrefix;
    // }
    //
    // public static void setprimaryUpdatePrefix(String prefix) {
    // primaryUpdatePrefix = prefix;
    // }
    //
    // public static void setprimaryUpdatePrefixfromServer(String prefix) {
    // primaryUpdatePrefixfromServer = prefix;
    // }
    //
    // public static void setsecondaryUpdatePrefix(String prefix) {
    // secondaryUpdatePrefix = prefix;
    // }
    //
    // public static void setsecondaryUpdatePrefixfromServer(String prefix) {
    // secondaryUpdatePrefixfromServer = prefix;
    // }
    //

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

    // /**
    // * @param listPath
    // * the listPath to set
    // */
    // public void setUrls(String listPath) {
    // JDUpdateUtils.setUpdateUrl(listPath);
    // onlinePath = listPath + "jd";
    // log("Update from " + listPath);
    // }

    public void setLogger(StringBuilder log) {
        logger = log;
    }

    public void setOSFilter(boolean filter) {
        this.OSFilter = filter;
    }

    public void updateFile(Vector<String> file) throws IOException {

        String[] tmp = file.elementAt(0).split("\\?");
        log("Webupdater: download " + tmp[1] + " to " + JDUtilities.getResourceFile(tmp[0]).getAbsolutePath());
        // , file.elementAt(1)
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
                    if (progressload != null)  progressload.setForeground(Color.RED);

                }
            } catch (Exception e) {
                e.printStackTrace();
                log(e.getLocalizedMessage());
                log(file.toString());
                log("Failed\r\n");
                if (progressload != null)progressload.setForeground(Color.RED);
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