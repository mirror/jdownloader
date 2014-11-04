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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.fc2.com" }, urls = { "http://video\\.fc2\\.com(/flv2\\.swf\\?i=|(/[a-z]{2})?(/a)?/content/)\\w+" }, flags = { 2 })
public class VideoFCTwoCom extends PluginForHost {

    private final String  cookieHost = "fc2.com";

    private static Object LOCK       = new Object();
    private String        finalURL   = null;

    public VideoFCTwoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fc2.com");
        setConfigElements();
    }

    private final boolean fastLinkCheck_default = true;
    private final String  fastLinkCheck         = "fastLinkCheck";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), fastLinkCheck, JDL.L("plugins.hoster.videofcttwocom.fastlinkcheck", "Force secure communication requests via 'https' over SSL/TLS")).setDefaultValue(fastLinkCheck_default));
    }

    private static final AtomicReference<String> userAgent = new AtomicReference<String>(null);

    private Browser prepareBrowser(Browser prepBr) {
        if (prepBr == null) {
            prepBr = new Browser();
        }
        if (userAgent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", userAgent.get());
        prepBr.setCookie(cookieHost, "language", "en");
        prepBr.setCustomCharset("utf-8");
        return prepBr;
    }

    @Override
    public String getAGBLink() {
        return "http://help.fc2.com/common/tos/en/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://video.fc2.com/en/content/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0) + "/");
    }

    /*
     * IMPORTANT NOTE: Free (unregistered) Users can watch (&download) videos up to 2 hours in length - if videos are longer, users can only
     * watch the first two hours of them - afterwards they will get this message: http://i.snag.gy/FGl1E.jpg
     */
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepareBrowser(br);
                Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            String key = cookieEntry.getKey();
                            String value = cookieEntry.getValue();
                            this.br.setCookie(cookieHost, key, value);
                        }
                        return;
                    }
                }
                // for debug purposes
                // br = prepareBrowser(new Browser());
                final boolean ifr = br.isFollowingRedirects();
                br.setFollowRedirects(true);
                br.getPage("http://fc2.com/en/login.php");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                br.postPage("https://secure.id.fc2.com/index.php?mode=login&switch_language=en", "email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&image.x=" + (int) (200 * Math.random() + 1) + "&image.y=" + (int) (47 * Math.random() + 1) + "&image=Log+in&keep_login=1&done=video");
                String loginDone = br.getRegex("(http://id\\.fc2\\.com/\\?.*?login=done.*?)").getMatch(0);
                if (loginDone == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage(loginDone);
                // Save cookies
                HashMap<String, String> cookies = new HashMap<String, String>();
                Cookies add = this.br.getCookies(cookieHost);
                for (Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                br.setFollowRedirects(ifr);
            } catch (PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://id.fc2.com");
        String userinfo = br.getRegex("(http://video\\.fc2\\.com(/[a-z]{2})?/mem_login\\.php\\?uid=[^\"]+)").getMatch(0);
        if (userinfo == null) {
            account.setValid(false);
            return ai;
        }
        br.getPage(userinfo);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("premium till (\\d{2}/\\d{2}/\\d{2})").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", null));
            ai.setStatus("Premium User");
            account.setProperty("free", false);
        } else if (br.containsHTML(">Paying Member</span>|>Type</li><li[^>]*>Premium Member</li>")) {
            ai.setStatus("Premium User");
            account.setProperty("free", false);
        } else if (br.containsHTML("Free Member")) {
            ai.setValidUntil(-1);
            ai.setStatus("Registered (free) User");
            account.setProperty("free", true);
        } else {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);

        return ai;
    }

    private void dofree(final DownloadLink downloadLink, final Account account) throws Exception {
        String error = br.getRegex("^err_code=(\\d+)").getMatch(0);
        if (error != null) {
            switch (Integer.parseInt(error)) {
            case 503:
                // :-)
                break;
            case 601:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading");
            case 602:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading");
            case 603:
                downloadLink.setProperty("ONLYFORPREMIUM", true);
                break;
            default:
                logger.info("video.fc2.com: Unknown error code: " + error);
            }
        }
        if (finalURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (onlyForPremiumUsers(downloadLink)) {
            if (account != null && !account.getBooleanProperty("free", false)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "premium only requirement, when premium account has been used!");
            }
            if (downloadLink.getDownloadSize() > 0) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This is a sample video. Full video is only downloadable for Premium Users!");
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for Premium Users!");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, -4);
        if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 503 && requestHeadersHasKeyNValueContains(br, "server", "nginx")) {
            throw new PluginException(LinkStatus.ERROR_RETRY, "Service unavailable. Try again later.", 5 * 60 * 1000l);
        } else if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("not found")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dofree(downloadLink, null);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        br = new Browser();
        login(account, false);
        requestFileInformation(downloadLink, account);
        dofree(downloadLink, account);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink, final Account iAccount) throws Exception {
        Account account = iAccount;
        br.setFollowRedirects(true);
        String dllink = downloadLink.getDownloadURL();
        // this comes first, due to subdoman issues and cached cookie etc.
        if (account == null) {
            // check for accounts
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
            if (accounts != null && accounts.size() != 0) {
                // lets sort, premium over non premium
                Collections.sort(accounts, new Comparator<Account>() {
                    @Override
                    public int compare(final Account o1, final Account o2) {
                        final int io1 = o1.getBooleanProperty("free", false) ? 0 : 1;
                        final int io2 = o2.getBooleanProperty("free", false) ? 0 : 1;
                        return io1 <= io2 ? io1 : io2;
                    }
                });
                final Iterator<Account> it = accounts.iterator();
                while (it.hasNext()) {
                    Account n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        try {
                            // because this site uses subdomain it causes all sorts of issues!
                            login(n, false);
                            account = n;
                            break;
                        } catch (final PluginException p) {
                            if (it.hasNext()) {
                                br = new Browser();
                                continue;
                            }
                        }
                    }
                }
            } else {
                // no accounts prepareBrowser wouldn't have happened!
                prepareBrowser(br);
            }
        }
        br.getPage(dllink);
        String filename = br.getRegex("<title>.*?◎?(.*?) -").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("title=\".*?◎([^\"]+)").getMatch(0);
        }

        if (dllink.endsWith("/")) {
            dllink = dllink.substring(0, dllink.length() - 1);
        }
        String upid = dllink.substring(dllink.lastIndexOf("/") + 1);
        String gk = getKey();
        if (filename == null || upid == null || gk == null) {
            // quite a few of these patterns are too generic, 'this content... is now in javascript variable. errmsg span is also present in
            // ALL pages just doesn't contain text when not valid...
            if (br.containsHTML("This content has already been deleted") || br.getURL().contains("/err.php") || br.getURL().contains("/404.php") || br.containsHTML("class=\"errmsg\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }

        filename = filename.replaceAll("\\p{Z}", " ");
        // why do we do this?? http://board.jdownloader.org/showthread.php?p=304933#post304933
        // filename = filename.replaceAll("[\\.\\d]{3,}$", "");
        filename = filename.trim();
        filename = filename.replaceAll("(:|,|\\s)", "_");
        filename = filename + (new Regex(filename, "\\.[0-9A-Za-z]{2,5}$").matches() ? "" : ".mp4");

        downloadLink.setFinalFileName(Encoding.htmlDecode(filename));

        /* get url */
        downloadLink.setProperty("ONLYFORPREMIUM", false);
        final String from = br.getRegex("\\&from=(\\d+)\\&").getMatch(0);
        final String tk = br.getRegex("\\&tk=([A-Za-z0-9]*?)\\&").getMatch(0);
        final String version = "WIN%2015%2C0%2C0%2C189";
        final String encodedlink = Encoding.urlEncode(br.getURL()).replaceAll("\\.", "%2E").replaceFirst("%2F$", "");
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Charset", null);
        if (account != null) {
            if (tk == null || from == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("/ginfo_payment.php?mimi=" + getMimi(upid) + "&upid=" + upid + "&gk=" + gk + "&tk=" + tk + "&from=" + from + "&href=" + encodedlink + "&lang=en%2F&v=" + upid + "&fversion=" + version + "&otag=0");
        } else {
            br.getPage("/ginfo.php?otag=0&tk=null&href=" + encodedlink + "&upid=" + upid + "&gk=" + gk + "&fversion=" + version + "&playid=null&lang=en&playlistid=null&mimi=" + getMimi(upid) + "&v=" + upid);
        }

        if (br.containsHTML("err_code=403")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        br.getHeaders().put("Referer", null);
        String error = br.getRegex("^err_code=(\\d+)").getMatch(0);
        if (br.getRegex("\\&charge_second=\\d+").matches()) {
            error = "603";
        }
        if (error != null) {
            switch (Integer.parseInt(error)) {
            case 503:
                // :-)
                break;
            case 601:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                return AvailableStatus.TRUE;
            case 602:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                return AvailableStatus.TRUE;
            case 603:
                downloadLink.setProperty("ONLYFORPREMIUM", true);
                break;
            default:
                logger.info("video.fc2.com: Unknown error code: " + error);
                return AvailableStatus.UNCHECKABLE;
            }
        }
        finalURL = br.getRegex("filepath=(http://.*?)$").getMatch(0);
        prepareFinalLink();
        if (finalURL == null) {
            logger.warning("video.fc2.com: Final downloadlink equals null. Error code: " + error);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.getPluginConfig().getBooleanProperty(fastLinkCheck, fastLinkCheck_default)) {
            URLConnectionAdapter con = null;
            try {
                if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
                    con = br.openHeadConnection(finalURL);
                } else {
                    con = br.openGetConnection(finalURL);
                }
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getMimi(String s) {
        return JDHash.getMD5(s + "_" + "gGddgPfeaf_gzyr");
    }

    private String getKey() {
        String javaScript = br.getRegex("eval(\\(f.*?)[\r\n]+").getMatch(0);
        if (javaScript == null) {
            javaScript = br.getRegex("(var __[0-9a-zA-Z]+ = \'undefined\'.*?\\})[\r\n]+\\-\\->").getMatch(0);
        }
        if (javaScript == null) {
            return null;
        }
        Object result = new Object();
        ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
        ScriptEngine engine = manager.getEngineByName("javascript");
        Invocable inv = (Invocable) engine;
        try {
            if (!javaScript.startsWith("var")) {
                engine.eval(engine.eval(javaScript).toString());
            }
            engine.eval(javaScript);
            engine.eval("var window = new Object();");
            result = inv.invokeFunction("getKey");
        } catch (Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

    private void prepareFinalLink() {
        if (finalURL != null) {
            finalURL = finalURL.replaceAll("\\&mid=", "?mid=");
            String t = new Regex(finalURL, "cdnt=(\\d+)").getMatch(0);
            String h = new Regex(finalURL, "cdnh=([0-9a-f]+)").getMatch(0);
            finalURL = new Regex(finalURL, "(.*?)\\&sec=").getMatch(0);
            if (t != null && h != null) {
                finalURL = finalURL + "&px-time=" + t + "&px-hash=" + h;
            }
        }
    }

    private boolean onlyForPremiumUsers(DownloadLink downloadLink) {
        return downloadLink.getBooleanProperty("ONLYFORPREMIUM", false);
    }

    /**
     * If import Browser request headerfield contains key of k && key value of v
     *
     * @author raztoki
     * */
    private boolean requestHeadersHasKeyNValueContains(final Browser ibr, final String k, final String v) {
        if (k == null || v == null || ibr == null || ibr.getHttpConnection() == null) {
            return false;
        }
        if (ibr.getHttpConnection().getHeaderField(k) != null && ibr.getHttpConnection().getHeaderField(k).toLowerCase(Locale.ENGLISH).contains(v.toLowerCase(Locale.ENGLISH))) {
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}