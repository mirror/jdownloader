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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mountfile.net" }, urls = { "http://(www\\.)?mountfile\\.net/(?!d/)[A-Za-z0-9]+" }, flags = { 0 })
public class MountFileNet extends PluginForHost {

    public MountFileNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mountfile.net/terms/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fileInfo = br.getRegex("<h2 style=\"margin:0\">([^<>\"]*?)</h2>[\t\n\r ]+<div class=\"comment\">([^<>\"]*?)</div>");
        final String filename = fileInfo.getMatch(0);
        final String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // First, bring up saved final links
        final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        br.postPage(br.getURL(), "free=Slow+download&hash=" + fid);
        final String rcID = br.getRegex("Recaptcha\\.create\\(\\'([^<>\"]*?)\\'").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final long timeBefore = System.currentTimeMillis();
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        for (int i = 1; i <= 5; i++) {
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            // Waittime can be skipped
            // if (i == 1) waitTime(timeBefore, downloadLink);
            br.postPage(br.getURL(), "free=Get+download+link&hash=" + fid + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
            final String reconnectWait = br.getRegex("You should wait (\\d+) minutes before downloading next file").getMatch(0);
            if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 60 * 1001l);
            if (br.containsHTML("Recaptcha\\.create\\(\\'")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML("Recaptcha\\.create\\(\\'")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("\"(http://d\\d+\\.mountfile.net/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<div style=\"margin: 10px auto 20px\" class=\"center\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("not found")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void waitTime(long timeBefore, final DownloadLink downloadLink) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        int wait = 60;
        final String ttt = br.getRegex("var sec = (\\d+)").getMatch(0);
        if (ttt != null) wait = Integer.parseInt(ttt);
        wait -= passedTime;
        if (wait > 0) sleep(wait * 1000l, downloadLink);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}