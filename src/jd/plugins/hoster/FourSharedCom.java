//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

//http://www.4shared.com/file/<FILEID[a-70-9]>/<FILENAME>.html
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "http://[\\w\\.]*?(4shared|4shared\\-china)\\.com/(get|file|document|photo|video|audio)/.+?/.*" }, flags = { 2 })
public class FourSharedCom extends PluginForHost {

    public FourSharedCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.4shared.com/ref/14368016/1");
    }

    public String getAGBLink() {
        return "http://www.4shared.com/terms.jsp";
    }

    public void login(Account account) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("4langcookie", "en");
        br.getPage("http://www.4shared.com/login.jsp");
        br.postPage("http://www.4shared.com/index.jsp", "afp=&afu=&df=&rdf=&cff=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&openid=");
        String premlogin = br.getCookie("http://www.4shared.com", "premiumLogin");
        if (premlogin == null || !premlogin.contains("true")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://www.4shared.com", "Password") == null || br.getCookie("http://www.4shared.com", "Login") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        // direct download or not?
        String link = br.getRedirectLocation() != null ? br.getRedirectLocation() : br.getRegex("function startDownload\\(\\)\\{.*?window.location = \"(.*?)\";").getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
        String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
        if (error != null) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short-time maintenance)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        try {
            this.setBrowserExclusive();
            br.getHeaders().put("4langcookie", "en");
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            // need password?
            if (br.containsHTML("enter a password to access")) {
                Form form = br.getFormbyProperty("name", "theForm");
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                // set password before in decrypter?
                if (downloadLink.getProperty("pass") != null) {
                    downloadLink.setDecrypterPassword(downloadLink.getProperty("pass").toString());
                    form.put("userPass2", downloadLink.getDecrypterPassword());
                    br.submitForm(form);
                    // password not correct?
                    // some subfolder can have different password
                    if (br.containsHTML("enter a password to access")) downloadLink.setDecrypterPassword(null);
                }
                if (downloadLink.getDecrypterPassword() == null) {
                    String text = JDL.L("plugins.hoster.general.enterpassword", "Enter password:");
                    for (int retry = 5; retry > 0; retry--) {
                        String pass = getUserInput(text, downloadLink);
                        form.put("userPass2", pass);
                        br.submitForm(form);
                        if (!br.containsHTML("enter a password to access")) {
                            downloadLink.setProperty("pass", pass);
                            downloadLink.setDecrypterPassword(pass);
                            break;
                        } else {
                            text = "(" + (retry - 1) + ") " + JDL.L("plugins.hoster.general.reenterpassword", "Wrong password. Please re-enter:");
                            if (retry == 1) logger.severe("Wrong Password!");
                        }
                    }
                }
            }
            String filename = br.getRegex(Pattern.compile("<title>4shared.com.*?download(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0).trim();
            String size = br.getRegex(Pattern.compile("<b>Size:</b></td>.*?<.*?>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (filename == null || size == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(size.replace(",", "")));
            return AvailableStatus.TRUE;
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.forceDebug(true);
        login(account);
        String redirect = br.getRegex("top.location = \"(.*?)\"").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(redirect);
        String[] dat = br.getRegex("Bandwidth\\:.*?<div class=\"quotacount\">(.+?)\\% of (.*?)</div>").getRow(0);
        ai.setTrafficMax(Regex.getSize(dat[1]));
        ai.setTrafficLeft((long) (ai.getTrafficMax() * ((100.0 - Float.parseFloat(dat[0])) / 100.0)));
        String accountDetails = br.getRegex("(/account/myAccount.jsp\\?sId=[^\"]+)").getMatch(0);
        br.getPage(accountDetails);
        String expire = br.getRegex("<td>Expiration Date:</td>.*?<td>(.*?)<span").getMatch(0).trim();
        ai.setValidUntil(Regex.getMilliSeconds(expire, "yyyy-MM-dd", Locale.UK));
        return ai;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String url = br.getRegex("<a href=\"(http://[\\w\\.]*?(4shared|4shared-china)\\.com/get[^\\;\"]*).*?\" class=\".*?dbtn.*?\" tabindex=\"1\"").getMatch(0);
        if (url == null) {
            /* maybe directdownload */
            url = br.getRegex("startDownload.*?window\\.location.*?(http://.*?)\"").getMatch(0);
            if (url == null) {
                /* maybe picture download */
                url = br.getRegex("<a href=\"(http://dc\\d+\\.(4shared|4shared-china)\\.com/download/.*?)\" class=\".*?dbtn.*?\" tabindex=\"1\"").getMatch(0);
            }
            if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            br.getPage(url);

            url = br.getRegex("id='divDLStart' >.*?<a href='(.*?)'").getMatch(0);
            if (url == null) url = br.getRegex("('|\")(http://dc[0-9]+\\.(4shared|4shared-china)\\.com/download/\\d+/.*?/.*?)('|\")").getMatch(1);
            if (url.contains("linkerror.jsp")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // Ticket Time
            String ttt = br.getRegex(" var c = (\\d+?);").getMatch(0);
            int tt = 40;
            if (ttt != null) {
                logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
                tt = Integer.parseInt(ttt);
            }
            sleep(tt * 1000l, downloadLink);
        }
        br.setDebug(true);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, false, 1);

        String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
        if (error != null) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short-time maintenance)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("red.com/(get|audio|video)", "red.com/file"));
    }

}
