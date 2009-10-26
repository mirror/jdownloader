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
import jd.controlling.JDLogger;
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
    private static final String MU_PORTROTATION = "MU_PORTROTATION";
    private final static String[] ports = new String[] { "80", "800", "1723" };
    private static String wwwWorkaround = null;
    private static final Object Lock = new Object();
    private static int simultanpremium = 1;
    private boolean onlyapi = false;
    private boolean usepremium = false;

    private static final Object PREMLOCK = new Object();

    public Megauploadcom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.megaupload.com/premium/en/");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload("http://www.megaupload.com/?d=" + getDownloadID(link));
    }

    public int usePort(DownloadLink link) {
        int port = this.getPluginConfig().getIntegerProperty(MU_PARAM_PORT, 0);
        if (this.getPluginConfig().getBooleanProperty("MU_PORTROTATION", true)) {
            port += link.getIntegerProperty(MU_PARAM_PORT, 0);
        }
        switch (port) {
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

    public boolean isPremium(Account account) throws IOException {
        if (account.getBooleanProperty("typeknown", false) == false) {
            br.getPage("http://" + wwwWorkaround + "megaupload.com/?c=account");
            String type = br.getRegex(Pattern.compile("<TD>Account type:</TD>.*?<TD><b>(.*?)</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (type == null || type.equalsIgnoreCase("regular")) {
                account.setProperty("ispremium", false);
                account.setProperty("typeknown", true);
                return false;
            } else {
                account.setProperty("ispremium", true);
                account.setProperty("typeknown", true);
                return true;
            }
        } else {
            return account.getBooleanProperty("ispremium", false);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        checkWWWWorkaround();
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account, false);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium(account)) {
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

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        boolean free = false;
        synchronized (PREMLOCK) {
            usepremium = true;
            requestFileInformation(link);
            login(account, true);
            if (!isPremium(account)) {
                simultanpremium = 1;
                free = true;
            } else {
                if (simultanpremium + 1 > 20) {
                    simultanpremium = 20;
                } else {
                    simultanpremium++;
                }
            }
        }
        if (free || onlyapi) {
            if (onlyapi) {
                handleAPIDownload(link, account);
            } else {
                handleWebsiteDownload(link, account);
            }
            return;
        }
        /* website download premium */
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
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        url = url.replaceFirst("megaupload\\.com/", "megaupload\\.com:" + usePort(link) + "/");
        br.setFollowRedirects(true);
        String waitb = br.getRegex("count=(\\d+);").getMatch(0);
        long waittime = 0;
        try {
            if (link.getIntegerProperty("waitworkaround2", 0) == 0) {
                /* try not to wait */
                waittime = 0;
            } else if (link.getIntegerProperty("waitworkaround2", 0) == 1) {
                /* try normal waittime */
                if (waitb != null) waittime = Long.parseLong(waitb);
            } else {
                /* last try with 60 secs */
                waittime = 60;
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
                    limitReached(link, Integer.parseInt(wait.trim()));
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            }
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                if (link.getIntegerProperty("waitworkaround2", 0) == 2) {
                    link.setProperty("waitworkaround2", 0);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
                }
                link.setProperty("waitworkaround2", link.getIntegerProperty("waitworkaround2", 0) + 1);
                if (br.containsHTML("gencap\\.php\\?")) {
                    logger.info("strange servererror: again a captcha?");
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                logger.info("MegaUpload Unknown Error: " + br.toString());
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

    @Override
    public String getAGBLink() {
        return "http://www.megaupload.com/terms/";
    }

    public void login(Account account, boolean cookielogin) throws IOException, PluginException {
        String user = account.getStringProperty("user", null);
        if (cookielogin && user != null) {
            br.setCookie("http://" + wwwWorkaround + "megaupload.com", "user", user);
            return;
        } else {
            if (account.getUser().trim().equalsIgnoreCase("cookie")) {
                this.setBrowserExclusive();
                br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
                br.setCookie("http://" + wwwWorkaround + "megaupload.com", "user", account.getPass());
                br.getPage("http://" + wwwWorkaround + "megaupload.com/");
            } else {
                this.setBrowserExclusive();
                br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
                br.getPage("http://" + wwwWorkaround + "megaupload.com/?c=login");
                br.postPage("http://" + wwwWorkaround + "megaupload.com/?c=login", "login=1&redir=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            }
            user = br.getCookie("http://" + wwwWorkaround + "megaupload.com", "user");
            br.setCookie("http://" + wwwWorkaround + "megaupload.com", "user", user);
            account.setProperty("user", user);
            if (user == null) {
                account.setProperty("ispremium", false);
                account.setProperty("typeknown", false);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @Override
    public boolean checkLinks(DownloadLink urls[]) {
        if (urls == null || urls.length == 0) return false;
        checkWWWWorkaround();
        Browser br = new Browser();
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
                        if (Dls.length < 4) {
                            websiteFileCheck(downloadLink, null);
                        }
                    }
                    downloadLink.setProperty("webcheck", true);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        /* check files that could not checked by api */
        if (!onlyapi) {
            for (DownloadLink u : urls) {
                if (urls[0].getBooleanProperty("webcheck", false) == false) {
                    websiteFileCheck(u, null);
                }
            }
        }
        return true;
    }

    private void websiteFileCheck(DownloadLink l, Browser br) {
        if (onlyapi) {
            l.setAvailable(true);
            /* api only modus, dont try to check via webpage */
            checkLinks(new DownloadLink[] { l });
            return;
        }
        try {
            if (br == null) {
                br = new Browser();
                br.setCookiesExclusive(true);
                br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
            }
            br.getPage("http://" + wwwWorkaround + "megaupload.com/?d=" + getDownloadID(l));
            if (br.containsHTML("location='http://www\\.megaupload\\.com/\\?c=msg")) br.getPage("http://www.megaupload.com/?c=msg");
            if (br.containsHTML("No htmlCode read") || br.containsHTML("This service is temporarily not available from your service area")) {
                logger.info("It seems Megaupload is blocked! Only API may work! " + br.toString());
                onlyapi = true;
                l.setAvailable(true);
                checkLinks(new DownloadLink[] { l });
                return;
            }
            if (br.containsHTML("A temporary access restriction is place") || br.containsHTML("We have detected an elevated")) {
                logger.info("Megaupload blocked this IP: log" + br.toString());
                l.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                return;
            }
            if (br.containsHTML("The file has been deleted because it was violating")) {
                l.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                l.getLinkStatus().setStatusText("Link abused or invalid");
                l.setAvailable(false);
                return;
            }
            if (br.containsHTML("Invalid link")) {
                l.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                l.getLinkStatus().setStatusText("Link invalid");
                l.setAvailable(false);
                return;
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
            return;
        } catch (Exception e) {
            logger.severe(e.toString());
            l.setAvailable(false);
        }
        return;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        onlyapi = false;
        checkWWWWorkaround();
        setBrowserExclusive();
        br.setCookie("http://" + wwwWorkaround + "megaupload.com", "l", "en");
        websiteFileCheck(downloadLink, br);
        /* in case of ip blocking, set ip blocked */
        if (downloadLink.getAvailableStatus() == AvailableStatus.UNCHECKABLE && usepremium == false) {
            String wait = br.getRegex("Please check back in (\\+d) minutes").getMatch(0);
            if (wait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait.trim()) * 60 * 1000l);
            } else
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 25 * 60 * 1000l);
        }
        if (downloadLink.getAvailableStatus() == AvailableStatus.FALSE) {
            try {
                logger.finest(br.getRequest().getHttpConnection() + "");
            } catch (Exception e) {
                JDLogger.exception(e);
            }
            logger.info("DebugInfo for maybe Wrong FileNotFound: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return downloadLink.getAvailableStatus();
    }

    public void handleWebsiteDownload(DownloadLink link, Account account) throws Exception {
        if (account != null) {
            login(account, true);
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
                int l = 30;
                if (wait != null) {
                    l = Integer.parseInt(wait.trim());
                }
                limitReached(link, l * 60);
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

    private void getRedirectforAPI(String url, DownloadLink downloadLink) throws PluginException, InterruptedException {
        try {
            br.getPage(url);
            if (br.getRequest().getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
                if (br.getRequest().getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setProperty("pass", passCode);
                return;
            } catch (IOException e2) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            }
        }
    }

    public void handleAPIDownload(DownloadLink link, Account account) throws Exception {
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
        String user = null;
        if (account != null) user = account.getStringProperty("user", null);
        if (user != null) {
            getRedirectforAPI("http://" + wwwWorkaround + "megaupload.com/mgr_dl.php?d=" + dlID + "&u=" + user, link);
        } else {
            getRedirectforAPI("http://" + wwwWorkaround + "megaupload.com/mgr_dl.php?d=" + dlID, link);
        }
        if (br.getRedirectLocation() == null || br.getRedirectLocation().toUpperCase().contains(dlID)) limitReached(link, 10 * 60);

        String url = br.getRedirectLocation();
        br.getHeaders().put("Host", new URL(url).getHost());
        br.getHeaders().put("Connection", "Keep-Alive,TE");
        doDownload(link, url, true, 1);
    }

    private void limitReached(DownloadLink link, int secs) throws PluginException {
        if (this.getPluginConfig().getBooleanProperty("MU_PORTROTATION", true)) {
            /* try portrotation */
            int port = link.getIntegerProperty(MU_PARAM_PORT, 0);
            port++;
            if (port > 2) {
                /* all ports tried, have to wait */
                port = 0;
                link.setProperty(MU_PARAM_PORT, port);
                logger.info("All ports tried, throw IP_Blocked");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "API Limit reached!", secs * 1000l);
            } else {
                /* try next port */
                link.getLinkStatus().setLatestStatus(link.getLinkStatus().getRetryCount() + 1);
                link.setProperty(MU_PARAM_PORT, port);
                logger.info("IP_Blocked: Lets try next port as workaround");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        } else {
            /* have to wait */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "API Limit reached!", secs * 1000l);
        }

    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        usepremium = false;
        requestFileInformation(parameter);
        if (onlyapi) {
            handleAPIDownload(parameter, null);
        } else {
            handleWebsiteDownload(parameter, null);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return this.getPluginConfig().getIntegerProperty("MAX_FREE_PARALELL_DOWNLOADS", 1);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        synchronized (PREMLOCK) {
            return simultanpremium;
        }
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("waitworkaround2", 0);
        link.setProperty(MU_PARAM_PORT, 0);
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, this.getPluginConfig(), MU_PARAM_PORT, ports, JDL.L("plugins.host.megaupload.ports", "Use this port:")).setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), MU_PORTROTATION, ports, JDL.L("plugins.host.megaupload.portrotation", "Use Portrotation to increase downloadlimit?")).setDefaultValue(true));
    }

}
