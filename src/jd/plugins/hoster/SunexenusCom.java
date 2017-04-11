//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sunexenus.com" }, urls = { "https?://sunexenus\\.com/[a-zA-Z0-9]+/.*" })
public class SunexenusCom extends antiDDoSForHost {

    private final String         baseURL = "https://sunexenus.com";

    /* don't touch the following! */
    private static AtomicInteger maxFree = new AtomicInteger(1);

    public SunexenusCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(null);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br.setFollowRedirects(false);
        getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("No such file with this filename") || br.containsHTML("<h2>File Not Found</h2>") || br.containsHTML("The file was removed by administrator")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Regex r = br.getRegex("<span id=\"download_file_name\">([^<>\"]+)</span> \\(([^)]+)\\)");
        String filename = r.getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String filesize = r.getMatch(1);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final Form download = br.getFormBySubmitvalue("Free+Download");
        submitForm(download);
        Regex r = br.getRegex("You have to wait (\\d+) minutes till");
        if (r.getMatch(0) != null) {
            long wait = Long.parseLong(r.getMatch(0)) * 60;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (wait + 5) * 1000l);
        }
        r = br.getRegex("You have to wait (\\d+) minutes, (\\d+) seconds till");
        if (r.getMatch(0) != null) {
            long wait = Long.parseLong(r.getMatch(0)) * 60 + Long.parseLong(r.getMatch(1));
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (wait + 5) * 1000l);
        }
        final String waittime = br.getRegex("<span class=\"seconds\">(\\d+)<").getMatch(0);
        final long t = System.currentTimeMillis();
        final Form start = br.getFormbyProperty("name", "F1");
        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
        start.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        long wait = 60;
        if (waittime != null) {
            // remove one second from past, to prevent returning too quickly.
            final long passedTime = ((System.currentTimeMillis() - t) / 1000) - 1;
            wait = Long.parseLong(waittime) - passedTime;
        }
        if (wait > 0) {
            sleep(wait * 1000l, downloadLink);
        }
        submitForm(start);
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 60 * 60 * 1000l);
        }
        try {
            /* add a download slot */
            maxFree.addAndGet(+1);
            /* start the dl */
            dl.startDownload();
        } finally {
            /* remove download slot */
            maxFree.addAndGet(-1);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public boolean hasCaptcha(final DownloadLink downloadLink, final jd.plugins.Account acc) {
        return true;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public String getAGBLink() {
        return baseURL + "/tos.html";
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
