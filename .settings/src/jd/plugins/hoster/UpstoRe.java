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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 16750 $", interfaceVersion = 2, names = { "upsto.re" }, urls = { "http://(www\\.)?upsto\\.re/(?!faq|privacy|terms)[A-Za-z0-9]+" }, flags = { 0 })
public class UpstoRe extends PluginForHost {

    public UpstoRe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://upsto.re/terms/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://upsto.re/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fileInfo = br.getRegex("<h2 style=\"margin:0\">([^<>\"]*?)</h2>[\t\n\r ]+<div class=\"comment\">([^<>\"]*?)</div>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download file ([^<>\"]*?) \\&mdash; Upload, store \\& share your files on Upsto\\.re</title>").getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "freelink");
        if (dllink == null) {
            final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            br.postPage(downloadLink.getDownloadURL(), "free=Slow+download&hash=" + fid);
            // Waittime can be skipped
            // final long timeBefore = System.currentTimeMillis();
            final String rcID = br.getRegex("Recaptcha\\.create\\(\\'([^<>\"]*?)\\'").getMatch(0);
            if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            // int wait = 60;
            // String waittime = br.getRegex("var sec = (\\d+)").getMatch(0);
            // if (waittime != null) wait = Integer.parseInt(waittime);
            // int passedTime = (int) ((System.currentTimeMillis() - timeBefore)
            // / 1000) - 1;
            // wait -= passedTime;
            // if (wait > 0) sleep(wait * 1000l, downloadLink);
            br.postPage(downloadLink.getDownloadURL(), "free=Get+download+link&hash=" + fid + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
            dllink = br.getRegex("<div style=\"margin: 10px auto 20px\" class=\"center\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://d\\d+\\.upsto\\.re/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                final String reconnectWait = br.getRegex("You should wait (\\d+) minutes before downloading next file").getMatch(0);
                if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 60 * 1001l);
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            sleep(3000l, downloadLink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("not found")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}