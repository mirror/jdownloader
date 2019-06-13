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

import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class WstreamVideo extends XFileSharingProBasic {
    public WstreamVideo(final PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null<br />
     * other:<br />
     */
    private static String[] domains = new String[] { "wstream.video", "download.wstream.video" };

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return -2;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
        }
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2019-04-24: Special */
        super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = new Regex(correctedBR, "<h3>(?:\\s*?<div class=\"[^\"]+\">)?([^<>\"]+)(?:</div>\\s*?)?</h3>").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    /** Checks if official video download is possible and returns downloadlink if possible. */
    public String checkOfficialVideoDownload(final DownloadLink link, final Account account) throws Exception {
        /* 2019-06-13: Special */
        String dllink = null;
        if (br.containsHTML("https://download\\.wstream\\.video/" + this.fuid)) {
            logger.info("Attempting official video download");
            final Browser brc = br.cloneBrowser();
            final String b64 = Encoding.Base64Encode(this.fuid + "|600");
            /* 2019-06-13: Skip waittime (6-10 seconds) */
            this.getPage(brc, "https://video.wstream.video/downloadlink.php?f=" + b64);
            final String[] dlinfo = brc.getRegex("class='buttonDownload' href='[^\\']+'>Download [^<>]+</a>").getColumn(-1);
            String dllink_last = null;
            String filesize_last = null;
            for (final String dlinfoSingle : dlinfo) {
                dllink_last = new Regex(dlinfoSingle, "href=\\'(http[^\"\\']+)").getMatch(0);
                filesize_last = new Regex(dlinfoSingle, ">Download [A-Za-z0-9 ]+ (\\d+(?:\\.\\d+)? [A-Za-z]{1,5})\\s*?<").getMatch(0);
                if (dlinfoSingle.contains("Download Original")) {
                    dllink = dllink_last;
                    break;
                }
            }
            if (dllink == null && dllink_last != null) {
                /* Failed to find original download? Fallback to ANY video-quality. */
                logger.info("Failed to find highest quality, falling back to (???)");
                dllink = dllink_last;
            }
            if (link.getDownloadSize() <= 0 && filesize_last != null) {
                /*
                 * 2019-06-13: Sure if everything goes as planned this makes no sense BUT in case the download fails to start, now we at
                 * least found our filesize :)
                 */
                logger.info("Setting filesize here as availablecheck was unable to find it");
                link.setDownloadSize(SizeFormatter.getSize(filesize_last));
            }
        }
        if (dllink != null) {
            logger.info("Successfully found official video downloadlink");
        } else {
            logger.info("Failed to find official video downloadlink");
        }
        return dllink;
    }

    @Override
    public boolean supports_https() {
        return super.supports_https();
    }

    @Override
    public boolean supports_precise_expire_date() {
        return super.supports_precise_expire_date();
    }

    @Override
    public boolean isVideohosterEmbed() {
        return true;
    }

    @Override
    public boolean isVideohoster_enforce_video_filename() {
        return true;
    }

    @Override
    public boolean isImagehoster() {
        return super.isImagehoster();
    }

    @Override
    public boolean supports_availablecheck_alt() {
        /* 2019-04-24: Special */
        return false;
    }

    @Override
    public boolean prefer_availablecheck_filesize_alt_type_old() {
        return super.prefer_availablecheck_filesize_alt_type_old();
    }

    @Override
    public boolean supports_availablecheck_filename_abuse() {
        /* 2019-04-24: Special */
        return false;
    }

    @Override
    public boolean supports_availablecheck_filesize_html() {
        /* 2019-04-24: Special */
        return true;
    }

    @Override
    public boolean requires_WWW() {
        return super.requires_WWW();
    }

    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    /**
     * returns the annotation pattern array: 'https?://(?:www\\.)?(?:domain1|domain2)/(?:embed\\-)?[a-z0-9]{12}'
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/(?:embed\\-|video/)?[a-z0-9]{12}(?:/[^/]+\\.html)?" };
    }

    /** returns 'https?://(?:www\\.)?(?:domain1|domain2)' */
    private static String getHostsPattern() {
        final String hosts = "https?://(?:www\\.)?" + "(?:" + getHostsPatternPart() + ")";
        return hosts;
    }

    /** Returns '(?:domain1|domain2)' */
    public static String getHostsPatternPart() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        return pattern.toString();
    }
}