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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ModelKarteiDe extends PluginForHost {
    public ModelKarteiDe(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://www." + getHost() + "/vip/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        for (final String[] domains : getPluginDomains()) {
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
        if (isVideo(link)) {
            return -5;
        } else {
            /* 2024-06-04: Set to 1 as we are only downloading small files. */
            return 1;
        }
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

    private boolean isVideo(final DownloadLink link) {
        if (StringUtils.containsIgnoreCase(link.getPluginPatternMatcher(), "/video/")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        final String extDefault;
        if (isVideo(link)) {
            /* Can be flv, webm, mp4 but mostly mp4 */
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
        /* Ensure English language */
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, link.getPluginPatternMatcher());
        /**
         * Setting only the English language cookie wasn't enough so this should redirect us to our target-URL and enforce English language.
         */
        br.getPage("https://www." + getHost() + "/l.php?l=en");
        /* Double-check: If we're not on our target-URL, navigate to it. */
        if (!br.getURL().contains(contentID)) {
            logger.warning("Expected redirect from language switcher to final URL did not happen");
            br.getPage(link.getPluginPatternMatcher());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*The video does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*The video is not active")) {
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
                if (dllink == null) {
                    /* Super old flash player .flv videos */
                    dllink = br.getRegex("\"(https?://[^/]+/[^\"]+\\.flv)\"").getMatch(0); // video FLV
                }
            }
        }
        String ext = extDefault;
        if (dllink != null) {
            ext = getFileNameExtensionFromURL(dllink, extDefault);
        }
        try {
            if (!StringUtils.isEmpty(dllink) && (isDownload == false || dateFormatted == null)) {
                /* Find filesize */
                final URLConnectionAdapter con = basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, null, null);
                if (dateFormatted == null) {
                    final Date lastModifiedDate = TimeFormatter.parseDateString(con.getHeaderField(HTTPConstants.HEADER_RESPONSE_LAST_MODFIED));
                    if (lastModifiedDate != null) {
                        dateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(lastModifiedDate);
                    }
                }
            }
        } finally {
            /* Always set filename, even if exception has happened */
            if (dateFormatted != null && title != null) {
                link.setFinalFileName(dateFormatted + "_" + contentID + "_" + title + ext);
            } else if (title != null) {
                link.setFinalFileName(contentID + "_" + title + ext);
            } else if (dateFormatted != null) {
                link.setFinalFileName(dateFormatted + "_" + contentID + ext);
            } else {
                link.setFinalFileName(contentID + ext);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (br.containsHTML("assets/images/no\\.jpg")) {
            /* Account needed to view this image */
            throw new AccountRequiredException("Account needed to download this image");
        } else if (br.containsHTML(">\\s*(Your are not authorized to see the video|Deine Nutzerrechte reichen leider nicht aus um das Video zu sehen)")) {
            /* Account needed to view this video */
            throw new AccountRequiredException("Account needed to download this video");
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
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