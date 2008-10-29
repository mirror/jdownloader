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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class BluehostTo extends PluginForHost {

    public BluehostTo(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium();
    }

    private void correctUrl(DownloadLink downloadLink) {
        String url = downloadLink.getDownloadURL();
        url = url.replaceFirst("\\?dl=", "dl=");
        downloadLink.setUrlDownload(url);
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        this.setBrowserExclusive();
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage("http://bluehost.to");
        br.postPage("http://bluehost.to/setcookie.php", "loginname=" + account.getUser() + "&loginpass=" + account.getPass() + "&gobutton.x=&gobutton.y=");
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL(), false, 1);
        if (dl.getConnection().getContentType().contains("text")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FATAL, "Premium Beta Error");
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        getFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (Regex.matches(br, "Sie haben diese Datei in der letzten Stunde")) {
            logger.info("File has been requestst more then 3 times in the last hour. Reconnect or wait 1 hour.");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        Form[] forms = br.getForms();
        dl = br.openDownload(downloadLink, forms[2], false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://bluehost.to/agb.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        correctUrl(downloadLink);
        // String page;
        // dateiname, dateihash, dateisize, dateidownloads, zeit bis
        // happyhour
        //           
        String page = br.getPage("http://bluehost.to/fileinfo/urls=" + downloadLink.getDownloadURL());

        String[] dat = page.split("\\, ");

        if (dat.length != 5) {
            Browser br = new Browser();
            br.getPage(downloadLink.getDownloadURL());
            String filename = br.getRegex("dl_filename2\">(.*?)</div>").getMatch(0);
            String filesize = br.getRegex("<div class=\"dl_groessefeld\">(\\d+?)<font style='font-size: 8px;'>(.*?)</font></div>").getMatch(0).trim() + " " + br.getRegex("<div class=\"dl_groessefeld\">(\\d+?)<font style='font-size: 8px;'>(.*?)</font></div>").getMatch(1);
            if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
            return true;
        } else {
            downloadLink.setName(dat[0]);
            downloadLink.setDownloadSize(Integer.parseInt(dat[2]));
        }
        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
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
