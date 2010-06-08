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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "slingfile.com" }, urls = { "http://[\\w\\.]*?slingfile\\.com/((file|audio|video)/.+|dl/[a-z0-9]+/.*?\\.html)" }, flags = { 0 })
public class SlingFileCom extends PluginForHost {

    public SlingFileCom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    public String getAGBLink() {
        return "http://www.slingfile.com/pages/tos.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL();
        theLink = theLink.replaceAll("(audio|video)", "file");
        link.setUrlDownload(theLink);
    }

    private static String ERRORREGEX = "class=\"errorbox\"><p><strong>(.*?</a>.</strong>";

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        // Prevents having to reconnect too often as their limit-check is a bit
        // buggy :D
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        // Prevents errors, i don't know why the page sometimes shows this
        // error!
        if (br.containsHTML(">Please enable cookies to use this website")) br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals("http://www.slingfile.com/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (!br.getRedirectLocation().contains("/dl/")) throw new PluginException(LinkStatus.ERROR_FATAL, "Link redirects to a wrong link!");
            downloadLink.setUrlDownload(br.getRedirectLocation());
            logger.info("New link was set in the availableCheck, opening page...");
            br.getPage(downloadLink.getDownloadURL());
        }
        String filename = br.getRegex("<title>(.*?) - SlingFile - Free File Hosting \\& Online Storage</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"title\">(.*?)</span>").getMatch(0);
        }
        String filesize = br.getRegex("class=\"maintitle\">Downloading</span><span class=\"title\">.*?</span></div>[\n\r\t ]+<p>(.*?)\\. File uploaded ").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("class=\"errorbox\"")) {
            String waitthat = br.getRegex("Please wait for another (\\d+) minutes to download another file").getMatch(0);
            if (waitthat != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waitthat) * 60 * 1001l);
            if (br.containsHTML("Please wait until the download is complete")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
            String errorMessage = "";
            if (br.getRegex(ERRORREGEX).getMatch(0) != null) errorMessage = br.getRegex("ERRORREGEX").getMatch(0);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, errorMessage);
        }
        // At the moment we can skip their (30 seconds) waittime so if the
        // plugin is broken this is the first thing to check!
        br.postPage(downloadLink.getDownloadURL(), "show_captcha=yes");
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.parse();
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        rc.setCode(c);
        if (br.containsHTML("api.recaptcha.net")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("'(http://sf\\d+\\.slingfile\\.com/gdl/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/.*?)'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("location\\.href='(http://.*?)'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
