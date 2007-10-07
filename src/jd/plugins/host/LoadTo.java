package jd.plugins.host;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public class LoadTo extends PluginForHost {
    private static final String  CODER                    = "Bo0nZ";

    private static final String  HOST                     = "load.to";

    private static final String  PLUGIN_NAME              = HOST;

    private static final String  PLUGIN_VERSION           = "1.0.0.0";

    private static final String  PLUGIN_ID                = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    private static final Pattern PAT_SUPPORTED            = getSupportPattern("http://[*]load.to/\\?d=[+]");

    private String               downloadURL              = "";

    private URLConnection        urlConnection;

    /*
     * Suchmasken (z.B. Fehler)
     */
    private static final String  ERROR_DOWNLOAD_NOT_FOUND = "Can't find file. Please check URL.";

    private static final String  DOWNLOAD_INFO            = "<tr><td width=\"80\">Filename:</td><td valign=\"top\"><b>°</b></td></tr><tr><td width=\"80\">Size:</td><td>° Bytes</td></tr>";

    private static final String  DOWNLOAD_LINK            = "action=\"°\" method=\"post\"><input type=\"submit\" value=\"Download the file\"";

    /*
     * Konstruktor
     */
    public LoadTo() {
        super();

        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    /*
     * Funktionen
     */
    // muss aufgrund eines Bugs in DistributeData true zurÃ¼ckgeben, auch wenn
    // die Zwischenablage nicht vom Plugin verarbeitet wird
    @Override
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public void reset() {
        this.downloadURL = "";
        urlConnection = null;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1; // max 1. Download
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()));

            String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 0));
            String fileSize = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 1));

            // Wurden DownloadInfos gefunden? --> Datei ist vorhanden/online
            if (fileName != null && fileSize != null) {
                downloadLink.setName(fileName);

                try {
                    int length = Integer.parseInt(fileSize.trim());
                    downloadLink.setDownloadMax(length);
                }
                catch (Exception e) {
                }

                // Datei ist noch verfuegbar
                return true;
            }

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Datei scheinbar nicht mehr verfuegbar, Fehler?
        return false;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {

            URL downloadUrl = new URL(downloadLink.getUrlDownloadDecrypted());

            switch (step.getStep()) {
                case PluginStep.STEP_PAGE:
                    requestInfo = getRequest(downloadUrl);

                    // Datei nicht gefunden?
                    if (requestInfo.getHtmlCode().indexOf(ERROR_DOWNLOAD_NOT_FOUND) > 0) {
                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 0));
                    String fileSize = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 1));
                    try {
                        int length = Integer.parseInt(fileSize.trim());
                        downloadLink.setDownloadMax(length);
                    }
                    catch (Exception e) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    downloadLink.setName(fileName);
                    // downloadLink auslesen
                    this.downloadURL = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_LINK, 0));

                    return step;

                case PluginStep.STEP_PENDING:

                    // immer 5 Sekunden vor dem Download warten!
                    step.setParameter(10l);
                    return step;
                case PluginStep.STEP_WAIT_TIME:
                    // Download vorbereiten
                    downloadLink.setStatusText("Verbindung aufbauen(0-20s)");
                    urlConnection = new URL(this.downloadURL).openConnection();
                    int length = urlConnection.getContentLength();
                    if (Math.abs(length - downloadLink.getDownloadMax()) > 1024) {
                        logger.warning("Filesize Check fehler. Neustart");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    downloadLink.setDownloadMax(length);
                    return step;

                case PluginStep.STEP_DOWNLOAD:

                    // Download starten
                    boolean downloadSuccess = download(downloadLink, urlConnection);

                    // Download erfolgreich oder fehlerhaft?
                    if (downloadSuccess == false) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

                    }
                    else {
                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    }

                    return step;

            }
            return step;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
