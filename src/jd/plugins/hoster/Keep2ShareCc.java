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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keep2share.cc" }, urls = { "http://(www\\.)?keep2share\\.cc/file/[a-z0-9]+" }, flags = { 0 })
public class Keep2ShareCc extends PluginForHost {

    public Keep2ShareCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://keep2share.cc/page/terms.html";
    }

    private static final String DOWNLOADPOSSIBLE = ">Downloading file:<";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Sorry, an error occurred while processing your request")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename, filesize = null;
        if (br.containsHTML(DOWNLOADPOSSIBLE)) {
            filename = br.getRegex(">Downloading file:</span><br>[\t\n\r ]+<span class=\"c2\">.*?alt=\"\" style=\"\">([^<>\"]*?)</span>").getMatch(0);
            filesize = br.getRegex("File size ([^<>\"]*?)</div>").getMatch(0);
        } else {
            filename = br.getRegex(">File: <span>([^<>\"]*?)</span>").getMatch(0);
            filesize = br.getRegex(">Size: ([^<>\"]*?)</div>").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (br.containsHTML("File size to large\\!<") || br.containsHTML("Only <b>Premium</b> access<br>")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to premium members");
        }
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (br.containsHTML(DOWNLOADPOSSIBLE)) {
                dllink = getDllink();
            } else {
                if (br.containsHTML("Traffic limit exceed\\!<br>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
                final String uniqueID = br.getRegex("<input type=\"hidden\" value=\"([a-z0-9]+)\" name=\"uniqueId\"").getMatch(0);
                if (uniqueID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                try {
                    /** Try to skip the captcha */
                    final Browser br2 = br.cloneBrowser();
                    br2.getPage("http://keep2share.cc/file/url.html?file=" + uniqueID);
                    dllink = br2.getRedirectLocation();
                } catch (final Exception e) {

                }
                if (dllink == null) {
                    final String captchaLink = br.getRegex("\"(/site/captcha\\.html\\?v=[a-z0-9]+)\"").getMatch(0);
                    if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    final String code = getCaptchaCode("http://keep2share.cc" + captchaLink, downloadLink);
                    br.postPage(br.getURL(), "CaptchaForm%5Bcode%5D=" + code + "&free=1&freeDownloadRequest=1&uniqueId=" + uniqueID);
                    if (br.containsHTML(">The verification code is incorrect|/site/captcha.html")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

                    /** Skippable */
                    // int wait = 30;
                    // final String waittime =
                    // br.getRegex("<div id=\"download\\-wait\\-timer\">[\t\n\r ]+(\\d+)[\t\n\r ]+</div>").getMatch(0);
                    // if (waittime != null) wait = Integer.parseInt(waittime);
                    // sleep(wait * 1001l, downloadLink);
                    br.postPage(br.getURL(), "free=1&uniqueId=" + uniqueID);
                    dllink = getDllink();
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String getDllink() throws PluginException {
        String dllink = br.getRegex("\\'(/file/url\\.html\\?file=[a-z0-9]+)\\';").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://keep2share.cc" + dllink;
        return dllink;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
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
        return 2;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}