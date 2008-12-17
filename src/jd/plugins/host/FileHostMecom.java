//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class FileHostMecom extends PluginForHost {
    private String passCode = null;
    private String url;

    public FileHostMecom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filehostme.com/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.filehostme.com/tos.html";
    }

    public void login(Account account) throws IOException, PluginException {
        br.postPage("http://www.filehostme.com/", "op=login&redirect=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&x=48&y=5");
        String cookie = br.getCookie("http://www.filehostme.com/", "xfss");
        if (cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public boolean isPremium() throws IOException {
        if (br.getURL() == null || !br.getURL().equalsIgnoreCase("http://www.filehostme.com/?op=my_account") || br.toString().startsWith("Not HTML Code.")) {
            br.getPage("http://www.filehostme.com/?op=my_account");
        }
        if (br.containsHTML("<TD>Premium-Account expire:</TD>")) { return true; }
        return false;
    }

    @Override
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setValid(false);
            ai.setStatus("No Premium Account!");
            return ai;
        }
        br.getPage("http://www.filehostme.com/?op=my_account");
        String points = br.getRegex(Pattern.compile("<TR><TD>You have collected:</TD><TD><b>(.*?)premium points</b>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        String expire = br.getRegex(Pattern.compile("<TR><TD>Premium-Account expire:</TD><TD><b>(.*?)</b>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMM yyyy", null));
        ai.setTrafficLeft(-1);
        ai.setValid(true);
        return ai;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<h2>Download File(.*?)</h2>").getMatch(0);
        String filesize = br.getRegex("You have requested <font.*?>.*?</font> \\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        downloadLink.setName(filename.trim());
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        DownloadLink downloadLink = (DownloadLink) parameter;
        getFileInformation(parameter);
        login(account);
        if (!this.isPremium()) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() == null) {
            Form form = br.getForm(0);
            if (form.getVars().containsKey("password")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                form.put("password", passCode);
            }
            br.submitForm(form);
            if (br.containsHTML(">Wrong password<")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            url = br.getRegex("24 hours<br><br>.*?<span style.*?>.*?<a href=\"(.*?)\">.*?</a>").getMatch(0);
        } else {
            url = br.getRedirectLocation();
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, url, true, 0);
        dl.startDownload();
    }

    public String getCaptcha() {
        String captcha = br.getRegex(Pattern.compile("<b>Enter code below:</b></td></tr>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String captchas[][] = new Regex(captcha, Pattern.compile("<span.*?padding-left:(.*?)px;.*?>(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
        String retcap = "";
        int currentorder = Integer.MAX_VALUE;
        for (int i = 0; i < captchas.length; i++) {
            int x = 0;
            currentorder = Integer.MAX_VALUE;
            for (int y = 0; y < captchas.length; y++) {
                if (Integer.parseInt(captchas[y][0]) < currentorder) {
                    currentorder = Integer.parseInt(captchas[y][0]);
                    x = y;
                }
            }
            retcap = retcap + captchas[x][1];
            captchas[x][0] = Integer.toString(Integer.MAX_VALUE);
        }
        return retcap;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        Form form = br.getForm(0);
        form.getVars().remove("method_premium");
        br.submitForm(form);

        String captcha = getCaptcha();
        form = br.getForm(0);
        form.put("code", captcha);
        if (form.getVars().containsKey("password")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("password", passCode);
        }
        br.setFollowRedirects(false);
        sleep(20000, downloadLink);
        br.submitForm(form);
        if (br.containsHTML(">Wrong password<")) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (br.containsHTML("<b>Enter code below:</b>")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        br.setDebug(true);
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), false, 1);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
