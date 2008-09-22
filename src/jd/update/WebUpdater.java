//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.JProgressBar;

import sun.misc.BASE64Encoder;

/**
 * @author JD-Team Webupdater lädt pfad und hash infos von einem server und
 *         vergleicht sie mit den lokalen versionen
 */
public class WebUpdater implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1946622313175234371L;

    public static final String USE_CAPTCHA_EXCHANGE_SERVER = "USE_CAPTCHA_EXCHANGE_SERVER";

    public static HashMap<String, Vector<String>> PLUGIN_LIST = null;

    public static String htmlDecode(String str) {
        //http://rs218.rapidshare.com/files/&#0052;&#x0037;&#0052;&#x0034;&#0049
        // ;&#x0032;&#0057;&#x0031;/STE_S04E04.Borderland.German.dTV.XviD-2
        // Br0th3rs.part1.rar
        if (str == null) { return null; }
        StringBuffer sb = new StringBuffer();
        String pattern = "(\\&\\#x[a-f0-9A-F]+\\;?)";
        Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1).replaceAll("\\&\\#x", "").replaceAll("\\;", ""), 16);
                if (c == '$' || c == '\\') {
                    r.appendReplacement(sb, "\\" + c);
                } else {
                    r.appendReplacement(sb, "" + c);
                }
            }
        }
        r.appendTail(sb);
        str = sb.toString();

        sb = new StringBuffer();
        pattern = "(\\&\\#\\d+\\;?)";
        r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str);
        while (r.find()) {

            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1).replaceAll("\\&\\#", "").replaceAll("\\;", ""), 10);
                if (c == '$' || c == '\\') {
                    r.appendReplacement(sb, "\\" + c);
                } else {
                    r.appendReplacement(sb, "" + c);
                }
            }
        }
        r.appendTail(sb);
        str = sb.toString();

        sb = new StringBuffer();
        pattern = "(\\%[a-f0-9A-F]{2})";
        r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1).replaceFirst("\\%", ""), 16);
                if (c == '$' || c == '\\') {
                    r.appendReplacement(sb, "\\" + c);
                } else {
                    r.appendReplacement(sb, "" + c);
                }
            }
        }
        r.appendTail(sb);
        str = sb.toString();

        try {
            str = URLDecoder.decode(str, "UTF-8");
        } catch (Exception e) {
        }
        return HTMLEntities.unhtmlentities(str);
    }

    private int cid = -1;

    private boolean OSFilter = true;

    /**
     * Pfad zur lis.php auf dem updateserver
     */
    public String listPath;

    private StringBuffer logger;

    /**
     * Pfad zum Online-Bin verzeichniss
     */
    public String onlinePath;

    private JProgressBar progresslist = null;

    private JProgressBar progressload = null;

    /**
     * Anzahl der ganzen Files
     */
    private transient int totalFiles = 0;

    /**
     * anzahl der aktualisierten Files
     */
    private transient int updatedFiles = 0;

    public byte[] sum;

    private boolean ignorePlugins = true;

    private static long LASTREQUEST=0;

    /**
     * @param path
     *            (Dir Pfad zum Updateserver)
     */
    public WebUpdater(String path) {
        logger = new StringBuffer();
        if (path != null) {
            setListPath(path);
        } else {
            setListPath("http://service.jdownloader.org/update/jd");
        }

    }

    public static String Base64Encode(String plain) {

        if (plain == null) { return null; }
        String base64 = new BASE64Encoder().encode(plain.getBytes());

        return base64;
    }

    public void setOSFilter(boolean filter) {
        this.OSFilter = filter;
    }

    public boolean getOSFilter() {
        return this.OSFilter;
    }

    /**
     * Lädt fileurl nach filepath herunter
     * 
     * @param filepath
     * @param fileurl
     * @return true/False
     */
    public boolean downloadBinary(String filepath, String fileurl) {

        try {

            log("downloading... You must NOT close the window!");
            fileurl = urlEncode(fileurl.replaceAll("\\\\", "/"));
            String org = filepath;
            filepath = new File(filepath + ".tmp").getAbsolutePath();
            File file = new File(filepath);
            if (file.exists() && file.isFile()) {
                if (!file.delete()) {
                    log("Konnte Datei nicht löschen " + file);
                    return false;
                }

            }

            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            fileurl = URLDecoder.decode(fileurl, "UTF-8");
            fileurl += (fileurl.contains("?") ? "&" : "?") + System.currentTimeMillis();
            URL url = new URL(fileurl);
            URLConnection con = url.openConnection();
            con.setReadTimeout(10000);
            con.setReadTimeout(10000);
            if (SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("USE_PROXY", false)) {
                String user = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_USER", "");
                String pass = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_PASS", "");

                con.setRequestProperty("Proxy-Authorization", "Basic " + Base64Encode(user + ":" + pass));

            }

            if (SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("USE_SOCKS", false)) {

                String user = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_USER_SOCKS", "");
                String pass = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_PASS_SOCKS", "");

                con.setRequestProperty("Proxy-Authorization", "Basic " + Base64Encode(user + ":" + pass));

            }
            BufferedInputStream input = new BufferedInputStream(con.getInputStream());

            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();

            log("Download ok...rename " + file.getName() + "to " + new File(org).getName());
            if (new File(org).exists() && new File(org).isFile()) {
                if (!new File(org).delete()) {
                    log("Could not delete file " + org);
                    return false;
                }

            }

            file.renameTo(new File(org));

            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        }

    }

    /**
     * löscht alles files aus files die nicht aktualisiert werden brauchen
     * 
     * @param files
     */
    public void filterAvailableUpdates(Vector<Vector<String>> files) {
        // log(files.toString());
        String akt;
        String hash;

        for (int i = files.size() - 1; i >= 0; i--) {
            akt = new File(files.elementAt(i).elementAt(0)).getAbsolutePath();
            String[] tmp = files.elementAt(i).elementAt(0).split("\\?");

            akt = new File(tmp[0]).getAbsolutePath();
            if (!new File(akt).exists()) {
                log("New file. " + files.elementAt(i) + " - " + akt);
                continue;
            }
            hash = getLocalHash(new File(akt));

            if (!hash.equalsIgnoreCase(files.elementAt(i).elementAt(1))) {
                log("UPDATE AV. " + files.elementAt(i) + " - " + hash);
                continue;
            }
            files.removeElementAt(i);
        }

    }

    public void filterAvailableUpdates(Vector<Vector<String>> files, File dir) {
        // log(files.toString());
        String akt;
        String hash;
        try {
            for (int i = files.size() - 1; i >= 0; i--) {
                String[] tmp = files.elementAt(i).elementAt(0).split("\\?");

                akt = new File(dir, tmp[0]).getAbsolutePath();

                if (!new File(akt).exists()) {
                    log("New file. " + files.elementAt(i) + " - " + akt);
                    continue;
                }
                hash = getLocalHash(new File(akt));
                log(new File(akt) + "");
                if (!hash.equalsIgnoreCase(files.elementAt(i).elementAt(1))) {
                    log("UPDATE AV. " + files.elementAt(i) + " - " + hash);
                    continue;
                }

                files.removeElementAt(i);
            }
        } catch (Exception e) {
            log(e.getLocalizedMessage());
        }

    }

    /**
     * Liest alle files vom server
     * 
     * @return Vector mit allen verfügbaren files
     * @throws UnsupportedEncodingException
     */
    public Vector<Vector<String>> getAvailableFiles() throws Exception {
        String source;
        if (progresslist != null) {
            progresslist.setMaximum(100);
        }
        HashMap<String, Vector<String>> plugins = new HashMap<String, Vector<String>>();
        Vector<Vector<String>> ret = new Vector<Vector<String>>();
        if((System.currentTimeMillis()-LASTREQUEST)<(30*60*1000l)){
            
            return ret;
        }
        try {
            if (progresslist != null) {
                progresslist.setValue(20);
            }

            if (cid > 0) {
                source = getRequest(new URL(listPath + "?cid=" + cid));
            } else {

                source = getRequest(new URL(listPath));
            }
            if (cid < 0 && source != null && source.indexOf("<br>") > 0) {
                String cid = source.substring(0, source.indexOf("<br>")).trim();
                if (cid != null) {
                    try {
                        log("New CID: " + cid);
                        this.cid = Integer.parseInt(cid);
                    } catch (Exception e) {
                        log("CID Error: " + cid);
                        this.cid = 0;
                    }
                }

            } else {
                log("NO CID: ");
            }

            if (progresslist != null) {
                progresslist.setValue(80);
            }
            String pattern = "\\$(.*?)\\=\\\"(.*?)\\\"\\;";

            if (source == null) {
                log(listPath + " nicht verfüpgbar");
                return new Vector<Vector<String>>();
            }
            Vector<String> entry;
            String tmp;
            String[] os = new String[] { "windows", "mac", "linux" };
            ArrayList<Byte> sum = new ArrayList<Byte>();
            for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(source); r.find();) {
                entry = new Vector<String>();
                String tmp2 = "";
                for (int x = 1; x <= r.groupCount(); x++) {
                    if ((tmp = r.group(x).trim()).length() > 0) {
                        entry.add(UTF8Decode(tmp));
                        if (tmp.length() == 32) {
                            sum.add((byte) tmp.charAt(0));
                        }
                        tmp2 += WebUpdater.htmlDecode(UTF8Decode(tmp)) + " ";

                    }
                }
                String file = entry.get(0).split("\\?")[0];

                if (file.endsWith(".class")) {
                    plugins.put(file, entry);
                }

                if (!file.endsWith(".class") || !this.ignorePlugins) {
                    boolean osFound = false;
                    boolean correctOS = false;
                    for (String element : os) {
                        if (tmp2.toLowerCase().indexOf(element) >= 0) {
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
                            log("OS Filter: " + tmp2);

                        }
                    } else {
                        ret.add(entry);
                    }
                }

            }
            this.sum = new byte[sum.size()];
            int i = 0;
            for (byte b : sum) {
                this.sum[i++] = b;
            }

            WebUpdater.PLUGIN_LIST = plugins;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (progresslist != null) {
            progresslist.setValue(100);
        }
        return ret;
    }

    public int getCid() {
        return cid;
    }

    /**
     * @return the listPath
     */
    public String getListPath() {
        return listPath;
    }

    public String getLocalHash(File f) {
        try {
            if (!f.exists()) { return null; }
            MessageDigest md;
            md = MessageDigest.getInstance("md5");
            byte[] b = new byte[1024];
            InputStream in = new FileInputStream(f);
            for (int n = 0; (n = in.read(b)) > -1;) {
                md.update(b, 0, n);
            }
            byte[] digest = md.digest();
            String ret = "";
            for (byte element : digest) {
                String tmp = Integer.toHexString(element & 0xFF);
                if (tmp.length() < 2) {
                    tmp = "0" + tmp;
                }
                ret += tmp;
            }
            in.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public StringBuffer getLogger() {
        return logger;
    }

    public String getOnlinePath() {
        return onlinePath;
    }

    /**
     * LIest eine webseite ein und gibt deren source zurück
     * 
     * @param urlStr
     * @return String inhalt von urlStr
     */
    public String getRequest(URL link) throws Exception {
        
        LASTREQUEST = System.currentTimeMillis();
        HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();

        if (SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("USE_PROXY", false)) {
            String user = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_USER", "");
            String pass = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_PASS", "");

            httpConnection.setRequestProperty("Proxy-Authorization", "Basic " + Base64Encode(user + ":" + pass));

        }

        if (SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("USE_SOCKS", false)) {

            String user = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_USER_SOCKS", "");
            String pass = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("ROXY_PASS_SOCKS", "");

            httpConnection.setRequestProperty("Proxy-Authorization", "Basic " + Base64Encode(user + ":" + pass));

        }

        httpConnection.setReadTimeout(20000);
        httpConnection.setReadTimeout(20000);
        httpConnection.setInstanceFollowRedirects(true);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        // Content-Encoding: gzip
        BufferedReader rd;
        if (httpConnection.getHeaderField("Content-Encoding") != null && httpConnection.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(httpConnection.getInputStream())));
        } else {
            rd = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        }
        String line;
        StringBuffer htmlCode = new StringBuffer();
        while ((line = rd.readLine()) != null) {

            htmlCode.append(line + "\n");
        }
        httpConnection.disconnect();

        return htmlCode.toString();
    }

    /**
     * @return the totalFiles
     */
    public int getTotalFiles() {
        return totalFiles;
    }

    /**
     * Gibt die Anzhal der aktualisierten Files zurück
     * 
     * @return the updatedFiles
     */
    public int getUpdatedFiles() {
        return updatedFiles;
    }

    /**
     * Gibt die Anzahl der aktualisierbaren files zurück.
     * 
     * @return Anzahld er neuen Datein
     * @throws Exception
     */
    public int getUpdateNum() throws Exception {
        Vector<Vector<String>> files = getAvailableFiles();

        if (files == null) { return 0; }

        totalFiles = files.size();
        filterAvailableUpdates(files);
        return files.size();

    }

    public void log(String buf) {
        System.out.println(buf);
        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        if (logger != null) {
            logger.append(df.format(dt) + ":" + buf + System.getProperty("line.separator"));
        }
    }

    /**
     * Startet das Updaten
     * 
     * @throws Exception
     */
    public void run() throws Exception {

        Vector<Vector<String>> files = getAvailableFiles();
        if (files != null) {
            // log(files.toString());
            totalFiles = files.size();
            filterAvailableUpdates(files);
            updateFiles(files);
        }
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public void setDownloadProgress(JProgressBar progresslist) {
        progressload = progresslist;

    }

    /**
     * @param listPath
     *            the listPath to set
     */
    public void setListPath(String listPath) {
        this.listPath = listPath + "/list.php?&r=" + System.currentTimeMillis();
        onlinePath = listPath + "/bin";
        log("Update from " + listPath);

    }

    public void setListProgress(JProgressBar progresslist) {
        this.progresslist = progresslist;

    }

    public void setLogger(StringBuffer log) {

        logger = log;
    }

    /**
     * Updated alle files in files
     * 
     * @param files
     */
    public void updateFiles(Vector<Vector<String>> files) {
        String akt;
        if (progressload != null) {
            progressload.setMaximum(files.size());
        }
        updatedFiles = 0;
        for (int i = files.size() - 1; i >= 0; i--) {

            akt = new File(files.elementAt(i).elementAt(0)).getAbsolutePath();
            if (!new File(akt + ".noUpdate").exists()) {
                updatedFiles++;

                if (files.elementAt(i).elementAt(0).indexOf("?") >= 0) {
                    String[] tmp = files.elementAt(i).elementAt(0).split("\\?");
                    log("Webupdater: download" + tmp[1] + " to " + new File(tmp[0]).getAbsolutePath());
                    downloadBinary(tmp[0], tmp[1]);
                } else {
                    log("Webupdater:  download" + onlinePath + "/" + files.elementAt(i).elementAt(0) + " to " + akt);
                    downloadBinary(akt, onlinePath + "/" + files.elementAt(i).elementAt(0));
                }
                log("Webupdater: ready");
            }
            if (progressload != null) {
                progressload.setValue(files.size() - i);
            }
        }
        if (progressload != null) {
            progressload.setValue(100);
        }
    }

    /**
     * @author JD-Team Macht ein urlRawEncode und spart dabei die angegebenen
     *         Zeichen aus
     * @param str
     * @return str URLCodiert
     */
    private String urlEncode(String str) {
        try {
            str = URLDecoder.decode(str, "UTF-8");
            String allowed = "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.?/\\:&=;";
            String ret = "";
            int i;
            for (i = 0; i < str.length(); i++) {
                char letter = str.charAt(i);
                if (allowed.indexOf(letter) >= 0) {
                    ret += letter;
                } else {
                    ret += "%" + Integer.toString(letter, 16);
                }
            }
            return ret;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * @author JD-Team
     * @param str
     * @return str als UTF8Decodiert
     */
    private String UTF8Decode(String str) {
        try {
            return new String(str.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updateFile(Vector<String> file) {

        String[] tmp = file.elementAt(0).split("\\?");
        log("Webupdater: download" + tmp[1] + " to " + new File(tmp[0]).getAbsolutePath());
        downloadBinary(tmp[0], tmp[1]);

    }

    public void ignorePlugins(boolean b) {
        this.ignorePlugins = b;

    }

}