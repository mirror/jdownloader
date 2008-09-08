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

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class BluehostTo extends PluginForHost {

    public BluehostTo(String cfgName) {
        super(cfgName);
        // TODO Auto-generated constructor stub
    }

    static private final String CODER = "JD-Team";

 

    private void correctUrl(DownloadLink downloadLink) {
        String url = downloadLink.getDownloadURL();
        url = url.replaceFirst("\\?dl=", "dl=");
        downloadLink.setUrlDownload(url);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String page = null;
        Browser br = new Browser();

        correctUrl(downloadLink);

        page = br.getPage("http://bluehost.to/fileinfo/url=" + downloadLink.getDownloadURL());
        String[] dat = page.split("\\, ");

        if (dat.length != 5) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        br.getPage("http://bluehost.to/fetchinfo");
        br.getPage(downloadLink.getDownloadURL());
        if (Regex.matches(br, "Sie haben diese Datei in der letzten Stunde")) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(60 * 60 * 1000);
            logger.info("File has been requestst more then 3 times in the last hour. Reconnect or wait 1 hour.");
            return;
        }
        Form[] forms = br.getForms();
        HTTPConnection con = br.openFormConnection(forms[2]);
        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        dl = new RAFDownload(this, downloadLink, con);
        dl.setResume(false);
        dl.setChunkNum(1);
        dl.startDownload();

    }

    @Override
    public String getAGBLink() {
        return "http://bluehost.to/agb.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            correctUrl(downloadLink);
            String page;
            // dateiname, dateihash, dateisize, dateidownloads, zeit bis
            // happyhour
            Browser br = new Browser();
            page = br.getPage("http://bluehost.to/fileinfo/url=" + downloadLink.getDownloadURL());
            String[] dat = page.split("\\, ");
            if (dat.length != 5) { return false; }
            downloadLink.setName(dat[0]);
            downloadLink.setDownloadSize(Integer.parseInt(dat[2]));
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetPluginGlobals() {

    }
}
