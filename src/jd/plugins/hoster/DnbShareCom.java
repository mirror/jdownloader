//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.net.URL;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

/**
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dnbshare.com" }, urls = { "^https?://[\\w\\.]*?dnbshare\\.com/download/[^<>\"/]*?(?:\\.mp3|\\.html)$" })
public class DnbShareCom extends PluginForHost {
    @SuppressWarnings("deprecation")
    public DnbShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/faq#tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (!link.isNameSet()) {
            /* Set weak-filename */
            String nameFromURL = Plugin.getFileNameFromURL(new URL(link.getPluginPatternMatcher()));
            if (nameFromURL != null) {
                nameFromURL = nameFromURL.replaceFirst("\\.html$", "");
                link.setName(nameFromURL);
            }
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("not found\\.|was deleted due to low activity|was deleted due to reported infringement")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("name=\"file\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<em>Filename</em>:([^<>\"]*?)<").getMatch(0);
        }
        if (filename == null) {
            filename = new Regex(link.getDownloadURL(), "/([^/]+)\\.html").getMatch(0);
        }
        final String filesize = br.getRegex("<em>Filesize</em>: (.*?)</li>").getMatch(0);
        if (filename != null) {
            link.setName(filename.trim());
        } else {
            logger.warning("Failed to find filename");
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
        } else {
            logger.warning("Failed to find filesize");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        String dllink = null;
        // look for download page?
        final Form download = br.getFormbyProperty("id", "dlform");
        if (download != null) {
            // cleanup required
            for (final InputField i : download.getInputFields()) {
                i.setKey(i.getKey().replaceFirst("^dlform-", ""));
            }
            int waitSeconds = 10;
            final String waittimeSecondsStr = br.getRegex("var c = (\\d+);").getMatch(0);
            if (waittimeSecondsStr != null) {
                waitSeconds = Integer.parseInt(waittimeSecondsStr);
            }
            sleep(waitSeconds * 1001l, link);
            br.setFollowRedirects(false);
            br.submitForm(download);
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) {
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            for (final String url : urls) {
                if (StringUtils.containsIgnoreCase(url, "play=")) {
                    dllink = url;
                    break;
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = dllink.replace("play=1", "play=0");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 503 Connection limit reached", 2 * 60 * 1000l);
            } else {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}