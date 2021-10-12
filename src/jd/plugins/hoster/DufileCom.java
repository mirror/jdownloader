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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dufile.com" }, urls = { "https?://(?:www\\.)?dufile\\.com/(?:file|down)/([a-z0-9]+)\\.html" })
public class DufileCom extends PluginForHost {
    public DufileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://" + getHost() + "/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 1;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML(">404 Not Found|对不起，没有找到您要访问的页面|Sorry,The requested URL was not found on this server|请确认您输入的网址是正确的")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"title\">([^<>]+)<").getMatch(0);
        String filesize = br.getRegex("文件大小：<b>(\\d+)</b>").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            this.br.getPage("/down/" + this.getFID(link) + ".html");
            String js_source = this.br.getRegex("down_file\\(([^<>\"]+)\\)").getMatch(0).replace("'", "");
            final String[] js_params = js_source.split(",");
            if (js_params.length < 2) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String code = this.getCaptchaCode("/down_code.php", link);
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.br.postPage("/down_code.php", "action=yz&id=" + this.getFID(link) + "&code=" + Encoding.urlEncode(code));
            if (!this.br.toString().equals("1")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            final String p = js_params[1];
            this.br.getPage("/dd.php?file_key=" + this.getFID(link) + "&p=" + p);
            dllink = br.getRegex("(http[^<>\"]+/down/[^<>\"]+)").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
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
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
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
        return SiteTemplate.PhpDisk;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}