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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.http.Request;
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
import jd.utils.JDUtilities;

public class Megauploadcom extends PluginForHost {

    static private final String ERROR_FILENOTFOUND = "Die Datei konnte leider nicht gefunden werden";

    static private final String ERROR_TEMP_NOT_AVAILABLE = "Zugriff auf die Datei ist vor";

    private static final String PATTERN_PASSWORD_WRONG = "Wrong password! Please try again";

    static private final String SIMPLEPATTERN_CAPTCHA_POST_URL = "<form method=\"POST\" action=\"(.*?)\" target";

    static private final String SIMPLEPATTERN_CAPTCHA_URl = " <img src=\"/capgen\\.php?(.*?)\">";

    static private final String SIMPLEPATTERN_GEN_DOWNLOADLINK = "var (.*?) = String\\.fromCharCode\\(Math\\.abs\\((.*?)\\)\\);(.*?)var (.*?) = '(.*?)' \\+ String\\.fromCharCode\\(Math\\.sqrt\\((.*?)\\)\\);";

    static private final String SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK = "Math\\.sqrt\\((.*?)\\)\\);(.*?)document\\.getElementById\\(\"(.*?)\"\\)\\.innerHTML = '<a href=\"(.*?)' (.*?) '(.*?)\"(.*?)onclick=\"loadingdownload\\(\\)";

    private static final String MU_PARAM_PORT = "MU_PARAM_PORT";

    private String captchaPost;

    private String captchaURL;

    private HashMap<String, String> fields;

    private boolean tempUnavailable = false;

    private static int simultanpremium = 1;

