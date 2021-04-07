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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tumblr.com" }, urls = { "http://[\\w\\.\\-]*?tumblrdecrypted\\.com/post/\\d+" })
public class TumblrCom extends PluginForHost {
    public static final long trust_cookie_age = 300000l;
    private String           dllink           = null;

    public TumblrCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.tumblr.com/register");
    }

    @Override
    public String getAGBLink() {
        return "http://www.tumblr.com/terms_of_service";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        // Links come from a decrypter
        link.setUrlDownload(link.getDownloadURL().replace("tumblrdecrypted.com/", "tumblr.com/"));
    }

    private void getDllink() throws IOException {
        br.setFollowRedirects(false);
        dllink = br.getRegex("\"><img src=\"(( +)?https?://\\d+\\.media\\.tumblr\\.com/[^<>\"/\\']*?\\.jpg)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(( +)?https?://\\d+\\.media\\.tumblr\\.com/[^<>\"/\\']*?\\.(jpg|gif|png))\"").getMatch(0);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String ADDITION        = "P3BsZWFkPXBsZWFzZS1kb250LWRvd25sb2FkLXRoaXMtb3Itb3VyLWxhd3llcnMtd29udC1sZXQtdXMtaG9zdC1hdWRpbw==";
    /* API docs: https://www.tumblr.com/docs/en/api/v2#what-you-need */
    public static final String  API_BASE        = "https://www.tumblr.com/api/v2";
    private static final String PROPERTY_APIKEY = "apikey";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            /* Login whenever possible to be able to download account-only-stuff */
            login(aa, false);
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The URL you requested could not be found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = checkDirectLink(downloadLink, "audiodirectlink");
        if (dllink != null) {
            dllink += Encoding.Base64Decode(ADDITION);
        } else {
            String filename = downloadLink.getFinalFileName();
            if (filename == null) {
                filename = new Regex(br.getURL(), "tumblr\\.com/post/\\d+/(.+)").getMatch(0);
            }
            if (filename == null) {
                filename = new Regex(downloadLink.getDownloadURL(), "tumblr\\.com/post/(\\d+)").getMatch(0);
            }
            filename = filename.trim();
            if (br.containsHTML(">renderVideo\\(")) {
                dllink = br.getRegex("\\'(https?://[^<>\"/]*?\\.tumblr\\.com/video_file/\\d+/[^<>\"/]*?)\\'").getMatch(0);
                downloadLink.setFinalFileName(filename + ".mp4");
            } else if (br.containsHTML("class=\"audio_player\"")) {
                dllink = br.getRegex("\\?audio_file=(http[^<>\"]*?)\\&color").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = Encoding.htmlDecode(dllink.trim()) + Encoding.Base64Decode(ADDITION);
            } else {
                getDllink();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = Encoding.htmlDecode(dllink.trim());
                final String ext = getFileNameExtensionFromString(dllink, ".jpg");
                downloadLink.setFinalFileName(filename + ext);
            }
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass());
                String apikey = account.getStringProperty(PROPERTY_APIKEY);
                if (cookies != null && apikey != null) {
                    br.setCookies(account.getHoster(), cookies);
                    br.getHeaders().put("Authorization", "Bearer " + apikey);
                    if (!force) {
                        logger.info("Trust cookies without check");
                        return;
                    } else {
                        /* Check cookies */
                        br.getHeaders().put("Authorization", "Bearer " + apikey);
                        br.getPage(API_BASE + "/user/info?fields%5Bblogs%5D=avatar%2Cname%2Ctitle%2Curl%2Ccan_message%2Cdescription%2Cis_adult%2Cuuid%2Cis_private_channel%2Cposts%2Cis_group_channel%2C%3Fprimary%2C%3Fadmin%2C%3Fdrafts%2C%3Ffollowers%2C%3Fqueue%2C%3Fhas_flagged_posts%2Cmessages%2Cask%2C%3Fcan_submit%2C%3Ftweet%2Cmention_key%2C%3Ftimezone_offset%2C%3Fanalytics_url%2C%3Fis_premium_partner%2C%3Fis_blogless_advertiser%2C%3Fcan_onboard_to_paywall%2C%3Fis_tumblrpay_onboarded%2C%3Fis_paywall_on%2C%3Flinked_accounts");
                        try {
                            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                            if (entries.containsKey("errors")) {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                            }
                            logger.info("Cookie login successful");
                            return;
                        } catch (final Throwable e) {
                            logger.info("Cookie login failed");
                        }
                        /* Delete cookies / Headers to perform a full login */
                        this.br.clearAll();
                    }
                }
                if (userCookies == null) {
                    showCookieLoginInformation();
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login required", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.setCookies(userCookies);
                br.getPage("https://www." + this.getHost() + "/dashboard");
                apikey = PluginJSonUtils.getJson(br, "API_TOKEN");
                if (StringUtils.isEmpty(apikey)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getHeaders().put("Authorization", "Bearer " + apikey);
                br.getPage(API_BASE + "/user/info?fields%5Bblogs%5D=avatar%2Cname%2Ctitle%2Curl%2Ccan_message%2Cdescription%2Cis_adult%2Cuuid%2Cis_private_channel%2Cposts%2Cis_group_channel%2C%3Fprimary%2C%3Fadmin%2C%3Fdrafts%2C%3Ffollowers%2C%3Fqueue%2C%3Fhas_flagged_posts%2Cmessages%2Cask%2C%3Fcan_submit%2C%3Ftweet%2Cmention_key%2C%3Ftimezone_offset%2C%3Fanalytics_url%2C%3Fis_premium_partner%2C%3Fis_blogless_advertiser%2C%3Fcan_onboard_to_paywall%2C%3Fis_tumblrpay_onboarded%2C%3Fis_paywall_on%2C%3Flinked_accounts");
                try {
                    final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    if (entries.containsKey("errors")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } catch (final Throwable e) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
                account.setProperty(PROPERTY_APIKEY, apikey);
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private Thread showCookieLoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String help_article_url = "https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions";
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Tumblr.com - Login";
                        message += "Hallo liebe(r) Tumblr NutzerIn\r\n";
                        message += "Um deinen Tumblr Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "Folge der Anleitung im Hilfe-Artikel:\r\n";
                        message += help_article_url;
                    } else {
                        title = "Tumblr.com - Login";
                        message += "Hello dear Tumblr user\r\n";
                        message += "In order to use an account of this service in JDownloader, you need to follow these instructions:\r\n";
                        message += help_article_url;
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(help_article_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "response/user");
        /* User could enter any name as username during cookie login -> Fixed this -> Make sure that name is unique */
        account.setUser((String) entries.get("name"));
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* No need to login here - we're already logged in! */
        doFree(link);
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