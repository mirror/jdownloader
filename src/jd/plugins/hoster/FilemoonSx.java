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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideoFilemoonSx;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.decrypter.FilemoonSxCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FilemoonSx extends XFileSharingProBasic {
    public FilemoonSx(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2022-06-21: No limits at all <br />
     * captchatype-info: 2022-06-21: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        return FilemoonSxCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return FilemoonSxCrawler.getAnnotationUrls();
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
        /* 2022-06-21: Special */
        return true;
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        try {
            final String url = link.getPluginPatternMatcher();
            if (url != null) {
                final String result = new Regex(new URL(url).getPath(), "/./([a-z0-9]+)").getMatch(0);
                return result;
            }
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public String getFilenameFromURL(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/./[^/]+/(.+)").getMatch(0);
    }

    @Override
    protected String buildURLPath(final DownloadLink link, final String fuid, final URL_TYPE type) {
        switch (type) {
        case EMBED:
            return "/e/" + fuid;
        case NORMAL:
            return "/d/" + fuid;
        default:
            throw new IllegalArgumentException("Unsupported type:" + type + "|" + fuid);
        }
    }

    @Override
    protected URL_TYPE getURLType(final String url) {
        if (url != null) {
            if (url.matches("(?i)^https?://[^/]+/d/([a-z0-9]+).*")) {
                return URL_TYPE.NORMAL;
            } else if (url.matches("(?i)^https?://[A-Za-z0-9\\-\\.:]+/e/([a-z0-9]{12}).*")) {
                return URL_TYPE.EMBED;
            } else {
                logger.info("Unknown URL_TYPE:" + url);
            }
        }
        return null;
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filesize_alt_fast() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    protected boolean isShortURL(DownloadLink link) {
        return false;
    }

    @Override
    public String[] scanInfo(final String html, final String[] fileInfo) {
        super.scanInfo(html, fileInfo);
        /* 2022-11-04 */
        final String betterFilename = new Regex(html, "<h3[^>]*>([^<]+)</h3>").getMatch(0);
        if (betterFilename != null) {
            fileInfo[0] = betterFilename;
        }
        return fileInfo;
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        String dllink = checkDirectLink(link, account);
        if (StringUtils.isEmpty(dllink)) {
            requestFileInformationWebsite(link, account, true);
            this.getPage("/download/" + this.getFUIDFromURL(link));
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LdiBGAgAAAAAIQm_arJfGYrzjUNP_TCwkvPlv8k").getToken();
            final Form dlform = new Form();
            dlform.setMethod(MethodType.POST);
            dlform.put("b", "download");
            dlform.put("file_code", this.getFUIDFromURL(link));
            dlform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            this.submitForm(dlform);
            dllink = this.getDllink(link, account, br, br.getRequest().getHtmlCode());
        }
        handleDownload(link, account, dllink, null);
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        /* 2022-11-04: Website failure after captcha on "/download/..." page */
        if (br.containsHTML("(?i)class=\"error e404\"|>\\s*Page not found")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404");
        }
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br, final String correctedBR) {
        if (br.containsHTML("(?i)<h1>\\s*Page not found|class=\"error e404\"")) {
            return true;
        } else {
            return super.isOffline(link, br, correctedBR);
        }
    }

    @Override
    public Class<? extends XFSConfigVideoFilemoonSx> getConfigInterface() {
        return XFSConfigVideoFilemoonSx.class;
    }
}