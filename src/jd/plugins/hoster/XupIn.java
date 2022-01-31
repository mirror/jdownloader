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
package jd.plugins.hoster;

import java.io.IOException;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xup.in" }, urls = { "https?://[\\w\\.]*?xup\\.((in|to)/dl,\\d+(/.+)?|raidrush\\.ws/ndl_[a-z0-9]+)" })
public class XupIn extends PluginForHost {
    private static final String AGB_LINK = "http://www.xup.in/terms/";

    public XupIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("Datei existiert nicht") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        String filesize = null;
        if (link.getPluginPatternMatcher().contains("xup.raidrush.ws/")) {
            filename = br.getRegex("<title>XUP - Download (.*?) \\| ").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1>XUP - Download (.*?) \\| ").getMatch(0);
            }
            filesize = br.getRegex("Size</font></td>[\t\n\r ]+<td>(\\d+)</td>").getMatch(0);
        } else {
            filename = br.getRegex("<legend>.*?<.*?>Download:(.*?)</.*?>").getMatch(0);
            filesize = br.getRegex("File Size:(.*?)</li>").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        link.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        br.setDebug(true);
        this.requestFileInformation(link);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        Form download = br.getForm(0);
        if (download == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String passCode = null;
        if (download.hasInputFieldByName("vpass")) {
            if (link.getDownloadPassword() == null) {
                passCode = getUserInput(null, link);
            } else {
                /* Use saved password */
                passCode = link.getDownloadPassword();
            }
            download.put("vpass", passCode);
        }
        if (download.hasInputFieldByName("vchep")) {
            final String code = this.getCaptchaCode("http://www0.xup.in/captcha.php", link);
            download.put("vchep", code);
        }
        download.remove(null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, download);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            String page = br.followConnection(true);
            if (page.contains("richtige Passwort erneut ein")) {
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.xupin.errors.passwrong", "Password wrong"));
            } else if (br.containsHTML(">Die Sicherheitsfrage wurde falsch eingegeben|/captcha\\.php\"")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                logger.warning("Unexpected error occured");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (passCode != null) {
            link.setDownloadPassword(passCode);
        }
        dl.startDownload();
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}