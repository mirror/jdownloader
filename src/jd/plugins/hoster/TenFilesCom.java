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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tenfiles.com" }, urls = { "https?://(?:www\\.)?tenfiles\\.(?:com|info)/file/([a-z0-9]+)" })
public class TenFilesCom extends PluginForHost {
    public TenFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // Gehört zu gigapeta.com
    @Override
    public String getAGBLink() {
        return "http://tenfiles.com/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("tenfiles.info/", "tenfiles.com/"));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "lang", "us");
        br.getPage(link.getPluginPatternMatcher());
        final String filename = br.getRegex("alt=\"file\" />\\-\\->([^<>\"]+)</td>").getMatch(0);
        final String filesize = br.getRegex("Size\\s*</th>\\s*<td>([^<>\"]+)</td>").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        /*
         * This filehost sometimes still displays filename and filesize for offline files --> Grab those first and then check for offline
         * status.
         */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">404<|Attention! This file was removed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("This file has been deleted from our server by user who uploaded it on the server")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink link) throws Exception {
        br.setFollowRedirects(false);
        final String captchaKey = (int) (Math.random() * 100000000) + "";
        final String captchaUrl = "http://tenfiles.com/img/captcha.gif?x=" + captchaKey;
        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode("gigapeta.com", captchaUrl, link);
            br.postPage(br.getURL(), "download=&captcha_key=" + captchaKey + "&captcha=" + captchaCode);
            if (br.getRedirectLocation() != null) {
                break;
            }
        }
        if (br.getRedirectLocation() == null) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML("All threads for IP")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your IP is already downloading a file");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
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
        return true;
    }
}