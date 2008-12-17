package jd.plugins.host;

import java.io.File;
import java.io.IOException;
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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class FileHostMecom extends PluginForHost {
    private String captchaCode;
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
        return getVersion("$Revision: 4056 $");
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
            br.submitForm(form);
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
        String captchas[] = new Regex(captcha, Pattern.compile("<span.*?>(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getColumn(0);
        String retcap = "";
        for (String cap : captchas) {
            retcap = retcap + cap;
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
        sleep(20000, downloadLink);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        br.setDebug(true);
        dl = br.openDownload(downloadLink, form);
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
