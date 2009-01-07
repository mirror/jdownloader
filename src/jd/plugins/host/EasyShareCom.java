package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
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
    }

    @Override
    public String getAGBLink() {
        return "http://www.easy-share.com/tos.html";
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.easy-share.com/");
        br.getPage("http://www.easy-share.com/cgi-bin/owner.cgi?action=login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://www.easy-share.com/", "PREMIUM") == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private void isexpired(Account account) throws MalformedURLException, PluginException {
        if (br.getCookie("http://www.easy-share.com/", "PREMIUMSTATUS") == null || !br.getCookie("http://www.easy-share.com/", "PREMIUMSTATUS").equalsIgnoreCase("ACTIVE")) {
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
        try {
            isexpired(account);
        } catch (PluginException e) {
            ai.setValid(false);
            ai.setExpired(true);
            return ai;
        }
        String expires = br.getRegex(Pattern.compile("<b>Expires on:  </b>(.*?)<br>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (expires == null) {
            ai.setValid(false);
            return ai;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
        try {
            Date date = dateFormat.parse(expires);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
        }
        return ai;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex(Pattern.compile("<title>Download(.*?), upload your files and earn money.</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        String followurl = br.getRegex(Pattern.compile("<div id=\"dwait\">.*?<br>.*?<script type=\"text/javascript\">.*?u='(.*?)'", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (followurl == null) {
            String filesize = br.getRegex("File size:(.*?)\\.").getMatch(0);
            if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
            return true;
        }
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
        if (!br.getRegex("File size:(.*?)\\.").matches()) {
            String wait = br.getRegex(Pattern.compile("<script type=\"text/javascript\">.*?u='.*?';.*?w='(.*?)';.*?setTimeout", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            long waitfor = new Long(wait) * 1000;
            if (waitfor > 40000l) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitfor); }
            sleep(waitfor, downloadLink);
            br.getPage(downloadLink.getDownloadURL());
        }
        if (br.containsHTML("Hourly traffic exceeded")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }
        Form form = br.getForm(0);
        String captchaUrl = br.getRegex("<form action=\".*?\".*?method=\"POST\">.*?<br>.*?<img src=\"(.*?)\">").getMatch(0);
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
