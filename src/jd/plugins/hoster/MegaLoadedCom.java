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
import java.util.Locale;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megaloaded.com" }, urls = { "http://[\\w\\.]*?megaloaded\\.com/[a-z|0-9]+/.+" }, flags = { 2 })
public class MegaLoadedCom extends PluginForHost {

    public MegaLoadedCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://megaloaded.com/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://megaloaded.com/tos.html";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.megaloaded.com", "lang", "english");
        br.setFollowRedirects(true);
        br.getPage("http://megaloaded.com/login.html");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("login", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.setFollowRedirects(false);
        if (!br.containsHTML(">Renew premium</a>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://megaloaded.com/?op=my_account");
        if (!br.containsHTML("Premium-Account expire")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String usedspace = br.getRegex("Used space (.*?) of").getMatch(0);
        if (usedspace != null) ai.setUsedSpace(usedspace);
        String validUntil = br.getRegex("Premium-Account expire:</TD><TD><b>(.*?)</b>").getMatch(0);
        if (validUntil == null) {
            account.setValid(false);
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd MMMMM yyyy", Locale.ENGLISH));
            account.setValid(true);
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        String url = null;
        if (br.getRedirectLocation() == null) {
            Form form = br.getFormbyProperty("name", "F1");
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
            url = br.getRegex("#bbb;padding:[0-9]px;\">.*?<a href=\"(.*?)\"").getMatch(0);
        } else {
            url = br.getRedirectLocation();
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, url, true, 0);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 20;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.megaloaded.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<h2>Download File (.*?)</h2>").getMatch(0));
        String filesize = br.getRegex("</font> ((.*?))</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        // Form um auf free zu "klicken"
        Form DLForm0 = br.getForm(0);
        if (DLForm0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLForm0.remove("method_premium");
        br.submitForm(DLForm0);
        // Form um auf "Datei herunterladen" zu klicken
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
            Form DLForm = br.getFormbyProperty("name", "F1");
            if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String passCode = null;
            if (br.containsHTML("<br><b>Password:</b>")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                DLForm.put("password", passCode);
            }
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLForm, false, 1);
            if (!(dl.getConnection().isContentDisposition())) {
                br.followConnection();
                if (br.containsHTML("Wrong password")) {
                    logger.warning("Wrong password!");
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            dl.startDownload();
        }
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