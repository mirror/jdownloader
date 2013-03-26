//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadur.com" }, urls = { "https?://(www\\.)?uploadur\\.com/files/\\w+\\.html" }, flags = { 0 })
public class UploadURCom extends PluginForHost {

    private static final String HOST = "http://uploadur.com";

    // DEV NOTES
    // mods:
    // non account: 1 * unlimited
    // free account:
    // premium account:
    // protocol: no https
    // other: redirects to uploadur.com
    // other: without .html it reports links as deleted/removed
    // other: link checker not very useful, it doesn't return file name or size

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("https://(www\\.)?", "http://"));
    }

    public UploadURCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(HOST + "/premium.html");
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    @Override
    public String getAGBLink() {
        return HOST + "/help/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        checkErrors();
        String[][] fileInfo = br.getRegex("(?i)<div class=\"row2\\-download\\-top\"><.*?> ([^>]+)</.*?><.*?>([\\d\\.]+ (KB|MB|GB|TB))").getMatches();
        if (fileInfo == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(fileInfo[0][0].trim()));
        link.setDownloadSize(SizeFormatter.getSize(fileInfo[0][1]));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String freelink = br.getRegex("value=\"Free Download \\( Normal \\)\" id=\"dlbutton\" disabled=\"disabled\" onclick=\"document\\.location=\\'([^\\']+)").getMatch(0);
        if (freelink == null) {
            freelink = br.getRegex("(https?://.+?/get/[^\\']+)").getMatch(0);
            if (freelink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(freelink);
        Form dlForm = br.getForm(1);
        if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dlForm.setAction(dlForm.getAction().replace("./get/", "/get/"));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlForm, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            checkErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", br.getURL());
        dl.startDownload();
    }

    private void checkErrors() throws PluginException {
        if (br.containsHTML("<div class=\"file\\-error\"><h1> File not found \\!</h1>|<h2>File have been remove or deleted</h2></div>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}