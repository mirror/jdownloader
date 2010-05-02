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
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        br.setDebug(true);
        br.setFollowRedirects(true);
        String downlink = br.getRegex("alias_id.*?=.*?'(.*?)';").getMatch(0);
        if (downlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Example how current links(last updated plugin-links) look(ed)
        // like
        // http://ifile.it/download:dl_request?alias_id=3180283&type=na&esn=1
        String finaldownlink = "http://ifile.it/download:dl_request?alias_id=" + downlink + "&type=na&esn=1";
        // Br2 is our xml browser now!
        Browser br2 = br.cloneBrowser();
        xmlrequest(br2, finaldownlink);
        if (!br2.containsHTML("status\":\"ok\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br2.containsHTML("download:captcha")) {
            // Old captcha handling
            for (int i = 0; i <= 5; i++) {
                String captchashit = br.getRegex("url \\+=.*?\\+.*?\\+.*?\"(.*?)\"").getMatch(0);
                String captchacrap = br.getRegex("var.*?x.*?c = '(.*?)'").getMatch(0);
                if (captchashit == null || captchacrap == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String code = getCaptchaCode("http://ifile.it/download:captcha?0." + Math.random(), downloadLink);
                String captchaget = "http://ifile.it/download:dl_request?alias_id=" + downlink + "&type=simple&esn=0&" + captchacrap + "=" + code + "&" + captchashit;
                logger.info("Captchagetpage = " + captchaget);
                // Example of the last working captchaget
                // http://ifile.it/download:dl_request?x65=549427&type=simple&esn=1&8a1e7=9fa&920e4e7d3666c587258c93ef87cb3365=a8c5e3fdae3471388ec44741b41b3c2d&d51500b7a7cd5292d9db0b98dc022447=98f13708210194c475687be6106a3b84
                xmlrequest(br2, captchaget);
                if (br2.containsHTML("\"retry\":\"retry\"")) continue;
                break;
            }
        } else if (br2.containsHTML("\"captcha\":1")) {
            for (int i = 0; i <= 5; i++) {
                // Manuel Re Captcha handling
                String k = br.getRegex("recaptcha_public.*?=.*?'(.*?)'").getMatch(0);
                if (k == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br2.getPage("http://api.recaptcha.net/challenge?k=" + k);
                String challenge = br2.getRegex("challenge:'(.*?)',").getMatch(0);
                String server = br2.getRegex("server:'(.*?)'").getMatch(0);
                if (challenge == null || server == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String captchaAddress = server + "image?c=" + challenge;
                String code = getCaptchaCode(captchaAddress, downloadLink);
                String recaptchaget = "http://ifile.it/download:dl_request?alias_id=" + downlink + "&type=recaptcha&esn=1&recaptcha_response_field=" + code + "&recaptcha_challenge_field=" + challenge;
                xmlrequest(br2, recaptchaget);
                if (br2.containsHTML("\"retry\":1")) {
                    xmlrequest(br2, finaldownlink);
                    continue;
                }
                break;
            }
        }
        if (br2.containsHTML("(\"retry\":\"retry\"|\"retry\":1)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (!br2.containsHTML("status\":\"ok\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://ifile.it/dl");
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
            br.followConnection();
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
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
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("directLink", null);
    }
}
