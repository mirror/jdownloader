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


package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class LoadTo extends PluginForHost {
    private static final String  CODER                    = "Bo0nZ";

    private static final String  HOST                     = "load.to";

    private static final String  PLUGIN_NAME              = HOST;

    private static final String  PLUGIN_VERSION           = "1.0.0.0";

    private static final String  PLUGIN_ID                = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    //www.load.to/?d=f8tM7YMcq5
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?load\\.to/\\?d\\=.{10}", Pattern.CASE_INSENSITIVE);

    private String               downloadURL              = "";

    private HTTPConnection        urlConnection;

    /*
     * Suchmasken (z.B. Fehler)
     */
    private static final String  ERROR_DOWNLOAD_NOT_FOUND = "Can't find file. Please check URL.";

    private static final String  DOWNLOAD_INFO            = "You are going to download<br>°<a href=\"http://www.load.to/?d=°\" style=\"font-size:16px; color:#960000; text-decoration:none; cursor:default;\"><b>°</b></a><br>Size: ° Bytes &nbsp;<font";

    private static final String  DOWNLOAD_LINK            = "<form action=\"°\" method=\"post\" name=\"";

    /*
     * Konstruktor
     */
    public LoadTo() {
        super();

        //steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        //steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        //steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        //steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    /*
     * Funktionen
     */
    // muss aufgrund eines Bugs in DistributeData true zurÃ¼ckgeben, auch wenn
    // die Zwischenablage nicht vom Plugin verarbeitet wird
 

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
        return 3; // max 1. Download
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            RequestInfo requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));

String fileName = JDUtilities.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 2));
String fileSize = JDUtilities.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 3));


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

    public void handle( DownloadLink downloadLink) {
        try {

            URL downloadUrl = new URL(downloadLink.getDownloadURL());

            switch (step.getStep()) {
                case PluginStep.STEP_PAGE:
                    requestInfo = HTTP.getRequest(downloadUrl);

                    // Datei nicht gefunden?
                    if (requestInfo.getHtmlCode().indexOf(ERROR_DOWNLOAD_NOT_FOUND) > 0) {
                        logger.severe("download not found");
                        downloadLink.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    String fileName = JDUtilities.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 2));
                    String fileSize = JDUtilities.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 3));
                   
                    try {
                        int length = Integer.parseInt(fileSize.trim());
                        downloadLink.setDownloadMax(length);
                    }
                    catch (Exception e) {

                        downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    downloadLink.setName(fileName);
                    // downloadLink auslesen
                 
                    this.downloadURL = JDUtilities.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_LINK, 0));

                    return step;

                case PluginStep.STEP_PENDING:

                    // immer 5 Sekunden vor dem Download warten!
                    //step.setParameter(10l);
                    return step;
                case PluginStep.STEP_WAIT_TIME:
                    // Download vorbereiten
                    downloadLink.getLinkStatus().setStatusText("Verbindung aufbauen(0-20s)");
                    urlConnection = new HTTPConnection(new URL(this.downloadURL).openConnection());
                    int length = urlConnection.getContentLength();
                    if (Math.abs(length - downloadLink.getDownloadMax()) > 1024) {
                        logger.warning("Filesize Check fehler. Neustart");
                        downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    downloadLink.setDownloadMax(length);
                    return step;

                case PluginStep.STEP_DOWNLOAD:
              
                   
                  dl = new RAFDownload(this, downloadLink,  urlConnection);
              
                   dl.startDownload();
                    return step;

            }
            return step;
        }
        catch (IOException e) {
             e.printStackTrace();
             downloadLink.setStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
             downloadLink.getLinkStatus().setStatusText(e.getMessage());
             step.setStatus(PluginStep.STATUS_ERROR);
             return step;
        }
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://www.load.to/terms.php";
    }

}
