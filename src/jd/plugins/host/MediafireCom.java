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
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class MediafireCom extends PluginForHost {

    private static final String HOST = "mediafire.com";

    static private final String offlinelink = "tos_aup_violation";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?.+|\\?.+)", Pattern.CASE_INSENSITIVE);

    private String url;

    public MediafireCom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));

            if (requestInfo.containsHTML(offlinelink)) { return false; }

            downloadLink.setName(SimpleMatches.getBetween(requestInfo.getHtmlCode(), "<title>", "</title>"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        // switch (step.getStep()) {
        // case PluginStep.STEP_PAGE:
        url = downloadLink.getDownloadURL();
        requestInfo = HTTP.getRequest(new URL(url));
        if (requestInfo.containsHTML(offlinelink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // case PluginStep.STEP_DOWNLOAD:
        String[] para = new Regex(requestInfo.getHtmlCode(), "cg\\(\'(.*?)\',\'(.*?)\',\'(.*?)\'\\)").getMatches(0);
        para = para[0].split("'");
        requestInfo = HTTP.getRequest(new URL("http://www.mediafire.com/dynamic/download.php?qk=" + para[1] + "&pk=" + para[3] + "&r=" + para[5]), requestInfo.getCookie(), url, true);
        String finishURL = "http://" + SimpleMatches.getBetween(requestInfo.getHtmlCode(), "jn='", "'") + "/" + SimpleMatches.getBetween(requestInfo.getHtmlCode(), SimpleMatches.getBetween(requestInfo.getHtmlCode(), "jn\\+'/'\\+ ", " \\+'g/'") + " = '", "'") + "g/" + SimpleMatches.getBetween(requestInfo.getHtmlCode(), "jU='", "'") + "/" + SimpleMatches.getBetween(requestInfo.getHtmlCode(), "jK='", "'");
        HTTPConnection urlConnection = new HTTPConnection(new URL(finishURL).openConnection());
        downloadLink.setDownloadMax(urlConnection.getContentLength());
        downloadLink.setName(getFileNameFormHeader(urlConnection));
        long length = downloadLink.getDownloadMax();
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setFilesize(length);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}