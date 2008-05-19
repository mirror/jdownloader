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

import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.download.RAFDownload;

public class MediafireCom extends PluginForHost {

    private static final String  HOST             = "mediafire.com";

    private static final String  VERSION          = "1.0.0";

    static private final Pattern patternSupported = getSupportPattern("http://[*]mediafire.com/download.php?[+]");

    static private final String offlinelink = "The quickkey you provided for file download was invalid";
    
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
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public PluginStep doStep(PluginStep step, final DownloadLink downloadLink) {
        if (aborted) {
            logger.warning("Plugin aborted");
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            step.setStatus(PluginStep.STATUS_TODO);
            return step;
        }
        try {
            String url = downloadLink.getDownloadURL();                    
            requestInfo = getRequest(new URL(url));
            
            if(requestInfo.containsHTML(offlinelink)) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            
            String[] para = new Regexp(requestInfo.getHtmlCode(), "cg\\(\'(.*?)\',\'(.*?)\',\'(.*?)\'\\)").getMatches(0);
            para = para[0].split("'");
            requestInfo = getRequest(new URL("http://www.mediafire.com/dynamic/download.php?qk=" + para[1] + "&pk=" + para[3] + "&r=" + para[5]), requestInfo.getCookie(), url, true);
            
            String finishURL = "http://" + getBetween(requestInfo.getHtmlCode(), "jn='", "'") + "/" + getBetween(requestInfo.getHtmlCode(), getBetween(requestInfo.getHtmlCode(), "jn\\+'/'\\+ ", " \\+'g/'") + " = '", "'") + "g/" + getBetween(requestInfo.getHtmlCode(), "jU='", "'") + "/" + getBetween(requestInfo.getHtmlCode(), "jK='", "'");
            
            HTTPConnection urlConnection = new HTTPConnection(new URL(finishURL).openConnection());
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            downloadLink.setDownloadMax(urlConnection.getContentLength());
            downloadLink.setName(this.getFileNameFormHeader(urlConnection));
            final long length = downloadLink.getDownloadMax();

            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setFilesize(length);
            
            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            return step;

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

        return step;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = getRequest(new URL(url));
            
            if(requestInfo.containsHTML(offlinelink))
                return false;
            
            downloadLink.setName(getBetween(requestInfo.getHtmlCode(), "<title>", "</title>"));
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {}

    @Override
    public void resetPluginGlobals() {}

    @Override
    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }
}