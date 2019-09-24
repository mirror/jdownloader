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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PicflashOrg extends PluginForHost {
    public PicflashOrg(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://www.picflash.org/userpanel.php?chkp=1");
    }

    @Override
    public String getAGBLink() {
        return "https://www.picflash.org/agb.php";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:picture\\.php\\?key=[A-Z0-9]+|viewer\\.php\\?img=.+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;

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
        return new Regex(link.getPluginPatternMatcher(), "=(.+)$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        /* Set fid as name so that offline content also has 'nice' filenames! */
        /* 2019-09-23: This is a JD-friendly project :) */
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().contains("viewer.php")) {
            /*
             * Direct-URLs - we only have support for this via this ´plugin to work-around their Content-Type issue:
             * https://ngb.to/threads/9824-Bugs-Alles-was-an-Bugs-auff%C3%A4llt/page24?p=767647&highlight=pspzockerscene#post767647
             */
            link.setFinalFileName(this.getFID(link));
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(link.getPluginPatternMatcher());
                if (con.getResponseCode() != 200) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    final long filesize = con.getLongContentLength();
                    /*
                     * 2019-09-23: Offline content will lead to an 'error-picture' e.g.:
                     * https://www.picflash.org/viewer.php?img=error_test.webm In comparison to images which are online, there will be no
                     * content-disposition which is why we can easily filter-out offline content.
                     */
                    if (filesize <= 1 && !con.isContentDisposition()) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    link.setVerifiedFileSize(filesize);
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
            // link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            link.setName(this.getFID(link));
            br.getPage(link.getPluginPatternMatcher() + "&action=show");
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"content-box error top-space\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = br.getRegex("<li><b>Name:</b>([^<>\"]+)</li>").getMatch(0);
            if (StringUtils.isEmpty(filename)) {
                /* Fallback */
                filename = this.getFID(link);
            }
            String filesize = br.getRegex("([0-9,]+)\\s*Bytes</li>").getMatch(0);
            if (StringUtils.isEmpty(filename)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
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
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink;
        if (link.getPluginPatternMatcher().contains("viewer.php")) {
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
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        final long minimumFilesize = link.getView().getBytesTotal() - 100;
        /*
         * 2019-09-23: For .webm videos they sometimes return 'text/html; charset=UTF-8' so we cannot only rely on the content-type here!
         * This issue has been reported already: https://ngb.to/threads/9824-Bugs-Alles-was-an-Bugs-auff%C3%A4llt/page30
         */
        if (dl.getConnection().getContentType().contains("html") && dl.getConnection().getContentLength() < minimumFilesize) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* 2019-09-23: No captchas at all */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}