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
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "luckfile.com" }, urls = { "https?://(?:www\\.)?luckfile\\.com/(?:file|down)\\-([A-Za-z0-9]+)\\.html" })
public class LuckfileCom extends PluginForHost {
    public LuckfileCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://www.luckfile.com/upgrade.html");
    }

    @Override
    public String getAGBLink() {
        return "https://www.luckfile.com/terms.html";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = -2;
    private static final int     FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_FREE_RESUME = false;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 1;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 1;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = false;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 1;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 1;
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("文件不存在或已删除") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h2><i[^>]+></i>文件名\\：([^<]+)</h2>").getMatch(0);
        String filesize = br.getRegex("<span>文件大小：<b>([^<>\"]+)<").getMatch(0);
        if (filename != null) {
            /* Set final filename here because server filenames are bad. */
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        } else {
            link.setName(this.getLinkID(link));
        }
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
        final String fid = this.getLinkID(link);
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            String down2_url = null;
            if (br.containsHTML("/down2-" + fid)) {
                br.getPage("/down2-" + fid + ".html");
                down2_url = this.br.getURL();
            }
            final Browser ajax = this.br.cloneBrowser();
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String continue_url = br.getRegex("id=\"downpage_link\" href=\"(down\\-\\d+\\.html)").getMatch(0);
            if (continue_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* 2019-07-09: 30 seconds pre-download-waittime is skippable */
            br.getPage(continue_url);
            if (br.containsHTML("imagecode\\.php")) {
                /* 2019-06-27: TODO: Improve this captcha-check! */
                final String code = getCaptchaCode("/imagecode.php?t=" + System.currentTimeMillis(), link);
                ajax.postPage("/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
                if (ajax.toString().equals("false")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (down2_url != null) {
                    this.br.getHeaders().put("Referer", down2_url);
                }
                /* If we don't wait for some seconds here, the continue_url will redirect us to the main url!! */
                this.sleep(5 * 1001l, link);
                ajax.postPage("/ajax.php", "action=load_down_addr1&file_id=" + Encoding.urlEncode(fid));
            }
            // final String dlarg = br.getRegex("url : \\'ajax\\.php\\',\\s*?data\\s*?:\\s*?\\'action=(pc_\\d+)").getMatch(0);
            // if (dlarg != null) {
            // ajax.postPage("/ajax.php", "action=" + dlarg + "&file_id=" + fid + "&ms=" + System.currentTimeMillis() + "&sc=640*480");
            // }
            /* After the fmdown.php */
            if (br.containsHTML(">该文件暂无普通下载点，请使用SVIP")) {
                throw new AccountRequiredException();
            } else if (this.br.containsHTML(">非VIP用户每次下载间隔为")) {
                /* Usually 10 minute wait --> Let's reconnect! */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            dllink = ajax.getRegex("true\\|<a href=\"(http[^<>\"]+)").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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