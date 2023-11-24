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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "peekvids.com" }, urls = { "https?://(?:www\\.)?peekvids\\.com/(?:watch\\?v=|v/)([A-Za-z0-9\\-_]+)(?:/\\w+)?" })
public class PeekVidsCom extends PluginForHost {
    public PeekVidsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://accounts.playvid.com/peekvids/join");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        /* 2021-07-30: Up2date User-Agent --> Less rate-limit captchas (??) */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36");
        br.setFollowRedirects(true);
        br.addAllowedResponseCodes(410, 429);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 5000);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    private String                            dllink      = null;
    private long                              filesize    = 0;
    private static HashMap<String, Cookies>   cookies     = new HashMap<String, Cookies>();
    /* Don't touch the following! */
    private static Map<String, AtomicInteger> freeRunning = new HashMap<String, AtomicInteger>();

    @Override
    public String getAGBLink() {
        return "https://www.peekvids.com/terms.html";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    /**
     * Free account = HD (720p) versions are (sometimes) available.
     *
     * @throws Exception
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this);
        return this.requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + extDefault);
        }
        this.setBrowserExclusive();
        synchronized (cookies) {
            if (cookies.containsKey(this.getHost())) {
                br.setCookies(cookies.get(this.getHost()));
            }
        }
        final String[] qualities = { "1080", "720", "480", "360", "240" };
        if (account != null) {
            logger.info("Account available --> Logging in");
            this.login(account, false);
        } else {
            logger.info("No account available --> Continuing without account");
        }
        br.getPage(link.getPluginPatternMatcher());
        synchronized (cookies) {
            int retryNumber = 0;
            boolean trustCaptchaAnswer = false;
            boolean saveNewCookies = false;
            while (br.getHttpConnection().getResponseCode() == 429) {
                logger.info("Bot protection triggered --> Captcha required");
                if (!isDownload) {
                    /* Don't handle captcha during availablecheck */
                    return AvailableStatus.UNCHECKABLE;
                } else if (retryNumber > 3 || trustCaptchaAnswer) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Rate Limit Exceeded and too many failed captcha attempts");
                }
                retryNumber++;
                // captcha event
                final Form captcha = br.getForm(0);
                if (captcha == null) {
                    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Rate Limit Exceeded and failed to process captcha");
                }
                if (captcha.containsHTML("recaptcha")) {
                    final String rcKey = br.getRegex("data-public-key=\"([^<>\"]+)\"").getMatch(0);
                    final String recaptchaV2Response;
                    if (rcKey != null) {
                        recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcKey).getToken();
                    } else {
                        recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    }
                    captcha.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    trustCaptchaAnswer = true;
                } else {
                    final String img = captcha.getRegex("<img\\s+[^>]*src=\"(.*?)\"").getMatch(0);
                    if (img == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String code = getCaptchaCode(img, link);
                    captcha.put("secimginp", Encoding.urlEncode(code));
                }
                br.submitForm(captcha);
                saveNewCookies = true;
            }
            /* Passed bot protection? Save cookies! */
            if (saveNewCookies) {
                logger.info("Saving new cookies");
                /* These cookies are not necessarily required */
                br.setCookie(this.getHost(), "mediaPlayerMute", "0");
                br.setCookie(this.getHost(), "mediaPlayerVolume", "1");
                cookies.put(this.getHost(), this.br.getCookies(br.getHost()));
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Video not found<|class=\"play\\-error\"|This video was (deleted|removed)")) {
            // title can be present with offline links, so lets set it!
            String title = br.getRegex("<h2>((?!Related Videos).*?)</h2>").getMatch(0);
            if (title != null) {
                title = Encoding.htmlDecode(title);
                title = title.trim();
                link.setFinalFileName(title + extDefault);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>\\s*([^<>]+?)( - PeekVids)?\\s*</title>").getMatch(0);
        // String flashvars = br.getRegex("flashvars=\"(.*?)\"").getMatch(0);
        String flashvars = br.getRegex("(<video.*?</video>)").getMatch(0);
        if (flashvars == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        flashvars = Encoding.htmlDecode(flashvars);
        int counter = 0;
        for (final String quality : qualities) {
            // dllink = new Regex(flashvars, "\\[" + quality + "\\]=(http[^<>\"]*?)\\&").getMatch(0);
            dllink = new Regex(flashvars, "data-(?:hls-)?src" + quality + "=\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink != null) {
                counter++;
                if (dllink.contains(".m3u8")) {
                    /* Do not check hls URLs */
                    break;
                } else {
                    if (checkDirectLink()) {
                        if (filesize > 0) {
                            link.setVerifiedFileSize(filesize);
                        }
                        break;
                    }
                }
            }
        }
        if (dllink == null && counter == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            title += extDefault;
            link.setFinalFileName(title);
        }
        return AvailableStatus.TRUE;
    }

    private boolean checkDirectLink() {
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    return false;
                }
                filesize = con.getCompleteContentLength();
                return true;
            } catch (final Exception e) {
                if (e instanceof BrowserException) {
                    if (e.getCause() != null && e.getCause().toString().contains("Could not generate DH keypair")) {
                        dllink = dllink.replace("https://", "http://");
                        return checkDirectLink();
                    }
                }
                return false;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return false;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        doFree(link, null, true, 0, "free_directlink");
    }

    private void doFree(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (dllink == null) {
            /* Very rare case! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        }
        if (dllink.contains(".m3u8")) {
            /* HLS download - new since 2020-04-22 */
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
        } else {
            /* http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
        /* Add a download slot */
        controlMaxFreeDownloads(account, link, +1);
        try {
            /* start the dl */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxFreeDownloads(account, link, -1);
        }
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            final AtomicInteger freeRunning = getFreeRunning();
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    protected AtomicInteger getFreeRunning() {
        synchronized (freeRunning) {
            AtomicInteger ret = freeRunning.get(getHost());
            if (ret == null) {
                ret = new AtomicInteger(0);
                freeRunning.put(getHost(), ret);
            }
            return ret;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final int max = 20;
        final int running = getFreeRunning().get();
        final int ret = Math.min(running + 1, max);
        return ret;
    }

    private void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(cookies);
                    if (!validateCookies) {
                        logger.info("Trust cookies without checking");
                        return;
                    } else {
                        logger.info("Validating cookies...");
                        br.getPage("https://www." + this.getHost());
                        if (this.isLoggedIN(this.br)) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(br.getHost()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            br.clearCookies(br.getHost());
                        }
                    }
                }
                logger.info("Performing full login");
                br.setFollowRedirects(true);
                br.postPage("https://accounts.playvids.com/de/login/peekvids", "remember_me=on&back_url=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final String status = (String) entries.get("status");
                final String redirect = (String) entries.get("redirect");
                if (!"ok".equals(status)) {
                    throw new AccountInvalidException();
                } else if (StringUtils.isEmpty(redirect)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* 2021-07-30: Error 429 rate limit reached may happen here but then we're still logged in properly! */
                br.getPage(redirect);
                if (!this.isLoggedIN(this.br)) {
                    /* Double-check though it should not fail here! */
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        return br.containsHTML("/account/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(-1);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        doFree(link, account, true, 0, "account_free_directlink");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
