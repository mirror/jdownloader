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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "peekvids.com" }, urls = { "https?://(?:www\\.)?peekvids\\.com/(?:watch\\?v=|v/)([A-Za-z0-9\\-_]+)(?:/\\w+)?" })
public class PeekVidsCom extends PluginForHost {
    public PeekVidsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://accounts.playvid.com/peekvids/join");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    private String            dllink   = null;
    private long              filesize = 0;
    private static AtomicLong time     = new AtomicLong();

    @Override
    public String getAGBLink() {
        return "https://www.peekvids.com/terms.html";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        /* Forced https */
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
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
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this);
        return this.requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        synchronized (time) {
            try {
                final long passedTime = ((System.currentTimeMillis() - time.get()) / 1000) - 1;
                if (passedTime < 15) {
                    Thread.sleep(((15 - passedTime) + new Random().nextInt(5)) * 1000l);
                }
                // final String[] qualities = { "1080p", "720p", "480p", "360p", "240p" };
                final String[] qualities = { "1080", "720", "480", "360", "240" };
                this.setBrowserExclusive();
                br.setFollowRedirects(true);
                br.addAllowedResponseCodes(410, 429);
                final Request getRequest = br.createGetRequest(link.getDownloadURL());
                if (account != null) {
                    logger.info("Account available --> Logging in");
                    try {
                        this.login(account, false);
                    } catch (final Throwable e) {
                        logger.warning("Failed to login");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else {
                    logger.info("No account available --> Continuing without account");
                }
                br.getPage(getRequest);
                int i = 0;
                while (br.getHttpConnection().getResponseCode() == 429) {
                    logger.info("Bot protection triggered --> Captcha required");
                    if (!isDownload) {
                        /* Don't handle captcha during availablecheck */
                        return AvailableStatus.UNCHECKABLE;
                    }
                    if (i > 3) {
                        return AvailableStatus.UNCHECKABLE;
                    }
                    i++;
                    // captcha event
                    final Form captcha = br.getForm(0);
                    if (captcha == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                    } else {
                        final String img = captcha.getRegex("<img\\s+[^>]*src=\"(.*?)\"").getMatch(0);
                        if (img == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final String code = getCaptchaCode(img, link);
                        captcha.put("secimginp", Encoding.urlEncode(code));
                    }
                    br.submitForm(captcha);
                }
                if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Video not found<|class=\"play\\-error\"|This video was (deleted|removed)")) {
                    // filename can be present with offline links, so lets set it!
                    String filename = br.getRegex("<h2>((?!Related Videos).*?)</h2>").getMatch(0);
                    if (filename != null) {
                        filename = Encoding.htmlDecode(filename);
                        filename = filename.trim();
                        filename = encodeUnicode(filename);
                        link.setFinalFileName(filename + ".mp4");
                    }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String filename = br.getRegex("<title>\\s*([^<>]+?)( - PeekVids)?\\s*</title>").getMatch(0);
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
                                    link.setDownloadSize(filesize);
                                }
                                break;
                            }
                        }
                    }
                }
                if (dllink == null && counter == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (filename == null) {
                    filename = this.getFID(link);
                }
                filename = Encoding.htmlDecode(filename);
                filename = filename.trim();
                filename = encodeUnicode(filename);
                if (dllink == null) {
                    /* Download not possible at this moment. */
                    link.setName(filename + ".mp4");
                    return AvailableStatus.TRUE;
                }
                dllink = Encoding.htmlDecode(dllink);
                final String ext = ".mp4";
                if (!filename.endsWith(ext)) {
                    filename += ext;
                }
                link.setFinalFileName(filename);
                return AvailableStatus.TRUE;
                /* Don't check filesize here as this can lead to server errors */
            } finally {
                time.set(System.currentTimeMillis());
            }
        }
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
        return true;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        doFree(link, true, 0, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (dllink == null) {
            /* Very rare case! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        }
        if (dllink.contains(".m3u8")) {
            /* HLS download - new since 2020-04-22 */
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            /* http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("https://accounts.playvids.com/de/login/peekvids", "remember_me=on&back_url=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final String status = PluginJSonUtils.getJsonValue(br, "status");
                final String redirect = PluginJSonUtils.getJsonValue(br, "redirect");
                if (!"ok".equals(status)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (redirect == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage(redirect);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* free accounts can still have captcha */
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        doFree(link, true, 0, "account_free_directlink");
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
