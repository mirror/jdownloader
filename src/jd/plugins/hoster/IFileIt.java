//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ifile.it" }, urls = { "http://[\\w\\.]*?ifile\\.it/[a-z0-9]+" }, flags = { 2 })
public class IFileIt extends PluginForHost {

    private final String        useragent           = RandomUserAgent.generate();

    /* must be static so all plugins share same lock */
    private static final Object LOCK                = new Object();

    private static final String CHALLENGEREGEX      = "challenge[ ]+:[ ]+\\'(.*?)\\',";
    private static final String SERVER              = "server[ ]+:[ ]+\\'(.*?)\\'";
    private static final String RECAPTCHPUBLICREGEX = "recaptcha_public.*?=.*?\\'(.*?)\\'";
    private static final String RECAPTCHAIMAGEPART  = "image?c=";
    private boolean             showDialog          = false;
    private boolean             RESUMING            = false;
    private int                 MAXSIMDOWNLOADS     = 1;

    public IFileIt(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://secure.ifile.it/account-signup.html");
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        br.setFollowRedirects(true);
        // generating first request
        final String c = br.getRegex("(var.*?eval.*?\r?\n)").getMatch(0);
        final String fnName = br.getRegex("url\\s+=\\s+([0-9a-z]+)\\(").getMatch(0);
        final String dec = br.getRegex(fnName + "\\( \'(.*?)\' \\)").getMatch(0);
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            if (fnName == null || dec == null) {
                engine.eval(c);
                result = engine.get("__rurl2");
            } else {
                engine.eval(c);
                result = inv.invokeFunction(fnName, dec);
            }
        } catch (final Throwable e) {
            result = "";
        }
        final String finaldownlink = result.toString();
        if (finaldownlink == null || finaldownlink.equals("") || !finaldownlink.startsWith("http")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final String downlink = br.getRegex("var _url\\s+\\t+=\\s+\\t+\'(.*?)\';").getMatch(0);
        String type = null, extra = null;
        if (downlink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        // Br2 is our xml browser now!
        final Browser br2 = br.cloneBrowser();
        br2.setReadTimeout(40 * 1000);
        xmlrequest(br2, finaldownlink, "");
        if (!br2.containsHTML("status\":\"ok\"")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        if (br2.containsHTML("download:captcha")) {
            // Old captcha handling
            for (int i = 0; i <= 5; i++) {
                final String captchashit = br.getRegex("url \\+=.*?\\+.*?\\+.*?\"(.*?)\"").getMatch(0);
                final String captchacrap = br.getRegex("var.*?x.*?c = '(.*?)'").getMatch(0);
                if (captchashit == null || captchacrap == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                final String code = getCaptchaCode("http://ifile.it/download:captcha?0." + Math.random(), downloadLink);
                type = "ctype=simple";
                extra = "&esn=1&" + captchacrap + "=" + Encoding.urlEncode_light(code) + "&" + captchashit;
                xmlrequest(br2, finaldownlink, type + extra);
                if (br2.containsHTML("\"retry\":\"retry\"")) {
                    continue;
                }
                break;
            }
        } else if (br2.containsHTML("\"captcha\":1")) {
            for (int i = 0; i <= 5; i++) {
                // Manuel Re Captcha handling
                final String k = br.getRegex(RECAPTCHPUBLICREGEX).getMatch(0);
                if (k == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                br2.getPage("http://api.recaptcha.net/challenge?k=" + k);
                final String challenge = br2.getRegex(CHALLENGEREGEX).getMatch(0);
                final String server = br2.getRegex(SERVER).getMatch(0);
                if (challenge == null || server == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                final String captchaAddress = server + RECAPTCHAIMAGEPART + challenge;
                final String code = getCaptchaCode(captchaAddress, downloadLink);
                type = "ctype=recaptcha";
                extra = "&recaptcha_response=" + Encoding.urlEncode_light(code) + "&recaptcha_challenge=" + challenge;
                xmlrequest(br2, finaldownlink, type + extra);
                if (br2.containsHTML("\"captcha\":1")) {
                    xmlrequest(br2, finaldownlink, type + extra);
                    continue;
                }
                break;
            }
        }
        if (br2.containsHTML("(\"retry\":\"retry\"|\"retry\":1)")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        if (br2.containsHTML("an error has occured while processing your request")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error"); }
        if (!br2.containsHTML("status\":\"ok\"")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage(downlink);
        try {
            br.getPage(downlink);
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        }
        String dllink = br.getRegex("req_btn.*?target=\".*?\" href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.info("first try getting dllink failed");
            dllink = br.getRegex("\"?(http://i[0-9]+\\.ifile\\.it/.*?/\\d+/.*?)\"").getMatch(0);
            if (dllink == null) {
                logger.info("second try getting dllink failed");
                final String pp = br.getRegex("<br /><br />(.*?)</div>").getMatch(0);
                final String[] lol = HTMLParser.getHttpLinks(pp, "");
                if (lol.length != 1) {
                } else {
                    for (final String link : lol) {
                        dllink = link;
                    }
                }
            }
        }
        if (dllink == null) {
            logger.info("last try getting dllink failed, plugin must be defect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, RESUMING, MAXSIMDOWNLOADS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        showDialog = true;
        try {
            loginAPI(account, ai);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        /* account info */
        br.postPage("http://ifile.it/api-fetch_account_info.api", "akey=" + account.getProperty("akey", null));
        final String[] response = br.getRegex("\\{\"status\":\"(.*?)\",\"num_files\":\"?(\\d+)\"?,\"num_folders\":\"?(\\d+)\"?,\"storage_used\":\"?(\\d+)\"?,\"bw_used_24hrs\":\"?(\\d+)\"?,\"user_id\":\"(.*?)\",\"user_name\":\"(.*?)\",\"user_group\":\"(.*?)\"\\}").getRow(0);
        if (response == null || response.length != 8 || response[0].equals("error")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }

        account.setValid(true);
        account.setProperty("typ", "unknown");
        if (response[7].equals("vip")) {
            ai.setStatus("VIP User");
            account.setProperty("typ", "vip");
        } else if (response[7].equals("premium")) {
            ai.setStatus("Premium User");
            account.setProperty("typ", "premium");
        } else if (response[7].equals("unverified")) {
            if (showDialog) {
                UserIO.getInstance().requestMessageDialog(0, "ifile.it Premium Error", "Account '" + account.getUser() + "' is not verified.\r\nPlease activate your Account!");
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (response[7].equals("normal")) {
            ai.setStatus("Normal User");
            account.setProperty("typ", "free");
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://ifile.it/help-tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        showDialog = false;
        loginAPI(account, null);
        if (account.getProperty("typ", null).equals("free")) {
            MAXSIMDOWNLOADS = 2;
            RESUMING = true;
            doFree(downloadLink);
        } else {
            final String ukey = new Regex(downloadLink.getDownloadURL(), "http://.*?/(.*?)(/.*?)?$").getMatch(0);

            br.postPage("http://ifile.it/api-fetch_download_url.api", "akey=" + account.getProperty("akey", null) + "&ukey=" + ukey);
            final String[] response = br.getRegex("\\{\"status\":\"(.*?)\",\"(.*?)\":\"(.*?)\"\\}").getRow(0);
            if (response == null || response.length != 3 || response[0].equals("error")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

            final String dllink = response[2].replace("\\", "");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -1);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 503) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void loginAPI(final Account account, AccountInfo ai) throws Exception {
        synchronized (LOCK) {
            if (ai == null) {
                ai = account.getAccountInfo();
                if (ai == null) {
                    ai = new AccountInfo();
                    account.setAccountInfo(ai);
                }
            }
            if (account.getProperty("akey", null) != null) { return; }
            /* login get apikey */
            br.postPage("https://secure.ifile.it/api-fetch_apikey.api", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            final String[] response = br.getRegex("\\{\"status\":\"(.*?)\",\"(.*?)\":\"(.*?)\"\\}").getRow(0);
            if (response == null || response.length != 3 || response[0].equals("error")) {
                if (response[1].equals("message")) {
                    if (response[2].equals("invalid username and\\/or password")) {
                        if (showDialog) {
                            UserIO.getInstance().requestMessageDialog(0, "ifile.it Account Error", "Invalid username '" + account.getUser() + "' and/or password.\r\nPlease check your Account!");
                        }
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("akey", response[2]);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", useragent);
        br.setRequestIntervalLimit(getHost(), 250);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        simulateBrowser();
        if (br.containsHTML("file not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("font-size: [0-9]+%; color: gray;\">(.*?)\\&nbsp;").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"descriptive_link\" value=\"http://ifile.it/.*?/(.*?)\"").getMatch(0);
        }
        final String filesize = br.getRegex(".*?(([0-9]+|[0-9]+\\.[0-9]+) (MB|KB|B|GB)).*?").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim().replaceAll("(\r|\n|\t)", ""));
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private void simulateBrowser() throws IOException {
        br.cloneBrowser().getPage("http://ifile.it/ads/adframe.js");
    }

    private void xmlrequest(final Browser br, final String url, final String postData) throws IOException {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(url, postData);
        br.getHeaders().remove("X-Requested-With");
    }

}