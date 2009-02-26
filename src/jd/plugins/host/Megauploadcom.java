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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.http.requests.Request;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.parser.html.Form;
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

    private static final String MU_PARAM_PORT = "MU_PARAM_PORT";

    private static final String CAPTCHA_MODE = "CAPTCHAMODE";

    private String user;

    private String dlID;
    private static ArrayList<String[]> CACHE = new ArrayList<String[]>();
    private HashMap<String, String> UserInfo = new HashMap<String, String>();

    private static int simultanpremium = 1;

    public Megauploadcom(PluginWrapper wrapper) {
        super(wrapper);

        this.enablePremium("http://megaupload.com/premium/en/");
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

    public boolean isPremium() {
        if (UserInfo.containsKey("p")) {
            Date d = new Date(Long.parseLong(UserInfo.get("p")) * 1000l);
            if (d.compareTo(new Date()) >= 0) return true;
        }
        return false;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!UserInfo.containsKey("s")) {
            ai.setValid(false);
            return ai;
        }
        if (!UserInfo.containsKey("p")) {
            ai.setValid(true);
            ai.setStatus("Free Membership");
            return ai;
        }
        Date d = new Date(Long.parseLong(UserInfo.get("p")) * 1000l);
        if (d.compareTo(new Date()) >= 0) {
            ai.setValid(true);
            ai.setValidUntil(d.getTime());
        } else {
            ai.setValid(true);
            ai.setStatus("Free Membership");
        }
        return ai;
    }

    public String getDownloadID(DownloadLink link) throws MalformedURLException {
        return Request.parseQuery(link.getDownloadURL()).get("d");
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        getFileInformation(link);
        login(account);
        if (!this.isPremium()) {
            simultanpremium = 1;
            handleFree0(link, account);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        br.setFollowRedirects(false);
        getRedirect("http://megaupload.com/mgr_dl.php?d=" + dlID + "&u=" + user, link);
        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(dlID)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l); }
        String url = br.getRedirectLocation();
        doDownload(link, url, true, 0);
    }

    private void doDownload(DownloadLink link, String url, boolean resume, int chunks) throws Exception {
        url = url.replaceFirst("megaupload\\.com/", "megaupload\\.com:" + usePort() + "/");
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = br.openDownload(link, url, resume, chunks);
        if (!dl.getConnection().isOK()) {
            dl.getConnection().disconnect();
            if (dl.getConnection().getResponseCode() == 503) {
                String wait = dl.getConnection().getHeaderField("Retry-After");
                if (wait == null) wait = "120";
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already Loading", Integer.parseInt(wait) * 1000l);
            }
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
        if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && link.getLinkStatus().getErrorMessage().contains("Limit Exceeded")) {
            link.setChunksProgress(null);
            link.getLinkStatus().setStatus(LinkStatus.ERROR_RETRY);
        }
    }

    private void getRedirect(String url, DownloadLink downloadLink) throws PluginException, InterruptedException {
        try {
            br.getPage(url);
        } catch (IOException e) {
            try {
                String passCode;
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                br.getPage(url + "&p=" + passCode);
                downloadLink.setProperty("pass", passCode);
                return;
            } catch (IOException e2) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.errors.wrongpassword", "Password wrong"));
            }
        }
    }

    public String getAGBLink() {
        return "http://megaupload.com/terms/";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.postPage("http://megaupload.com/mgr_login.php", "u=" + account.getUser() + "&b=0&p=" + JDHash.getMD5(account.getPass()));
        if (br.getRegex("^e$").matches()) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
        UserInfo = Request.parseQuery(br + "");
        user = UserInfo.get("s");
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        dlID = getDownloadID(downloadLink);
        user = null;
        br.postPage("http://megaupload.com/mgr_linkcheck.php", "id0=" + dlID);
        String temp = Encoding.htmlDecode(br + "");
        HashMap<String, String> query = Request.parseQuery(temp);
        if (!query.containsKey("id0") || !query.get("id0").equals("0") || query.get("n") == null || query.get("s") == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(query.get("n"));
        downloadLink.setDownloadSize(Long.parseLong(query.get("s")));
        downloadLink.setDupecheckAllowed(true);
        return true;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handleFree1(DownloadLink link, Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://megaupload.com", "l", "en");
        if (account != null) {
            br.getPage("http://megaupload.com/?c=login");
            br.postPage("http://megaupload.com/?c=login", "login=1&redir=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        }
        br.getPage("http://megaupload.com/?d=" + dlID);
        if (br.containsHTML("trying to download is larger than")) throw new PluginException(LinkStatus.ERROR_FATAL, "File is over 1GB and needs Premium Account");
        Form form = br.getForm(0);
        if (form.containsHTML("logout")) form = br.getForm(1);
        if (form.containsHTML("filepassword")) {
            String passCode;
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            form.put("filepassword", passCode);
            br.submitForm(form);
            form = br.getForm(0);
            if (form != null && form.containsHTML("logout")) form = br.getForm(1);
            if (form != null && form.containsHTML("filepassword")) {
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                link.setProperty("pass", passCode);
            }
        }
        if (form != null && form.containsHTML("captchacode")) {
            String captcha = form.getRegex("Enter this.*?src=\"(.*?gencap.*?)\"").getMatch(0);
            File file = this.getLocalCaptchaFile(this);
            URLConnectionAdapter con = br.cloneBrowser().openGetConnection(captcha);
            Browser.download(file, con);
            String code = getCode(file);
            boolean db = false;
            if (code != null) {
                db = true;
            } else {

                try {
                    code = Plugin.getCaptchaCode(file, this, link);
                } catch (PluginException ee) {

                }
            }
            if (this.getPluginConfig().getIntegerProperty(CAPTCHA_MODE, 0) != 1) {
                if (code == null || code.contains("-") || code.trim().length() != 4) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 1000l); }
            }
            if (code == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            form.put("captcha", code);
            br.submitForm(form);
            form = br.getForm(0);
            if (form != null && form.containsHTML("logout")) form = br.getForm(1);
            if (form != null && form.containsHTML("captchacode")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (CACHE != null && !db) {
                CACHE.add(new String[] { JDHash.getMD5(file), code });

                HashMap<String, String> map = new HashMap<String, String>();
                if (CACHE.size() > 2) {
                    for (String[] h : CACHE) {
                        map.put(h[0], h[1]);
                    }
                    Browser c = br.cloneBrowser();

                    try {
                        c.postPage("http://service.jdownloader.org/tools/c.php", map);
                    } catch (Exception e) {

                    }
                    if (!c.getRequest().getHttpConnection().isOK()) CACHE = null;
                    CACHE.clear();
                }
            }

        }
        // String Waittime =
        // br.getRegex(Pattern.compile("<script.*?java.*?>.*?count=(\\d+);",
        // Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        // if (Waittime == null) {
        // sleep(45 * 1000l, link);
        // } else {
        // sleep(Integer.parseInt(Waittime.trim()) * 1000l, link);
        // }
        String url = br.getRegex("id=\"downloadlink\"><a href=\"(.*?)\"").getMatch(0);
        doDownload(link, url, true, 1);
    }

    private String getCode(File file) {
        String hash = JDHash.getMD5(file);

        String list = JDIO.getLocalFile(JDUtilities.getResourceFile("jd/captcha/methods/megaupload.com/c.db"));

        int id = list.indexOf(hash);
        if (id < 0) return null;
        String code = list.substring(id + 33, id + 33 + 4);
        return code;
    }

    public void handleFree0(DownloadLink link, Account account) throws Exception {
        br.setFollowRedirects(false);
        if (user != null) {
            getRedirect("http://megaupload.com/mgr_dl.php?d=" + dlID + "&u=" + user, link);
        } else {
            getRedirect("http://megaupload.com/mgr_dl.php?d=" + dlID, link);
        }
        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(dlID)) {
            if (this.getPluginConfig().getIntegerProperty(CAPTCHA_MODE, 0) != 2) {
                handleFree1(link, account);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            }
            return;
        }
        String url = br.getRedirectLocation();
        doDownload(link, url, true, 1);
    }

    public void handleFree(DownloadLink parameter) throws Exception {
        user = null;
        getFileInformation(parameter);
        handleFree0(parameter, null);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        String[] ports = new String[] { "80", "800", "1723" };

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, JDUtilities.getConfiguration(), MU_PARAM_PORT, ports, JDLocale.L("plugins.host.megaupload.ports", "Use this port:")).setDefaultValue("80"));
        String[] captchmodes = new String[] { JDLocale.L("plugins.host.megaupload.captchamode_auto", "auto"), JDLocale.L("plugins.host.megaupload.captchamode_no_reconnect", "avoid reconnects"), JDLocale.L("plugins.host.megaupload.captchamode_no_captcha", "avoid captchas") };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, this.getPluginConfig(), CAPTCHA_MODE, captchmodes, JDLocale.L("plugins.host.megaupload.captchamode.title", "Captcha mode:")).setDefaultValue(JDLocale.L("plugins.host.megaupload.captchamode_auto", "auto")));

    }
}
