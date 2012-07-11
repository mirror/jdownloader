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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nzbload.com" }, urls = { "http://(www\\.)?nzbload\\.com/en/download/[a-z0-9]+/\\d+" }, flags = { 0 })
public class NzbLoadCom extends PluginForHost {

    public NzbLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.nzbload.com/en/legal/terms-of-service";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "text/plain, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final Regex params = new Regex(link.getDownloadURL(), "http://(www\\.)?nzbload\\.com/en/download/([a-z0-9]+)/(\\d+)");
        br.getPage("http://www.nzbload.com/data/download.json?t=" + System.currentTimeMillis() + "&sub=" + params.getMatch(1) + "&params[0]=" + params.getMatch(2));
        if (br.containsHTML("\"filename\":null,\"filesize\":null")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = get("filename");
        String filesize = get("filesize");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final Regex params = new Regex(downloadLink.getDownloadURL(), "http://(www\\.)?nzbload\\.com/en/download/([a-z0-9]+)/(\\d+)");
        final Browser br2 = br.cloneBrowser();
        br2.getPage("http://www.nzbload.com/tpl/download/" + params.getMatch(1) + ".js?version=1.050");
        final String sleep = br2.getRegex("updateTimer = setTimeout\\(\\'checkProgress\\(\\);\\', (\\d+)\\);").getMatch(0);
        final String rcID = br2.getRegex("Recaptcha\\.create\\(\\'([^<>\"]*?)\\'").getMatch(0);
        if (rcID == null || sleep == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.nzbload.com/data/download.json?overwrite=start-download&t=" + System.currentTimeMillis() + "&sub=" + params.getMatch(1) + "&params[0]=" + params.getMatch(2));
        final String expiry = get("expiry");
        final String hash = get("hash");
        if (expiry == null || hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(Long.parseLong(sleep) + 500, downloadLink);
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            br.postPage("http://www.nzbload.com/action/download.json?act=verify_captcha&t=" + System.currentTimeMillis(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
            if (br.containsHTML("\"success\":false")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML("\"success\":false")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getPage("http://www.nzbload.com/data/download.json?overwrite=get_url&t=" + System.currentTimeMillis() + "&sub=" + params.getMatch(1) + "&params[0]=" + params.getMatch(2) + "&params[1]=" + hash + "&params[2]=" + expiry + "&params[3]=" + downloadLink.getName());
        if (br.containsHTML("Free users can download 1 file at the same time")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
        String dllink = get("url");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String get(final String parameter) {
        String output = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        if (output == null) output = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        return output;
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

}