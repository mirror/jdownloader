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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "netload.in" }, urls = { "http://[\\w\\.]*?netload\\.in/[^(http://)].+" }, flags = { 2 })
public class Netloadin extends PluginForHost {
    static private final String AGB_LINK   = "http://netload.in/index.php?id=13";

    static public final Object  LOGINLOCK  = new Object();
    private boolean             showDialog = false;

    private static String getID(String link) {
        String id = new Regex(link, "\\/datei([a-zA-Z0-9]+)").getMatch(0);
        if (id == null) id = new Regex(link, "file_id=([a-zA-Z0-9]+)").getMatch(0);
        if (id == null) id = new Regex(link, "netload\\.in\\/([a-zA-Z0-9]+)\\/.+").getMatch(0);
        return id;
    }

    public Netloadin(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://netload.in/index.php?refer_id=134847&id=39");
    }

    /* TODO: remove me after 0.9xx public */
    private void workAroundTimeOut(Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(30000);
                br.setReadTimeout(120000);
            }
        } catch (Throwable e) {
        }
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://netload.in/datei" + Netloadin.getID(link.getDownloadURL()) + ".htm");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        workAroundTimeOut(br);
        requestFileInformation(downloadLink);
        br.setDebug(true);
        this.setBrowserExclusive();
        String ID = getID(downloadLink.getDownloadURL());
        br.getPage("http://netload.in/json/datei" + ID + ".htm");
        checkPassword(downloadLink, br);
        checkErrors(downloadLink, br, false);
        checkLimit(downloadLink, br);
        String url = br.getRegex("link\":\"(http.*?)\"").getMatch(0);
        if (url != null) {
            url = url.replaceAll("\\\\/", "/");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, false, 1);
            try {
                /* remove next major update */
                /* workaround for broken timeout in 0.9xx public */
                dl.getConnection().setConnectTimeout(30000);
                dl.getConnection().setReadTimeout(120000);
            } catch (Throwable e) {
            }
            dl.startDownload();
        } finally {
            logger.info("used serverurl: " + url);
        }

    }

    private void checkPassword(DownloadLink downloadLink, Browser br) throws Exception {
        String state = br.getRegex("state\":\"(.*?)\"").getMatch(0);
        if (!"failpass".equalsIgnoreCase(state)) return;
        String ID = getID(downloadLink.getDownloadURL());
        String pass = downloadLink.getStringProperty("pass", null);
        if (pass == null) {
            pass = Plugin.getUserInput(JDL.LF("plugins.hoster.netload.downloadPassword_question", "Password protected. Enter Password for %s", downloadLink.getName()), downloadLink);
        }
        br.getPage("http://netload.in/json/datei" + ID + ".htm?password=" + Encoding.urlEncode(pass));
        state = br.getRegex("state\":\"(.*?)\"").getMatch(0);
        if ("failpass".equalsIgnoreCase(state)) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password wrong");
        } else {
            downloadLink.setProperty("pass", pass);
        }
    }

    private void checkLimit(DownloadLink downloadLink, Browser br) throws Exception {
        String state = br.getRegex("state\":\"(.*?)\"").getMatch(0);
        String countdown = br.getRegex("countdown\":(\\d+)").getMatch(0);
        if (countdown == null) return;
        if ("limitexceeded".equalsIgnoreCase(state)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(countdown) * 1000l);
        } else if ("ok".equalsIgnoreCase(state)) {
            this.sleep(Integer.parseInt(countdown) * 1000l, downloadLink);
        } else {
            if ("fail".equalsIgnoreCase(state)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 4 * 60 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private void checkErrors(DownloadLink downloadLink, Browser br, boolean checkFail) throws Exception {
        String state = br.getRegex("state\":\"(.*?)\"").getMatch(0);
        if ("hddcrash".equalsIgnoreCase(state)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "HDDCrash(In Recovery)", 12 * 60 * 60 * 1000l);
        if ("maintenance".equalsIgnoreCase(state)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Maintenance", 2 * 60 * 60 * 1000l);
        if (checkFail && "fail".equalsIgnoreCase(state)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 4 * 60 * 60 * 1000l); }
    }

    private void loginAPI(Account account, AccountInfo ai) throws IOException, PluginException {
        synchronized (LOGINLOCK) {
            workAroundTimeOut(br);
            if (ai == null) {
                ai = account.getAccountInfo();
                if (ai == null) {
                    ai = new AccountInfo();
                    account.setAccountInfo(ai);
                }
            }
            try {
                String res = br.getPage("http://api.netload.in/user_info.php?auth=BVm96BWDSoB4WkfbEhn42HgnjIe1ilMt&user_id=" + Encoding.urlEncode(account.getUser()) + "&user_password=" + Encoding.urlEncode(account.getPass()));
                if (res == null || res.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                res = res.trim();
                account.setValid(true);
                if ("disallowd_agent".equalsIgnoreCase(res) || "unknown_auth".equalsIgnoreCase(res)) {
                    logger.severe("api reports: " + res);
                    ai.setStatus("api reports: " + res);

                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Netload.in Premium Error", "Unexpected error occured during login: '" + res + "'\r\nPlease contact JDownloader Support.");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if ("0".equalsIgnoreCase(res)) {
                    /* free user */
                    ai.setStatus("No premium user");
                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Netload.in Premium Error", "Account '" + account.getUser() + "' is a free account and this not supported.\r\nPlease buy a Netload.in Premium account!");

                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if ("unknown_user".equalsIgnoreCase(res)) {
                    ai.setStatus("Unknown user");
                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Netload.in Premium Error", "The username '" + account.getUser() + "' is unknown.\r\nPlease check your Username!");

                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if ("unknown_password".equalsIgnoreCase(res) || "wrong_password".equalsIgnoreCase(res)) {
                    ai.setStatus("Wrong password");
                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Netload.in Premium Error", "The username '" + account.getUser() + "' is ok, but the given password is wrong.\r\nPlease check your Password!");

                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if ("-1".equalsIgnoreCase(res)) {
                    /* lifetime */
                    ai.setStatus("Lifetime premium");
                    ai.setValidUntil(-1);
                    return;
                } else {
                    /* normal premium */
                    ai.setStatus("Premium");
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(res, "yyyy-MM-dd HH:mm", null));
                    if (ai.isExpired()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

            } catch (PluginException e) {
                try {
                    /* verbose debug */
                    logger.info(br.toString());
                    logger.info(br.getHttpConnection().toString());
                } catch (Throwable e2) {
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        showDialog = true;
        try {
            setBrowserExclusive();
            loginAPI(account, ai);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        return ai;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        setBrowserExclusive();
        workAroundTimeOut(br);
        requestFileInformation(downloadLink);
        showDialog = false;
        loginAPI(account, null);
        String cookie = br.getCookie("http://www.netload.in", "cookie_user");
        if (cookie == null) {
            logger.severe("no cookie!");
            logger.severe(br.toString());
            try {
                logger.info(br.getHttpConnection().toString());
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
        }
        String ID = getID(downloadLink.getDownloadURL());
        br.getPage("http://netload.in/json/datei" + ID + ".htm");
        checkPassword(downloadLink, br);
        checkErrors(downloadLink, br, true);
        workAroundTimeOut(br);
        String url = br.getRegex("link\":\"(http.*?)\"").getMatch(0);
        if (url != null) {
            url = url.replaceAll("\\\\/", "/");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
            try {
                /* remove next major update */
                /* workaround for broken timeout in 0.9xx public */
                dl.getConnection().setConnectTimeout(30000);
                dl.getConnection().setReadTimeout(120000);
            } catch (Throwable e) {
            }
            if (!dl.startDownload()) {
                if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaderparseerror", "Unexpected rangeheader format:"))) {
                    logger.severe("Workaround for Netload Server-Problem! Setting Resume to false and Chunks to 1!");
                    downloadLink.setProperty("nochunk", true);
                    downloadLink.setChunksProgress(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } finally {
            logger.info("used serverurl: " + url);
        }
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("nochunk", false);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    public AvailableStatus websiteFileCheck(DownloadLink downloadLink) throws PluginException {
        this.setBrowserExclusive();
        logger.info("FileCheckAPI error, try website check!");
        workAroundTimeOut(br);
        IOException ex = null;
        String id = Netloadin.getID(downloadLink.getDownloadURL());
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(500 + (i * 200));
            } catch (InterruptedException e) {
                return AvailableStatus.UNCHECKABLE;
            }
            ex = null;
            try {
                br.getPage("http://netload.in/index.php?id=10&file_id=" + id + "&lang=de");
                break;
            } catch (IOException e2) {
                ex = e2;
            }
        }
        if (ex != null) return AvailableStatus.UNCHECKABLE;
        String filename = br.getRegex("<div class=\"dl_first_filename\">(.*?)<").getMatch(0);
        String filesize = br.getRegex("<div class=\"dl_first_filename\">.*?style=.*?>.*?(\\d+.*?)<").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            workAroundTimeOut(br);
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
                sb.append("auth=BVm96BWDSoB4WkfbEhn42HgnjIe1ilMt&bz=1&md5=1&file_id=");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) sb.append(";");
                    sb.append(Netloadin.getID(dl.getDownloadURL()));
                    c++;
                }
                br.postPage("http://api.netload.in/info.php", sb.toString());
                String infos[][] = br.getRegex(Pattern.compile("(.*?);(.*?);(\\d+);(.*?);([0-9a-fA-F]+)")).getMatches();
                for (DownloadLink dl : links) {
                    String id = Netloadin.getID(dl.getDownloadURL());
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
                        dl.setFinalFileName(infos[hit][1].trim());
                        dl.setDownloadSize(SizeFormatter.getSize(infos[hit][2]));
                        if (infos[hit][3].trim().equalsIgnoreCase("online")) {
                            dl.setAvailable(true);
                            dl.setMD5Hash(infos[hit][4].trim());
                        } else {
                            dl.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            DownloadLink urls[] = new DownloadLink[1];
            urls[0] = downloadLink;
            checkLinks(urls);
            if (!downloadLink.isAvailabilityStatusChecked()) return AvailableStatus.UNCHECKED;
            if (downloadLink.isAvailable()) return AvailableStatus.TRUE;
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } catch (PluginException e) {
            /* workaround for buggy api */
            /* workaround for stable */
            DownloadLink tmpLink = new DownloadLink(null, "temp", "temp", "temp", false);
            LinkStatus linkState = new LinkStatus(tmpLink);
            e.fillLinkStatus(linkState);
            if (linkState.hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                return websiteFileCheck(downloadLink);
            } else {
                throw e;
            }
        }
    }

    @Override
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
