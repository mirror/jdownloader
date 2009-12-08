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

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bigandfree.com" }, urls = { "http://[\\w\\.]*?(megaftp|bigandfree)\\.com/[0-9]+" }, flags = { 2 })
public class BigAndFreeCom extends PluginForHost {

    public static final Object LOCK = new Object();

    public BigAndFreeCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
        this.enablePremium("http://www.bigandfree.com/join");
    }

    @Override
    public String getAGBLink() {
        return "http://www.bigandfree.com/tos";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(megaftp|bigandfree)", "bigandfree"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception, PluginException, InterruptedException {
        synchronized (LOCK) {
            if (this.isAborted(downloadLink)) return AvailableStatus.TRUE;
            /* wait 3 seconds between filechecks */
            Thread.sleep(3000);
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.bigandfree.com", "geoCode", "EN");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The file you requested has been removed")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File Name: </font><font class=.*?>(.*?)</font>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("addthis_open\\(this, '', 'http://.*?', '(.*?)'\\)").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        synchronized (LOCK) {
            if (this.isAborted(downloadLink)) return;
        }
        if (br.containsHTML("You have exceeded your download limit")) {
            int wait = 60;
            String time = br.getRegex("Please wait (\\d+) Minut").getMatch(0);
            if (time != null) wait = Integer.parseInt(time);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1000l * 60);
        }
        Form freeform = br.getFormbyProperty("name", "chosen");
        if (freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        freeform.setAction(downloadLink.getDownloadURL());
        freeform.remove("chosen_prem");
        br.submitForm(freeform);
        // Datei hat Passwortschutz?
        if (br.containsHTML("This file is password-protected")) {
            String passCode;
            DownloadLink link = downloadLink;
            Form form = br.getFormbyProperty("name", "pswcheck");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (link.getStringProperty("pass", null) == null) {
                /* Usereingabe */
                passCode = Plugin.getUserInput(null, link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            /* Passwort übergeben */
            form.put("psw", passCode);
            form.setAction(downloadLink.getDownloadURL());
            br.submitForm(form);
            form = br.getFormbyProperty("name", "pswcheck");
            if (form != null && br.containsHTML("Invalid Password")) {
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                link.setProperty("pass", passCode);
            }
        }

        // often they only change this form
        Form downloadForm = br.getForm(1);
        if (downloadForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String current = br.getRegex("name=\"current\" value=\"(.*?)\"").getMatch(0);
        if (current == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String wait = br.getRegex("var x = (\\d+);").getMatch(0);
        if (wait != null) sleep(Long.parseLong(wait.trim()) * 1000, downloadLink);
        downloadForm.put("current", current);
        downloadForm.put("limit_reached", "0");
        downloadForm.put("download_now", "Click+here+to+download");
        downloadForm.setAction(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadForm, true, 0);
        if (!(dl.getConnection().isContentDisposition()) && !dl.getConnection().getContentType().contains("octet")) {
            br.followConnection();
            if (br.containsHTML("You have exceeded your download limit")) {
                int wait2 = 60;
                String time = br.getRegex("Please wait (\\d+) Minut").getMatch(0);
                if (time != null) wait2 = Integer.parseInt(time);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait2 * 1000l * 60);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage("http://www.bigandfree.com/members");
        Form form = br.getFormbyProperty("name", "login");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("uname", Encoding.urlEncode(account.getUser()));
        form.put("pwd", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        String acctype = br.getRegex(">Account Type: </font><font class=.*?>(.*?)</font>").getMatch(0);
        if (br.containsHTML("Account not found") || acctype == null || !acctype.contains("PREMIUM")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        Cookies check = br.getCookies("bigandfree.com");
        System.out.print(br.toString());
        Form premform = br.getForm(1);
        if (premform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        premform.setPreferredSubmit("chosen_prem");
        br.submitForm(premform);
        System.out.print(br.toString());
        String dllink = br.getRegex("Direct Link:.*?value=\"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("Proxy Link:.*?value=\"(http.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("Proxy Link:.*?value=\"(http.*?)\"").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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

}