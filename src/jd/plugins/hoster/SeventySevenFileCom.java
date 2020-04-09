//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "77file.com" }, urls = { "https?://(?:www\\.)?77file\\.com/(?:file|down)/([^/]+)\\.html" })
public class SeventySevenFileCom extends PluginForHost {
    public SeventySevenFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.77file.com/terms.php";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 1;

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("/down/", "/file/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        /* Empty / Missing filesize --> File offline */
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<span id=\"file_size\"></span>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("align='absbottom' border='0' />([^<>\"]+)<").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = getFID(link);
        }
        String filesize = br.getRegex("<span id=\"file_size\">([^<>\"]+)</span>").getMatch(0);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            filesize += "b";
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        // final String fid = getFID(link);
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final String fileID2 = this.br.getRegex("file_id=(\\d+)").getMatch(0);
            if (fileID2 == null) {
                logger.warning("Failed to find fileID2");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser brAjax = this.br.cloneBrowser();
            brAjax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brAjax.getPage("/ajax_new.php??a=1&ctime=" + System.currentTimeMillis());
            /* 2020-04-09: Waittime is skippable */
            final String longWaittimeStr = PluginJSonUtils.getJson(brAjax, "wtime");
            if (longWaittimeStr != null && longWaittimeStr.matches("\\d+")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(longWaittimeStr) * 1001l);
            }
            /* 2020-04-09: Waittime is skippable */
            // final String waittimeStr = PluginJSonUtils.getJson(brAjax, "waittime");
            // if (waittimeStr == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // final int wait = Integer.parseInt(waittimeStr);
            // /* Too high waittime --> Reconnect required */
            // if (wait > 75) {
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
            // }
            // this.sleep(wait * 1001l, link);
            /* 2020-04-09: '/down' page is skippable */
            // br.getPage(br.getURL().replace("/file/", "/down/"));
            /* 2020-04-09: Captcha is skippable */
            // final String code = getCaptchaCode("/imagecode.php", link);
            // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // br.postPage("/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
            // if (br.toString().equals("false")) {
            // throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            // }
            br.postPage("/ajax.php", "action=load_down_addr1&file_id=" + fileID2);
            dllink = br.getRegex("true\\|<a href=\"([^<>\"]+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://down\\.[^<>\"]+)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
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

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_ChineseFileHosting;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}