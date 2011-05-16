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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-online.biz" }, urls = { "http://[\\w\\.]*?(share\\-online\\.biz|egoshare\\.com)/(download.php\\?id\\=|dl/)[\\w]+" }, flags = { 2 })
public class ShareOnlineBiz extends PluginForHost {

    private final static HashMap<Account, HashMap<String, String>> ACCOUNTINFOS   = new HashMap<Account, HashMap<String, String>>();
    private final static Object                                    LOCK           = new Object();
    private final static HashMap<Long, Long>                       noFreeSlot     = new HashMap<Long, Long>();
    private long                                                   server         = -1;
    private final static long                                      waitNoFreeSlot = 10 * 60 * 1000l;
    private final static String                                    UA             = RandomUserAgent.generate();
    private boolean                                                hideID         = true;

    public ShareOnlineBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.share-online.biz/service.php?p=31353834353B4A44616363");
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        // We do not have to change anything here, the regexp also works for
        // egoshare links!
        String id = new Regex(link.getDownloadURL(), "(id\\=|/dl/)([a-zA-Z0-9]+)").getMatch(1);
        link.setUrlDownload("http://www.share-online.biz/dl/" + id);
        if (hideID) link.setName("download.php");
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
            if (data.length == 1) {
                ret.put(data[0].trim(), null);
            } else if (data.length == 2) {
                ret.put(data[0].trim(), data[1].trim());
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
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
    }

