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

package jd.plugins.host;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.requests.Request;
import jd.nutils.JDHash;
import jd.parser.html.HTMLParser;
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

    private String user;

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

        br.postPage("http://www.megaupload.com/mgr_login.php", "u=" + account.getUser() + "&b=0&p=" + JDHash.getMD5(account.getPass()));

        HashMap<String, String> query = Request.parseQuery(br + "");
        this.user = query.get("s");
        String validUntil = query.get("p");
        Date d = new Date(Long.parseLong(validUntil.trim()) * 1000l);
        if (d.compareTo(new Date()) >= 0) {
            ai.setValid(true);
        } else {
            ai.setValid(false);
        }
        ai.setValidUntil(d.getTime());
        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {

        LinkStatus linkStatus = parameter.getLinkStatus();
        DownloadLink downloadLink = (DownloadLink) parameter;
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        String id = Request.parseQuery(link).get("d");
        br.forceDebug(true);
        AccountInfo ai = this.getAccountInformation(account);
        if (!ai.isValid()) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
        br.setFollowRedirects(false);
        br.getPage("http://megaupload.com/mgr_dl.php?d=" + id + "&u=" + user);

        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(id)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l); }
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, 0);
        if (!dl.getConnection().isOK()) {

            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);

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
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        String id = Request.parseQuery(link).get("d");
        br.postPage("http://www.megaupload.com/mgr_linkcheck.php", "id0=" + id);

        HashMap<String, String> query = Request.parseQuery(br + "");
        if (!query.get("id0").equals("0")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(query.get("n"));

        downloadLink.setDownloadSize(Long.parseLong(query.get("s")));
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
        String id = Request.parseQuery(link).get("d");
        br.forceDebug(true);
        br.setFollowRedirects(false);
        br.getPage("/mgr_dl.php?d=" + id);

        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(id)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l); }
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, 1);
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
        // String[] ports = new String[] { "80", "800", "1723" };
        // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX,
        // JDUtilities.getConfiguration(), MU_PARAM_PORT, ports,
        // "Use this Port:").setDefaultValue("80"));
    }
}
