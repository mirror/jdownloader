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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-online.biz" }, urls = { "http://[\\w\\.]*?(share\\-online\\.biz|egoshare\\.com)/(download.php\\?id\\=|dl/)[\\w]+" }, flags = { 2 })
public class ShareOnlineBiz extends PluginForHost {

    private final static HashMap<Account, HashMap<String, String>> ACCOUNTINFOS = new HashMap<Account, HashMap<String, String>>();
    private final static Object                                    LOCK         = new Object();

    public ShareOnlineBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.share-online.biz/service.php?p=31353834353B4A44616363");
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        // We do not have to change anything here, the regexp also works for
        // egoshare links!
        String id = new Regex(link.getDownloadURL(), "(id\\=|/dl/)([a-zA-Z0-9]+)").getMatch(1);
        link.setUrlDownload("http://www.share-online.biz/download.php?id=" + id + "&?setlang=en");
    }

    public void loginWebsite(Account account) throws IOException, PluginException {
        br.setCookie("http://www.share-online.biz", "king_mylang", "en");
        br.postPage("http://www.share-online.biz/login.php", "act=login&location=index.php&dieseid=&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&login=Login&folder_autologin=1");
        String cookie = br.getCookie("http://www.share-online.biz", "king_passhash");
        if (cookie == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://www.share-online.biz/members.php");
        String expired = br.getRegex(Pattern.compile("<b>Expired\\?</b></td>.*?<td align=\"left\">(.*?)<a", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (expired == null || !expired.trim().equalsIgnoreCase("no")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public HashMap<String, String> loginAPI(Account account, boolean forceLogin) throws IOException, PluginException {
        synchronized (LOCK) {
            HashMap<String, String> infos = ACCOUNTINFOS.get(account);
            if (infos == null || forceLogin) {
                String page = br.getPage("http://api.share-online.biz/account.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&act=userDetails");
                infos = getInfos(page, "=");
                ACCOUNTINFOS.put(account, infos);
            }
            /* check dl cookie, must be available for premium accounts */
            final String dlCookie = infos.get("dl");
            if (dlCookie == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if ("not_available".equalsIgnoreCase(dlCookie)) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            /*
             * check expire date, expire >0 (normal handling) expire<0 (never
             * expire)
             */
            final Long validUntil = Long.parseLong(infos.get("expire_date"));
            if (validUntil > 0 && System.currentTimeMillis() / 1000 > validUntil) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            return infos;
        }
    }

    /* parse the response from api into an hashmap */
    private HashMap<String, String> getInfos(String response, String seperator) throws PluginException {
        if (response == null || response.length() == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String infos[] = Regex.getLines(response);
        HashMap<String, String> ret = new HashMap<String, String>();
        for (String info : infos) {
            String data[] = info.split(seperator);
            if (data.length == 2) {
                ret.put(data[0].trim(), data[1].trim());
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }

    private boolean isPremium() throws IOException {
        if (br.getURL() == null || !br.getURL().equalsIgnoreCase("http://www.share-online.biz/members.php") || br.toString().startsWith("Not HTML Code.")) {
            br.getPage("http://www.share-online.biz/members.php");
        }
        if (br.containsHTML("<b>Premium account</b>")) return true;
        if (br.containsHTML("<b>VIP account</b>")) return true;
        return false;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        /* use api first */
        try {
            HashMap<String, String> infos = loginAPI(account, true);
            /* evaluate expire date */
            final Long validUntil = Long.parseLong(infos.get("expire_date"));
            account.setValid(true);
            if (validUntil > 0) {
                ai.setValidUntil(validUntil * 1000);
            } else {
                ai.setValidUntil(-1);
            }
            if (infos.containsKey("points")) ai.setPremiumPoints(Long.parseLong(infos.get("points")));
            if (infos.containsKey("money")) ai.setAccountBalance(infos.get("money"));
            /* set account type */
            ai.setStatus(infos.get("group"));
            return ai;
        } catch (PluginException e) {
            /* workaround for stable */
            DownloadLink tmpLink = new DownloadLink(null, "temp", "temp", "temp", false);
            LinkStatus linkState = new LinkStatus(tmpLink);
            e.fillLinkStatus(linkState);
            if (linkState.hasStatus(LinkStatus.ERROR_PREMIUM)) {
                account.setValid(false);
                return ai;
            }
        } catch (Exception e) {
        }
        /* fallback to normal website login */
        try {
            loginWebsite(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            account.setValid(false);
            ai.setStatus("No Premium Account!");
            return ai;
        }
        br.getPage("http://www.share-online.biz/members.php");
        String points = br.getRegex(Pattern.compile("<b>Total Points:</b></td>.*?<td align=\"left\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        String expire = br.getRegex(Pattern.compile("<b>Package Expire Date:</b></td>.*?<td align=\"left\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (expire != null && expire.contains("Never")) {
            ai.setValidUntil(-1);
        } else {
            /*
             * they only say the day, so we need to make it work the whole last
             * day
             */
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", null) + (1000l * 60 * 60 * 24));
        }
        account.setValid(true);
        return ai;
    }

    private final String getID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "id\\=([a-zA-Z0-9]+)").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.share-online.biz", "king_mylang", "en");
        br.setAcceptLanguage("en, en-gb;q=0.8");
        String id = getID(downloadLink);
        br.setDebug(true);
        if (br.postPage("http://api.share-online.biz/linkcheck.php?md5=1", "links=" + id).matches("\\s*")) {
            String startURL = downloadLink.getDownloadURL();
            // workaround to bypass new layout and use old site
            br.getPage(startURL += startURL.contains("?") ? "&v2=1" : "?v2=1");
            String[] strings = br.getRegex("</font> \\((.*?)\\) \\.</b></div></td>.*?<b>File name:</b>.*?<b>(.*?)</b></div></td>").getRow(0);
            if (strings == null || strings.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setDownloadSize(SizeFormatter.getSize(strings[0].trim()));
            downloadLink.setName(strings[1].trim());
            return AvailableStatus.TRUE;
        }
        String infos[] = br.getRegex("(.*?);(.*?);(.*?);(.*?);(.+)").getRow(0);
        if (infos == null || !infos[1].equalsIgnoreCase("OK")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Long.parseLong(infos[3].trim()));
        downloadLink.setName(infos[2].trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            br.setCookiesExclusive(true);
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 80 links at once */
                    if (index == urls.length || links.size() > 80) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("links=");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) sb.append("\n");
                    sb.append(getID(dl));
                    c++;
                }
                br.postPage("http://api.share-online.biz/linkcheck.php?md5=1", sb.toString());
                String infos[][] = br.getRegex(Pattern.compile("(.*?);(.*?);(.*?);(.*?);([0-9a-fA-F]+)")).getMatches();
                for (DownloadLink dl : links) {
                    String id = getID(dl);
                    int hit = -1;
                    for (int i = 0; i < infos.length; i++) {
                        if (infos[i][0].equalsIgnoreCase(id)) {
                            hit = i;
                            break;
                        }
                    }
                    if (hit == -1) {
                        /* id not in response, so its offline */
                        dl.setAvailable(false);
                    } else {
                        dl.setFinalFileName(infos[hit][2].trim());
                        dl.setDownloadSize(SizeFormatter.getSize(infos[hit][3]));
                        if (infos[hit][1].trim().equalsIgnoreCase("OK")) {
                            dl.setAvailable(true);
                            dl.setMD5Hash(infos[hit][4].trim());
                        } else {
                            dl.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    @Override
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        /* try api first */
        boolean useAPI = false;
        try {
            this.setBrowserExclusive();
            final HashMap<String, String> infos = loginAPI(account, false);
            final String linkID = getID(parameter);
            br.setCookie("http://www.share-online.biz", "dl", infos.get("dl"));
            final String response = br.getPage("http://api.share-online.biz/account.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&act=download&lid=" + linkID);
            final HashMap<String, String> dlInfos = getInfos(response, ": ");
            final String filename = dlInfos.get("NAME");
            final String size = dlInfos.get("SIZE");
            final String status = dlInfos.get("STATUS");
            parameter.setMD5Hash(dlInfos.get("MD5"));
            if (!"online".equalsIgnoreCase(status)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (size != null) parameter.setDownloadSize(Long.parseLong(size));
            if (filename != null) parameter.setFinalFileName(filename);
            final String dlURL = dlInfos.get("URL");
            if (dlURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(true);
            /* Datei herunterladen */
            /* api does allow resume, but only 1 chunk */
            dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dlURL, true, 1);
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* signal to use API for download */
            useAPI = true;
        } catch (PluginException e) {
            /* workaround for stable */
            DownloadLink tmpLink = new DownloadLink(null, "temp", "temp", "temp", false);
            LinkStatus linkState = new LinkStatus(tmpLink);
            e.fillLinkStatus(linkState);
            if (linkState.hasStatus(LinkStatus.ERROR_PREMIUM)) throw e;
            if (linkState.hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) throw e;
            if (linkState.hasStatus(LinkStatus.ERROR_PLUGIN_DEFECT)) {
                logger.severe(br.toString());
            } else {
                logger.severe(e.getErrorMessage());
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        } finally {
            /* remove downloadCookie */
            Cookie dlCookie = br.getCookies("http://www.share-online.biz").get("dl");
            if (dlCookie != null) br.getCookies("http://www.share-online.biz").remove(dlCookie);
        }
        if (useAPI) {
            /* let us use API to download the file */
            dl.startDownload();
            return;
        }
        /* fallback to website download */
        requestFileInformation(parameter);
        loginWebsite(account);
        if (!this.isPremium()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);

        String startURL = parameter.getDownloadURL();
        // workaround to bypass new layout and use old site
        br.getPage(startURL += startURL.contains("?") ? "&v2=1" : "?v2=1");
        // Account banned/deactivated because too many IPs used it
        if (br.containsHTML(">DL_GotMaxIPPerUid<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        // Sometimes the API is wrong so a file is marked as online but it's
        // offline so here we chack that
        if (br.containsHTML("(strong>Your desired download could not be found|/>There isn't any usable file behind the URL)")) {
            logger.info("The following link was marked as online by the API but is offline: " + parameter.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("You have got max allowed threads from same download session")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        Form form = br.getForm(0);
        if (form == null) {
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("FileNotFound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (form.containsHTML("name=downloadpw")) {
            String passCode = null;
            if (parameter.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, parameter);
            } else {
                /* gespeicherten PassCode holen */
                passCode = parameter.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
            br.submitForm(form);
            if (br.containsHTML("Unfortunately the password you entered is not correct")) {
                /* PassCode war falsch, also Löschen */
                parameter.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            /* PassCode war richtig, also Speichern */
            parameter.setProperty("pass", passCode);
        }

        if (br.containsHTML("DL_GotMaxIPPerUid")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);

        String url = br.getRegex("loadfilelink\\.decode\\(\"(.*?)\"\\);").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        /*
         * website does NOT allow resume, more chunks are possible but
         * deactivated due api limitation
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, url, false, 1);

        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            handleErrors(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);

        String startURL = downloadLink.getDownloadURL();
        // workaround to bypass new layout and use old site
        br.getPage(startURL += startURL.contains("?") ? "&v2=1" : "?v2=1");
        // Sometimes the API is wrong so a file is marked as online but it's
        // offline so here we chack that
        if (br.containsHTML("(strong>Your desired download could not be found|/>There isn't any usable file behind the URL)")) {
            logger.info("The following link was marked as online by the API but is offline: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("Probleme mit einem Fileserver")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable", "Server temporarily down"), 15 * 60 * 1000l);

        if (br.containsHTML("Server Info: no slots available")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable3", "No free Free-User Slots! Get PremiumAccount or wait!"), 5 * 60 * 1000l);

        /* CaptchaCode holen */
        String captchaCode = getCaptchaCode("http://www.share-online.biz/captcha.php", downloadLink);
        Form form = br.getForm(1);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String passCode = null;
        if (form.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }

        /* Überprüfen(Captcha,Password) */
        form.put("captchacode", captchaCode);
        br.submitForm(form);
        System.out.print(br.toString());
        if (br.containsHTML("Captcha number error or expired") || br.containsHTML("Unfortunately the password you entered is not correct")) {
            if (br.containsHTML("Unfortunately the password you entered is not correct")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        /* Downloadlimit erreicht */
        if (br.containsHTML("max allowed download sessions") || br.containsHTML("this download is too big")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 20 * 60 * 1000l); }

        /* PassCode war richtig, also Speichern */
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        /* DownloadLink holen, thx @dwd */
        String all = br.getRegex("eval\\(unescape\\(.*?\"\\)\\)\\);").getMatch(-1);
        String dec = br.getRegex("loadfilelink\\.decode\\(\".*?\"\\);").getMatch(-1);
        Context cx = null;
        String url = null;
        try {
            cx = ContextFactory.getGlobal().enterContext();
            Scriptable scope = cx.initStandardObjects();
            String fun = "function f(){ " + all + "\nreturn " + dec + "} f()";
            Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
            url = Context.toString(result);
        } finally {
            if (cx != null) Context.exit();
        }
        if (br.containsHTML("Probleme mit einem Fileserver")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable", "Server temporarily down"), 15 * 60 * 1000l);
        if (br.containsHTML("Server Info: no slots available")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable3", "No free Free-User Slots! Get PremiumAccount or wait!"), 5 * 60 * 1000l);

        // Keine Zwangswartezeit, deswegen auskommentiert
        // sleep(15000, downloadLink);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            handleErrors(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleErrors(Browser br) throws PluginException {
        if (br.containsHTML("max allowed download sessions") || br.containsHTML("this download is too big")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 20 * 60 * 1000l); }
        if (br.containsHTML("the database is currently") || br.containsHTML("Database not found")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 20 * 60 * 1000l);
        if (br.containsHTML("any usable file behind the URL you")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Your desired download could not be found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        /* server issue, file not online, redirects to mainpage */
        if (br.getURL().equalsIgnoreCase("http://www.share-online.biz")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /*
         * because of You have got max allowed threads from same download
         * session
         */
        return 10;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
