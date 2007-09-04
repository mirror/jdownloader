package jd.update;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.JDUtilities;

import jd.plugins.Plugin;

/**
 * @author coalado Webupdater lädt pfad und hash infos von einem server und
 *         vergleicht sie mit den lokalen versionen
 */
public class WebUpdater {
    /**
     * Pfad zur lis.php auf dem updateserver
     */
    public String listPath;
    /**
     * Pfad zum Online-Bin verzeichniss
     */
    public String onlinePath;

    /**
     * Logger
     */
    public Logger logger       = Plugin.getLogger();
    /**
     * anzahl der aktualisierten Files
     */
    private int   updatedFiles = 0;
    /**
     * Anzahl der ganzen Files
     */
    private int   totalFiles   = 0;

    /**
     * @param path
     *            (Dir Pfad zum Updateserver)
     */
    public WebUpdater(String path) {
        this.setListPath(path);

    }

    /**
     * Startet das Updaten
     */
    public void run() {

        Vector<Vector<String>> files = getAvailableFiles();
        if (files != null) {
            logger.info(files.toString());
            totalFiles = files.size();
            filterAvailableUpdates(files);
            updateFiles(files);
        }
    }
    /**
     * 
     * @return Anzahld er neuen Datein
     */
public int getUpdateNum(){
    Vector<Vector<String>> files = getAvailableFiles();
    
    if (files == null) return 0;
       
        totalFiles = files.size();
        filterAvailableUpdates(files);
        return files.size();
    
}
    /**
     * Updated alle files in files
     * 
     * @param files
     */
    private void updateFiles(Vector<Vector<String>> files) {
        String akt;
        updatedFiles = 0;
        for (int i = files.size() - 1; i >= 0; i--) {

            akt = JDUtilities.getResourceFile(files.elementAt(i).elementAt(0)).getAbsolutePath();
            if (!new File(akt + ".noUpdate").exists()) {
                updatedFiles++;
                logger.info("Webupdater: file: " + onlinePath + "/" + files.elementAt(i).elementAt(0) + " to " + akt);
                downloadBinary(akt, onlinePath + "/" + files.elementAt(i).elementAt(0));
                logger.info("Webupdater: ready");
            }
        }

    }

    /**
     * löscht alles files aus files die nicht aktualisiert werden brauchen
     * 
     * @param files
     */
    private void filterAvailableUpdates(Vector<Vector<String>> files) {

        String akt;
        String hash;
        for (int i = files.size() - 1; i >= 0; i--) {
            akt = JDUtilities.getResourceFile(files.elementAt(i).elementAt(0)).getAbsolutePath();
            if (!new File(akt).exists()) {
                continue;
            }
            hash = JDUtilities.getLocalHash(new File(akt));
          
            if (!hash.equalsIgnoreCase(files.elementAt(i).elementAt(1))) {
                continue;
            }
            files.removeElementAt(i);
        }

    }

    /**
     * Liest alle files vom server
     * 
     * @return Vector mit allen verfügbaren files
     */
    public Vector<Vector<String>> getAvailableFiles() {
        String source = getPage(listPath);
       
        String pattern = "\\$(.*?)\\=\\\"(.*?)\\\"\\;(.*?)";
      
        Vector<Vector<String>> ret = new Vector<Vector<String>>();
        if (source == null) {
            logger.severe(listPath + " nicht verfüpgbar");
            return null;
        }
        Vector<String> entry;
        String tmp;
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(source); r.find();) {
            entry = new Vector<String>();

            for (int x = 1; x <= r.groupCount(); x++) {
                if ((tmp = r.group(x).trim()).length() > 0) {
                    entry.add(JDUtilities.UTF8Decode(tmp));
                }
            }
            ret.add(entry);

        }
       

        return ret;
    }

    /**
     * LIest eine webseite ein und gibt deren source zurück
     * 
     * @param urlStr
     * @return String inhalt von urlStr
     */
    private String getPage(String urlStr) {
        URL url;
        URLConnection con;
        Scanner reader;
        BufferedReader input;
        try {
            url = new URL(urlStr);
            con = url.openConnection();
            input = new BufferedReader(new InputStreamReader(con.getInputStream()));
            reader = new Scanner(input).useDelimiter("\\Z");
            String ret = "";
            while (reader.hasNext()) {
                ret += reader.next();
            }
            return ret;

        } catch (FileNotFoundException e) {
            logger.severe(urlStr + " nicht gefunden");
        } catch (MalformedURLException e) {
            logger.severe(urlStr + " Malformed URL");
        } catch (SocketTimeoutException e) {
            logger.severe(urlStr + " Socket Timeout");
        } catch (IOException e) {
            logger.severe("IOException " + e);
        }
        return null;
    }

    /**
     * Lädt fileurl nach filepath herunter
     * 
     * @param filepath
     * @param fileurl
     * @return true/False
     */
    private boolean downloadBinary(String filepath, String fileurl) {

        try {
            fileurl = JDUtilities.urlEncode(fileurl.replaceAll("\\\\", "/"));
            File file = new File(filepath);
            if (file.isFile()) {
                if (!file.delete()) {
                    logger.severe("Konnte Datei nicht löschen " + file);
                    return false;
                }

            }

            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            fileurl = URLDecoder.decode(fileurl, "UTF-8");

            URL url = new URL(fileurl);
            URLConnection con = url.openConnection();

            BufferedInputStream input = new BufferedInputStream(con.getInputStream());

            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();

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
     * @return the listPath
     */
    public String getListPath() {
        return listPath;
    }

    /**
     * @param listPath
     *            the listPath to set
     */
    public void setListPath(String listPath) {
        this.listPath = listPath + "/list.php";
        this.onlinePath = listPath + "/bin";

    }

    /**
     * @return the totalFiles
     */
    public int getTotalFiles() {
        return totalFiles;
    }

    /**
     * @return the updatedFiles
     */
    public int getUpdatedFiles() {
        return updatedFiles;
    }

}