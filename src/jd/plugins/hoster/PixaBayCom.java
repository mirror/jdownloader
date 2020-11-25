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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pixabay.com" }, urls = { "https?://(?:www\\.)?pixabay\\.com/(?:en/)?(?:photos/)?([a-z0-9\\-]+)-(\\d+)/" })
public class PixaBayCom extends PluginForHost {
    public PixaBayCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://pixabay.com/en/accounts/register/");
    }

    @Override
    public String getAGBLink() {
        return "https://pixabay.com/en/service/terms/";
    }

    /* TODO: Maybe add support for SVG format(s) */
    // private final String[] qualities = { "Original", "O", "XXL", "XL", "L", "M", "S" };
    private String quality_max         = null;
    private String quality_download_id = null;

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("http://", "https://"));
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

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            this.login(aa, false);
        }
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("?")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fallback_filename = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        String filename = br.getRegex("<title>\\s*([^<>]*?)\\s*(?: - Free photo on Pixabay)?</title>").getMatch(0);
        /* Find filesize based on whether user has an account or not. Users with account can download the best quality/original. */
        String filesize = null;
        if (aa != null) {
            int heightMax = 0;
            int heightTmp = 0;
            final String[] qualityInfo = br.getRegex("(<td><input type=\"radio\" name=\"download\".*?/td></tr>)").getColumn(0);
            for (final String quality : qualityInfo) {
                // logger.info("quality: " + quality);
                /* Old: TODO: Check if these ones still exist. */
                boolean isResolution = false;
                String quality_name = new Regex(quality, "(ORIGINAL|O|S|M|L|XXL|XL|SVG)</td>").getMatch(0);
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
        if (filename == null) {
            filename = fallback_filename;
        }
        /* Also as fallback for account download: Grab publicly visible image (lowest quality). */
        if (filesize == null || quality_download_id == null) { // No account
            String dllink = br.getRegex("(https?://cdn[^<>\"\\s]+) 1\\.333x").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https?://cdn[^<>\"\\s]+) 1x").getMatch(0);
            }
            if (dllink != null) {
                String ext = dllink.substring(dllink.lastIndexOf("."));
                link.setProperty("free_directlink", dllink);
                filename = Encoding.htmlDecode(filename.trim());
                filename = encodeUnicode(filename) + ext;
                link.setFinalFileName(filename);
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
        filename = Encoding.htmlDecode(filename.trim());
        filename = encodeUnicode(filename) + ".jpg";
        link.setFinalFileName(filename);
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
        /* Free accounts do not have captchas. */
        account.setConcurrentUsePossible(true);
        ai.setStatus("Registered (free) user");
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
            dllink = "https://pixabay.com/images/download/" + this.quality_download_id + "?attachment";
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
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
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
        requestFileInformation(link);
        /* Login not needed here as we already logged in above. */
        String dllink = this.br.getRegex("name=\"download\" value=\"([^<>\"]*?)\" data-perm=\"auth\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDownload(link, account);
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