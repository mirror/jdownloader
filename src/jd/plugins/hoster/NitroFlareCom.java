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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nitroflare.com" }, urls = { "https?://(www\\.)?nitroflare\\.com/view/[A-Z0-9]+" }, flags = { 0 })
public class NitroFlareCom extends PluginForHost {

    public NitroFlareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.nitroflare.com/tos";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");
        br.setCookie("http://nitroflare.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File doesn\\'t exist<|This file has been removed due")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("<b>File Name: </b><span title=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("dir=\"ltr\" style=\"text\\-align: left;\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            final Browser br2 = br.cloneBrowser();
            final String fid = new Regex(downloadLink.getDownloadURL(), "/view/([A-Z0-9]+)/").getMatch(0);
            br.postPage(br.getURL(), "goToFreePage=");

            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.findID();

            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("https://www.nitroflare.com/ajax/freeDownload.php", "method=startTimer&fileId=" + fid);
            if (br.containsHTML("This file is available with premium key only")) {
                logger.info("Only downloadable via premium");
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) {
                        throw (PluginException) e;
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
            } else if (br.containsHTML("﻿Downloading is not possible")) {
                final String waitminutes = br.getRegex("You have to wait (\\d+) minutes to download your next file").getMatch(0);
                if (waitminutes != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitminutes) * 60 * 1001l);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }

            br2.getPage("https://www.nitroflare.com/js/downloadFree.js?v=1.0.1");
            final String waittime = br2.getRegex("var time = (\\d+);").getMatch(0);
            int wait = 30;
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            this.sleep(wait * 1001l, downloadLink);

            for (int i = 1; i <= 5; i++) {
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                br.postPage("https://www.nitroflare.com/ajax/freeDownload.php", "method=fetchDownload&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                if (br.containsHTML("The captcha wasn\\'t entered correctly|You have to fill the captcha")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("The captcha wasn\\'t entered correctly|You have to fill the captcha")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (br.containsHTML("File doesn\\'t exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            dllink = br.getRegex("\"(https?://[a-z0-9\\-_]+\\.nitroflare\\.com/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}