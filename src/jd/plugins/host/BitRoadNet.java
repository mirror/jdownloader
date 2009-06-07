//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class BitRoadNet extends PluginForHost {

    public BitRoadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://bitroad.net/tmpl/terms.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File:<b[^>]*>(.*?)</b>").getMatch(0);
        String size = br.getRegex("Size:<b[^>]*>(.*?)</b>").getMatch(0);
        if (filename == null || size == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(size));
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String previousLink = downloadLink.getStringProperty("directLink", null);
        String url = null;
        if (previousLink == null) {
            requestFileInformation(downloadLink);
            Form dl1 = br.getFormbyProperty("id", "Premium");
            if (dl1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            br.submitForm(dl1);
            dl1 = null;
            dl1 = br.getForm(0);
            if (dl1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String id = dl1.getVarsMap().get("uid");
            String captchaurl = "http://bitroad.net/cap/" + id;
            Browser br2 = br.cloneBrowser();
            br2.getPage(captchaurl);
            String code = br2.toString().replaceAll("\\W", "");
            dl1.setAction("http://bitroad.net/download3.php");
            if (code.length() > 7 || code.length() < 3) {
                logger.warning("Cannot use psp's captcha method, trying normal OCR...");
                captchaurl = captchaurl + ".jpg";
                code = getCaptchaCode(captchaurl, downloadLink);
            }
            dl1.put("cap", code);
            br.submitForm(dl1);
            if (!br.containsHTML("Your link to")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            url = br.getRegex("<div id=\"links\"[^>]*><a href='(.*?)'").getMatch(0);
            this.sleep(2000, downloadLink);
            downloadLink.setProperty("directLink", url);
        } else {
            url = previousLink;
        }
        dl = br.openDownload(downloadLink, url, true, 1);
        URLConnectionAdapter cond = dl.getConnection();
        if (!cond.isOK()) {
            if (previousLink != null) {
                downloadLink.setProperty("directLink", null);
                cond.disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                cond.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void reset_downloadlink(DownloadLink link) {
        link.setProperty("directLink", null);
    }
}