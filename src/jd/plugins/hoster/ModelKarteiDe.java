//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.CtDiskComFolder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ModelKarteiDe extends PluginForHost {
    public ModelKarteiDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        for (final String[] domains : CtDiskComFolder.getPluginDomains()) {
            for (final String domain : domains) {
                br.setCookie(domain, "mk4_language", "2"); // English
            }
        }
        return br;
    }

    private String dllink = null;

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        /* 2024-06-04: Set to 1 as we are only downloading small files. */
        return 1;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "model-kartei.de" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:fotos/foto|videos/video)/(\\d+)/");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/agb/";
    }

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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        String extDefault;
        if (StringUtils.containsIgnoreCase(link.getPluginPatternMatcher(), "video")) {
            extDefault = ".mp4";
        } else {
            extDefault = ".jpg";
        }
        final String contentID = this.getFID(link);
        if (!link.isNameSet()) {
            /* Set weak filename */
            link.setName(contentID + extDefault);
        }
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("class=\"p-title\">\\s*<a href=\"[^\"]+\"[^<]*title=\"([^\"]+)\"").getMatch(0); // photo
        if (title == null) {
            // video
            title = br.getRegex("class=\"col c-3-2 vidHeader\"[^>]*>\\s*<h1>([^<]+)</h1>").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlOnlyDecode(title).trim();
        } else {
            logger.warning("Failed to find title");
        }
        final String dateStr = br.getRegex("(\\d{8})/\">\\s*<span[^>]*class=\"date\"").getMatch(0);
        String dateFormatted = null;
        if (dateStr != null) {
            dateFormatted = dateStr.substring(0, 4) + "_" + dateStr.substring(4, 6) + "_" + dateStr.substring(6, 8);
        } else {
            logger.warning("Failed to find date");
        }
        dllink = br.getRegex("id=\"gofullscreen\"[^<]*src=\"([^\"]+)").getMatch(0); // photo
        if (dllink == null) {
            dllink = br.getRegex("type=\"video/mp4\"[^>]*src=\"(https?://[^\"]+)").getMatch(0); // video MP4
            if (dllink == null) {
                dllink = br.getRegex("type=\"video/webm\"[^>]*src=\"(https?://[^\"]+)").getMatch(0); // video WEBM
                extDefault = ".webm";
            }
        }
        if (dateFormatted != null && title != null) {
            link.setFinalFileName(dateFormatted + "_" + contentID + "_" + title + extDefault);
        } else if (title != null) {
            link.setFinalFileName(contentID + "_" + title + extDefault);
        } else {
            link.setFinalFileName(contentID + extDefault);
        }
        if (isDownload) {
            /* Download */
            if (br.containsHTML("assets/images/no\\.jpg")) {
                /* Account needed to view this image */
                throw new AccountRequiredException("Account needed to download this image");
            } else if (br.containsHTML(">\\s*Deine Nutzerrechte reichen leider nicht aus um das Video zu sehen")) {
                /* Account needed to view this video */
                throw new AccountRequiredException("Account needed to download this video");
            }
        } else {
            /* Linkcheck - find filesize if possible */
            if (!StringUtils.isEmpty(dllink)) {
                /* Find filesize */
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    handleConnectionErrors(br, con);
                    if (con.getCompleteContentLength() > 0) {
                        if (con.isContentDecoded()) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        } else {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}