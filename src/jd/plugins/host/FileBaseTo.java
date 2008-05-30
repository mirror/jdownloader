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
import jd.plugins.download.RAFDownload;

public class FileBaseTo extends PluginForHost {

    private static final String  HOST             = "filebase.to";

    private static final String  VERSION          = "1.0.0";

    static private final Pattern patternSupported = getSupportPattern("http://[w]filebase.to/files/[+]/[+]");

    //
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

    public FileBaseTo() {
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
            String name = getBetween(requestInfo.getHtmlCode(), "<br>Du hast die Datei <b>", "</b>");
            downloadLink.setName(name);
            
            requestInfo = postRequestWithoutHtmlCode(new URL(getBetween(requestInfo.getHtmlCode(), "<form name=\"waitform\" action=\"", "\"")), "", url, getFormInputHidden(requestInfo.getHtmlCode(), "<form name=\"waitform\" action=\"", "</form>") + "&wait=Download+" + name, false);
            
            HTTPConnection urlConnection = requestInfo.getConnection();
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            downloadLink.setDownloadMax(urlConnection.getContentLength());
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
            downloadLink.setName(getBetween(requestInfo.getHtmlCode(), "<br>Du hast die Datei <b>", "</b>"));
            downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(getBetween(getBetween(requestInfo.getHtmlCode(), "<font style=\"font-size: 9pt;\" face=\"Verdana\">", "B</font>"), "font-size: 9pt\">", " M").trim())*1024*1024));
            
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