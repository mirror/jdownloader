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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VidhideCom extends XFileSharingProBasic {
    public VidhideCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null 4dignum solvemedia reCaptchaV2, hcaptcha<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vidhide.com", "vidhidepro.com", "moflix-stream.click" });
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
        return VidhideCom.buildAnnotationUrls(getPluginDomains());
    }

    public static final String getDefaultAnnotationPatternPartVidhideCom() {
        return "/(?:d/[A-Za-z0-9]+|(?:embed-|e/|f/)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?)";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?::\\d+)?" + VidhideCom.getDefaultAnnotationPatternPartVidhideCom());
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
    protected boolean supportsShortURLs() {
        return false;
    }

    private final String PATTERN_SPECIAL = "(?i)^https?://[^/]+/f/([a-z0-9]{12}).*";

    @Override
    protected URL_TYPE getURLType(final String url) {
        if (url != null && url.matches(PATTERN_SPECIAL)) {
            return URL_TYPE.OFFICIAL_VIDEO_DOWNLOAD;
        } else {
            return super.getURLType(url);
        }
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        if (link != null && link.getPluginPatternMatcher() != null) {
            final Regex special = new Regex(link.getPluginPatternMatcher(), PATTERN_SPECIAL);
            if (special.patternFind()) {
                return special.getMatch(0);
            }
        }
        return super.getFUIDFromURL(link);
    }

    @Override
    protected String getDllinkViaOfficialVideoDownloadNew(final Browser br, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        final String filesize = br.getRegex(">\\s*\\d+x\\d+ ([^<]+)</small>").getMatch(0);
        if (returnFilesize) {
            return filesize;
        }
        final String originalDownloadContinueLink = br.getRegex("(/download/[a-z0-9]{12}_o)").getMatch(0);
        if (originalDownloadContinueLink == null) {
            /* Fallback to upper handling */
            return super.getDllinkViaOfficialVideoDownloadNew(br, link, account, returnFilesize);
        }
        getPage(br, originalDownloadContinueLink);
        final Form download1 = br.getFormByInputFieldKeyValue("op", "download_orig");
        if (download1 != null) {
            this.handleCaptcha(link, br, download1);
            this.submitForm(br, download1);
            this.checkErrors(br, br.getRequest().getHtmlCode(), link, account, false);
        }
        String dllink = this.getDllink(link, account, br, br.getRequest().getHtmlCode());
        if (StringUtils.isEmpty(dllink)) {
            /*
             * 2019-05-30: Test - worked for: xvideosharing.com - not exactly required as getDllink will usually already return a result.
             */
            dllink = br.getRegex("<a href\\s*=\\s*\"(https?[^\"]+)\"[^>]*>\\s*Direct Download Link\\s*</a>").getMatch(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink via official video download");
            final String specialErrorDownloadImpossible = br.getRegex("<b class=\"err\"[^>]*>([^<]+)</b>").getMatch(0);
            if (specialErrorDownloadImpossible != null) {
                /* 2024-04-02: e.g. "Downloads disabled 6210" */
                throw new PluginException(LinkStatus.ERROR_FATAL, specialErrorDownloadImpossible);
            } else {
                return null;
            }
        }
        logger.info("Successfully found dllink via official video download");
        return dllink;
    }
}