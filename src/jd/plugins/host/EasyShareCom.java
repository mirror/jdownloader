package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
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

public class EasyShareCom extends PluginForHost {

    public EasyShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.easy-share.com/cgi-bin/premium.cgi");
        /* brauche neuen prem account zum einbauen und testen */
    }

    @Override
    public String getAGBLink() {
        return "http://www.easy-share.com/tos.html";
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.easy-share.com/");
        br.setDebug(true);
        Form login = br.getForm(0);
        login.put("login", Encoding.urlEncode(account.getUser()));
        login.put("password", Encoding.urlEncode(account.getPass()));
        login.action = "http://www.easy-share.com/accounts/login";

        br.submitForm(login);

        if (br.getCookie("http://www.easy-share.com/", "PREMIUM") == null) {
            account.setEnabled(false);

            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }

    }

    private Date isexpired(Account account) throws MalformedURLException, PluginException {
        HashMap<String, Cookie> cookies = br.getCookies().get("easy-share.com");
        Cookie premstatus = cookies.get("PREMIUMSTATUS");
        if (premstatus == null || !premstatus.getValue().equalsIgnoreCase("ACTIVE")) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
        return premstatus.getExpires();
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        try {
            ai.setValidUntil(isexpired(account).getTime());
        } catch (PluginException e) {
            e.printStackTrace();
            ai.setValid(false);
            ai.setExpired(true);
            return ai;
        }
        return ai;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        br.setCookie("http://www.easy-share.com", "language", "en");
        String filename = br.getRegex(Pattern.compile("You are requesting<strong>(.*?)</strong>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex("You are requesting<strong>.*?</strong>(.*?)<").getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        String wait = br.getRegex("id=\"freeButton\" value=\" Seconds to wait:(.*?)\"").getMatch(0);
        int waittime = 0;
        if (wait != null) waittime = Integer.parseInt(wait.trim());
        if (waittime > 60) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime * 1000l);
        } else {
            sleep(waittime * 1000l, downloadLink);
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Please wait or buy a Premium membership")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        String id = new Regex(downloadLink.getDownloadURL(), "/(\\d+)\\.html").getMatch(0);
        if (id == null) id = new Regex(downloadLink.getDownloadURL(), "/(\\d+)/.+").getMatch(0);
        br.getPage("http://www.easy-share.com/c/" + id);

        Form form = br.getForm(3);
        String captchaUrl = form.getRegex("<img src=\"(.*?)\">").getMatch(0);
        File captchaFile = this.getLocalCaptchaFile(this);
        try {
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaUrl));
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
        form.put("captcha", captchaCode);
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, form, true, 1);
        if (!dl.getConnection().isContentDisposition()) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        dl.startDownload();
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        login(account);
        isexpired(account);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL(), true, 0);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
