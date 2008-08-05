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
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class RamZal extends PluginForHost {
    private static final String HOST = "ramzal.com";

    // http://ramzal.com//upload_files/1280838337_wallpaper-1280x1024-007.jpg
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?ramzal\\.com//?upload_files/.*", Pattern.CASE_INSENSITIVE);

    //

    public RamZal() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getAGBLink() {

        return "http://ramzal.com/";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadLink.getDownloadURL()), null, null, true);
            HTTPConnection urlConnection = requestInfo.getConnection();
            downloadLink.setName(Plugin.getFileNameFormHeader(urlConnection));
            downloadLink.setDownloadSize(urlConnection.getContentLength());
            return true;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
   */ public String getPluginName() {
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
    public void handleFree(DownloadLink downloadLink) throws Exception {

        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // if (aborted) {
        // logger.warning("Plugin abgebrochen");
        // linkStatus.addStatus(LinkStatus.TODO);
        // //step.setStatus(PluginStep.STATUS_TODO);
        // return;
        // }

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadLink.getDownloadURL()), null, null, true);

        HTTPConnection urlConnection = requestInfo.getConnection();
        dl = new RAFDownload(this, downloadLink, urlConnection);

        dl.startDownload();

        // \r\n if (!dl.startDownload() && step.getStatus() !=
        // PluginStep.STATUS_ERROR && step.getStatus() !=
        // PluginStep.STATUS_TODO) {
        // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        //			
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // }
        // return;
        //
        // } catch (MalformedURLException e) {
        //			
        // e.printStackTrace();
        // } catch (IOException e) {
        //			
        // e.printStackTrace();
        // }
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        // return;
    }

    @Override
    public void reset() {
        // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Automatisch erstellter Methoden-Stub
    }
}
