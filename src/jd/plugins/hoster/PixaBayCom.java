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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pixabay.com" }, urls = { "https?://(?:www\\.)?pixabay\\.com/(?:en/)?(?:(?:photos|gifs|illustrations|vectors|images/download)/)?([a-z0-9\\-]+)-(\\d+)/?" })
public class PixaBayCom extends PluginForHost {
    public PixaBayCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://pixabay.com/en/accounts/register/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    @Override
    public String getAGBLink() {
        return "https://pixabay.com/en/service/terms/";
    }

    /* TODO: Maybe add support for SVG format(s) */
    // private final String[] qualities = { "Original", "O", "XXL", "XL", "L", "M", "S" };
    private String quality_max         = null;
    private String quality_download_id = null;

    private String getContentURL(final DownloadLink link) {
        String url = link.getPluginPatternMatcher().replace("http://", "https://");
        if (StringUtils.containsIgnoreCase(url, "/images/download/")) {
            final String fid = getFID(link);
            if (fid != null) {
                return "https://pixabay.com/dummy-" + fid;
            }
        }
        return url;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    private boolean isAnimation(final Browser br) {
        return isAnimation(br.getURL());
    }

    private boolean isAnimation(final String url) {
        if (StringUtils.containsIgnoreCase(url, "/gifs/")) {
            return true;
        } else {
            return false;
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String contenturl = getContentURL(link);
        final String fid = this.getFID(link);
        final String assumedFileExtension;
        if (isAnimation(link.getPluginPatternMatcher())) {
            assumedFileExtension = ".gif";
        } else {
            assumedFileExtension = ".jpg";
        }
        if (!link.isNameSet()) {
            /* Set weak filename */
            if (link.getPluginPatternMatcher().contains("/gifs/")) {
                link.setName(fid + assumedFileExtension);
            } else {
                link.setName(fid + assumedFileExtension);
            }
        }
        this.setBrowserExclusive();
        if (account != null) {
            this.login(account, false);
        }
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("?")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (StringUtils.containsIgnoreCase(contenturl, getHost() + "/dummy-") && this.canHandle(br.getURL())) {
            /* Correct added URL */
            link.setPluginPatternMatcher(br.getURL());
        }
        String fileTitle = br.getRegex("(?i)<title>\\s*([^<>]*?)\\s*(?:-\\s*Free \\w+ on Pixabay)?\\s*</title>").getMatch(0);
        /* Find filesize based on whether user has an account or not. Users with account can download the best quality/original. */
        String filesize = null;
        if (account != null) {
            int heightMax = 0;
            int heightTmp = 0;
            final String[] qualityInfo = br.getRegex("(<td>\\s*<input type=\"radio\" name=\"download\".*?</td>\\s*</tr>)").getColumn(0);
            for (final String quality : qualityInfo) {
                // logger.info("quality: " + quality);
                /* Old: TODO: Check if these ones still exist. */
                boolean isResolution = false;
                String quality_name = new Regex(quality, "(ORIGINAL|O|S|M|L|XXL|XL|SVG)\\s*</td>").getMatch(0);
                if (quality_name == null) {
                    /* 2020-11-25: New/current */
                    quality_name = new Regex(quality, "(\\d+(?:x|×)\\d+)").getMatch(0);
                    isResolution = true;
                }
                if (quality_name == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (isResolution) {
                    final String heightStr = new Regex(quality_name, "^(\\d+)").getMatch(0);
                    heightTmp = Integer.parseInt(heightStr);
                    if (heightTmp > heightMax) {
                        heightMax = heightTmp;
                        filesize = new Regex(quality, "class=\"hide-xs hide-md\">([^<>\"]*?)<").getMatch(0);
                        if (filesize == null) {
                            filesize = new Regex(quality, ">(\\d+(?:\\.\\d+)? (?:kB|mB|gB))<").getMatch(0);
                        }
                        quality_max = heightStr;
                        quality_download_id = new Regex(quality, "([^<>\"/]*?\\.jpg)").getMatch(0);
                        if (quality_download_id == null) {
                            quality_download_id = new Regex(quality, "name=\"download\" value=\"([^<>\"]*?)\"").getMatch(0);
                        }
                    }
                } else {
                    logger.info("Skipping unsupported quality");
                }
            }
        }
        if (fileTitle == null) {
            /* Fallback */
            fileTitle = fid;
        }
        /*
         * Also as fallback if account download fails or no official download button is given: Grab publicly visible image (lowest quality).
         */
        if (filesize == null || quality_download_id == null) { // No account
            String dllink = br.getRegex("id=\"media_container\" class=\"init\"[^>]*>\\s*<img src=\"(https?://[^\"]+)\"").getMatch(0); // gifs
            if (dllink == null) {
                dllink = br.getRegex("(https?://cdn[^<>\"\\s]+)\\s*1\\.333x").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("(https?://cdn[^<>\"\\s]+)\\s*1x").getMatch(0);
            }
            if (dllink != null) {
                final String ext = Plugin.getFileNameExtensionFromURL(dllink);
                link.setProperty("free_directlink", dllink);
                fileTitle = Encoding.htmlDecode(fileTitle.trim());
                fileTitle += ext;
                link.setFinalFileName(fileTitle);
                return AvailableStatus.TRUE; // <=== No account
            }
        }
        if (quality_download_id == null) {
            logger.info("quality_download_id: " + quality_download_id);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (quality_max == null) {
            quality_max = "10000";
        }
        fileTitle = Encoding.htmlDecode(fileTitle).trim();
        fileTitle += assumedFileExtension;
        link.setFinalFileName(fileTitle);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(link, null);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                /* 2019-09-07: If life was always as simple as that ... :D 2020-11-25: Cookie is still used :D */
                br.setCookie(this.getHost(), "is_human", "1");
                // br.setCookie(this.getHost(), "lang", "de");
                br.setCookie(this.getHost(), "client_width", "1920");
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting to login via cookies");
                    br.setCookies(account.getHoster(), cookies);
                    if (!force) {
                        logger.info("Trust cookies without login");
                        return;
                    } else {
                        br.getPage("https://" + this.getHost() + "/de/accounts/media/");
                        if (this.isLoggedIN()) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(account.getHoster()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            this.br.clearAll();
                        }
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/accounts/login/");
                final Form loginform = br.getFormbyKey("password");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", account.getUser());
                loginform.put("password", account.getPass());
                if (loginform.getAction() != null && loginform.getAction().equals(".")) {
                    loginform.setAction(br.getURL());
                }
                // if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform) ||
                // loginform.containsHTML("g-recaptcha-response")) {
                if (true) { /* 2020-11-25: Login captcha is always required */
                    /* 2020-11-25: New: Login captcha */
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    try {
                        final DownloadLink dl_dummy;
                        if (dlinkbefore != null) {
                            dl_dummy = dlinkbefore;
                        } else {
                            dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                            this.setDownloadLink(dl_dummy);
                        }
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    } finally {
                        this.setDownloadLink(dlinkbefore);
                    }
                }
                br.submitForm(loginform);
                if (!isLoggedIN()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.containsHTML("/accounts/logout/\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty;
        if (account != null) {
            directurlproperty = "premium_directlink";
        } else {
            directurlproperty = "free_directlink";
        }
        String dllink = checkDirectLink(link, directurlproperty);
        if (dllink == null) {
            if (this.quality_download_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String downloadtype;
            if (isAnimation(br)) {
                downloadtype = "animations";
            } else {
                downloadtype = "images";
            }
            dllink = "/" + downloadtype + "/download/" + this.quality_download_id + "?attachment";
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                dl.startDownload();
                return;
            }
            /* Captcha required e.g. download without account */
            br.followConnection(true);
            final Form dlform = br.getForm(0);
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            dlform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(dlform);
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directurlproperty, dllink);
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        /* Login not needed here as we already logged in above. */
        // 2022-10-27: wtf what does this do and why is it here?
        String dllink = this.br.getRegex("name=\"download\" value=\"([^<>\"]*?)\" data-perm=\"auth\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDownload(link, account);
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* Captcha is typically required when official download is used. */
        return true;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}