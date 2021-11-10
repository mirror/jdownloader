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
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "567yunpan.com" }, urls = { "https?://(?:www\\.)?567yunpan\\.com/file-(\\d+)\\.html" })
public class FiveSixSevenYunpanCom extends PluginForHost {
    public FiveSixSevenYunpanCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.567yunpan.com/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 1;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*文件不存在或已删除")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("div class=\"span9\">\\s*<h1>([^<>\"]+)</h1>").getMatch(0);
        String filesize = br.getRegex("(\\d+(?:\\.\\d{1,2})? (?:M|G))").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            filesize = Encoding.htmlDecode(filesize);
            filesize += "b";
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
            this.requestFileInformation(link);
            final String fid = this.getFID(link);
            if (fid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Skip pre-download-waittime and captcha */
            br.getPage("/down2-" + fid + ".html");
            br.getPage("/down-" + fid + ".html");
            String down2Params = br.getRegex("\\'(action=load_down_addr\\d+[^\\']*file_id=)'\\+file_id,").getMatch(0);
            if (down2Params == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            down2Params += fid;
            final Browser brc = this.br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brc.postPage("/ajax.php", down2Params);
            dllink = brc.getRegex("href=\"(https?://[^/]+/dl\\.php[^\"]+)").getMatch(0);
            if (dllink == null) {
                if (brc.containsHTML("vip\\.php")) {
                    /* Only 'downloadurls' leading to 'buy premium' page. */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Premiumonly or download limit reached");
                } else if (brc.containsHTML("true\\|")) {
                    /* No downloadurls available at all -> File should be downloadable as premium user */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Premiumonly");
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.containsHTML("err1")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download impossible for unknown reasons");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        /*
         * 2021-04-08: No content-disposition header given and file-typ0e is missing in filename most of all times but Content-Type is
         * given.
         */
        final String ext = getExtensionFromMimeType(dl.getConnection().getContentType());
        if (ext != null && !link.getName().toLowerCase(Locale.ENGLISH).endsWith("." + ext)) {
            link.setFinalFileName(link.getName() + "." + ext);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
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