package jd.captcha;

import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * @author coalado
 * Webupdater lädt pfad und hash infos von einem server und vergleicht sie mit den lokalen versionen
 * 
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
     * Pfad zum lokalen Vergleichsverzeichniss
     */
    private File  destPath     = new File("");
    /**
     * Logger
     */
    public Logger logger       = UTILITIES.getLogger();
    /**
     * anzahl der aktualisierten Files
     */
    private int   updatedFiles = 0;
    /**
     * Anzahl der ganzen Files
     */
    private int   totalFiles   = 0;

    /**
     * @param path  (Dir Pfad zum Updateserver)
     */
    public WebUpdater(String path) {
        this.setListPath(path);

    }

    /**
     * Startet das Updaten
     */
    public void run() {

        Vector<Vector<String>> files = getAvailableFiles();
        logger.info(files.toString());
        totalFiles = files.size();
        filterAvailableUpdates(files);
        updateFiles(files);
    }
/**
 * Updated alle files in files
 * @param files
 */
    private void updateFiles(Vector<Vector<String>> files) {
        String akt;
        updatedFiles = 0;
        for (int i = files.size() - 1; i >= 0; i--) {
            akt = destPath.getAbsolutePath() + UTILITIES.FS + files.elementAt(i).elementAt(0);

            if (!new File(akt + ".noUpdate").exists()) {
                updatedFiles++;
                logger.info("Webupdater: file: " + onlinePath + "/" + files.elementAt(i).elementAt(0) + " to " + akt);
                UTILITIES.downloadBinary(akt, onlinePath + "/" + files.elementAt(i).elementAt(0));
                logger.info("Webupdater: ready");
            }
        }

    }
/**
 * löscht alles files aus files die nicht aktualisiert werden brauchen
 * @param files
 */
    private void filterAvailableUpdates(Vector<Vector<String>> files) {
        logger.info(destPath.getAbsolutePath());
        String akt;
        String hash;
        for (int i = files.size() - 1; i >= 0; i--) {
            akt = destPath.getAbsolutePath() + UTILITIES.FS + files.elementAt(i).elementAt(0);
            if (!new File(akt).exists()) {
                continue;
            }
            hash = UTILITIES.getLocalHash(akt);
            if (!hash.equalsIgnoreCase(files.elementAt(i).elementAt(1))) {
                continue;
            }
            files.removeElementAt(i);
        }

    }
/**
 * Liest alle files vom server
 * @return Vector mit allen verfügbaren files
 */
    private Vector<Vector<String>> getAvailableFiles() {
        String source = UTILITIES.getPagewithScanner(listPath);
        if (source == null) {
            logger.severe(listPath + " nicht verfüpgbar");
            return null;
        }
        return UTILITIES.getAllMatches(source, "$°=\"°\";°");
    }

    /**
     * @return the destPath
     */
    public File getDestPath() {
        return destPath;
    }

    /**
     * @param destPath
     *            the destPath to set
     */
    public void setDestPath(File destPath) {
        this.destPath = destPath;
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