    public Megauploadcom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.megaupload.com/premium/en/");
        setConfigElements();
    }

    public int usePort() {
        switch (JDUtilities.getConfiguration().getIntegerProperty(MU_PARAM_PORT)) {
        case 1:
            return 800;
        case 2:
            return 1723;
        default:
            return 80;
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        br.setAcceptLanguage("en, en-gb;q=0.8");
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!this.isPremium()) {
            ai.setStatus("Free Membership");
            ai.setValid(true);
            return ai;
        }
        String cookie = br.getCookie("http://megaupload.com", "user");
        br.getPage("http://www.megaupload.com/xml/premiumstats.php?confirmcode=" + cookie + "&language=en&uniq=" + System.currentTimeMillis());
        String days = br.getRegex("daysremaining=\"(\\d*?)\"").getMatch(0);
        ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(days) * 24 * 50 * 50 * 1000));
        if (days == null || days.equals("0")) ai.setExpired(true);
        br.getPage("http://www.megaupload.com/xml/rewardpoints.php?confirmcode=" + br.getCookie("http://megaupload.com", "user") + "&language=en&uniq=" + System.currentTimeMillis());
        String points = br.getRegex("availablepoints=\"(\\d*?)\"").getMatch(0);
        ai.setPremiumPoints(Integer.parseInt(points));

        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        DownloadLink downloadLink = (DownloadLink) parameter;
        getFileInformation(parameter);
        login(account);
        if (!this.isPremium()) {
            simultanpremium = 1;
            handleFree0(downloadLink);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        String id = Request.parseQuery(link).get("d");
        br.setFollowRedirects(false);
        String dlUrl = "http://megaupload.com/?d=" + id;
        br.getPage(dlUrl);
        handlePw(downloadLink);
        if (br.getRedirectLocation() == null) {
            String[] tmp = br.getRegex(SIMPLEPATTERN_GEN_DOWNLOADLINK).getRow(0);
            if (tmp == null) throw new PluginException(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            Character l = (char) Math.abs(Integer.parseInt(tmp[1].trim()));
            String i = tmp[4] + (char) Math.sqrt(Integer.parseInt(tmp[5].trim()));
            tmp = br.getRegex(SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK).getRow(0);
            if (tmp == null) throw new PluginException(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            dlUrl = Encoding.htmlDecode(tmp[3] + i + l + tmp[5]);
        }
        br.setFollowRedirects(true);
        dlUrl = dlUrl.replaceFirst("megaupload\\.com/", "megaupload\\.com:" + usePort() + "/");
        dl = br.openDownload(downloadLink, dlUrl, true, 0);
        dl.startDownload();
    }

    public String getAGBLink() {
        return "http://www.megaupload.com/terms/";
    }

    public boolean isPremium() {
        return br.containsHTML("fo.addVariable\\(\"premium\",\"1\"\\);");
    }

    public void login(Account account) throws IOException, PluginException {
        br.postPage("http://megaupload.com/en/", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        String cookie = br.getCookie("http://megaupload.com", "user");
        if (cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">Dieser Link ist leider nicht verfügbar.<")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getXPathElement("/html/body/center/center/div/div[13]/div[2]/div[2]/table/tbody/tr/td/div").trim();
        String size = br.getXPathElement("/html/body/center/center/div/div[13]/div[2]/div[2]/table/tbody/tr/td/div[2]").trim();
        if (filename == null || filename.length() == 0) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(size));
        return true;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return (tempUnavailable ? "<Temp. unavailable> " : "") + downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handlePw(DownloadLink downloadLink) throws PluginException, InterruptedException, MalformedURLException, IOException {
        String pwdata = HTMLParser.getFormInputHidden(br + "", "passwordbox", "passwordcountdown");
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        if (pwdata != null && pwdata.indexOf("passkey") > 0) {
            String passCode;
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            br.postPage("http://" + new URL(link).getHost() + "/de/", pwdata + "&pass=" + Encoding.urlEncode(passCode));
            if (br.containsHTML(PATTERN_PASSWORD_WRONG)) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                downloadLink.setProperty("pass", passCode);
            }
        }
    }

    public void handleFree0(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();
        DownloadLink downloadLink = (DownloadLink) parameter;
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        br.setFollowRedirects(true);

        br.setCookie(parameter.getDownloadURL(), "l", "de");
        br.setCookie(parameter.getDownloadURL(), "v", "1");
        br.setCookie(parameter.getDownloadURL(), "ve_view", "1");
        br.getPage(link);
        if (br.containsHTML(ERROR_TEMP_NOT_AVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        if (br.containsHTML(ERROR_FILENOTFOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        captchaURL = "http://" + new URL(link).getHost() + "/capgen.php" + br.getRegex(SIMPLEPATTERN_CAPTCHA_URl).getMatch(0);
        fields = HTMLParser.getInputHiddenFields(br + "", "checkverificationform", "passwordhtml");
        captchaPost = br.getRegex(SIMPLEPATTERN_CAPTCHA_POST_URL).getMatch(0);

        if (captchaURL.endsWith("null") || captchaPost == null) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        }

        File file = this.getLocalCaptchaFile(this);
        logger.info("Captcha " + captchaURL);
        HTTPConnection con = br.cloneBrowser().openGetConnection(captchaURL);

        Browser.download(file, con);

        String code = Plugin.getCaptchaCode(file, this, downloadLink);

        br.postPage(captchaPost, Plugin.joinMap(fields, "=", "&") + "&imagestring=" + code);
        if (br.containsHTML(SIMPLEPATTERN_CAPTCHA_URl)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        handlePw(downloadLink);
        String Waittime = br.getRegex(Pattern.compile("<div style=.*?id=\"downloadhtml\"></div>.*?<script language=\"Javascript\">.*?x\\d+=(\\d+);", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (Waittime == null) {
            sleep(45 * 1000l, downloadLink);
        } else {
            sleep(Integer.parseInt(Waittime.trim()) * 1000l, downloadLink);
        }

        String[] tmp = br.getRegex(SIMPLEPATTERN_GEN_DOWNLOADLINK).getRow(0);
        Character l = (char) Math.abs(Integer.parseInt(tmp[1].trim()));
        String i = tmp[4] + (char) Math.sqrt(Integer.parseInt(tmp[5].trim()));
        tmp = br.getRegex(SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK).getRow(0);
        String url = Encoding.htmlDecode(tmp[3] + i + l + tmp[5]);
        url = url.replaceFirst("megaupload\\.com/", "megaupload\\.com:" + usePort() + "/");
        dl = br.openDownload(downloadLink, url, true, 1);
        if (!dl.getConnection().isOK()) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            if (dl.getConnection().getHeaderField("Retry-After") != null) {
                dl.getConnection().disconnect();
                br.getPage("http://www.megaupload.com/premium/de/?");
                String wait = br.getRegex("Warten Sie bitte (.*?)Minuten").getMatch(0);
                if (wait != null) {
                    linkStatus.setValue(Integer.parseInt(wait.trim()) * 60 * 1000l);
                } else {
                    linkStatus.setValue(120 * 60 * 1000);
                }
                return;
            } else {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            }

        }
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        }

        dl.startDownload();
        // Wenn ein Download Premium mit mehreren chunks angefangen wird, und
        // dann versucht wird ihn free zu resumen, schlägt das fehl, weil jd die
        // mehrfachchunks aus premium nicht resumen kann.
        // In diesem Fall wird der link resetted.
        if (linkStatus.hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && linkStatus.getErrorMessage().contains("Limit Exceeded")) {
            downloadLink.setChunksProgress(null);
            linkStatus.setStatus(LinkStatus.ERROR_RETRY);
        }
    }

    public void handleFree(DownloadLink parameter) throws Exception {
        getFileInformation(parameter);
        handleFree0(parameter);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
        captchaPost = null;
        captchaURL = null;
        fields = null;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        String[] ports = new String[] { "80", "800", "1723" };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, JDUtilities.getConfiguration(), MU_PARAM_PORT, ports, "Use this Port:").setDefaultValue("80"));
    }
}
