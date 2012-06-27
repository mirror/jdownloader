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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uptorch.com" }, urls = { "https?://(www\\.)?uptorch\\.com/\\?d=[A-Z0-9]{9}" }, flags = { 0 })
public class UpTorchCom extends PluginForHost {

    private static final String HOST = "http://uptorch.com";

    // DEV NOTES
    // mods:
    // non account: 6 * 1?
    // free account:
    // premium account:
    // protocol: no https
    // other: no redirects

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    public UpTorchCom(PluginWrapper wrapper) {
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
        return HOST + "/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        checkErrors();
        String filename = br.getRegex("(?i)<title>(.+)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("(?i)<meta name=\"description\" content=\"(.+), UpTorch").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("(?i)<h2 class=\"float\\-left\">(.+)</h2>").getMatch(0);
            }
        }
        String filesize = br.getRegex("(?i)<b>([\\d\\.]+ (KB|MB|GB|TB))</b> / \\d+ Times\\.</li>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("(?i)([\\d\\.]+ ?(KB|MB|GB))").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        String dllink = (checkDirectLink(downloadLink, "directlink"));
        if (dllink == null) {
            requestFileInformation(downloadLink);
            Form form = br.getForm(0);
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.submitForm(form);
            checkErrors();
            dllink = br.getRegex("ondblclick=\"ClipBoard\\(\\'downloadurl\\'\\);\" class=\"textinput\" value=\"([^\"]+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("document.location='([^\\']+)").getMatch(0);
                if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            checkErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private void checkErrors() throws PluginException {
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/redirect.php?")) {
            if (br.getRedirectLocation().contains("/redirect.php?error=1&code=DL_FileNotFound")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(":The allowed bandwidth assigned to your IP is used up\\.")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Session bandwidth has been used", 2 * 60 * 60 * 1000l);
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
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

}