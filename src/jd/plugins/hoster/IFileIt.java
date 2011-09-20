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
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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
    private static final String COOKIENAME          = "if_akey";
    private static final String MAINPAGE            = "http://ifile.it/";

    public IFileIt(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://secure.ifile.it/account-signup.html");
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        // generating first request
        final String c = br.getRegex("eval(.*?)\n").getMatch(0);
        final String dec = br.getRegex("dec\\( \'(.*?)\' \\),").getMatch(0);
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(engine.eval(c).toString());
            result = inv.invokeFunction("dec", dec);
        } catch (final Throwable e) {
            result = "";
        }
        final String finaldownlink = result.toString();
        if (finaldownlink == null || finaldownlink.equals("") || !finaldownlink.startsWith("http")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final String downlink = br.getRegex("var _url = \'(.*?)\';").getMatch(0);
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
                extra = "ctype=recaptcha&recaptcha_response=" + Encoding.urlEncode_light(code) + "&recaptcha_challenge=" + challenge;
                xmlrequest(br2, finaldownlink, type + extra);
                if (br2.containsHTML("\"retry\":1")) {
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Registered account ok");
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://ifile.it/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 18;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 18;
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
        login(account);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        doFree(downloadLink);
    }

    public void login(final Account account) throws Exception {
        setBrowserExclusive();
        synchronized (LOCK) {
            br.setCookie(MAINPAGE, COOKIENAME, getPluginConfig().getStringProperty("logincookie", null));
            br.getHeaders().put("User-Agent", useragent);
            boolean alreadyLoggedIn = false;
            try {
                br.getPage("https://secure.ifile.it/account-signin.html");
            } catch (final BrowserException e) {
                alreadyLoggedIn = true;
            }
            if (!alreadyLoggedIn) {
                final Form loginForm = br.getForm(0);
                if (loginForm != null) {
                    loginForm.put("username", Encoding.urlEncode(account.getUser()));
                    loginForm.put("password", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginForm);
                } else {
                    br.postPage("https://secure.ifile.it/account-signin_p.html", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                }
                if (br.getCookie(MAINPAGE, COOKIENAME) == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            }
            getPluginConfig().setProperty("logincookie", br.getCookie(MAINPAGE, COOKIENAME));
            getPluginConfig().save();
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
