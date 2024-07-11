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
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.VimmNetCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { VimmNetCrawler.class })
public class VimmNet extends PluginForHost {
    public VimmNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/?p=privacy";
    }

    private static List<String[]> getPluginDomains() {
        return VimmNetCrawler.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/vault/(\\d+).*");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        final String mediaID = link.getStringProperty(PROPERTY_MEDIA_ID);
        final String prefix_vimm = "vimm_net";
        if (fid != null) {
            return prefix_vimm + "://" + fid + "/media_id/" + mediaID + "/format_id/" + link.getStringProperty(PROPERTY_FORMAT_ID);
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        return getLinkID(link);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    public static final String  PROPERTY_MEDIA_ID           = "media_id";
    public static final String  PROPERTY_FORMAT_ID          = "format_id";
    public static final String  PROPERTY_FORMAT             = "format";
    public static final String  PROPERTY_PRE_GIVEN_FILENAME = "pre_given_filename";
    private static final String EXT_DEFAULT                 = ".7z";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getLinkID(link) + EXT_DEFAULT);
        }
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    public static void setFilename(final Plugin plg, final DownloadLink link, final boolean setAsFinalFileName) {
        String preGivenFilename = link.getStringProperty(PROPERTY_PRE_GIVEN_FILENAME);
        if (StringUtils.isEmpty(preGivenFilename)) {
            /* This should never happen but can happen for older items which do not have this property. */
            return;
        }
        final String formatFileExtension = link.getStringProperty(PROPERTY_FORMAT);
        String filename;
        if (formatFileExtension != null && preGivenFilename.contains(".")) {
            /**
             * Item is available in multiple different formats. </br>
             */
            final String originalFileExtension = preGivenFilename.substring(preGivenFilename.lastIndexOf("."));
            final String newFileEnding;
            if (formatFileExtension.equalsIgnoreCase(originalFileExtension)) {
                /* New extension == current extension */
                newFileEnding = originalFileExtension;
            } else {
                if (formatFileExtension.endsWith(originalFileExtension)) {
                    newFileEnding = formatFileExtension;
                } else {
                    newFileEnding = formatFileExtension + originalFileExtension;
                }
            }
            if (!preGivenFilename.endsWith(newFileEnding)) {
                filename = preGivenFilename.replaceFirst(Pattern.quote(originalFileExtension) + "$", newFileEnding);
            } else {
                filename = preGivenFilename;
            }
        } else {
            filename = preGivenFilename;
        }
        /**
         * We know that this website is always providing .7z files. </br>
         * Filename is the same serverside regardless of the user selected file format. </br>
         * In JDownloader however, we want to have different filenames for each format.
         */
        filename = plg.applyFilenameExtension(filename, EXT_DEFAULT);
        if (setAsFinalFileName) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link);
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final int maxChunks = 1;
        /* 2024-06-13: This is always inside their html code (didn't bother adding more filters) */
        final boolean looksLikeDownloadUnavailable = br.containsHTML(">\\s*Download unavailable");
        try {
            final String url = br.getRegex("\"([^\"]*download\\d+\\." + Pattern.quote(br.getHost(false)) + "/[^\"]*)\"").getMatch(0);
            if (url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String mediaID = link.getStringProperty(PROPERTY_MEDIA_ID);
            if (mediaID == null) {
                /*
                 * Older items until including revision 48248 where target-mediaID was not pre-given in crawler as crawler didn't exist yet.
                 */
                mediaID = br.getRegex("name=\"mediaId\" value=\"(\\d+)").getMatch(0);
            }
            if (mediaID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String directurl = br.getURL(url).toExternalForm() + "?mediaId=" + mediaID;
            final int format_id = link.getIntegerProperty(PROPERTY_FORMAT_ID, 0);
            if (format_id != 0) {
                /* Download 2nd/alternative version */
                directurl += "&alt=1";
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, isResumeable(link, null), maxChunks);
        } catch (final PluginException ple) {
            final String downloadUnavailableReason = VimmNetCrawler.findDownloadUnavailableText(br);
            if (downloadUnavailableReason != null) {
                /* Item is not downloadable. */
                throw new PluginException(LinkStatus.ERROR_FATAL, downloadUnavailableReason, ple);
            } else {
                throw ple;
            }
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.containsHTML("/download/cancel\\.php")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "You are currently downloading another file. Wait until you can start the next download.", 1 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                if (looksLikeDownloadUnavailable) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Download unavailable");
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}