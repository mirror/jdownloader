//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share4web.com" }, urls = { "http://(www\\.)?share4web\\.com/get/[\\w\\.\\-]{32}" })
public class Share4WebCom extends PluginForHost {
    public Share4WebCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.share4web.com/page/terms";
    }

    private static final String SECURITYCAPTCHA = "text from the image and click \"Continue\" to access the website";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.share4web.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found or removed|Page Not Found|File not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(SECURITYCAPTCHA)) {
            link.getLinkStatus().setStatusText("Can't check status, security captcha...");
            return AvailableStatus.UNCHECKABLE;
        }
        final Regex fileInfo = br.getRegex("<small>Download file:</small><br/>([^<>\"]*?)<small>\\(([^<>\"]*?)\\)</small>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Server sends filename with .exe ending, prevent it by setting final
        // filename here
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directurl");
        boolean isStoredDirecturl = true;
        if (dllink == null) {
            if (br.containsHTML("Somebody else is already downloading using your IP")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            /* 2018-12-12: No captcha anymore (?) */
            // if (br.containsHTML(SECURITYCAPTCHA)) {
            // final Form captchaForm = br.getForm(0);
            // if (captchaForm == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // final String code = getCaptchaCode("http://www." + this.getHost() + "/captcha/?rnd=", downloadLink);
            // captchaForm.put("captcha", code);
            // br.submitForm(captchaForm);
            // if (br.containsHTML(SECURITYCAPTCHA)) {
            // throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            // }
            // }
            br.setFollowRedirects(true);
            final String nextStep = br.getRegex("(/get/[^\"]+)\"").getMatch(0);
            if (nextStep == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(nextStep);
            dllink = br.getRegex("\"(http://[a-z0-9]+\\.share4web\\.com/getf/[^<>\"]*?)\"").getMatch(0);
            // final String waitStr = br.getRegex("var nn = (\\d+);").getMatch(0);
            // if (waitStr == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // this.sleep(Integer.parseInt(waitStr) * 1001l, downloadLink);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            isStoredDirecturl = false;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (isStoredDirecturl) {
                downloadLink.setProperty("directurl", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Bad downloadurl");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directurl", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}