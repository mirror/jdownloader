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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.http.requests.Request;
import jd.nutils.JDHash;
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
    private static int FREE = 30;
    // private static int FREE = 1;

    private static int simultanpremium = 1;

    private String user;

    private static Boolean DL = false;

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

    public boolean isPremium() throws IOException {
        br.getPage("http://www.megaupload.com/?c=account");
        String type = br.getRegex(Pattern.compile("<TD>Account type:</TD>.*?<TD><b>(.*?)</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (type == null || type.equalsIgnoreCase("regular")) return false;
        return true;
    }

    @Override
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setValid(true);
            ai.setStatus("Free Membership");
            return ai;
        }
        String days = br.getRegex("<TD><b>Premium</b>.*?\\((\\d+) days remaining - <a").getMatch(0);
        if (days != null && !days.equalsIgnoreCase("Unlimited")) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(days) * 24 * 50 * 50 * 1000));
        } else if (days == null || days.equals("0")) {
            ai.setExpired(true);
            ai.setValid(false);
            return ai;
        }
        String points = br.getRegex(Pattern.compile("<TD>Reward points available:</TD>.*?<TD><b>(\\d+)</b> ", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) ai.setPremiumPoints(Integer.parseInt(points));
        ai.setValid(true);
        return ai;
    }

    public String getDownloadID(DownloadLink link) throws MalformedURLException {
        return Request.parseQuery(link.getDownloadURL()).get("d");
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        getFileInformation(link);
        br.setDebug(true);
        login(account);

        if (!isPremium()) {
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
        String url = null;

        br.setFollowRedirects(false);
        br.getPage("http://megaupload.com/?d=" + getDownloadID(link));

        if (br.getRedirectLocation() == null) {
            Form form = br.getForm(0);
            if (form != null && form.containsHTML("logout")) form = br.getForm(1);
            if (form != null && form.containsHTML("filepassword")) {
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
            url = br.getRegex("id=\"downloadlink\">.*?<a href=\"(.*?)\"").getMatch(0);
            if (url == null) url = br.getRedirectLocation();
        } else {
            url = br.getRedirectLocation();
        }
        doDownload(link, url, true, 0);
    }

    private void doDownload(DownloadLink link, String url, boolean resume, int chunks) throws Exception {
        url = url.replaceFirst("megaupload\\.com/", "megaupload\\.com:" + usePort() + "/");
        br.setFollowRedirects(true);
        br.setDebug(true);
        try {
            dl = br.openDownload(link, url, resume, chunks);
            if (!dl.getConnection().isOK()) {
                dl.getConnection().disconnect();
                if (dl.getConnection().getResponseCode() == 503) {
                    String wait = dl.getConnection().getHeaderField("Retry-After");
                    if (wait == null) wait = "120";
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already Loading or DownloadLimit exceeded!", Integer.parseInt(wait) * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);

            }
            if (!dl.getConnection().isContentDisposition()) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            }

            dl.startDownload();
            // Wenn ein Download Premium mit mehreren chunks angefangen wird,
            // und
            // dann versucht wird ihn free zu resumen, schlägt das fehl, weil jd
            // die
            // mehrfachchunks aus premium nicht resumen kann.
            // In diesem Fall wird der link resetted.
            if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && link.getLinkStatus().getErrorMessage() != null && link.getLinkStatus().getErrorMessage().contains("Limit Exceeded")) {
                link.setChunksProgress(null);
                link.getLinkStatus().setStatus(LinkStatus.ERROR_RETRY);
            }
        } finally {
            if (dl.getConnection() != null) dl.getConnection().disconnect();

        }
    }

    @Override
    public String getAGBLink() {
        return "http://megaupload.com/terms/";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://megaupload.com", "l", "en");
        if (account.getUser().trim().equalsIgnoreCase("cookie")) {
            br.setCookie("http://megaupload.com", "user", account.getPass());
            br.setCookie("http://www.megaupload.com", "user", account.getPass());
            br.setDebug(true);
            br.getPage("http://megaupload.com/");
        } else {
            br.setCookie("http://megaupload.com", "l", "en");
            br.getPage("http://megaupload.com/?c=login");
            br.postPage("http://megaupload.com/?c=login", "login=1&redir=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));

        }
        user = br.getCookie("http://megaupload.com", "user");
        br.setCookie("http://megaupload.com", "user", user);
        br.setCookie("http://www.megaupload.com", "user", user);
        if (user == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null) return false;

        HashMap<String, String> map = new HashMap<String, String>();
        int i = 0;
        String id;
        for (DownloadLink u : urls) {
            id = "00000";
            try {
                id = getDownloadID(u);
            } catch (Exception e) {
                e.printStackTrace();
            }
            map.put("id" + i, id);
            i++;
        }

        // POST /mgr_linkcheck.php HTTP/1.1
        // Accept: */*
        // Accept-Encoding: *;q=0.1
        // TE: trailers
        // Expect: 100-continue
        // Host: www.megaupload.com
        // Connection: TE
        // Date: Tue, 10 Mar 2009 16:15:47 GMT
        // Cookie:
        // l=de;__utmz=216392970.1234861776.1.1.utmcsr=(direct)|utmccn=(direct
        // )|utmcmd
        // =(none);__utma=216392970.610707062448237200.1234861776.1234890668
        // .1234890671.8
        // User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)
        // Content-Length: 12
        // Content-Type: application/x-www-form-urlencoded
        //        
        // this.setBrowserExclusive();
        //        

        br.getHeaders().clear();
        br.getHeaders().setDominant(true);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Encoding", "*;q=0.1");
        br.getHeaders().put("TE", "trailers");
        br.getHeaders().put("Expect", "100-continue");
        br.getHeaders().put("Host", "www.megaupload.com");
        br.getHeaders().put("Connection", "TE");
        SimpleDateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);

        // Tue, 03 Mar 2009 18:29:35 GMT
        br.getHeaders().put("Date", df.format(new Date()) + " GMT");
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Accept-Language", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Referer", null);

        br.getHeaders().put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
        br.getHeaders().put("Content-Length", "12");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");

        try {

            String[] Dls = br.postPage("http://megaupload.com/mgr_linkcheck.php", map).split("&?(?=id[\\d]+=)");
            br.getHeaders().clear();
            br.getHeaders().setDominant(false);
            for (i = 1; i < Dls.length; i++) {
                String string = Dls[i];
                HashMap<String, String> queryQ = Request.parseQuery(Encoding.htmlDecode(string));
                try {
                    int d = Integer.parseInt(string.substring(2, string.indexOf('=')));
                    String name = queryQ.get("n");
                    DownloadLink downloadLink = urls[d];
                    if (name != null) {

                        downloadLink.setFinalFileName(name);
                        downloadLink.setDownloadSize(Long.parseLong(queryQ.get("s")));
                        downloadLink.setDupecheckAllowed(true);
                    }
                    downloadLink.setAvailable(false);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.checkLinks(new DownloadLink[] { downloadLink });

        return downloadLink.isAvailable();
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handleFree1(DownloadLink link, Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://megaupload.com", "l", "en");
        initHeaders(br);

        if (account != null) {
            login(account);
        }
        int captchTries = 10;
        Form form = null;
        String code = null;
        String hash = null;
        while (captchTries-- >= 0) {

            br.getPage("http://megaupload.com/?d=" + getDownloadID(link));
            String red = br.getRegex("document\\.location='(.*?)'").getMatch(0);
            if (red != null) {
                logger.severe("YOur IP got banned");
                br.getPage(red);
                String wait = br.getRegex("Please check back in (\\d+) minute").getMatch(0);
                long l = 30 * 60 * 1000l;
                if (wait != null) {
                    l = Long.parseLong(wait.trim());
                }
                // "throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 1000l); "
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, l);
            }
            if (br.containsHTML("trying to download is larger than")) throw new PluginException(LinkStatus.ERROR_FATAL, "File is over 1GB and needs Premium Account");
            form = br.getForm(0);

            if (form != null && form.containsHTML("logout")) form = br.getForm(1);
            if (form != null && form.containsHTML("filepassword")) {
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
                Browser c = br.cloneBrowser();

                c.getHeaders().put("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
                URLConnectionAdapter con = c.openGetConnection(captcha);
                Browser.download(file, con);
                hash = JDHash.getMD5(file);
                code = null;
                Browser br2 = new Browser();
                try {
                    br2.postPage("http://service.jdownloader.org/tools/cg.php", "h=" + hash);
                    if (br2.getHttpConnection().isOK() && !br2.containsHTML("error")) {
                        code = br2.toString().trim();

                    }
                } catch (Exception e) {

                }

                if (code == null) {

                    try {
                        code = Plugin.getCaptchaCode(file, this, link);

                    } catch (PluginException ee) {

                    }
                }
                if (this.getPluginConfig().getIntegerProperty(CAPTCHA_MODE, 0) != 1) {
                    if (code == null || code.contains("-") || code.trim().length() != 4) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 1000l); }
                }
                if (code == null) continue;
                form.put("captcha", code);
                br.submitForm(form);
                form = br.getForm(0);
                if (form != null && form.containsHTML("logout")) form = br.getForm(1);
                if (form != null && form.containsHTML("captchacode")) {
                    continue;
                } else {

                    break;
                }

            }
        }

        if (form != null && form.containsHTML("captchacode")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }

        if (code != null) {

            Browser br2 = new Browser();
            br2.postPage("http://service.jdownloader.org/tools/c.php", hash + "=" + code);
        }
        // waitForNext(link);
        link.getLinkStatus().setStatusText("Wait for start");
        link.requestGuiUpdate();

        String url = br.getRegex("id=\"downloadlink\">.*?<a href=\"(.*?)\"").getMatch(0);
        synchronized (DL) {
            if (Thread.currentThread().isInterrupted()) {
                link.getLinkStatus().setStatusText(null);
                link.requestGuiUpdate();
                return;
            }
            doDownload(link, url, true, 1);
        }
        // decreaseCounter();

    }

    private void initHeaders(Browser br) {
        br.getHeaders().clear();
        br.getHeaders().put("Referer", null);

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

    public void handleFree0(DownloadLink link, Account account) throws Exception {

        br.setFollowRedirects(false);
        String dlID = getDownloadID(link);
        br.getHeaders().clear();
        br.getHeaders().setDominant(true);
        br.getHeaders().put("Accept", "text/plain,text/html,*/*;q=0.3");
        br.getHeaders().put("Accept-Encoding", "*;q=0.1");
        br.getHeaders().put("TE", "trailers");
        br.getHeaders().put("Host", "www.megaupload.com");
        br.getHeaders().put("Connection", "TE");
        br.getHeaders().put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");

        if (user != null) {
            getRedirect("http://megaupload.com/mgr_dl.php?d=" + dlID + "&u=" + user, link);
        } else {
            getRedirect("http://megaupload.com/mgr_dl.php?d=" + dlID, link);
        }
        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(dlID)) {
            if (this.getPluginConfig().getIntegerProperty(CAPTCHA_MODE, 0) != 2) {
                handleFree1(link, account);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already Loading or DownloadLimit exceeded!", 5 * 60 * 1000l);
            }
            return;
        }

        link.getLinkStatus().setStatusText("Wait for start");
        link.requestGuiUpdate();

        String url = br.getRedirectLocation();

        br.getHeaders().put("Host", new URL(url).getHost());
        br.getHeaders().put("Connection", "Keep-Alive,TE");
        synchronized (DL) {
            if (Thread.currentThread().isInterrupted()) {
                link.getLinkStatus().setStatusText(null);
                link.requestGuiUpdate();
                br.getHeaders().clear();
                br.getHeaders().setDominant(false);
                return;
            }
            doDownload(link, url, true, 1);
        }
        br.getHeaders().clear();
        br.getHeaders().setDominant(false);
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        user = null;
        br.setCookie("http://megaupload.com", "l", "en");
        getFileInformation(parameter);
        handleFree0(parameter, null);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE;
    }

    @Override
    public void reset() {
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void reset_downloadlink(DownloadLink link) {
        // link.setProperty("pass", null);
    }

    private void setConfigElements() {
        String[] ports = new String[] { "80", "800", "1723" };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, JDUtilities.getConfiguration(), MU_PARAM_PORT, ports, JDLocale.L("plugins.host.megaupload.ports", "Use this port:")).setDefaultValue("80"));
        String[] captchmodes = new String[] { JDLocale.L("plugins.host.megaupload.captchamode_auto", "auto"), JDLocale.L("plugins.host.megaupload.captchamode_no_reconnect", "avoid reconnects"), JDLocale.L("plugins.host.megaupload.captchamode_no_captcha", "avoid captchas") };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, this.getPluginConfig(), CAPTCHA_MODE, captchmodes, JDLocale.L("plugins.host.megaupload.captchamode.title", "Captcha mode:")).setDefaultValue(JDLocale.L("plugins.host.megaupload.captchamode_auto", "auto")));
    }

    public static void main(String[] args) throws IOException {
        int i = 0;
        HashMap<String, File> map = new HashMap<String, File>();
        File dir = JDUtilities.getResourceFile("caps/megaupload.com/captchas/");
        for (File f : dir.listFiles()) {

            map.put(JDHash.getMD5(f), f);
        }
        int ii = 0;
        while (true) {
            i++;
            Browser br = new Browser();
            br.setCookie("http://megaupload.com", "l", "en");
            br.getPage("http://www.megaupload.com/?d=ML38NV20");
            Form form = br.getForm(0);

            String captcha = form.getRegex("Enter this.*?src=\"(.*?gencap.*?)\"").getMatch(0);

            br.getHeaders().put("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
            URLConnectionAdapter con = br.openGetConnection(captcha);
            File file;
            Browser.download(file = JDUtilities.getResourceFile("caps/megaupload.com/captchas/caps_" + System.currentTimeMillis() + ".png"), con);

            String hash = JDHash.getMD5(file);

            if (map.containsKey(hash)) {
                ii++;

                System.out.println("Rem " + ii);
                file.delete();
            } else {
                map.put(hash, file);
            }

        }
    }
}
