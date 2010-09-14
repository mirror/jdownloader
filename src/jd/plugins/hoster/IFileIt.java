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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ifile.it" }, urls = { "http://[\\w\\.]*?ifile\\.it/[a-z0-9]+" }, flags = { 2 })
public class IFileIt extends PluginForHost {

    private String useragent = RandomUserAgent.generate();

    public IFileIt(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://secure.ifile.it/signup");
    }

    @Override
    public String getAGBLink() {
        return "http://ifile.it/tos";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", useragent);
        br.setFollowRedirects(true);
        br.getPage("https://secure.ifile.it/signin");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("usernameFld", Encoding.urlEncode(account.getUser()));
        form.put("passwordFld", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.setFollowRedirects(false);
        if (!br.containsHTML("you have successfully signed in")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        // This isn't important but with it the login looks more like JD is a
        // real user^^
        String human = br.getRegex("refresh\".*?url=(.*?)\"").getMatch(0);
        if (human != null) br.getPage(human);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Registered account ok");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        doFree(downloadLink);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 18;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.getHeaders().put("User-Agent", useragent);
        br.setRequestIntervalLimit(getHost(), 250);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        simulateBrowser();
        if (br.containsHTML("file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("font-size: [0-9]+%; color: gray;\">(.*?)\\&nbsp;").getMatch(0);
        if (filename == null) filename = br.getRegex("id=\"descriptive_link\" value=\"http://ifile.it/.*?/(.*?)\"").getMatch(0);
        String filesize = br.getRegex(".*?(([0-9]+|[0-9]+\\.[0-9]+) (MB|KB|B|GB)).*?").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim().replaceAll("(\r|\n|\t)", ""));
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    private void simulateBrowser() throws IOException {
        br.cloneBrowser().getPage("http://ifile.it/ads/adframe.js");
    }

    private void xmlrequest(Browser br, String url) throws IOException {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(url);
        br.getHeaders().remove("X-Requested-With");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        br.setDebug(true);
        br.setFollowRedirects(true);
        String downlink = br.getRegex("alias_id.*?=.*?'(.*?)';").getMatch(0);
        String type = br.getRegex("makeUrl\\(.*?'(.*?)'").getMatch(0);
        String extra = br.getRegex("makeUrl\\(.*?'.*?,'(.*?)'").getMatch(0);
        String url = br.getRegex("(\"download:dl_.*?;)").getMatch(0);
        String add = br.getRegex("kIjs09[ ]*=[ ']*([a-zA-Z0-9]+)").getMatch(0);
        if (extra == null) extra = "";
        if (type == null) type = "";
        if (downlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        Context cx = ContextFactory.getGlobal().enterContext();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ var kIjs09='" + add + "'; var __alias_id='" + downlink + "'; var extra='" + extra + "'; var type='" + type + "'; \nreturn " + url + "} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        String finaldownlink = "http://ifile.it/" + Context.toString(result);
        Context.exit();

        // Br2 is our xml browser now!
        Browser br2 = br.cloneBrowser();
        br2.setReadTimeout(40 * 1000);
        xmlrequest(br2, finaldownlink);
        if (!br2.containsHTML("status\":\"ok\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br2.containsHTML("download:captcha")) {
            // Old captcha handling
            for (int i = 0; i <= 5; i++) {
                String captchashit = br.getRegex("url \\+=.*?\\+.*?\\+.*?\"(.*?)\"").getMatch(0);
                String captchacrap = br.getRegex("var.*?x.*?c = '(.*?)'").getMatch(0);
                if (captchashit == null || captchacrap == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String code = getCaptchaCode("http://ifile.it/download:captcha?0." + Math.random(), downloadLink);
                type = "simple";
                extra = "&esn=1&" + captchacrap + "=" + Encoding.urlEncode_light(code) + "&" + captchashit;
                cx = ContextFactory.getGlobal().enterContext();
                scope = cx.initStandardObjects();
                fun = "function f(){ var kIjs09='" + add + "'; var __alias_id='" + downlink + "'; var extra='" + extra + "'; var type='" + type + "'; \nreturn " + url + "} f()";
                result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                finaldownlink = "http://ifile.it/" + Context.toString(result);
                Context.exit();
                xmlrequest(br2, finaldownlink);
                if (br2.containsHTML("\"retry\":\"retry\"")) continue;
                break;
            }
        } else if (br2.containsHTML("\"captcha\":1")) {
            for (int i = 0; i <= 5; i++) {
                // Manuel Re Captcha handling
                String k = br.getRegex("recaptcha_public.*?=.*?'(.*?)'").getMatch(0);
                if (k == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br2.getPage("http://api.recaptcha.net/challenge?k=" + k);
                String challenge = br2.getRegex("challenge[ ]+:[ ]+'(.*?)',").getMatch(0);
                String server = br2.getRegex("server[ ]+:[ ]+'(.*?)'").getMatch(0);
                if (challenge == null || server == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String captchaAddress = server + "image?c=" + challenge;
                String code = getCaptchaCode(captchaAddress, downloadLink);
                type = "recaptcha";
                extra = "&recaptcha_response_field=" + Encoding.urlEncode_light(code) + "&recaptcha_challenge_field=" + challenge;
                cx = ContextFactory.getGlobal().enterContext();
                scope = cx.initStandardObjects();
                fun = "function f(){ var kIjs09='" + add + "'; var __alias_id='" + downlink + "'; var extra='" + extra + "'; var type='" + type + "'; \nreturn " + url + "} f()";
                result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                finaldownlink = "http://ifile.it/" + Context.toString(result);
                Context.exit();
                xmlrequest(br2, finaldownlink);
                if (br2.containsHTML("\"retry\":1")) {
                    xmlrequest(br2, finaldownlink);
                    continue;
                }
                break;
            }
        }
        if (br2.containsHTML("(\"retry\":\"retry\"|\"retry\":1)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br2.containsHTML("an error has occured while processing your request")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        if (!br2.containsHTML("status\":\"ok\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(downloadLink.getDownloadURL());
        try {
            br.getPage(downloadLink.getDownloadURL());
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        }
        String dllink = br.getRegex("req_btn.*?target=\".*?\" href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.info("first try getting dllink failed");
            dllink = br.getRegex("\"(http://s[0-9]+\\.ifile\\.it/.*?/.*?/.*?/.*?)\"").getMatch(0);
            if (dllink == null) {
                logger.info("second try getting dllink failed");
                String pp = br.getRegex("<br /><br />(.*?)</div>").getMatch(0);
                String[] lol = HTMLParser.getHttpLinks(pp, "");
                if (lol.length != 1) {
                } else {
                    for (String link : lol) {
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
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 18;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
