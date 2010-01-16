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

import java.util.Calendar;
import java.util.GregorianCalendar;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "Only4Devs2Test.com" }, urls = { "http://[\\w\\.]*?Only4Devs2Test\\.com/(\\?d|download\\.php\\?id)=[A-Z0-9]+" }, flags = { 0 })
public class MhfScriptBasic extends PluginForHost {

    public MhfScriptBasic(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://Only4Devs2Test.com/register.php?g=3");
    }

    // MhfScriptBasic 1.0
    // This plugin is for developers to easily implement hosters using the MHF
    // script, it's still just a beta but i will improve it till it works for
    // nearly all of those hosters!
    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    public String finalLink = null;
    private static final String COOKIE_HOST = "http://Only4Devs2Test.com";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<b>File name:</b></td>.*?<td align=.*?width=.*?>(.*?)</td>").getMatch(0);
        if (filename == null) {
            br.getRegex("\"Click this to report for(.*?)\"").getMatch(0);
            if (filename == null) {
                br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
            if (filename == null) {
                br.getRegex("content=\"(.*?), The best file hosting service").getMatch(0);
            }
        }
        String filesize = br.getRegex("<b>File size:</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        // br.postPage(link.getDownloadURL(), "Free=Free+Users");
        String passCode = null;
        for (int i = 0; i <= 3; i++) {
            Form captchaform = br.getFormbyProperty("name", "myform");
            if (captchaform == null) {
                captchaform = br.getFormbyProperty("name", "validateform");
                if (captchaform == null) {
                    captchaform = br.getFormbyProperty("name", "valideform");
                }
            }
            String captchaurl = COOKIE_HOST + "/captcha.php";
            if (captchaform == null || !br.containsHTML("captcha.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("downloadpw")) {
                if (link.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", link);

                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                captchaform.put("downloadpw", passCode);
            }
            String code = getCaptchaCode(captchaurl, link);
            captchaform.put("captchacode", code);
            br.submitForm(captchaform);
            if (br.containsHTML("Password Error")) {
                logger.warning("Wrong password!");
                link.setProperty("pass", null);
                continue;
            }
            if (br.containsHTML("Captcha number error") || br.containsHTML("captcha.php") && !br.containsHTML("You have got max allowed bandwidth size per hour")) {
                logger.warning("Wrong captcha or wrong password!");
                link.setProperty("pass", null);
                continue;
            }
            break;
        }
        if (br.containsHTML("Password Error")) {
            logger.warning("Wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (br.containsHTML("Captcha number error") || br.containsHTML("captcha.php") && !br.containsHTML("You have got max allowed bandwidth size per hour")) {
            logger.warning("Wrong captcha or wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        if (br.containsHTML("You have got max allowed bandwidth size per hour")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        findLink(link);
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void findLink(DownloadLink link) throws Exception {
        String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
        if (sitelinks == null || sitelinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (String alink : sitelinks) {
            alink = Encoding.htmlDecode(alink);
            if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                finalLink = alink;
                break;
            }
        }
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage("http://filecruz.com/login.php");
        Form form = br.getFormbyProperty("name", "lOGIN");
        if (form == null) form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("user", Encoding.urlEncode(account.getUser()));
        form.put("pass", Encoding.urlEncode(account.getPass()));
        // If the referer is still in the form (and if it is a valid
        // downloadlink) the download starts directly after logging in so we
        // MUST remove it!
        form.remove("refer_url");
        form.put("autologin", "0");
        br.submitForm(form);
        String premium = br.getRegex("<b>Your Package</b></td>.*?<b>(.*?)</b></A>").getMatch(0);
        if (br.getCookie("http://filecruz.com", "mfh_passhash") == null || br.getCookie("http://filecruz.com", "mfh_uid").equals("0") || premium == null || !premium.equals("Premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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

        br.getPage("http://filecruz.com/members.php");
        String premium = br.getRegex("<b>Your Package</b></td>.*?<b>(.*?)</b></A>").getMatch(0);
        if (!premium.equals("Premium")) {
            account.setValid(true);
            return ai;
        }
        String expired = br.getRegex("Expired\\?</b></td>.*?<td align=.*?>(.*?)<").getMatch(0);
        if (expired != null) {
            if (expired.trim().equalsIgnoreCase("No"))
                ai.setExpired(false);
            else if (expired.equalsIgnoreCase("Yes")) ai.setExpired(true);
        }

        String expires = br.getRegex("Package Expire Date</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0).trim();
        if (expires != null) {
            String[] e = expires.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + e[2]), Integer.parseInt(e[0]) - 1, Integer.parseInt(e[1]));
            ai.setValidUntil(cal.getTimeInMillis());
        }

        String create = br.getRegex("Register Date</b></td>.*?<td align=.*?>(.*?)<").getMatch(0).trim();
        if (create != null) {
            String[] c = create.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + c[2]), Integer.parseInt(c[0]) - 1, Integer.parseInt(c[1]));
            ai.setCreateTime(cal.getTimeInMillis());
        }

        ai.setFilesNum(0);
        String files = br.getRegex("<b>Hosted Files</b></td>.*?<td align=.*?>(.*?)<").getMatch(0).trim();
        if (files != null) {
            ai.setFilesNum(Integer.parseInt(files.trim()));
        }

        ai.setPremiumPoints(0);
        String points = br.getRegex("<b>Total Points</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0).trim();
        if (points != null) {
            ai.setPremiumPoints(Integer.parseInt(points.trim()));
        }

        ai.setStatus("Premium User");
        account.setValid(true);

        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.setFollowRedirects(false);
        br.setCookie("http://filecruz.com", "mfh_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        String dllink = null;
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("access_key=") || br.getRedirectLocation().contains("getfile.php")) {
            dllink = br.getRedirectLocation();
        } else {
            if (br.containsHTML("You have got max allowed download sessions from the same IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            String passCode = null;
            if (br.containsHTML("downloadpw")) {
                logger.info("The file you're trying to download seems to be password protected...");
                Form pwform = br.getFormbyProperty("name", "myform");
                if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (parameter.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", parameter);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = parameter.getStringProperty("pass", null);
                }
                pwform.put("downloadpw", passCode);
                br.submitForm(pwform);
            }
            if (br.containsHTML("You have got max allowed download sessions from the same IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            if (br.containsHTML("Password Error")) {
                logger.warning("Wrong password!");
                parameter.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }

            if (passCode != null) {
                parameter.setProperty("pass", passCode);
            }
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            if (sitelinks == null || sitelinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (String alink : sitelinks) {
                alink = Encoding.htmlDecode(alink);
                if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                    dllink = alink;
                    break;
                }
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
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
