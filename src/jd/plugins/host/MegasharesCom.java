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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

/*TODO: Support für andere Linkcards(bestimmte Anzahl Downloads,unlimited usw) einbauen*/

public class MegasharesCom extends PluginForHost {
    static private final String AGB_LINK = "http://d01.megashares.com/tos.php";

    private static String PLUGIN_PASS = null;

    public MegasharesCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.megashares.com/lc_order.php?tid=sasky");
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://d01.megashares.com/");
        br.postPage("http://d01.megashares.com/", "lc_email=" + Encoding.urlEncode(account.getUser()) + "&lc_pin=" + Encoding.urlEncode(account.getPass()) + "&lc_signin=Sign-In");
        if (br.getCookie("http://megashares.com", "linkcard") == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        String expires = br.getRegex("</font> Expires: <font.*?>(.*?)</font>").getMatch(0);
        if (expires == null) {
            ai.setValid(false);
            return ai;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.UK);
        try {
            Date date = dateFormat.parse(expires);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
        }
        return ai;
    }

    public void loadpage(DownloadLink downloadLink) throws IOException {
        boolean tmp = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            downloadLink.setUrlDownload(br.getRedirectLocation());
            br.getPage(downloadLink.getDownloadURL());
        }
        br.setFollowRedirects(tmp);
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        login(account);
        // Password protection
        loadpage(downloadLink);
        if (!checkPassword(downloadLink)) { return; }
        if (br.containsHTML("All download slots for this link are currently filled")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
        String dlLink = br.getRegex("<div id=\"dlink\"><a href=\"(.*?)\">Click").getMatch(0);
        if (dlLink == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, dlLink, true, -2);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        loadpage(downloadLink);
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // Cookies holen
        if (br.containsHTML("continue using Free service")) {
            loadpage(downloadLink);
        }
        // Password protection
        if (!checkPassword(downloadLink)) { return; }

        // Sie laden gerade eine datei herunter
        if (br.containsHTML("You already have the maximum")) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(60 * 1000l);
            return;
        }
        if (br.containsHTML("All download slots for this link are currently filled")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
        // Reconnet/wartezeit check
        String[] dat = br.getRegex("Your download passport will renew.*?in (\\d+):<strong>(\\d+)</strong>:<strong>(\\d+)</strong>").getRow(0);
        if (dat != null) {

            long wait = Long.parseLong(dat[1]) * 60000l + Long.parseLong(dat[2]) * 1000l;
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(wait);
            return;
        }

        // Captchacheck
        if (br.containsHTML("Your Passport needs to be reactivated.")) {
            String captchaAddress = br.getRegex("<dt>Enter the passport reactivation code in the graphic, then hit the \"Reactivate Passport\" button.</dt>.*?<dd><img src=\"(.*?)\" alt=\"Security Code\" style=.*?>").getMatch(0);
            File file = this.getLocalCaptchaFile(this);
            Browser c = br.cloneBrowser();
            Browser.download(file, c.openGetConnection(captchaAddress));

            HashMap<String, String> input = HTMLParser.getInputHiddenFields(br + "");

            String code = Plugin.getCaptchaCode(file, this, downloadLink);
            String geturl = downloadLink.getDownloadURL() + "&rs=check_passport_renewal&rsargs[]=" + code + "&rsargs[]=" + input.get("random_num") + "&rsargs[]=" + input.get("passport_num") + "&rsargs[]=replace_sec_pprenewal&rsrnd=" + (new Date().getTime());
            br.getPage(geturl);
            loadpage(downloadLink);

            if (br.containsHTML("You already have the maximum")) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setValue(30 * 1000l);
                return;
            }
            if (!checkPassword(downloadLink)) { return; }
        }
        // Downloadlink
        String url = br.getRegex("<div id=\"dlink\"><a href=\"(.*?)\">Click here to download</a>").getMatch(0);
        if (url == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        // Dateigröße holen
        dat = br.getRegex("<dt>Filename:&nbsp;<strong>(.*?)</strong>&nbsp;&nbsp;&nbsp;(.*?)</dt>").getRow(0);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, url, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl.startDownload();
    }

    private boolean checkPassword(DownloadLink link) throws IOException, PluginException, InterruptedException {

        if (br.containsHTML("This link requires a password")) {
            Form form = br.getFormbyValue("Validate Password");
            String pass = link.getStringProperty("password");
            if (pass != null) {
                form.put("passText", pass);
                br.submitForm(form);
                if (!br.containsHTML("This link requires a password")) { return true; }
            }
            pass = PLUGIN_PASS;
            if (pass != null) {
                form.put("passText", pass);
                br.submitForm(form);
                if (!br.containsHTML("This link requires a password")) { return true; }
            }
            int i = 0;
            while ((i++) < 5) {
                pass = Plugin.getUserInput(JDLocale.LF("plugins.hoster.passquestion", "Link '%s' is passwordprotected. Enter password:", link.getName()), link);
                if (pass != null) {
                    form.put("passText", pass);
                    br.submitForm(form);
                    if (!br.containsHTML("This link requires a password")) {
                        PLUGIN_PASS = pass;
                        link.setProperty("password", pass);
                        return true;
                    }
                }
            }

            link.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);
            link.getLinkStatus().setErrorMessage("Link password wrong");
            return false;
        }
        return true;
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        setBrowserExclusive();
        loadpage(downloadLink);

        if (br.containsHTML("continue using Free service")) {
            loadpage(downloadLink);
        }
        if (br.containsHTML("You already have the maximum")) {
            downloadLink.getLinkStatus().setStatusText("Unchecked due to already loading");
            return true;
        }
        if (br.containsHTML("All download slots for this link are currently filled")) {
            downloadLink.getLinkStatus().setStatusText("Unchecked due to already loading");
            return true;
        }
        if (br.containsHTML("This link requires a password")) {
            downloadLink.getLinkStatus().setStatusText("Password protected");
            return true;
        }
        String[] dat = br.getRegex("<dt>Filename:.*?<strong>(.*?)</strong>.*?size:(.*?)</dt>").getRow(0);
        if (dat == null) { return false; }
        downloadLink.setName(dat[0].trim());
        downloadLink.setDownloadSize(Regex.getSize(dat[1]));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}