    private final String getID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "(id\\=|/dl/)([a-zA-Z0-9]+)").getMatch(1);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        hideID = false;
        correctDownloadLink(downloadLink);
        this.setBrowserExclusive();
        server = -1;
        br.setCookie("http://www.share-online.biz", "king_mylang", "en");
        br.setAcceptLanguage("en, en-gb;q=0.8");
        String id = getID(downloadLink);
        br.setDebug(true);
        if (br.postPage("http://api.share-online.biz/linkcheck.php?md5=1&snr=1", "links=" + id).matches("\\s*")) {
            String startURL = downloadLink.getDownloadURL();
            // workaround to bypass new layout and use old site
            br.getPage(startURL += startURL.contains("?") ? "&v2=1" : "?v2=1");
            String[] strings = br.getRegex("</font> \\((.*?)\\) \\.</b></div></td>.*?<b>File name:</b>.*?<b>(.*?)</b></div></td>").getRow(0);
            if (strings == null || strings.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setDownloadSize(SizeFormatter.getSize(strings[0].trim()));
            downloadLink.setName(strings[1].trim());
            return AvailableStatus.TRUE;
        }
        String infos[] = br.getRegex("(.*?);(.*?);(.*?);(.*?);(.*?);(\\d+)").getRow(0);
        if (infos == null || !infos[1].equalsIgnoreCase("OK")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Long.parseLong(infos[3].trim()));
        downloadLink.setName(infos[2].trim());
        server = Long.parseLong(infos[5].trim());
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
                    if (index == urls.length || links.size() > 200) break;
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
        this.setBrowserExclusive();
        final HashMap<String, String> infos = loginAPI(account, false);
        final String linkID = getID(parameter);
        br.setCookie("http://www.share-online.biz", "dl", infos.get("dl"));
        final String response = br.getPage("http://api.share-online.biz/account.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&act=download&lid=" + linkID);
        if (response.contains("EXCEPTION request download link not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final HashMap<String, String> dlInfos = getInfos(response, ": ");
        final String filename = dlInfos.get("NAME");
        final String size = dlInfos.get("SIZE");
        final String status = dlInfos.get("STATUS");
        if (filename == null || size == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setMD5Hash(dlInfos.get("MD5"));
        if (!"online".equalsIgnoreCase(status)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (size != null) parameter.setDownloadSize(Long.parseLong(size));
        if (filename != null) parameter.setFinalFileName(filename);
        final String dlURL = dlInfos.get("URL");
        // http://api.share-online.biz/api/account.php?act=fileError&fid=FILE_ID
        if (dlURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        /* api does allow resume, but only 1 chunk */
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dlURL, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            errorHandling(br, parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void errorHandling(Browser br, DownloadLink downloadLink) throws PluginException {
        /* file is offline */
        if (br.containsHTML("The requested file is not available")) {
            logger.info("The following link was marked as online by the API but is offline: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* no free slot */
        if (br.containsHTML("No free slots for free users") || br.getURL().contains("failure/full")) {
            downloadLink.getLinkStatus().setRetryCount(0);
            if (server != -1) {
                synchronized (noFreeSlot) {
                    noFreeSlot.put(server, System.currentTimeMillis());
                }
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable3", "No free Free-User Slots! Get PremiumAccount or wait!"), waitNoFreeSlot);
        }
        if (br.getURL().contains("failure/threads")) {
            /* already loading,too many threads */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        }
        if (br.getURL().contains("failure/bandwidth")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }
        if (br.getURL().contains("failure/filenotfound")) {
            try {
                final Browser br2 = new Browser();
                final String id = this.getID(downloadLink);
                br2.getPage("http://api.share-online.biz/api/account.php?act=fileError&fid=" + id);
            } catch (Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().contains("failure/invalid")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l); }
        if (br.getURL().contains("failure/ip")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP Already loading", 15 * 60 * 1000l); }
        if (br.getURL().contains("failure/size")) { throw new PluginException(LinkStatus.ERROR_FATAL, "File too big. Premium needed!"); }
        if (br.getURL().contains("failure/expired") || br.getURL().contains("failure/session")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait for new ticket", 60 * 1000l); }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        if (server != -1) {
            synchronized (noFreeSlot) {
                Long ret = noFreeSlot.get(server);
                if (ret != null) {
                    if (System.currentTimeMillis() - ret < waitNoFreeSlot) {
                        if (downloadLink.getLinkStatus().getRetryCount() >= getMaxRetries()) {
                            /*
                             * reset counter this error does not cause plugin to
                             * stop
                             */
                            downloadLink.getLinkStatus().setRetryCount(0);
                        }
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable3", "No free Free-User Slots! Get PremiumAccount or wait!"), waitNoFreeSlot);
                    } else {
                        noFreeSlot.remove(server);
                    }
                }
            }
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", UA);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Cache-Control", null);
        br.setCookie("http://www.share-online.biz", "page_language", "english");
        br.getPage(downloadLink.getDownloadURL());
        Browser brc = br.cloneBrowser();
        try {
            brc.openGetConnection("http://www.share-online.biz/template/images/corp/uploadking.php?show=last");
        } finally {
            try {
                brc.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
        errorHandling(br, downloadLink);
        if (!br.containsHTML(">>> continue for free <<<")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String ID = getID(downloadLink);
        br.postPage("http://www.share-online.biz/dl/" + ID + "/free/", "dl_free=1");
        errorHandling(br, downloadLink);
        String wait = br.getRegex("var wait=(\\d+)").getMatch(0);
        if (wait != null) {
            this.sleep(Integer.parseInt(wait) * 1000l, downloadLink);
        }
        /* DownloadLink holen, thx @dwd */
        String dlINFO = br.getRegex("var dl=\"(.*?)\"").getMatch(0);
        String url = Encoding.Base64Decode(dlINFO);
        // String nfoINFO = br.getRegex("var nfo=\"(.*?)\"").getMatch(0);
        // Browser brc = br.cloneBrowser();
        // brc.getPage("http://www.share-online.biz/template/js/tools.js");
        // String fun =
        // brc.getRegex("(function info.*?)(function|$)").getMatch(0);
        // Context cx = null;
        // String res = null;
        // try {
        // cx = ContextFactory.getGlobal().enterContext();
        // Scriptable scope = cx.initStandardObjects();
        // String fun2 = "function f(){ " + fun.trim() + ";\nreturn info('" +
        // nfoINFO + "')  } f()";
        // Object result = cx.evaluateString(scope, fun2, "<cmd>", 1, null);
        // res = Context.toString(result);
        // } catch (final Throwable e) {
        // e.printStackTrace();
        // } finally {
        // if (cx != null) Context.exit();
        // }
        // if (res != null) res = Encoding.Base64Decode(res);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            errorHandling(br, downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
        synchronized (noFreeSlot) {
            noFreeSlot.clear();
        }
    }

}
