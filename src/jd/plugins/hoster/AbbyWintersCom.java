//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.AbbyWintersComGallery;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AbbyWintersCom extends PluginForHost {
    public AbbyWintersCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.abbywinters.com/tour");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "https://www.abbywinters.com/about/termsandconditions";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "abbywinters.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.+/video/\\w+");
        }
        return ret.toArray(new String[0]);
    }

    public static final String PATTERN_VIDEO           = "https?://[^/]+/([a-z0-9\\-_]+)/([a-z0-9\\-_]+)/([a-z0-9\\-_]+)/video/\\w+";
    public static final String PROPERTY_MAINLINK       = "mainlink";
    public static final String PROPERTY_IMAGE_POSITION = "image_position";
    private final String       PROPERTY_DIRECTURL      = "directurl";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (account == null) {
            link.getLinkStatus().setStatusText("Only downlodable/checkable via account!");
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = null;
        if (link.getPluginPatternMatcher().matches(PATTERN_VIDEO)) {
            login(account, link.getPluginPatternMatcher(), true);
            br.setFollowRedirects(true);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("(?i)404 Page not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String videoDataResources = br.getRegex("data-sources=\"([^\"]+)").getMatch(0);
            if (videoDataResources == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String[] roughFilesizes = br.getRegex("<small>(\\d+(\\.\\d+)?M) \\| \\d+x\\d+ | \\d+ Kbps</small>").getColumn(0);
            if (roughFilesizes != null && roughFilesizes.length > 0) {
                long biggestEstimatedFilesize = -1;
                for (final String roughFilesize : roughFilesizes) {
                    final long filesizeTmp = SizeFormatter.getSize(roughFilesize + "b");
                    if (filesizeTmp > biggestEstimatedFilesize) {
                        biggestEstimatedFilesize = filesizeTmp;
                    }
                }
                link.setDownloadSize(biggestEstimatedFilesize);
            } else {
                logger.warning("Failed to find any estimated filesize");
            }
            final List<Object> ressourcelist = JSonStorage.restoreFromString(Encoding.htmlDecode(videoDataResources), TypeRef.LIST);
            /* Last item = Best video quality */
            final Map<String, Object> bestVideo = (Map<String, Object>) ressourcelist.get(ressourcelist.size() - 1);
            final String directurl = bestVideo.get("src").toString();
            link.setProperty(PROPERTY_DIRECTURL, directurl);
            filename = Plugin.getFileNameFromURL(new URL(directurl));
        } else {
            /* Should be directurl */
            login(account, "https://www." + this.getHost() + "/", false);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(link.getPluginPatternMatcher());
                if (!this.looksLikeDownloadableContent(con)) {
                    br.followConnection(true);
                    logger.info("Trying to refresh directurl");
                    final String mainlink = link.getStringProperty(PROPERTY_MAINLINK);
                    final int targetPage = link.getIntegerProperty(PROPERTY_IMAGE_POSITION, -1);
                    if (mainlink == null || targetPage == -1) {
                        /* This should never happen! */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final AbbyWintersComGallery decrypterPlugin = (AbbyWintersComGallery) this.getNewPluginForDecryptInstance(this.getHost());
                    final ArrayList<DownloadLink> results = decrypterPlugin.decryptIt(new CryptedLink(mainlink), account);
                    DownloadLink newDownloadlink = null;
                    for (final DownloadLink result : results) {
                        if (result.getIntegerProperty(PROPERTY_IMAGE_POSITION, -1) == targetPage) {
                            newDownloadlink = result;
                            break;
                        }
                    }
                    if (newDownloadlink == null) {
                        /* This should never happen! */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    link.setPluginPatternMatcher(newDownloadlink.getPluginPatternMatcher());
                    con = br.openHeadConnection(newDownloadlink.getPluginPatternMatcher());
                    if (!this.looksLikeDownloadableContent(con)) {
                        /* This should never happen! */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to refresh directurl");
                    }
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        if (filename != null) {
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        throw new AccountRequiredException();
    }

    public void login(final Account account, final String loginCheckURLStr, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setCookie(this.getHost(), "ageverify", "1");
                br.setFollowRedirects(true);
                final URL loginURL = new URL(loginCheckURLStr);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(cookies);
                    if (!force) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage(loginURL);
                    if (this.isLoggedInHTML(br)) {
                        logger.info("Cookie login successful via URL: " + loginCheckURLStr);
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                    }
                }
                logger.info("Performing full login");
                br.setFollowRedirects(false);
                br.getPage("https://www." + this.getHost() + "/members");
                final Form loginform = br.getFormbyProperty("id", "login-form");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.setAction("/rpc/login");
                loginform.setMethod(MethodType.POST);
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "on");
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                    @Override
                    public TYPE getType() {
                        return TYPE.INVISIBLE;
                    }
                }.getToken();
                loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                final Browser br2 = br.cloneBrowser();
                br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br2.submitForm(loginform);
                if (br2.containsHTML("\"result\":\"failed\"") || !br2.containsHTML("\"result\":\"ok\"") || br2.getCookie(br2.getHost(), "user", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br2.getCookies(br2.getHost()), "");
                /* Ensure that we've accessed our target-URL. */
                if (!br._getURL().getPath().endsWith(loginURL.getPath())) {
                    br.getPage(loginURL);
                }
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedInHTML(final Browser br) {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, "https://www." + this.getHost() + "/myaccount/subscriptions", true);
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("(?i)<th>\\s*Rebill date\\s*:?\\s*</th>\\s*<td>([^<]+)</td>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMM yyyy", Locale.ENGLISH));
        }
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        String dllink = null;
        if (link.getPluginPatternMatcher().matches(PATTERN_VIDEO)) {
            dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        } else {
            dllink = link.getPluginPatternMatcher();
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            logger.warning("The final dllink seems not to be a file!");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken media file?");
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}