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

import java.io.File;
import java.io.IOException;
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
import jd.plugins.pluginUtils.Recaptcha;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "oron.com" }, urls = { "http://[\\w\\.]*?oron\\.com/[a-z0-9]{12}" }, flags = { 2 })
public class OronCom extends PluginForHost {

    public OronCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://oron.com/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://oron.com/tos.html";
    }

    public boolean nopremium = false;
    private static final String COOKIE_HOST = "http://oron.com";

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://oron.com", "lang", "english");
        br.setDebug(true);
        br.getPage("http://oron.com/login.html");
        Form loginform = br.getForm(0);
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.put("login", Encoding.urlEncode(account.getUser()));
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginform);
        br.getPage("http://oron.com/?op=my_account");
        if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!br.containsHTML("Premium-Account expire") && !br.containsHTML("Upgrade to premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!br.containsHTML("Premium-Account expire")) nopremium = true;
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
        String space = br.getRegex(Pattern.compile("<td>Used space:</td>.*?<td.*?>(.*?)of.*?Mb</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space + " Mb");
        String points = br.getRegex(Pattern.compile("You have collected:</td>.*?(\\d+) premium", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) ai.setPremiumPoints(Long.parseLong(points));
        account.setValid(true);
        String availableTraffic = br.getRegex("<td>Available:</td>.*?<td>(.*?)</td>").getMatch(0);
        if (availableTraffic != null) ai.setTrafficLeft(Regex.getSize(availableTraffic));
        if (!nopremium) {
            String expire = br.getRegex("<td>Premium-Account expire:</td>.*?<td>(.*?)</td>").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM yyyy", null));
            }
            ai.setStatus("Premium User");
        } else {
            ai.setStatus("Registered (free) User");
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (nopremium) {
            doFree(link);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                if (br.containsHTML("You have reached the download-limit")) {
                    String errormessage = "You have reached the download-limit!";
                    if (br.getRegex("class=\"err\">(.*?)<br>").getMatch(0) != null) errormessage = br.getRegex("class=\"err\">(.*?)<br>").getMatch(0);
                    logger.warning(errormessage);
                    link.getLinkStatus().setStatusText(errormessage);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, errormessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                Form DLForm = br.getFormbyProperty("name", "F1");
                if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (br.containsHTML("name=\"password\"")) {
                    if (link.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", link);
                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = link.getStringProperty("pass", null);
                    }
                    DLForm.put("password", passCode);
                    logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                }
                br.submitForm(DLForm);
                // Premium also got limits...
                if (br.containsHTML("You have reached the download-limit")) {
                    String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
                    String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
                    String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
                    String tmpdays = br.getRegex("\\s+(\\d+)\\s+days?").getMatch(0);
                    if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
                    } else {
                        int minutes = 0, seconds = 0, hours = 0, days = 0;
                        if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                        if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                        if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                        if (tmpdays != null) days = Integer.parseInt(tmpdays);
                        int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                        logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
                    }
                }
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    if (br.containsHTML("(name=\"password\"|Wrong password)")) {
                        logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                        link.setProperty("pass", null);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                    if (dllink == null) {
                        dllink = br.getRegex("<td align=\"center\" height=\"100\"><a href=\"(http.*?)\"").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("\"(http://[a-zA-Z0-9]+\\.oron\\.com/.*?/.*?)\"").getMatch(0);
                        }
                    }
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\" regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            // Hoster allows up to 10 Chunks but then you can only start one
            // download...
            jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (passCode != null) {
                link.setProperty("pass", passCode);
            }
            if (!(dl.getConnection().isContentDisposition())) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                if (br.containsHTML("File Not Found")) {
                    logger.warning("Server says link offline, please recheck that!");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.oron.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("div.*?Filename:.*?<.*?>(.*?)<").getMatch(0));
        String filesize = br.getRegex("Size: (.*?)<").getMatch(0);
        // Handling for links which can only be downloaded by premium users
        if (br.containsHTML("The file status can only be queried by Premium Users")) {
            // return AvailableStatus.UNCHECKABLE;
            link.getLinkStatus().setStatusText(JDL.L("plugins.host.errormsg.only4premium", "Only downloadable for premium users!"));
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        if (br.containsHTML("The file status can only be queried by Premium Users")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.host.errormsg.only4premium", "Only downloadable for premium users!"));
        br.setFollowRedirects(true);
        // Form um auf free zu "klicken"
        Form DLForm0 = br.getForm(0);
        if (DLForm0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLForm0.remove("method_premium");
        br.submitForm(DLForm0);
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        } else {
            // waittime
            String ttt = br.getRegex("countdown\">(\\d+)</span>").getMatch(0);
            if (ttt != null) {
                int tt = Integer.parseInt(ttt);
                sleep(tt * 1001l, downloadLink);
            }
            String passCode = null;
            // Re Captcha handling
            if (br.containsHTML("api.recaptcha.net")) {
                Recaptcha rc = new Recaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                if (br.containsHTML("name=\"password\"")) {
                    if (downloadLink.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", downloadLink);
                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = downloadLink.getStringProperty("pass", null);
                    }
                    rc.getForm().put("password", passCode);
                }
                rc.setCode(c);
            } else {
                // No captcha handling
                Form dlForm = br.getFormbyProperty("name", "F1");
                if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (br.containsHTML("name=\"password\"")) {
                    if (downloadLink.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", downloadLink);
                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = downloadLink.getStringProperty("pass", null);
                    }
                    dlForm.put("password", passCode);
                }
                br.submitForm(dlForm);
            }
            if (br.containsHTML("Wrong password") || br.containsHTML("Wrong captcha")) {
                logger.warning("Wrong password or wrong captcha");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            String dllink = br.getRegex("height=\"[0-9]+\"><a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
            dl.startDownload();
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}