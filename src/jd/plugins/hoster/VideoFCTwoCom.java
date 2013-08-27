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

import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.fc2.com" }, urls = { "http://video\\.fc2\\.com(/[a-z]{2})?(/a)?/content(/.*?)?/\\w+" }, flags = { 2 })
public class VideoFCTwoCom extends PluginForHost {

    private String        finalURL = null;
    private String        MAINPAGE = "http://video.fc2.com/";
    private static Object LOCK     = new Object();

    public VideoFCTwoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fc2.com");
    }

    @Override
    public String getAGBLink() {
        return "http://help.fc2.com/common/tos/en/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            String key = cookieEntry.getKey();
                            String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getHeaders().put("Cache-Control", null);
                br.getHeaders().put("Pragma", null);
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                br.getHeaders().put("Referer", "http://fc2.com/en/login.php?ref=video");
                br.setCookie(br.getHost(), "language", "en");
                br.postPage("https://secure.id.fc2.com/index.php?mode=login&switch_language=en", "email=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&done=&image.x=" + (int) (200 * Math.random() + 1) + "&image.y=" + (int) (47 * Math.random() + 1) + "&done=video");
                String loginDone = br.getRegex("(http://id\\.fc2\\.com/\\?mode=redirect&login=done)").getMatch(0);
                if (loginDone == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.getPage(loginDone);
                // Save cookies
                HashMap<String, String> cookies = new HashMap<String, String>();
                Cookies add = this.br.getCookies(MAINPAGE);
                for (Cookie c : add.getCookies()) {
                    if ("login_status".equals(c.getKey()) || "secure_check_fc2".equals(c.getKey()) || "Max-Age".equals(c.getKey()) || "glgd_val".equals(c.getKey())) continue;
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
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
        if (expire == null) {
            if (br.containsHTML("Free Member")) {
                ai.setValidUntil(-1);
                ai.setStatus("Registered (free) User");
            } else {
                account.setValid(false);
                return ai;
            }
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", null));
            ai.setStatus("Premium User");
        }
        account.setValid(true);

        return ai;
    }

    private void dofree(DownloadLink downloadLink) throws Exception {
        String error = br.getRegex("^err_code=(\\d+)$").getMatch(0);
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
        if (finalURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (onlyForPremiumUsers(downloadLink)) {
            if (downloadLink.getDownloadSize() > 0) throw new PluginException(LinkStatus.ERROR_FATAL, "This is a sample video. Full video is only downloadable for Premium Users!");
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for Premium Users!");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("not found")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
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
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        login(account, false);
        requestFileInformation(downloadLink);
        dofree(downloadLink);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        return requestVideo(downloadLink);
    }

    private AvailableStatus requestVideo(DownloadLink downloadLink) throws Exception {
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (br.containsHTML("This content has already been deleted") || br.getURL().contains("/err.php") || br.getURL().contains("/404.php")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<title>.*?◎?(.*?) \\-").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("title=\".*?◎([^\"]+)").getMatch(0);
        }

        if (dllink.endsWith("/")) dllink = dllink.substring(0, dllink.length() - 1);
        String upid = dllink.substring(dllink.lastIndexOf("/") + 1);
        String gk = getKey();
        if (filename == null || upid == null || gk == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        filename = filename.replaceAll("\\p{Z}", " ");
        filename = filename.replaceAll("[\\.\\d]{3,}$", "");
        filename = filename.trim();
        filename = filename.replaceAll("(:|,|\\s)", "_");
        filename = filename + (new Regex(filename, "\\.[0-9A-Za-z]{2,5}$").matches() ? "" : ".mp4");

        downloadLink.setFinalFileName(Encoding.htmlDecode(filename));

        /* get url */
        downloadLink.setProperty("ONLYFORPREMIUM", false);
        br.getPage("/ginfo.php?otag=0&tk=null&href=" + Encoding.urlEncode(dllink).replaceAll("\\.", "%2E") + "&upid=" + upid + "&gk=" + gk + "&fversion=" + Encoding.urlEncode("WIN 11,1,102,62").replaceAll("\\+", "%20") + "&playid=null&lang=en&playlistid=null&mimi=" + getMimi(upid) + "&v=" + upid);

        if (br.containsHTML("err_code=403")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        br.getHeaders().put("Referer", null);
        String error = br.getRegex("^err_code=(\\d+)$").getMatch(0);
        if (br.getRegex("\\&charge_second=\\d+").matches()) error = "603";
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
        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(finalURL);
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
        return AvailableStatus.TRUE;
    }

    private String getMimi(String s) {
        return JDHash.getMD5(s + "_" + "gGddgPfeaf_gzyr");
    }

    private String getKey() {
        String javaScript = br.getRegex("eval(\\(f.*?)[\r\n]+").getMatch(0);
        if (javaScript == null) javaScript = br.getRegex("(var __[0-9a-zA-Z]+ = \'undefined\'.*?\\})[\r\n]+\\-\\->").getMatch(0);
        if (javaScript == null) return null;
        Object result = new Object();
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        Invocable inv = (Invocable) engine;
        try {
            if (!javaScript.startsWith("var")) engine.eval(engine.eval(javaScript).toString());
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
            if (t != null && h != null) finalURL = finalURL + "&px-time=" + t + "&px-hash=" + h;
        }
    }

    private boolean onlyForPremiumUsers(DownloadLink downloadLink) {
        return downloadLink.getBooleanProperty("ONLYFORPREMIUM", false);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}