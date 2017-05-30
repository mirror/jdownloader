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
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.fc2.com" }, urls = { "http://(?:video\\.fc2\\.com|xiaojiadianvideo\\.asia)/((?:[a-z]{2}/)?(?:a/)?flv2\\.swf\\?i=|(?:[a-z]{2}/)?(?:a/)?content/)\\w+" })
public class VideoFCTwoCom extends PluginForHost {

    public VideoFCTwoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fc2.com");
        setConfigElements();
    }

    private final String  cookieHost            = "fc2.com";
    private String        finalURL              = null;
    private final boolean fastLinkCheck_default = true;
    private final String  fastLinkCheck         = "fastLinkCheck";
    private Account       account               = null;
    private static Object LOCK                  = new Object();

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), fastLinkCheck, JDL.L("plugins.hoster.videofcttwocom.fastlinkcheck", "Enable fast linkcheck, doesn't perform filesize checks! Filesize will be updated when download starts.")).setDefaultValue(fastLinkCheck_default));
    }

    private final static AtomicReference<String> userAgent = new AtomicReference<String>(null);

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

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://video.fc2.com/en/content/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0) + "/");
    }

    /*
     * IMPORTANT NOTE: Free (unregistered) Users can watch (&download) videos up to 2 hours in length - if videos are longer, users can only
     * watch the first two hours of them - afterwards they will get this message: http://i.snag.gy/FGl1E.jpg
     */
    @SuppressWarnings("deprecation")
    private AccountInfo login(Account account, boolean force, final AccountInfo iai) throws Exception {
        synchronized (LOCK) {
            this.account = account;
            final AccountInfo ai = iai != null ? iai : account.getAccountInfo();
            try {
                // Load cookies
                prepareBrowser(br);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return ai;
                }
                // for debug purposes
                // br = prepareBrowser(new Browser());
                final boolean ifr = br.isFollowingRedirects();
                br.setFollowRedirects(true);
                br.getPage("http://fc2.com/en/login.php");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                // Thread.sleep(4000l);
                br.postPage("https://secure.id.fc2.com/index.php?mode=login&switch_language=en", "email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&image.x=" + (int) (200 * Math.random() + 1) + "&image.y=" + (int) (47 * Math.random() + 1) + "&keep_login=1&done=");
                String loginDone = br.getRegex("(http://id\\.fc2\\.com/.*?\\?.*?login=done.*?)").getMatch(0);
                if (loginDone == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage(loginDone);
                br.getPage("http://id.fc2.com");
                final String userinfo = br.getRegex("(http://video\\.fc2\\.com(/[a-z]{2})?/mem_login\\.php\\?uid=[^\"]+)").getMatch(0);
                if (userinfo != null) {
                    br.getPage(userinfo);
                }
                ai.setUnlimitedTraffic();
                String expire = br.getRegex("premium till (\\d{2}/\\d{2}/\\d{2})").getMatch(0);
                if (expire != null) {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", null));
                    ai.setStatus("Premium Account");
                    account.setProperty("free", false);
                } else if (br.containsHTML(">Paying Member</span>|>Type</li><li[^>]*>Premium Member</li>")) {
                    ai.setStatus("Premium Account");
                    account.setProperty("free", false);
                } else {
                    ai.setValidUntil(-1);
                    ai.setStatus("Free Account");
                    account.setProperty("free", true);
                }
                account.setValid(true);
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                br.setFollowRedirects(ifr);
            } catch (PluginException e) {
                account.clearCookies("");
                throw e;
            }
            return ai;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        return login(account, true, new AccountInfo());
    }

    private void dofree(final DownloadLink downloadLink) throws Exception {
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
        dofree(downloadLink);
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        br = new Browser();
        login(account, true, null);
        requestFileInformation(downloadLink);
        dofree(downloadLink);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
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
                            login(n, true, null);
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
            }
        }
        if (account == null) {
            // no accounts prepareBrowser wouldn't have happened!
            prepareBrowser(br);
        }
        br.setFollowRedirects(true);
        br.getPage(dllink);
        // capturing the title in this manner reduces lazy regex scope to just this found string vs entire document.
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (filename != null) {
            filename = new Regex(filename, ".*?◎?(.*?) \\-.*?").getMatch(0);
        }
        if (filename == null || filename.isEmpty() || filename.matches("[\\s\\p{Z}]+")) {
            filename = br.getRegex("title=\".*?◎([^\"]+)").getMatch(0);
        }

        if (dllink.endsWith("/")) {
            dllink = dllink.substring(0, dllink.length() - 1);
        }
        String upid = dllink.substring(dllink.lastIndexOf("/") + 1);
        String gk = getKey();
        if (upid == null || gk == null) {
            // quite a few of these patterns are too generic, 'this content... is now in javascript variable. errmsg span is also present in
            // ALL pages just doesn't contain text when not valid...
            if (br.containsHTML("This content has already been deleted") || br.getURL().contains("/err.php") || br.getURL().contains("/404.php") || br.containsHTML("class=\"errmsg\"") || br.getURL().endsWith("://video.fc2.com/")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }

        /* get url */
        downloadLink.setProperty("ONLYFORPREMIUM", false);
        final String from = br.getRegex("\\&from=(\\d+)\\&").getMatch(0);
        final String tk = br.getRegex("\\&tk=([A-Za-z0-9]*?)\\&").getMatch(0);
        final String version = "WIN%2015%2C0%2C0%2C189";
        final String encodedlink = Encoding.urlEncode(br.getURL()).replaceAll("\\.", "%2E").replaceFirst("%2F$", "");
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Charset", null);
        /* Extra step is only needed for premium accounts. */
        if (account != null && !account.getBooleanProperty("free", true)) {
            if (tk == null || from == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("/ginfo_payment.php?mimi=" + getMimi(upid) + "&upid=" + upid + "&gk=" + gk + "&tk=" + tk + "&from=" + from + "&href=" + encodedlink + "&lang=en&v=" + upid + "&fversion=" + version + "&otag=0");
        } else {
            br.getPage("/ginfo.php?otag=0&tk=null&href=" + encodedlink + "&upid=" + upid + "&gk=" + gk + "&fversion=" + version + "&playid=null&lang=en&playlistid=null&mimi=" + getMimi(upid) + "&v=" + upid);
        }
        if (br.getHttpConnection() == null) {
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else if ("23764902a26fbd6345d3cc3533d1d5eb".equalsIgnoreCase(JDHash.getMD5(br.toString()))) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String error = br.getRegex("^err_code=(\\d+)").getMatch(0);
        if (br.getRegex("\\&charge_second=\\d+").matches()) {
            error = "603";
        }
        AvailableStatus aError = null;
        if (error != null) {
            switch (Integer.parseInt(error)) {
            case 403:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 503:
                // :-)
                break;
            case 601:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                aError = AvailableStatus.TRUE;
            case 602:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                aError = AvailableStatus.TRUE;
            case 603:
                downloadLink.setProperty("ONLYFORPREMIUM", true);
                break;
            default:
                logger.info("video.fc2.com: Unknown error code: " + error);
                aError = AvailableStatus.UNCHECKABLE;
            }
        }

        // prevent NPE
        if (filename != null) {
            filename = filename.replaceAll("\\p{Z}", " ");
            // why do we do this?? http://board.jdownloader.org/showthread.php?p=304933#post304933
            // filename = filename.replaceAll("[\\.\\d]{3,}$", "");
            filename = filename.trim();
            filename = filename.replaceAll("(:|,|\\s)", "_");
            filename = filename + (new Regex(filename, "\\.[0-9A-Za-z]{2,5}$").matches() ? "" : ".mp4");
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename));
        }
        // return aError
        if (aError != null) {
            return aError;
        }

        br.getHeaders().put("Referer", null);

        finalURL = br.getRegex("filepath=(https?://.*?)$").getMatch(0);
        prepareFinalLink();
        if (finalURL == null) {
            logger.warning("video.fc2.com: Final downloadlink equals null. Error code: " + error);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.getPluginConfig().getBooleanProperty(fastLinkCheck, fastLinkCheck_default)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(finalURL);
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
        ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
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
     */
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