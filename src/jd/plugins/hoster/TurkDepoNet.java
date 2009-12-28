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

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turkdepo.net" }, urls = { "http://[\\w\\.]*?turkdepo\\.net/([0-9a-z]{12}|.*?/[a-z0-9]{12})" }, flags = { 2 })
public class TurkDepoNet extends PluginForHost {

    public TurkDepoNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://turkdepo.net/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://turkdepo.net/tos.html";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://turkdepo.net/", "lang", "english");
        br.getPage("http://turkdepo.net/login.html");
        Form premform = br.getForm(0);
        if (premform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        premform.put("login", account.getUser());
        premform.put("password", account.getPass());
        br.submitForm(premform);
        br.getPage("http://turkdepo.net/?op=my_account");
        if (!br.containsHTML("Your Premium-Account is valid until")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://www.turkdepo.net/", "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("Your Premium-Account is valid until <b>(.*?)</b>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM yyyy", null));
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account);
        br.setDebug(true);
        br.getPage(link.getDownloadURL());
        br.setFollowRedirects(false);
        // Form um auf "Datei herunterladen" zu klicken
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (DLForm == null && br.getRedirectLocation() != null) {
            br.setFollowRedirects(true);
            dl = BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), false, 1);
        } else {
            if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("name=\"password\"")) {
                if (link.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", link);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                DLForm.put("password", passCode);
            }
            br.setFollowRedirects(true);
            dl = BrowserAdapter.openDownload(br, link, DLForm, false, 1);
        }
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Wrong password") || br.containsHTML("name=\"password\"")) {
                logger.warning("Wrong password!");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                String url = br.getRegex("dotted #bbb;padding:[0-9]+px;.*?<a href=\"(.*?)\"").getMatch(0);
                if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.setFollowRedirects(true);
                dl = BrowserAdapter.openDownload(br, link, url, false, 1);
                if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("Error happened when generating")) throw new PluginException(LinkStatus.ERROR_RETRY);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.setDebug(true);
        Form form = br.getFormByKey("method_free");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.remove("method_premium");
        br.submitForm(form);
        if (br.containsHTML("You have to wait") || br.containsHTML("You have reached the download-limit")) {
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
            Form dlform = br.getFormbyProperty("name", "F1");
            if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String passCode = null;
            // Password protected links handling
            if (br.containsHTML("name=\"password\"")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                dlform.put("password", passCode);
            }
            // Ticket Time
            String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
            if (ttt != null) {
                int tt = Integer.parseInt(ttt);
                sleep(tt * 1001, downloadLink);
            }
            br.submitForm(dlform);
            String dllink = br.getRedirectLocation();
            if (br.containsHTML("Wrong password")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (dllink == null) dllink = br.getRegex("padding:[0-9]+px;\">\\s+<a\\s+href=\"(.*?)\">").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
            if (!(dl.getConnection().isContentDisposition())) {
                br.followConnection();
                if (br.containsHTML("Error happened when generating")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://turkdepo.net/", "lang", "english");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("No such (file|user)|File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<Title>Download(.*?)</Title>").getMatch(0);
        if (filename == null) filename = br.getRegex("fname\" value=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("</font>\\s*\\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}