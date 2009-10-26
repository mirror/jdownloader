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
import java.util.SortedMap;
import java.util.TreeMap;
import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "putshare.com" }, urls = { "http://[\\w\\.]*?putshare\\.com/[0-9a-z]{12}" }, flags = { 2 })
public class PutShareCom extends PluginForHost {

    public PutShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.putshare.com/premium.html");
    }

    public String getAGBLink() {
        return "http://putshare.com/tos.html";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://putshare.com/", "lang", "english");
        br.setFollowRedirects(true);
        br.getPage("http://www.putshare.com/login.html");
        Form premform = br.getForm(0);
        if (premform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        premform.put("login", account.getUser());
        premform.put("password", account.getPass());
        br.submitForm(premform);
        if (!br.containsHTML("Renew premium") || br.containsHTML("Upgrade to premium") || br.containsHTML("Incorrect Login or Password")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String hostedFiles = br.getRegex("/.*?(\\d+).*?files").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        String usedspace = br.getRegex("Used space(.*?)of").getMatch(0);
        if (usedspace != null) {
            ai.setUsedSpace(usedspace.trim());
        }
        br.getPage("http://www.putshare.com/?op=payments");
        Regex timeleft = br.getRegex("Premium-Account expire:</b><br>(\\d+).*?da.*?,.*?(\\d+).*?hou.*?,.*?(\\d+).*?minut.*?,.*?(\\d+).*?secon.*?<br>");
        if (timeleft.getMatch(0) != null && timeleft.getMatch(1) != null && timeleft.getMatch(2) != null && timeleft.getMatch(3) != null) {
            long check = (System.currentTimeMillis() + Long.parseLong(timeleft.getMatch(0)) * 24 * 60 * 60 * 1000 + Long.parseLong(timeleft.getMatch(1)) * 60 * 60 * 1000 + Long.parseLong(timeleft.getMatch(2)) * 60 * 1000 + Long.parseLong(timeleft.getMatch(3)) * 1000);
            ai.setValidUntil(check);
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String passCode = null;
        Browser brc = br.cloneBrowser();
        String dllink = null;
        URLConnectionAdapter con = brc.openGetConnection(downloadLink.getDownloadURL());
        if ((con.getContentType().contains("html"))) {
            br.getPage(downloadLink.getDownloadURL());
            for (int i = 0; i <= 3; i++) {
                Form pwform = br.getFormbyProperty("name", "F1");
                if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                pwform.put("password", passCode);
                br.submitForm(pwform);
                System.out.print(br.toString());
                if (br.containsHTML("Wrong password")) {
                    logger.warning("Wrong password!");
                    downloadLink.setProperty("pass", null);
                    continue;
                }
                break;
            }
            if (br.containsHTML("Wrong password")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            dllink = br.getRegex("background.*?border:[0-9]+px dotted #bbb;padding:[0-9]+px;\">.*?<a href=\"(.*?)\"").getMatch(0);
        } else {
            dllink = downloadLink.getDownloadURL();
        }

        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://putshare.com/", "lang", "english");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("No such (file|user)|File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<h2>Download File(.*?)</h2>").getMatch(0));
        String filesize = br.getRegex("</font>\\s*\\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(filename.replace(" - www.putShare.com - free data hosting ", "").replace("www.putShare.com", "").replace("free data hosting", "").trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.setDebug(true);
        Form form = br.getFormBySubmitvalue("Free+Download");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.remove("method_premium");
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
        } else {
            // Ticket Time
            int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
            sleep(tt * 1001, downloadLink);
            String passCode = null;
            form = br.getFormbyProperty("name", "F1");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            /* "Captcha Method" */
            String[][] letters = br.getRegex("<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(\\d)</span>").getMatches();
            if (letters.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
            for (String[] letter : letters) {
                capMap.put(Integer.parseInt(letter[0]), letter[1]);
            }
            StringBuilder code = new StringBuilder();
            for (String value : capMap.values()) {
                code.append(value);
            }
            form.put("code", code.toString());
            form.setAction(downloadLink.getDownloadURL());
            if (br.containsHTML("name=\"password\"")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                form.put("password", passCode);
            }
            br.submitForm(form);
            String dllink = br.getRedirectLocation();
            if (br.containsHTML("Wrong password")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Wrong captcha")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            if (br.containsHTML("Download Link Generated")) dllink = br.getRegex("padding:7px;\">\\s+<a\\s+href=\"(.*?)\">").getMatch(0);
            br.setFollowRedirects(true);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
            dl.startDownload();
        }
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
