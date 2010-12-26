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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "usershare.net" }, urls = { "http(s)?://[\\w\\.]*?usershare\\.net/(.*?/[0-9a-z]{12}|[0-9a-z]{12})" }, flags = { 2 })
public class Usershare extends PluginForHost {

    public Usershare(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    private static final String COOKIE_HOST = "http://usershare.net";

    @Override
    public String getAGBLink() {
        return "http://usershare.net/tos.html";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("https", "http"));
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String passCode = null;
        String linkurl = null;
        if (br.containsHTML("(<b>Passwort:</b>|<b>Password:</b>)")) {
            logger.info("The downloadlink seems to be password protected.");
            Form pwform = br.getFormbyProperty("name", "F1");
            if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            pwform.put("password", passCode);
            logger.info("Put password \"" + passCode + "\" entered by user in the DLForm and submitted it.");
            br.submitForm(pwform);
            if (br.containsHTML("(Wrong password|<b>Passwort:</b>|<b>Password:</b>)")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            linkurl = br.getRedirectLocation();
        } else {
            linkurl = getDllink();
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, linkurl, true, 1);
        dl.startDownload();
    }

    private String getDllink() {
        String linkurl = br.getRegex("\"(http://[a-zA-Z0-9]+\\.usershare\\.net/files/.*?/.*?/.*?)\"").getMatch(0);
        if (linkurl == null) {
            linkurl = br.getRegex("href=\"(http://[0-9]+\\..*?:[0-9]+/d/.*?/.*?)\"").getMatch(0);
        }
        return linkurl;
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
        String space = br.getRegex(Pattern.compile("<td>Used space:</td>.*?<td.*?b>(.*?)of.*?Mb</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim() + " Mb");
        String points = br.getRegex(Pattern.compile("<td>You have collected:</td.*?b>(.*?)premium points", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) {
            // Who needs half points ? If we have a dot in the points, just
            // remove it
            if (points.contains(".")) {
                String dot = new Regex(points, ".*?(\\.(\\d+))").getMatch(0);
                points = points.replace(dot, "");
            }
            ai.setPremiumPoints(Long.parseLong(points.trim()));
        }
        account.setValid(true);
        String availabletraffic = new Regex(br.toString(), "Traffic available.*?:</TD><TD><b>(.*?)</b>").getMatch(0);
        if (availabletraffic != null) {
            ai.setTrafficLeft(Regex.getSize(availabletraffic));
        } else {
            ai.setUnlimitedTraffic();
        }
        String expire = new Regex(br.toString(), "<td>Premium-Account expire:</td>.*?<td>(.*?)</td>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            expire = expire.replaceAll("(<b>|</b>)", "");
            ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM yyyy", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.getPage(COOKIE_HOST + "/login.html");
        Form loginform = br.getForm(1);
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.put("login", Encoding.urlEncode(account.getUser()));
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginform);
        br.getPage(COOKIE_HOST + "/?op=my_account");
        if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!br.containsHTML("Premium-Account expire") && !br.containsHTML(">Renew premium<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            Form DLForm = br.getFormbyProperty("name", "F1");
            if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("(<b>Passwort:</b>|<b>Password:</b>)")) {
                logger.info("The downloadlink seems to be password protected.");
                if (link.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", link);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                DLForm.put("password", passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm and submitted it.");
            }
            br.submitForm(DLForm);
            if (br.containsHTML("(Wrong password|<b>Passwort:</b>|<b>Password:</b>)")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            dllink = br.getRedirectLocation();
            if (dllink == null) dllink = getDllink();
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such user exist") || br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"hdr\"><TD colspan=2>(.*?)</TD>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<b>Filename:</b></td><td nowrap>(.*?)</b>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h3>Download File:(.*?)</h3>").getMatch(0);
                if (filename == null) filename = br.getRegex("<title>Download(.*?)</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex("Size:</b></td><td>(.*?)<small>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("label>Size:</label> <span>(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }
}
