//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideoStreamhideCom;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class StreamhideCom extends XFileSharingProBasic {
    public StreamhideCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2023-02-03: null <br />
     * other: Similar to: media.cm <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "streamhide.to", "streamhide.com", "guccihide.com", "streamhide.com", "streamtb.me", "louishide.com", "ahvsh.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?::\\d+)?" + "/(?:d|e|w)/([a-z0-9]{12})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 0;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        return true;
    }

    @Override
    protected String getContentURL(final DownloadLink link) {
        return this.getMainPage(link) + "/d/" + this.getFUIDFromURL(link);
    }

    protected String getFUID(final String url, URL_TYPE type) {
        if (url != null) {
            return new Regex(url, this.getSupportedLinks()).getMatch(0);
        } else {
            return null;
        }
    }

    @Override
    protected boolean supportsShortURLs() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    private final String PROPERTY_HAS_LOOKED_FOR_ADDITIONAL_FILENAME_SOURCE = "has_looked_for_additional_filename_source";
    private final String PROPERTY_STREAM_FILENAME                           = "stream_filename";

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final AvailableStatus status = super.requestFileInformationWebsite(link, account, isDownload);
        final String[] finfo = this.scanInfo(internal_getFileInfoArray());
        final String storedStreamFilename = link.getStringProperty(PROPERTY_STREAM_FILENAME);
        if (storedStreamFilename != null) {
            this.setFilename(storedStreamFilename, link, br);
        } else {
            final String filename = finfo[0];
            if (StringUtils.isEmpty(filename) && br.getURL() != null && !br.getURL().matches("(?i)https?://[^/]+/w/[a-z0-9]{12}") && link.getBooleanProperty(PROPERTY_HAS_LOOKED_FOR_ADDITIONAL_FILENAME_SOURCE) == false) {
                /*
                 * 2023-07-06: Workaround for items where uploader has disabled official filenames -> Website does not show any
                 * title/filename on official download page which we are accessing first.
                 */
                logger.info("Looking for filename on streaming page");
                final Browser brc = br.cloneBrowser();
                brc.getPage("/w/" + this.getFUIDFromURL(link));
                final String streamFilename = brc.getRegex("class=\"h4 mb-3 text-white\"[^>]*>([^<]+)</h1>").getMatch(0);
                if (streamFilename != null) {
                    logger.info("Successfully found filename on streaming page: " + streamFilename);
                    link.setProperty(PROPERTY_STREAM_FILENAME, streamFilename);
                    this.setFilename(streamFilename, link, brc);
                } else {
                    logger.warning("Failed to find filename on streaming page");
                }
                link.setProperty(PROPERTY_HAS_LOOKED_FOR_ADDITIONAL_FILENAME_SOURCE, true);
            }
        }
        return status;
    }

    @Override
    public Class<? extends XFSConfigVideoStreamhideCom> getConfigInterface() {
        return XFSConfigVideoStreamhideCom.class;
    }
}