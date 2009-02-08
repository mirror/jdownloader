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

    private static final long serialVersionUID = 1946622313175234371L;

    public static HashMap<String, Vector<String>> PLUGIN_LIST = null;

    private boolean ignorePlugins = true;

    private StringBuilder logger;

    /**
     * Pfad zum Online-Bin verzeichniss
     */
    public String onlinePath;

    private boolean OSFilter = true;

    private JProgressBar progresslist = null;

    private JProgressBar progressload = null;

    private String primaryUpdatePrefix;
    private String secondaryUpdatePrefix;

    private Integer switchtosecondary = 0;

    private Integer errors = 0;

    private static File JD_ROOT_DIRECTORY = null;

    public byte[] sum;

    /**
     * @param path
     *            (Dir Pfad zum Updateserver)
     */
    public WebUpdater() {
        logger = new StringBuilder();
        setUrls("http://212.117.163.148/update/");
        setprimaryUpdatePrefix("http://78.143.20.68/update/jd/");
        setsecondaryUpdatePrefix("http://212.117.163.148/update/jd/");
        switchtosecondary = 0;
        errors = 0;
        JD_ROOT_DIRECTORY = utils.getJDHomeDirectoryFromEnvironment();
    }

    public static File getJDDirectory() {
        return JD_ROOT_DIRECTORY;
    }



    /**
     * Lädt fileurl nach filepath herunter
     * 
     * @param filepath
     * @param fileurl
     * @return true/False
     */
    public boolean downloadBinary(String filepath, String fileurl, String Hash) {
        String finalurl = fileurl;
        boolean useprefixes = false;
        boolean returnval = false;
        boolean primaryfirst = true;
        synchronized (switchtosecondary) {
            if (switchtosecondary > 10) primaryfirst = false;
        }
        String localhash;
        try {
            /* wurde komplette url oder nur relativ angegeben? */
            try {
                new URL(fileurl);
            } catch (Exception e1) {
                /* primary update server */
                if (primaryfirst) {
                    finalurl = this.getprimaryUpdatePrefix() + fileurl;
                } else {
                    finalurl = this.getsecondaryUpdatePrefix() + fileurl;
                }
                useprefixes = true;
            }
            /* von absolut oder primary laden */
            returnval = downloadBinaryIntern(filepath, finalurl);

            /* hashcheck 1 */
            if (Hash != null) {
                localhash = getLocalHash(new File(filepath));
                if (localhash != null && localhash.equalsIgnoreCase(Hash)) {
                    if (useprefixes) {
                        synchronized (switchtosecondary) {
                            if (primaryfirst) switchtosecondary--;
                        }
                    }
                    return true;
                }
            }
            /* falls von absolut geladen wurde, dann hier stop */
            if (!useprefixes) {
                if (returnval == false) {
                    synchronized (errors) {
                        errors++;
                    }
                }
                return returnval;
            }
            synchronized (switchtosecondary) {
                if (primaryfirst) switchtosecondary++;
            }
            /* secondary update server */
            if (!primaryfirst) {
                finalurl = this.getprimaryUpdatePrefix() + fileurl;
            } else {
                finalurl = this.getsecondaryUpdatePrefix() + fileurl;
            }
            returnval = downloadBinaryIntern(filepath, finalurl);
            if (Hash != null) {
                localhash = getLocalHash(new File(filepath));
                if (localhash != null && localhash.equalsIgnoreCase(Hash)) {
                    if (useprefixes) {
                        synchronized (switchtosecondary) {
                            if (!primaryfirst) switchtosecondary = 0;
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e2) {
            log(e2.toString());
            log("Fehler beim laden von " + finalurl);
        }
        synchronized (errors) {
            errors++;
        }
        return false;
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

    public boolean downloadBinaryIntern(String filepath, String fileurl) {

        try {
            log("downloading... You must NOT close the window!");
            fileurl = urlEncode(fileurl.replaceAll("\\\\", "/"));
            String org = filepath;
            File file = new File(filepath + ".tmp");
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
            con.setReadTimeout(20000);
            con.setConnectTimeout(20000);
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/2008111317 Ubuntu/8.04 (hardy) Firefox/3.0.4");
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
            String[] tmp = files.elementAt(i).elementAt(0).split("\\?");

            akt = new File(WebUpdater.getJDDirectory(), tmp[0]).getAbsolutePath();
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

        if (progresslist != null) {
            progresslist.setValue(20);
        }

        source = JDUpdateUtils.get_UpdateList();

        if (progresslist != null) {
            progresslist.setValue(80);
        }
        String pattern = "\\$(.*?)\\=\\\"(.*?)\\\"\\;";

        if (source == null) {
            log(this.getListPath() + " nicht verfüpgbar");
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
        if (progresslist != null) {
            progresslist.setValue(100);
        }
        return ret;
    }

    /**
     * @return the listPath
     */
    public String getListPath() {
        return JDUpdateUtils.getUpdateUrl();
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

    public StringBuilder getLogger() {
        return logger;
    }

    public String getOnlinePath() {
        return onlinePath;
    }

    public boolean getOSFilter() {
        return this.OSFilter;
    }

    public String getprimaryUpdatePrefix() {
        return this.primaryUpdatePrefix;
    }

    public String getsecondaryUpdatePrefix() {
        return this.secondaryUpdatePrefix;
    }

    public void setprimaryUpdatePrefix(String prefix) {
        this.primaryUpdatePrefix = prefix;
    }

    public void setsecondaryUpdatePrefix(String prefix) {
        this.secondaryUpdatePrefix = prefix;
    }

    /**
     * LIest eine webseite ein und gibt deren source zurück
     * 
     * @param urlStr
     * @return String inhalt von urlStr
     */
    public String getRequest(URL link) throws Exception {

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
        httpConnection.setConnectTimeout(20000);
        httpConnection.setInstanceFollowRedirects(true);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/2008111317 Ubuntu/8.04 (hardy) Firefox/3.0.4");
        // Content-Encoding: gzip
        BufferedReader rd;
        if (httpConnection.getHeaderField("Content-Encoding") != null && httpConnection.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(httpConnection.getInputStream())));
        } else {
            rd = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        }
        String line;
        StringBuilder htmlCode = new StringBuilder();
        while ((line = rd.readLine()) != null) {

            htmlCode.append(line + "\n");
        }
        httpConnection.disconnect();

        return htmlCode.toString();
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

    /**
     * @param listPath
     *            the listPath to set
     */
    public void setUrls(String listPath) {
        JDUpdateUtils.setUpdateUrl(listPath);
        onlinePath = listPath + "jd";
        log("Update from " + listPath);
    }

    public void setListProgress(JProgressBar progresslist) {
        this.progresslist = progresslist;
    }

    public void setLogger(StringBuilder log) {
        logger = log;
    }

    public void setOSFilter(boolean filter) {
        this.OSFilter = filter;
    }

    public void updateFile(Vector<String> file) {

        String[] tmp = file.elementAt(0).split("\\?");
        log("Webupdater: download " + tmp[1] + " to " + new File(WebUpdater.JD_ROOT_DIRECTORY, tmp[0]).getAbsolutePath());
        downloadBinary(new File(WebUpdater.JD_ROOT_DIRECTORY, tmp[0]).getAbsolutePath(), tmp[0], file.elementAt(1));

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
        File file = null;
        for (int i = files.size() - 1; i >= 0; i--) {

            akt = new File(WebUpdater.JD_ROOT_DIRECTORY, files.elementAt(i).elementAt(0)).getAbsolutePath();
            if (!new File(akt + ".noUpdate").exists()) {

                if (files.elementAt(i).elementAt(0).indexOf("?") >= 0) {
                    String[] tmp = files.elementAt(i).elementAt(0).split("\\?");
                    file = new File(WebUpdater.getJDDirectory(), tmp[0]);
                    log("Webupdater: download " + tmp[1] + " to " + file.getAbsolutePath());
                    downloadBinary(file.getAbsolutePath(), tmp[0], files.elementAt(i).elementAt(1));
                } else {
                    log("Webupdater:  download " + onlinePath + "/" + files.elementAt(i).elementAt(0) + " to " + akt);
                    downloadBinary(akt, onlinePath + "/" + files.elementAt(i).elementAt(0), null);
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
     * Macht ein urlRawEncode und spart dabei die angegebenen Zeichen aus
     * 
     * @param str
     * @return str URLCodiert
     * @author JD-Team
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

    public static String Base64Encode(String plain) {

        if (plain == null) { return null; }
        String base64 = new BASE64Encoder().encode(plain.getBytes());

        return base64;
    }

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

    public static void setJDDirectory(File workingdir) {
        WebUpdater.JD_ROOT_DIRECTORY=workingdir;
        
    }

}