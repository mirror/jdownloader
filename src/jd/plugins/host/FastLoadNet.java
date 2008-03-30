//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.plugins.download.ChunkFileDownload;
import jd.utils.JDUtilities;

public class FastLoadNet extends PluginForHost {

    private static final String  CODER                  = "eXecuTe";

    private static final String  HOST                   = "fast-load.net";

    private static final String  PLUGIN_NAME            = HOST;

    private static final String  PLUGIN_VERSION         = "0.1.4";

    private static final String  PLUGIN_ID              = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final Pattern PAT_SUPPORTED          = Pattern.compile("http://.*?fast-load\\.net(/|//)index\\.php\\?pid=[a-zA-Z0-9]+");

    private static final int     MAX_SIMULTAN_DOWNLOADS = 1;

    private String               downloadURL            = "";

    // Suchmasken
    private static final String  DOWNLOAD_SIZE          = "<div id=\"dlpan_size\" style=\".*?\">(.*?) MB</div>";

    private static final String  DOWNLOAD_NAME          = "<div id=\"dlpan_file\" style=\".*?\">(.*?)</div>";

    private static final String  DOWNLOAD_LINK          = "<div id=\"dlpan_btn\" style=\".*?\"><a href=\"(.*?)\">";

    private static final String  NOT_FOUND              = "Datei existiert nicht";

    private static final String  FAULTY_LINK            = "Fehlerhafter Link";

    public FastLoadNet() {

        super();
        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

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
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        try {

            RequestInfo requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));

            if (requestInfo.getHtmlCode().contains(NOT_FOUND)) {

                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                return false;

            }

            String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
            Integer length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim()) * 1024 * 1024);

            // downloadinfos gefunden? -> download verfügbar
            if (fileName != null && length != null) {

                downloadLink.setName(fileName);

                try {
                    downloadLink.setDownloadMax(length);
                }
                catch (Exception e) {
                }

                return true;

            }

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // unbekannter fehler
        return false;

    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {

        try {

            URL downloadUrl = new URL(downloadLink.getDownloadURL());

            switch (step.getStep()) {

                case PluginStep.STEP_PAGE:
logger.info(downloadUrl+"");
                    requestInfo = getRequest(downloadUrl);

                    if (requestInfo.getHtmlCode().contains(NOT_FOUND)) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }

                    String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
                    downloadLink.setName(fileName);

                    try {

                        int length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim()) * 1024 * 1024);
                        downloadLink.setDownloadMax(length);

                    }
                    catch (Exception e) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }

                    // downloadLink auslesen
                    downloadURL = new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_LINK).getFirstMatch();

                    return step;

                case PluginStep.STEP_DOWNLOAD:
                    String host = new URL(downloadURL).getHost();
                    String finalURL = getRequest(new URL(downloadURL)).getConnection().getHeaderField("Location");

                    if (finalURL == null) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    downloadURL = "http://" + host + finalURL;

                    // Download vorbereiten
                    HTTPConnection urlConnection = new HTTPConnection(new URL(downloadURL).openConnection());
                    int length = urlConnection.getContentLength();

                    if (Math.abs(length - downloadLink.getDownloadMax()) > 1024 * 1024) {

                        requestInfo = getRequest(new URL(downloadURL));

                        if (requestInfo.containsHTML(FAULTY_LINK)) {

                            logger.severe("faulty Link");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;

                        }

                        logger.warning("Filesize Error");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }

                    downloadLink.setDownloadMax(length);

                    // Download starten
                    ChunkFileDownload dl = new ChunkFileDownload(this, downloadLink, urlConnection);
                    dl.setResume(true);dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS,3));

                    dl.startDownload();

                    return step;

            }

            return step;

        }
        catch (IOException e) {

            e.printStackTrace();
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);

        }
        return step;

    }

    @Override
    public void resetPluginGlobals() {
        this.downloadURL = null;
    }

    @Override
    public String getAGBLink() {
        return "http://www.fast-load.net/infos.php";
    }

}