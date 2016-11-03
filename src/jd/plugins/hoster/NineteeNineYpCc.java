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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "99yp.cc" }, urls = { "https?://(?:www\\.)?99yp\\.cc/(?:file|down)\\-\\d+\\.html" })
public class NineteeNineYpCc extends PluginForHost {

    public NineteeNineYpCc(PluginWrapper wrapper) {
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

    private String               fuid              = null;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        fuid = new Regex(link.getDownloadURL(), "(\\d+)\\.html$").getMatch(0);
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String sha1 = this.br.getRegex("SHA1:([^<>]+)<").getMatch(0);
        final String md5 = this.br.getRegex("MD5:([^<>]+)<").getMatch(0);
        String filename = null;
        String filesize = null;

        filename = br.getRegex("\\.AddFavorite\\(\\'[^\\']*?\\',\\'([^<>\"\\']+)\\'\\)").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = fuid;
        }
        filesize = br.getRegex(">文件大小: ([^<>\"]+)<").getMatch(0);
        link.setName(Encoding.htmlDecode(filename).trim());
        if (filesize != null) {
            if (filesize.endsWith("M")) {
                filesize += "b";
            }
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (sha1 != null) {
            link.setSha1Hash(sha1);
        }
        if (md5 != null) {
            link.setSha1Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            this.br.getPage("/down2-" + this.fuid + ".html");
            this.br.getPage("/down-" + this.fuid + ".html");
            String js_source = this.br.getRegex("down_file\\(([^<>\"]+)\\)").getMatch(0).replace("'", "");
            final String[] js_params = js_source.split(",");
            if (js_params.length < 6) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String code = this.getCaptchaCode("/imagecode.php", downloadLink);
            // this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.br.postPage(this.br.getURL(), "action=check_code&code=" + Encoding.urlEncode(code));
            if (this.br.getURL().contains("/file")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            dllink = br.getRegex("(http[^<>\"]+/downfile/[^<>\"]+)").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
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
        return SiteTemplate.Unknown_ChineseFileHosting;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}