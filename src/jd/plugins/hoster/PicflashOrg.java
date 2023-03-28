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
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PicflashOrg extends PluginForHost {
    public PicflashOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.picflash.org/agb.php";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "picflash.org" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:picture\\.php\\?key=[A-Z0-9]+|viewer\\.php\\?img=[^/&]+)");
        }
        return ret.toArray(new String[0]);
    }

    private final String        TYPE_1            = "https?://[^/]+/viewer\\.php\\?img=([^/]+)";
    private final String        TYPE_2            = "https?://[^/]+/picture\\.php\\?key=([A-Z0-9]+)";
    private static final String PROPERTY_FILENAME = "picflash_filename";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = this.getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + this.getFID(link);
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        }
        if (link.getPluginPatternMatcher().matches(TYPE_1)) {
            if (link.hasProperty(PROPERTY_FILENAME)) {
                return link.getStringProperty(PROPERTY_FILENAME);
            } else {
                return new Regex(link.getPluginPatternMatcher(), TYPE_1).getMatch(0);
            }
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_2).getMatch(0);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        /* 2019-09-23: picflash.org is a JD-friendly project :) */
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(TYPE_1)) {
            link.setFinalFileName(this.getFID(link));
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(link.getPluginPatternMatcher());
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* 'normal' URLs */
            /*
             * 2019-09-23: Most of the content is pictures. Sometimes they may also host small Videofiles which is why we rather not set a
             * MimeHint!
             */
            if (!link.isNameSet()) {
                link.setName(this.getFID(link));
            }
            br.getPage(link.getPluginPatternMatcher() + "&action=show");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("class=\"content-box error top-space\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = br.getRegex("(?i)<li>\\s*<b>Name\\s*:\\s*</b>([^<>\"]+)</li>").getMatch(0);
            if (!StringUtils.isEmpty(filename)) {
                /* 2019-10-15: Do this so that duplicate recognization works throughout directurls and these URLs. */
                filename = Encoding.htmlDecode(filename).trim();
                link.setProperty(PROPERTY_FILENAME, filename);
            }
            String filesize = br.getRegex("(?i)([0-9,]+)\\s*Bytes</li>").getMatch(0);
            if (StringUtils.isEmpty(filename)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (filesize != null) {
                filesize = filesize.replace(",", "");
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink;
        if (link.getPluginPatternMatcher().matches(TYPE_1)) {
            /* Directurl */
            dllink = link.getPluginPatternMatcher();
        } else {
            /* 'normal' URL */
            /* For videos */
            dllink = br.getRegex("<source[^<>]*src=\"(http[^<>\"]+)\"[^<>]*type=\\'video[^<>]*>").getMatch(0);
            if (dllink == null) {
                /* For pictures */
                dllink = br.getRegex("<a href=\"(http[^<>\"]+)\"[^<>]*class=\"full-image top-space\">").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}