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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.Browser.BrowserException;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megaupload.com" }, urls = { "http://[\\w\\.]*?(megaupload)\\.com/.*?(\\?|&)d=[0-9A-Za-z]+" }, flags = { 2 })
public class Megauploadcom extends PluginForHost {

    private static final String MU_PARAM_PORT = "MU_PARAM_PORT";
    private static final String CAPTCHA_MODE = "CAPTCHAMODE";

    private static String wwwWorkaround = null;
    private static final Object Lock = new Object();
    private static int simultanpremium = 1;
    private boolean onlyapi = false;

    private String user;

    public Megauploadcom(PluginWrapper wrapper) {
        super(wrapper);

        this.enablePremium("http://www.megaupload.com/premium/en/");
        setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload("http://www.megaupload.com/?d=" + getDownloadID(link));
    }

    public int usePort() {
        switch (this.getPluginConfig().getIntegerProperty(MU_PARAM_PORT, 0)) {
        case 1:
            return 800;
        case 2:
            return 1723;
        default:
            return 80;
        }
    }

    private void checkWWWWorkaround() {
        synchronized (Lock) {
            if (wwwWorkaround != null) return;
            Browser tbr = new Browser();
            try {
                tbr.setConnectTimeout(10000);
                tbr.setReadTimeout(10000);
                tbr.getPage("http://www.megaupload.com");
                wwwWorkaround = "www.";
            } catch (BrowserException e) {
                if (e.getException() != null && e.getException() instanceof UnknownHostException) {
                    logger.info("Using Workaround for Megaupload DNS Problem!");
                    wwwWorkaround = "";
                    return;
                }

            } catch (IOException e) {
            } finally {
                if (wwwWorkaround == null) wwwWorkaround = "";
            }
        }
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://" + wwwWorkaround + "megaupload.com/?c=account");
        String type = br.getRegex(Pattern.compile("<TD>Account type:</TD>.*?<TD><b>(.*?)</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (type == null || type.equalsIgnoreCase("regular")) return false;
        return true;
    }

    // @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        checkWWWWorkaround();
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            account.setValid(true);
            ai.setStatus("Free Membership");
            return ai;
        }
        String type = br.getRegex(Pattern.compile("<TD>Account type:</TD>.*?<TD><b>(.*?)</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (type != null && !type.contains("Lifetime")) {
            String days = br.getRegex("<TD><b>Premium</b>.*?\\((\\d+) days remaining - <a").getMatch(0);
            if (days != null && !days.equalsIgnoreCase("Unlimited")) {
                ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(days) * 24 * 50 * 50 * 1000));
            } else if (days == null || days.equals("0")) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            }
        }
        String points = br.getRegex(Pattern.compile("<TD>Reward points available:</TD>.*?<TD><b>(\\d+)</b> ", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) ai.setPremiumPoints(Long.parseLong(points));
        account.setValid(true);
        return ai;
    }

    public String getDownloadID(DownloadLink link) throws MalformedURLException {
        HashMap<String, String> p = Request.parseQuery(link.getDownloadURL());
        return p.get("d").toUpperCase();
    }

    // @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        br = new Browser();
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
        br.getPage("http://" + wwwWorkaround + "megaupload.com/?d=" + getDownloadID(link));

        if (br.containsHTML("Unfortunately, the link you have clicked is not available")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        if (br.getRedirectLocation() == null) {
            Form form = br.getForm(0);
            if (form != null && form.containsHTML("logout")) form = br.getForm(1);
            if (form != null && form.containsHTML("filepassword")) {
                String passCode;
                if (link.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", link);
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
                    throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
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
        String waitb = br.getRegex("count=(\\d+);").getMatch(0);
        long waittime = 0;
        try {
            if (link.getBooleanProperty("waitworkaround", false)) {
                waittime = 60;
            } else if (waitb != null) {
                // MU seems not to check this currently. leave back the
                // waittimeworkaround to have a fallback feature
                waittime = 0;
                // waittime = Long.parseLong(waitb);
            }
        } catch (Exception e) {
        }
        if (waittime > 0) this.sleep(waittime * 1000, link);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, resume, chunks);
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
                if (link.getBooleanProperty("waitworkaround", false)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
                link.setProperty("waitworkaround", true);
                throw new PluginException(LinkStatus.ERROR_RETRY);
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

    // @Override
    public String getAGBLink() {
        return "http://www.megaupload.com/terms/";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
        if (account.getUser().trim().equalsIgnoreCase("cookie")) {
            br.setCookie("http://" + wwwWorkaround + "megaupload.com", "user", account.getPass());
            br.setDebug(true);
            br.getPage("http://" + wwwWorkaround + "megaupload.com/");
        } else {
            br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
            br.getPage("http://" + wwwWorkaround + "megaupload.com/?c=login");
            br.postPage("http://" + wwwWorkaround + "megaupload.com/?c=login", "login=1&redir=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        }
        user = br.getCookie("http://" + wwwWorkaround + "megaupload.com", "user");
        br.setCookie("http://" + wwwWorkaround + "megaupload.com", "user", user);
        if (user == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public boolean checkLinks(DownloadLink urls[]) {
        if (urls == null || urls.length == 0) return false;
        checkWWWWorkaround();
        if (urls.length == 1 && urls[0].getBooleanProperty("webcheck", false) == true && !onlyapi) {
            // SingleFileCheck before Download, bypass api only if api check
            // already done
            websiteFileCheck(urls[0], null);
            return true;
        }
        this.setBrowserExclusive();
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        int i = 0;
        String id;
        for (DownloadLink u : urls) {
            /* mark link being checked by api */
            id = "00000";
            try {
                id = getDownloadID(u);
                map.put("id" + i, id);
            } catch (Exception e) {
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            }
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
        br.getHeaders().put("Host", "" + wwwWorkaround + "megaupload.com");
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
        boolean ret = false;
        try {
            String[] Dls = br.postPage("http://" + wwwWorkaround + "megaupload.com/mgr_linkcheck.php", map).split("&?(?=id[\\d]+=)");
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
                        downloadLink.setAvailable(true);
                    } else {
                        downloadLink.setAvailable(false);
                        /* crosscheck if api says offline */
                        if (Dls.length < 10) {
                            websiteFileCheck(downloadLink, null);
                        }
                    }
                    downloadLink.setProperty("webcheck", true);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            ret = false;
        }
        ret = true;
        /* check files that could not checked by api */
        for (DownloadLink u : urls) {
            if (urls[0].getBooleanProperty("webcheck", false) == false) {
                websiteFileCheck(u, null);
            }
        }
        return ret;
    }

    private boolean websiteFileCheck(DownloadLink l, Browser br) {
        if (onlyapi) {
            l.setAvailable(true);
            /* api only, modus, dont try to check via webpage */
            return true;
        }
        try {
            if (br == null) {
                br = new Browser();
                br.setCookiesExclusive(true);
                br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
            }
            br.getPage("http://" + wwwWorkaround + "megaupload.com/?d=" + getDownloadID(l));
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            if (br.containsHTML("No htmlCode read") || br.containsHTML("This service is temporarily not available from your service area")) {
                logger.info("It seems Megaupload is blocked! Only API may work!");
                onlyapi = true;
                l.setAvailable(true);
                return true;
            }
            if (br.containsHTML("A temporary access restriction is place") || br.containsHTML("We have detected an elevated")) {
                logger.info("Megaupload blocked this IP: log" + br.toString());
                l.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                return true;
            }
            if (br.containsHTML("The file has been deleted because it was violating")) {
                l.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                l.getLinkStatus().setStatusText("Link abused or invalid");
                l.setAvailable(false);
                return false;
            }
            if (br.containsHTML("Invalid link")) {
                l.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                l.getLinkStatus().setStatusText("Link invalid");
                l.setAvailable(false);
                return false;
            }

            String filename = br.getRegex("<font style=.*?>Filename:</font> <font style=.*?>(.*?)</font><br>").getMatch(0).trim();
            String filesize = br.getRegex("<font style=.*?>File size:</font> <font style=.*?>(.*?)</font>").getMatch(0).trim();
            if (filename == null || filesize == null) {
                l.setAvailable(false);
            } else {
                /* maybe api check failed, then set name,size here */
                if (l.getBooleanProperty("webcheck", false) == false) {
                    l.setName(filename.trim());
                    l.setDownloadSize(Regex.getSize(filesize.trim()));
                    l.setProperty("webcheck", true);
                }
                l.setAvailable(true);
            }
            l.setAvailable(true);
            return true;
        } catch (Exception e) {
            l.setAvailable(false);
            return false;
        }
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        onlyapi = false;
        checkWWWWorkaround();
        this.setBrowserExclusive();
        br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
        websiteFileCheck(downloadLink, br);
        /* TODO: remove after needed info found */
        if (downloadLink.getAvailableStatus() != AvailableStatus.TRUE) {
            logger.info("DebugInfo: FIXME Filecheck! " + br.toString());
        }
        /* in case of ip blocking, set ip blocked */
        if (downloadLink.getAvailableStatus() == AvailableStatus.UNCHECKABLE) {
            String wait = br.getRegex("Please check back in (\\+d) minutes").getMatch(0);
            if (wait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait.trim()) * 60 * 1000l);
            } else
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 25 * 60 * 1000l);
        }
        if (downloadLink.getAvailableStatus() == AvailableStatus.FALSE) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return downloadLink.getAvailableStatus();
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    public void handleFree1(DownloadLink link, Account account) throws Exception {
        this.setBrowserExclusive();
        br = new Browser();
        br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
        if (account != null) {
            login(account);
        }
        int captchTries = 10;
        Form form = null;
        String code = null;

        while (captchTries-- >= 0) {

            br.getPage("http://" + wwwWorkaround + "megaupload.com/?d=" + getDownloadID(link));
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
                    passCode = Plugin.getUserInput("Password?", link);
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
                    throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
                } else {
                    link.setProperty("pass", passCode);
                }
            }
            if (form != null && form.containsHTML("captchacode")) {
                String captcha = form.getRegex("Enter this.*?src=\"(.*?gencap.*?)\"").getMatch(0);
                File file = this.getLocalCaptchaFile();
                Browser c = br.cloneBrowser();

                c.getHeaders().put("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
                URLConnectionAdapter con = c.openGetConnection(captcha);
                Browser.download(file, con);

                code = getCaptchaCode(file, link);

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
        if (form != null && form.containsHTML("captchacode")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        String url = br.getRegex("id=\"downloadlink\">.*?<a href=\"(.*?)\"").getMatch(0);
        doDownload(link, url, true, 1);
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
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            }
        }
    }

    public void handleFree0(DownloadLink link, Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        String dlID = getDownloadID(link);
        br.setDebug(true);
        br.getHeaders().clear();
        br.getHeaders().setDominant(true);
        br.getHeaders().put("Accept", "text/plain,text/html,*/*;q=0.3");
        br.getHeaders().put("Accept-Encoding", "*;q=0.1");
        br.getHeaders().put("TE", "trailers");
        br.getHeaders().put("Host", "" + wwwWorkaround + "megaupload.com");
        br.getHeaders().put("Connection", "TE");
        br.getHeaders().put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");

        if (user != null) {
            getRedirect("http://" + wwwWorkaround + "megaupload.com/mgr_dl.php?d=" + dlID + "&u=" + user, link);
        } else {
            getRedirect("http://" + wwwWorkaround + "megaupload.com/mgr_dl.php?d=" + dlID, link);
        }
        if (br.getRedirectLocation() == null || br.getRedirectLocation().toUpperCase().contains(dlID)) {
            if (this.getPluginConfig().getIntegerProperty(CAPTCHA_MODE, 0) != 2) {
                if (onlyapi) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "API ONLY! IP is already Loading or DownloadLimit exceeded!", 5 * 60 * 1000l);
                } else {
                    handleFree1(link, account);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already Loading or DownloadLimit exceeded!", 5 * 60 * 1000l);
            }
            return;
        }

        String url = br.getRedirectLocation();

        br.getHeaders().put("Host", new URL(url).getHost());
        br.getHeaders().put("Connection", "Keep-Alive,TE");

        doDownload(link, url, true, 1);

        br.getHeaders().clear();
        br.getHeaders().setDominant(false);
    }

    // @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        user = null;
        br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
        requestFileInformation(parameter);
        handleFree0(parameter, null);
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return this.getPluginConfig().getIntegerProperty("MAX_FREE_PARALELL_DOWNLOADS", 1);
    }

    // @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("waitworkaround", false);
    }

    private void setConfigElements() {
        String[] ports = new String[] { "80", "800", "1723" };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, this.getPluginConfig(), MU_PARAM_PORT, ports, JDL.L("plugins.host.megaupload.ports", "Use this port:")).setDefaultValue(0));
        String[] captchmodes = new String[] { JDL.L("plugins.host.megaupload.captchamode_auto", "auto"), JDL.L("plugins.host.megaupload.captchamode_no_reconnect", "avoid reconnects"), JDL.L("plugins.host.megaupload.captchamode_no_captcha", "avoid captchas") };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, this.getPluginConfig(), CAPTCHA_MODE, captchmodes, JDL.L("plugins.host.megaupload.captchamode.title", "Captcha mode:")).setDefaultValue(0));
    }

}
