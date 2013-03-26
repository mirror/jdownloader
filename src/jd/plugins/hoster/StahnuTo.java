//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stahnu.to" }, urls = { "http://(www\\.)?stahnu\\.to/files/get/[A-Za-z0-9]+/[A-Za-z0-9_\\-% ]+" }, flags = { 0 })
public class StahnuTo extends PluginForHost {

    private static final String CAPTCHATEXT = "captcha/captcha\\.php";

    public StahnuTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://stahnu.to/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        // Try to find stream links as they have the same site as the original
        // and we don't have to type in captchas :)
        String dllink = br.getRegex("<div align=\"center\"><audio src=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://stahnu\\.to/files/stream/.*?)\"").getMatch(0);
        if (dllink == null) {
            // Important: Captchas can be skipped right now, if they change it
            // just use the code below
            // if (!br.containsHTML(CAPTCHATEXT)) throw new
            // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // String code =
            // getCaptchaCode("http://stahnu.to/captcha/captcha.php",
            // downloadLink);
            // br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
            // if (br.containsHTML(CAPTCHATEXT)) throw new
            // PluginException(LinkStatus.ERROR_CAPTCHA)
            // ;
            br.postPage(downloadLink.getDownloadURL().replace("/files/get/", "/files/gen/"), "pass=&waited=1");
            dllink = br.getRedirectLocation();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>D\\&#283;kujem za pochopen\\&iacute;|\">Tento soubor neexistuje  nebo byl odstran\\&#283;n)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = new Regex(link.getDownloadURL(), "stahnu\\.to/files/get/[A-Za-z0-9]+/(.+)").getMatch(0);
        link.setName(filename.trim());
        String md5 = br.getRegex("<span id=\"md5\">(.*?)</span>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}