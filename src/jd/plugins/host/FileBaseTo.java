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
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class FileBaseTo extends PluginForHost {

    private static final String HOST = "filebase.to";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?filebase\\.to/files/\\d{1,}/.*", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;

    //

    public FileBaseTo() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        Browser.clearCookies(HOST);
        try {
            String url = downloadLink.getDownloadURL();
            br.getPage(url);
     
            String[] helpurl = url.split("/");
            downloadLink.setName(helpurl[helpurl.length - 1]);
            if ( br.containsHTML("Angeforderte Datei herunterladen")) {
              
                br.getPage(url + "&dl=1");
            }

            if (br.containsHTML("Vielleicht wurde der Eintrag")) {
               
                return false;
            }
            
            String size=br.getRegex("<font style=\"font-size: 9pt;\" face=\"Verdana\">Datei.*?font-size: 9pt\">(.*?)</font>").getFirstMatch();
            downloadLink.setDownloadSize(Regex.getSize(size));

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
    /*
     * public int getMaxSimultanDownloadNum() { return Integer.MAX_VALUE; }
     * 
     * @Override
     */public String getPluginName() {
        return HOST;
    }

    private int getSize(String size) {
        if (size == null) { return 0; }
        String[] help = size.split(" ");
        int loops = 0;
        Double s = Double.parseDouble(help[0]);

        if (help[1].equals("KB")) {
            loops = 1;
        }
        if (help[1].equals("MB")) {
            loops = 2;
        }
        if (help[1].equals("GB")) {
            loops = 3;
        }
        if (help[1].equals("TB")) {
            loops = 4;
        }

        for (int i = 0; i < loops; i++) {
            s = s * 1024;
        }

        return (int) Math.round(s);
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

        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        HTTPConnection urlConnection = br.openFormConnection(1);
        downloadLink.setDownloadSize(urlConnection.getContentLength());
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setResume(false);
        dl.setChunkNum(1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}