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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
        this.enablePremium(super.getPurchasePremiumURL());
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        /* 2019-07-11: Special */
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:w?embed\\-|video/)?[a-z0-9]{12}(?:/[^/]+\\.html)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* 2019-07-11: Special */
        final String fuid = this.fuid != null ? this.fuid : getFUIDFromURL(link);
        if (fuid != null) {
            /* link cleanup, prefer https if possible */
            if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches("https?://[A-Za-z0-9\\-\\.]+/w?embed-[a-z0-9]{12}")) {
                link.setContentUrl(getMainPage() + "/embed-" + fuid + ".html");
            }
            link.setPluginPatternMatcher(getMainPage() + "/" + fuid);
            link.setLinkID(getHost() + "://" + fuid);
        }
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        /* 2019-07-11: Special */
        try {
            final String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "/(?:w?embed-)?([a-z0-9]{12})").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null<br />
     * other:<br />
     */
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
        /* 2019-06-27: Special - used for ALL download modes (including premium!) */
        String dllink = null;
        final String redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"\\d+;URL=(https[^<>\"]+)\">").getMatch(0);
        if (redirect != null) {
            this.getPage(redirect);
        }
        final boolean force_download_attempt = true;
        if (br.containsHTML("https://download\\.wstream\\.video/" + this.fuid) || force_download_attempt) {
            logger.info("Attempting official video download");
            final Browser brc = br.cloneBrowser();
            this.getPage(brc, "https://download.wstream.video/" + this.fuid);
            String dlFunctionURL = brc.getRegex("\\$\\(document\\)\\.ready\\(function\\(\\)\\{\\s*?\\$\\.get\\(\"([^\"]+)\"").getMatch(0);
            if (dlFunctionURL == null) {
                /* Fallback! Last updated: 2019-06-27: They might frequently change this!! */
                final String b64 = Encoding.Base64Encode(this.fuid + "|600");
                dlFunctionURL = "https://video.wstream.video/dwn.php?f=" + b64;
            }
            /* 2019-06-13: Skip waittime (6-10 seconds) */
            this.getPage(brc, dlFunctionURL);
            /* Cat/mouse games... */
            // final String[] hosts = siteSupportedNames();
            final String[] dlinfo = brc.getRegex("class='[^\\']+' href='[^\\']+'>Download [^<>]+</a>").getColumn(-1);
            String dllink_last = null;
            String filesize_last = null;
            int qualityMax = 0;
            int qualityTemp = 0;
            for (final String dlinfoSingle : dlinfo) {
                final String dllink_temp = new Regex(dlinfoSingle, "href=\\'(http[^\"\\']+)").getMatch(0);
                if (StringUtils.isEmpty(dllink_temp)) {
                    continue;
                }
                boolean dllinkValid = dllink_temp.contains(".mp4") || dllink_temp.contains(".mkv") || dllink_temp.contains(link.getName());
                // for (final String host : hosts) {
                // if (dllink_temp.contains(host)) {
                // dllinkValid = true;
                // break;
                // }
                // }
                if (!dllinkValid) {
                    /* Skip invalid URLs */
                    continue;
                }
                dllink_last = dllink_temp;
                filesize_last = new Regex(dlinfoSingle, ">Download [A-Za-z0-9 ]+ (\\d+(?:\\.\\d+)? [A-Za-z]{1,5})\\s*?<").getMatch(0);
                if (dlinfoSingle.contains("Download Original")) {
                    qualityTemp = 100;
                } else if (dlinfoSingle.contains("Download Standard")) {
                    qualityTemp = 50;
                } else if (dlinfoSingle.contains("Download Mobile")) {
                    qualityTemp = 10;
                } else {
                    /* Unknown quality */
                    qualityTemp = 1;
                }
                if (qualityTemp > qualityMax) {
                    qualityMax = qualityTemp;
                    dllink = dllink_temp;
                }
            }
            if (dllink == null && dllink_last != null) {
                /* Failed to find original download? Fallback to ANY video-quality. */
                logger.info("Failed to find highest quality, falling back to (???)");
                dllink = dllink_last;
            }
            if (link.getView().getBytesTotal() <= 0 && filesize_last != null) {
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
    public boolean isVideohosterEmbed() {
        return true;
    }

    @Override
    public boolean isVideohoster_enforce_video_filename() {
        return true;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-04-24: Special */
        return true;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wstream.video", "download.wstream.video" });
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
        return buildAnnotationUrls(getPluginDomains());
    }
}