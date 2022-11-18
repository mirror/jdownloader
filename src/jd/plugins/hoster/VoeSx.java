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
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.decrypter.VoeSxCrawler;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideoVoeSx;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { VoeSxCrawler.class })
public class VoeSx extends XFileSharingProBasic {
    public VoeSx(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-11-27: Premium untested, set FREE limits <br />
     * captchatype-info: 2020-08-19: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        return VoeSxCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return VoeSxCrawler.getAnnotationUrls();
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        try {
            final String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "/(?:embed-|e/)?([a-z0-9]{12})").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    protected void processFileInfo(String[] fileInfo, Browser altbr, DownloadLink link) {
        if (fileInfo != null && fileInfo[0] != null) {
            fileInfo[0] = fileInfo[0].replaceFirst("(\\s*-\\s*VOE\\s*\\|\\s*Content\\s*Delivery\\s*Network\\s*\\(CDN\\)\\s*&\\s*Video\\s*Cloud)", "");
        }
        super.processFileInfo(fileInfo, altbr, link);
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
        /* 2020-08-19 */
        return true;
    }

    @Override
    protected boolean supportsAPIMassLinkcheck() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean supportsAPISingleLinkcheck() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean isVideohosterEmbed() {
        /* 2021-03-09 */
        return true;
    }

    @Override
    protected String getDllinkVideohost(DownloadLink link, Account account, Browser br, final String src) {
        final String mp4Master = new Regex(src, "(\"|')mp4\\1\\s*:\\s*(\"|')(https?://[^\"']+)").getMatch(2);
        if (mp4Master != null) {
            return mp4Master;
        }
        final String hlsMaster = new Regex(src, "(\"|')hls\\1\\s*:\\s*(\"|')(https?://[^\"']+)").getMatch(2);
        if (hlsMaster != null) {
            return hlsMaster;
        } else {
            return super.getDllinkVideohost(link, account, br, src);
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        // if (br.containsHTML(">\\s*This video can be watched as embed only")) {
        // throw new PluginException(LinkStatus.ERROR_FATAL, "This video can be watched as embed only");
        // }
    }

    @Override
    protected String requestFileInformationVideoEmbed(final Browser br, final DownloadLink link, final Account account, final boolean findFilesize) throws Exception {
        /* 2021-03-09: Special: New browser required else they won't let us stream some videos at all! */
        final boolean embedOnly = br.containsHTML(">\\s*This video can be watched as embed only");
        br.setFollowRedirects(true);
        br.getPage("https://" + this.getHost() + "/e/" + this.getFUIDFromURL(link));
        final String dllink = getDllinkVideohost(link, account, null, br.toString());
        if (StringUtils.isEmpty(dllink) && embedOnly) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video can be watched as embed only");
        }
        return dllink;
    }

    @Override
    protected String getDllinkViaOfficialVideoDownload(final Browser br, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        if (returnFilesize) {
            return null;
        } else {
            logger.info("[DownloadMode] Trying to find official video downloads");
            String continueLink = br.getRegex("(?:\"|')(/dl\\?op=download_orig[^\"\\']+)").getMatch(0);
            if (continueLink == null) {
                /* No official download available */
                logger.info("Failed to find any official video downloads");
                return null;
            }
            if (br.containsHTML("&embed=&adb=")) {
                /* 2022-08-24: This might give us more download-speed, not sure though. */
                continueLink += "&embed=&adb=0";
            }
            this.getPage(br, continueLink);
            String dllink = this.getDllink(link, account, br, br.getRequest().getHtmlCode());
            if (StringUtils.isEmpty(dllink)) {
                /*
                 * 2019-05-30: Test - worked for: xvideosharing.com - not exactly required as getDllink will usually already return a
                 * result.
                 */
                dllink = br.getRegex("(?i)>\\s*Download Link\\s*</td>\\s*<td><a href=\"(https?://[^\"]+)\"").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find dllink via official video download");
            } else {
                logger.info("Successfully found dllink via official video download");
                final String filesizeBytesStr = br.getRegex("File Size \\(bytes\\)</td>\\s*<td>\\s*(\\d+)\\s*<").getMatch(0);
                if (filesizeBytesStr != null) {
                    link.setVerifiedFileSize(Long.parseLong(filesizeBytesStr));
                }
            }
            return dllink;
        }
    }

    @Override
    public Class<? extends XFSConfigVideoVoeSx> getConfigInterface() {
        return XFSConfigVideoVoeSx.class;
    }
}