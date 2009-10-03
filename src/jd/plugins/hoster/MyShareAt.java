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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "my-share.at" }, urls = { "http://[\\w\\.]*?my-share\\.at/[a-zA-Z0-9]+/?" }, flags = { 2 })
public class MyShareAt extends PluginForHost {

    public MyShareAt(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://my-share.at/premium.html");
    }

    public String getAGBLink() {
        return "http://my-share.at/tos.html";
    }

    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        Form form = br.getForm(1);
        form.setPreferredSubmit("Free+Download");
        br.submitForm(form);
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
        }
        if (br.containsHTML("You can download files up to 100 Mb only")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium"); }
        Recaptcha rc = new Recaptcha(br);
        rc.parse();
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, link);
        form = rc.getForm();
        if (form.hasInputFieldByName("password")) {
            String passCode = null;
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            form.put("password", passCode);
            link.setProperty("pass", passCode);
        }
        rc.setCode(c);
        String dl_url = br.getRedirectLocation();
        if (br.containsHTML(">Wrong password<")) {
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (br.containsHTML("Wrong captcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dl_url, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://my-share.at/", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        String filename = br.getRegex("<h2>Download File (.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("Filename:</b>.*?nowrap>(.*?)</td>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        String filesize = br.getRegex("You have requested.*?</font> \\((.*?)\\)</font>").getMatch(0);
        if (filesize != null) {
            parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        } else {
            filesize = br.getRegex("Size:.*?<small>\\((\\d+) bytes\\)").getMatch(0);
            if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setDownloadSize(Long.parseLong(filesize.trim()));
        }
        return AvailableStatus.TRUE;
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://my-share.at/", "lang", "english");
        br.getPage("http://my-share.at/login.html");
        br.postPage("http://my-share.at/", "op=login&redirect=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&x=22&y=13");
        if (br.getCookie("http://my-share.at/", "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://my-share.at/", "login") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://my-share.at/?op=my_account");
        if (!br.containsHTML("Premium-Account expire")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String points = br.getRegex("You have collected:</TD><TD><b>(\\d+) premium points<").getMatch(0);
        if (points != null) ai.setPremiumPoints(points);

        String validUntil = br.getRegex("Premium-Account expire:</TD><TD><b>(.*?)<").getMatch(0);
        if (validUntil == null) {
            account.setValid(false);
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd MMM yyyy", null));
            account.setValid(true);
        }
        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        String url = null;
        if (br.getRedirectLocation() == null) {
            Form form = br.getForm(1);
            if (form.hasInputFieldByName("password")) {
                String passCode = null;
                if (parameter.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, parameter);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = parameter.getStringProperty("pass", null);
                }
                form.put("password", passCode);
                parameter.setProperty("pass", passCode);
            }
            br.submitForm(form);
            if (br.containsHTML(">Wrong password<")) {
                parameter.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            url = br.getRegex("hours<br><br>.*?<span style.*?>.*?<a href=\"(.*?)\">.*?</a>").getMatch(0);
        } else {
            url = br.getRedirectLocation();
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, url, true, 0);
        dl.startDownload();
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
