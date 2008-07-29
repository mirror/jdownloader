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
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.CRequest;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.download.RAFDownload;

public class zShare extends PluginForHost {
    private static final String  HOST             = "zshare.net";

    private static final String  VERSION          = "1.0.0.0";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?zshare\\.net/(download|video|image|audio|flash)/.*", Pattern.CASE_INSENSITIVE);

    //
    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

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

    public zShare() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public void handle( DownloadLink downloadLink) {
//        if (aborted) {
//            logger.warning("Plugin abgebrochen");
//            downloadLink.setStatus(LinkStatus.TODO);
//            //step.setStatus(PluginStep.STATUS_TODO);
//            return;
//        }
        try {
            logger.info(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image"));
            request= new CRequest();
            request = request.getRequest(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image"));
         
             Regex reg = request.getRegexp("<img src=\"(http://[^\"]*?/download/[a-f0-9]*?/[\\d]*?/[\\d]*?/.*?)\"");
            
            String url=reg.getMatches()[0][0];
            request.withHtmlCode = false;
            HTTPConnection urlConnection = request.getRequest(url).getConnection();
            downloadLink.setName(getFileNameFormHeader(urlConnection));
            downloadLink.setDownloadMax(urlConnection.getContentLength());
  
           dl = new RAFDownload(this, downloadLink, urlConnection);
    
            dl.startDownload();
            return;
        }
        catch (Exception e) {
            e.printStackTrace();
            //step.setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
            return;
        }
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            String[] fileInfo = request.getRequest(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image")).getRegexp("File Name: .*?<font color=\".666666\">(.*?)</font>.*?Image Size: <font color=\".666666\">([0-9\\.\\,]*)(.*?)</font></td>").getMatches()[0];
            downloadLink.setName(fileInfo[0]);
            try {
                double length = Double.parseDouble(fileInfo[1].replaceAll("\\,", "").trim());
                int bytes;
                String type = fileInfo[2].toLowerCase();
                if (type.equalsIgnoreCase("kb")) {
                    bytes = (int) (length * 1024);
                }
                else if (type.equalsIgnoreCase("mb")) {
                    bytes = (int) (length * 1024 * 1024);
                }
                else {
                    bytes = (int) length;
                }
                downloadLink.setDownloadMax(bytes);
            }
            catch (Exception e) {
            }
            // Datei ist noch verfuegbar
            return true;
        }
        catch (Exception e) {
            // TODO: handle exception
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public void resetPluginGlobals() {
    // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public String getAGBLink() {

        return "http://www.zshare.net/TOS.html";
    }
}
