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
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "teradepot.com" }, urls = { "http://[\\w\\.]*?teradepot\\.com/[0-9a-z]{12}" }, flags = { 2 })
public class TeraDepotCom extends PluginForHost {

    public TeraDepotCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.teradepot.com/premium.html");
    }

    public boolean freeAccount = false;

    @Override
    public String getAGBLink() {
        return "http://www.teradepot.com/tos.html";
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.teradepot.com", "lang", "english");
        br.setDebug(true);
        br.getPage("http://www.teradepot.com/");
        Form login = br.getForm(0);
        login.put("login", Encoding.urlEncode(account.getUser()));
        login.put("password", Encoding.urlEncode(account.getPass()));
        login.setAction("http://www.teradepot.com/");
        br.submitForm(login);
        if (br.getCookie("http://www.teradepot.com/", "login") == null || br.getCookie("http://www.teradepot.com/", "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://www.teradepot.com/?op=my_account");
        if (!br.containsHTML("Premium-Account expire")) freeAccount = true;
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
        String space = br.getRegex(Pattern.compile("Used space:</TD><TD><b>(.*?) of", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space + " Mb");
        String points = br.getRegex(Pattern.compile("You have collected:</TD><TD><b>(\\d+) premium ", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) ai.setPremiumPoints(Long.parseLong(points));
        account.setValid(true);
        ai.setUnlimitedTraffic();
        if (!freeAccount) {
            ai.setStatus("Premium User");
            String expire = br.getRegex("Premium-Account expire:</TD><TD><b>(.*?)</b>").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM yyyy", null));
            }
        } else {
            ai.setStatus("Registered (free) User");
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        if (freeAccount) {
            doFree(link);
        } else {
            String passCode = null;
            br.setFollowRedirects(true);
            // Form um auf "Datei herunterladen" zu klicken
            Form DLForm = br.getFormbyProperty("name", "F1");
            if (DLForm == null && br.getRedirectLocation() != null) {
                br.setFollowRedirects(true);
                dl = BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), true, 0);
            } else {
                if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (br.containsHTML("valign=top><b>Password:</b></td>")) {
                    if (link.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", link);
                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = link.getStringProperty("pass", null);
                    }
                    DLForm.put("password", passCode);
                }
                br.setFollowRedirects(true);
                dl = BrowserAdapter.openDownload(br, link, DLForm, true, 0);
            }
            if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML("Wrong password")) {
                    logger.warning("Wrong password!");
                    link.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else {
                    String url = br.getRegex("direct link.*?href=\"(http:.*?)\"").getMatch(0);
                    if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    br.setFollowRedirects(true);
                    dl = BrowserAdapter.openDownload(br, link, url, true, 0);
                    if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
                        br.followConnection();
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            if (passCode != null) {
                link.setProperty("pass", passCode);
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.teradepot.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<Center><h2>Download File(.*?)</").getMatch(0);
        String filesize = br.getRegex("You have requested.*?</font></b> \\((.*?)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        doFree(link);
    }

    public void doFree(DownloadLink link) throws Exception {
        Form freeform = br.getFormBySubmitvalue("Kostenloser+Download");
        if (freeform == null) {
            freeform = br.getFormBySubmitvalue("Free+Download");
            if (freeform == null) {
                freeform = br.getFormbyKey("download1");
            }
        }
        if (freeform != null) {
            freeform.remove("method_premium");
            br.submitForm(freeform);
        }
        if (br.containsHTML("reached the download-limit")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 120 * 60 * 1001l); }
        if (br.containsHTML("You have to wait")) {
            if (br.containsHTML("minute")) {
                int minute = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(0));
                int sec = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(1));
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (minute * 60 + sec) * 1001l);
            } else {
                int sec = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute, (\\d+) seconds till next download").getMatch(1));
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, sec * 1001l);
            }
        }
        Form captchaForm = br.getFormbyProperty("name", "F1");
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String passCode = null;
        if (br.containsHTML("<br><b>Passwort:</b>")) {
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            captchaForm.put("password", passCode);
        }
        String captchaurl = br.getRegex("Enter code below:</b>.*?<img src=\"(.*?)\">").getMatch(0);
        String code = getCaptchaCode(captchaurl, link); // Captcha Usereingabe
        // in die Form einfügen
        captchaForm.put("code", code);
        String wait = br.getRegex("Wait.*?countdown\">(\\d+)<").getMatch(0);
        if (wait != null) sleep(Integer.parseInt(wait.trim()) * 1001l, link);
        dl = BrowserAdapter.openDownload(br, link, captchaForm, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("Wrong password")) {
                logger.warning("Wrong password!");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Wrong captcha") || br.containsHTML("Skipped countdown")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
