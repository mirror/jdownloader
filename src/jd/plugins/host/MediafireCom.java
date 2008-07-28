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

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.download.RAFDownload;

public class MediafireCom extends PluginForHost {

    private static final String HOST = "mediafire.com";

    private static final String VERSION = "1.0.0";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?.+|\\?.+)", Pattern.CASE_INSENSITIVE);

    static private final String offlinelink = "tos_aup_violation";

    private String url;

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public MediafireCom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        //steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public void handle( final DownloadLink downloadLink) {

        try {
            switch (step.getStep()) {
            case PluginStep.STEP_PAGE:
                url = downloadLink.getDownloadURL();
                requestInfo = HTTP.getRequest(new URL(url));
                if (requestInfo.containsHTML(offlinelink)) {
                    downloadLink.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                return step;
            case PluginStep.STEP_DOWNLOAD:
                String[] para = new Regex(requestInfo.getHtmlCode(), "cg\\(\'(.*?)\',\'(.*?)\',\'(.*?)\'\\)").getMatches(0);
                para = para[0].split("'");
                requestInfo = HTTP.getRequest(new URL("http://www.mediafire.com/dynamic/download.php?qk=" + para[1] + "&pk=" + para[3] + "&r=" + para[5]), requestInfo.getCookie(), url, true);
                String finishURL = "http://" + SimpleMatches.getBetween(requestInfo.getHtmlCode(), "jn='", "'") + "/" + SimpleMatches.getBetween(requestInfo.getHtmlCode(), SimpleMatches.getBetween(requestInfo.getHtmlCode(), "jn\\+'/'\\+ ", " \\+'g/'") + " = '", "'") + "g/" + SimpleMatches.getBetween(requestInfo.getHtmlCode(), "jU='", "'") + "/" + SimpleMatches.getBetween(requestInfo.getHtmlCode(), "jK='", "'");
                HTTPConnection urlConnection = new HTTPConnection(new URL(finishURL).openConnection());
                downloadLink.setDownloadMax(urlConnection.getContentLength());
                downloadLink.setName(this.getFileNameFormHeader(urlConnection));
                long length = downloadLink.getDownloadMax();
                dl = new RAFDownload(this, downloadLink, urlConnection);
                dl.setFilesize(length);
                if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                    downloadLink.setStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                return step;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);

        return step;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));

            if (requestInfo.containsHTML(offlinelink)) return false;

            downloadLink.setName(SimpleMatches.getBetween(requestInfo.getHtmlCode(), "<title>", "</title>"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }
}