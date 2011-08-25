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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.Formatter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.com" }, urls = { "https?://[\\w\\.]*?rapidshare\\.com/(files/\\d+/[^\"\r\n ]+|\\#\\!download\\|\\d+.*?\\|\\d+\\|.+?($|\\|\\d+))" }, flags = { 2 })
public class Rapidshare extends PluginForHost {

    public static class RSLink {

        public static RSLink parse(final DownloadLink link) {
            final RSLink ret = new RSLink(link);
            ret.id = Long.parseLong(new Regex(ret.url, "files/(\\d+)/").getMatch(0));
            if (ret.link.getProperty("htmlworkaround", null) == null) {
                /*
                 * remove html ending, because rs now checks the complete
                 * filename
                 */
                ret.name = new Regex(ret.url, "files/\\d+/(.*?/)?(.*?)(\\.html?|$)").getMatch(1);
                ret.secMD5 = new Regex(ret.url, "files/\\d+/t(.*?)-(.*?)/").getMatch(1);
                ret.secTim = new Regex(ret.url, "files/\\d+/t(.*?)-(.*?)/").getMatch(0);
            } else {
                ret.name = new Regex(ret.url, "files/\\d+/(.*?/)?(.*?)$").getMatch(1);
                ret.secMD5 = new Regex(ret.url, "files/\\d+/t(.*?)-(.*?)/").getMatch(1);
                ret.secTim = new Regex(ret.url, "files/\\d+/t(.*?)-(.*?)/").getMatch(0);
            }
            return ret;
        }

        /**
         * @return the secTim
         */
        public String getSecTimout() {
            return secTim;
        }

        private long   id;

        private String name;

        private String url;

        private String secMD5;
        private String secTim;

        /**
         * @return the secMD5
         */
        public String getSecMD5() {
            return secMD5;
        }

        private DownloadLink link;

        public RSLink(final DownloadLink link) {
            this.link = link;
            this.url = link.getDownloadURL();
        }

        public long getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public String getUrl() {
            return this.url;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

    }

    private static final String  WAIT_HOSTERFULL  = "WAIT_HOSTERFULL";

    private static final String  SSL_CONNECTION   = "SSL_CONNECTION2";

    private static final String  HTTPS_WORKAROUND = "HTTPS_WORKAROUND";

    private static final Object  LOCK             = new Object();

    private static final Boolean HTMLWORKAROUND   = new Boolean(false);

    private static long          RS_API_WAIT      = 0;

    private static final String  COOKIEPROP       = "cookiesv2";

    private static final Account dummyAccount     = new Account("TRAFSHARE", "TRAFSHARE");

    private static final String  PRE_RESOLVE      = "PRE_RESOLVE2";

    /* returns file id of link */
    private static String getID(final String link) {
        String ret = new Regex(link, "files/(\\d+)/").getMatch(0);
        if (ret == null) {
            ret = new Regex(link, "\\#\\!download\\|(\\d+.*?)\\|(\\d+)\\|(.+?)($|\\|)").getMatch(1);
        }
        return ret;
    }

    private String accName = null;

    public Rapidshare(final PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
        this.enablePremium("http://rapidshare.com/premium.html");
    }

    /**
     * Bietet der hoster eine Möglichkeit mehrere links gleichzeitig zu prüfen,
     * kann das über diese Funktion gemacht werden.
     */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            if (Rapidshare.RS_API_WAIT > System.currentTimeMillis()) {
                for (final DownloadLink u : urls) {
                    u.setAvailable(true);
                    u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.apiflood", "unchecked (API Flood)"));
                }
                return true;
            }
            logger.finest("OnlineCheck: " + urls.length + " links");
            final StringBuilder idlist = new StringBuilder();
            final StringBuilder namelist = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int size = 0;
            for (final DownloadLink u : urls) {
                if (size > 3000) {
                    logger.finest("OnlineCheck: SplitCheck " + links.size() + "/" + urls.length + " links");
                    /* do not stop here because we are not finished yet */
                    this.checkLinksIntern2(links);
                    links.clear();
                    idlist.delete(0, idlist.capacity());
                    namelist.delete(0, namelist.capacity());
                }
                /* workaround reset */
                u.setProperty("htmlworkaround", null);
                idlist.append(",").append(Rapidshare.getID(u.getDownloadURL()));
                namelist.append(",").append(this.getName(u));
                links.add(u);
                size = ("https://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=checkfiles&files=" + idlist.toString().substring(1) + "&filenames=" + namelist.toString().substring(1) + "&incmd5=1").length();
            }
            if (links.size() != 0) {
                if (links.size() != urls.length) {
                    logger.finest("OnlineCheck: SplitCheck " + links.size() + "/" + urls.length + " links");
                } else {
                    logger.finest("OnlineCheck: Check " + urls.length + " links");
                }
                if (!this.checkLinksIntern2(links)) { return false; }
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean checkLinksIntern(final DownloadLink[] urls) {
        if (urls == null) { return false; }
        final ArrayList<DownloadLink> checkurls = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> finishedurls = new ArrayList<DownloadLink>();
        for (final DownloadLink u : urls) {
            checkurls.add(u);
        }
        try {
            for (int retry = 0; retry < 3; retry++) {
                final StringBuilder idlist = new StringBuilder();
                final StringBuilder namelist = new StringBuilder();
                checkurls.removeAll(finishedurls);
                for (final DownloadLink u : checkurls) {
                    idlist.append(",").append(Rapidshare.getID(u.getDownloadURL()));
                    namelist.append(",").append(this.getName(u));
                }
                final String req = "https://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=checkfiles&files=" + idlist.toString().substring(1) + "&filenames=" + namelist.toString().substring(1) + "&incmd5=1";

                this.queryAPI(null, req, null);

                if (this.br.containsHTML("access flood")) {
                    logger.warning("RS API flooded! Will not check again the next 5 minutes!");
                    Rapidshare.RS_API_WAIT = System.currentTimeMillis() + 5 * 60 * 1000l;
                    return false;
                }

                final String[][] matches = this.br.getRegex("([^\n^\r^,]+),([^,]+),([^,]+),([^,]+),([^,]+),([^,]+),([^\n^\r]*)").getMatches();
                int i = 0;
                boolean doretry = false;
                for (final DownloadLink u : checkurls) {
                    finishedurls.add(u);
                    if (i > matches.length - 1) {
                        doretry = true;
                        break;
                    }
                    u.setDownloadSize(Long.parseLong(matches[i][2]));
                    u.setFinalFileName(matches[i][1]);
                    if (matches[i][6].trim().length() == 32) {
                        u.setMD5Hash(matches[i][6]);
                    } else {
                        u.setMD5Hash(null);
                    }
                    // 0=File not found 1=File OK 2=File OK (direct download)
                    // 3=Server down 4=File abused 5
                    switch (Integer.parseInt(matches[i][4])) {
                    case 0:
                        if (new Regex(u.getDownloadURL(), ".*?(html?)$").matches() && tryWorkaround(u)) {
                            /* retry this link with workaround */
                            finishedurls.remove(u);
                            doretry = true;
                        }
                        u.getLinkStatus().setErrorMessage(JDL.L("plugin.host.rapidshare.status.filenotfound", "File not found"));
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.filenotfound", "File not found"));
                        u.setAvailable(false);
                        break;
                    case 1:
                        // u.getLinkStatus().setStatusText("alles prima");
                        u.setAvailable(true);
                        u.getLinkStatus().setStatusText("");
                        u.getLinkStatus().setErrorMessage(null);
                        break;
                    case 5:
                        u.setAvailable(true);
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.directdownload", "Direct Download"));
                        u.getLinkStatus().setErrorMessage(null);
                        break;
                    case 3:
                        u.getLinkStatus().setErrorMessage(JDL.L("plugin.host.rapidshare.status.servernotavailable", "Server temp. not available. Try later!"));
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.servernotavailable", "Server temp. not available. Try later!"));
                        u.setAvailable(false);
                        break;
                    case 4:
                        u.setAvailable(false);
                        u.getLinkStatus().setErrorMessage(JDL.L("plugin.host.rapidshare.status.abused", "File abused"));
                        u.getLinkStatus().setStatusText(JDL.L("plugin.host.rapidshare.status.abused", "File abused"));
                        break;
                    }
                    i++;
                }
                if (!doretry) { return true; }
            }
            return false;
        } catch (final Exception e) {
            if (this.br.containsHTML("access flood")) {
                logger.warning("RS API flooded! Will not check again the next 5 minutes!");
                Rapidshare.RS_API_WAIT = System.currentTimeMillis() + 5 * 60 * 1000l;
            }
            return false;
        }
    }

    private boolean checkLinksIntern2(final ArrayList<DownloadLink> links) {
        if (!this.checkLinksIntern(links.toArray(new DownloadLink[] {}))) { return false; }
        final ArrayList<DownloadLink> retry = new ArrayList<DownloadLink>();
        for (final DownloadLink link : links) {
            if (link.getProperty("htmlworkaround", null) != null) {
                retry.add(link);
            }
        }
        if (retry.size() > 0) {
            if (!this.checkLinksIntern(retry.toArray(new DownloadLink[] {}))) { return false; }
        }
        return true;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(this.getCorrectedURL(link.getDownloadURL()));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        try {
            this.login(account, true);
        } catch (final PluginException e) {
            if (e.getValue() != PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                if (account.getAccountInfo() == null) {
                    account.setAccountInfo(new AccountInfo());
                }
                account.getAccountInfo().setStatus(e.getErrorMessage());
                account.setValid(false);
            }
            return account.getAccountInfo();
        }
        return account.getAccountInfo();
    }

    @Override
    public String getAGBLink() {
        return "http://rapidshare.com/#!rapidshare-ag/rapidshare-ag_agb";
    }

    /**
     * Korrigiert die URL und befreit von subdomains etc.
     * 
     * @param link
     * @return
     */
    private String getCorrectedURL(String link) {
        if (link == null) { return null; }
        if (link.contains("://ssl.") || !link.startsWith("http://rapidshare.com")) {
            link = "http://rapidshare.com" + link.substring(link.indexOf("rapidshare.com") + 14);
        }

        String filename = new Regex(link, "http://[\\w\\.]*?rapidshare\\.com/files/\\d+/?(.*?)($|\\?)").getMatch(0);
        if (filename == null) {
            filename = new Regex(link, "\\#\\!download\\|(\\d+.*?)\\|(\\d+)\\|(.+?)($|\\|)").getMatch(2);

        }
        return "http://rapidshare.com/files/" + Rapidshare.getID(link) + "/" + filename;
    }

    private HashMap<String, String> getMap(final String[][] matches) {
        final HashMap<String, String> map = new HashMap<String, String>();

        for (final String[] m : matches) {
            map.put(m[0].trim(), m[1].trim());
        }

        return map;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    /* get filename of link */
    private String getName(final DownloadLink link) {
        String name;
        if (link.getProperty("htmlworkaround", null) == null) {
            /* remove html ending, because rs now checks the complete filename */
            name = new Regex(link.getDownloadURL(), "files/\\d+/(.*?/)?(.*?)(\\.html?|$)").getMatch(1);

        } else {
            name = new Regex(link.getDownloadURL(), "files/\\d+/(.*?/)?(.*?)$").getMatch(1);
        }

        return name;
    }

    @Override
    public String getSessionInfo() {
        if (this.accName != null) { return this.accName; }
        return super.getSessionInfo();
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 100;
    }

    private void handleErrors(final Browser br) throws PluginException {
        String error = null;
        if (this.br.toString().startsWith("ERROR: ")) {
            error = this.br.getRegex("ERROR: ([^\r\n]+)").getMatch(0);
            int index = error.lastIndexOf("(");
            if (index > 0) error = error.substring(0, index).trim();
            final String ipwait = new Regex(error, "You need to wait (\\d+) seconds until you can download another file without having RapidPro.").getMatch(0);
            if (ipwait != null) {
                logger.info(error);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, Long.parseLong(ipwait) * 1000l);
            }
            if ("RapidPro expired.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("File not found.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, error);
            } else if ("All free download slots are full. Please try again later.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("jd.plugins.hoster.Rapidshare.handleErrors.message.nofreeslots", "Download as freeuser currently not possible"), 5 * 60 * 1000l);
            } else if ("Please stop flooding our download servers.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("jd.plugins.hoster.Rapidshare.handleErrors.message.stopflood", "Please stop flooding our download servers"), 10 * 60 * 1000l);

            } else if ("No traffic left.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if ("You need RapidPro to download more files from your IP address.".equals(error)) {

                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            } else {
                JDLogger.getLogger().fine(br.toString());
                throw new PluginException(LinkStatus.ERROR_FATAL, error);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.accName = "FreeUser";
        if ("MD5NOTFOUND".equalsIgnoreCase(downloadLink.getMD5Hash())) {
            downloadLink.setMD5Hash(null);
        }
        this.workAroundTimeOut(this.br);/* TODO: remove me after 0.9xx public */
        /* we need file size to calculate left traffic */
        if (downloadLink.getDownloadSize() <= 0) {
            this.requestFileInformation(downloadLink);
        }
        try {

            this.br.setFollowRedirects(false);
            RSLink link = null;
            try {
                link = RSLink.parse(downloadLink);
            } catch (final NumberFormatException e) {
                /* invalid link format */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String query = "http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=download&try=1&fileid=" + link.getId() + "&filename=" + link.getName();
            /* needed for secured links */
            if (link.getSecMD5() != null) {
                query += "&seclinkmd5=" + link.getSecMD5();
            }
            if (link.getSecTimout() != null) {
                query += "&seclinktimeout=" + link.getSecTimout();
            }
            this.queryAPI(this.br, query, null);
            this.handleErrors(this.br);
            // RS URL wird aufgerufen
            // this.br.getPage(link);
            final String host = this.br.getRegex("DL:(.*?),").getMatch(0);
            final String auth = this.br.getRegex("DL:(.*?),(.*?),").getMatch(1);
            final String wait = this.br.getRegex("DL:(.*?),(.*?),(\\d+)").getMatch(2);
            if (wait == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            this.sleep(Long.parseLong(wait) * 1000l, downloadLink);

            String directurl = "http://" + host + "/cgi-bin/rsapi.cgi?sub=download&dlauth=" + auth + "&bin=1&noflvheader=1&fileid=" + link.getId() + "&filename=" + link.getName();
            /* needed for secured links */
            if (link.getSecMD5() != null) {
                directurl += "&seclinkmd5=" + link.getSecMD5();
            }
            if (link.getSecTimout() != null) {
                directurl += "&seclinktimeout=" + link.getSecTimout();
            }
            logger.finest("Direct-Download: Server-Selection not available!");

            br.setFollowRedirects(true);
            try {
                br.setVerbose(true);
            } catch (Throwable e) {
                /* only available after 0.9xx version */
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, directurl, true, 1);
            URLConnectionAdapter urlConnection = dl.getConnection();
            if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
                urlConnection.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }
            this.dl.startDownload();
        } finally {
            if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                this.accName = null;
            }
        }
    }

    private boolean useSSL() {
        try {
            if (Browser.getGlobalProxy() != null) {
                /* workaround for ssl proxy bug in 09581 stable */
                return false;
            }
        } catch (Throwable e) {
            /* catch not found in old nightly */
        }
        return this.getPluginConfig().getBooleanProperty(Rapidshare.SSL_CONNECTION, true) || this.getPluginConfig().getBooleanProperty(Rapidshare.HTTPS_WORKAROUND, false);
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.br.forceDebug(true);
        this.workAroundTimeOut(this.br);

        /* TODO: remove me after 0.9xx public */
        if ("MD5NOTFOUND".equalsIgnoreCase(downloadLink.getMD5Hash())) {
            downloadLink.setMD5Hash(null);
        }
        /* we need file size to calculate left traffic */
        if (downloadLink.getDownloadSize() <= 0) {
            this.requestFileInformation(downloadLink);
        }
        try {
            this.br.forceDebug(true);

            Request request = null;

            this.accName = account.getUser();
            /* synchronized check of account, package handling */
            synchronized (Rapidshare.LOCK) {
                this.br = this.login(account, false);
            }
            this.br.setFollowRedirects(false);

            RSLink link = null;
            try {
                link = RSLink.parse(downloadLink);
            } catch (final NumberFormatException e) {
                /* invalid link format */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String query = "https://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=download&try=1&fileid=" + link.getId() + "&filename=" + link.getName() + "&cookie=" + account.getProperty("cookie");
            /* needed for secured links */
            if (link.getSecMD5() != null) {
                query += "&seclinkmd5=" + link.getSecMD5();
            }
            if (link.getSecTimout() != null) {
                query += "&seclinktimeout=" + link.getSecTimout();
            }
            this.queryAPI(this.br, query, account);
            this.handleErrors(this.br);

            String host = this.br.getRegex("DL:(.*?),").getMatch(0);

            // this might bypass some isps limit restrictions on rapidshare host
            // names
            if (this.getPluginConfig().getBooleanProperty(Rapidshare.PRE_RESOLVE, false)) {
                try {
                    logger.fine("Try to resolve adress " + host);
                    final InetAddress inetAddress = InetAddress.getByName(host);
                    host = inetAddress.getHostAddress();
                } catch (final Exception e) {
                    JDLogger.exception(e);
                }
            }
            String directurl = "https://" + host + "/cgi-bin/rsapi.cgi?sub=download&bin=1&noflvheader=1&fileid=" + link.getId() + "&filename=" + link.getName() + "&cookie=" + account.getProperty("cookie");
            /* needed for secured links */
            if (link.getSecMD5() != null) {
                directurl += "&seclinkmd5=" + link.getSecMD5();
            }
            if (link.getSecTimout() != null) {
                directurl += "&seclinktimeout=" + link.getSecTimout();
            }

            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, directurl, true, 0);
            URLConnectionAdapter urlConnection = dl.getConnection();
            /*
             * Download starten prüft ob ein content disposition header
             * geschickt wurde. Falls nicht, ist es eintweder eine Bilddatei
             * oder eine Fehlerseite. BIldfiles haben keinen Cache-Control
             * Header
             */
            if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
                // Lädt die zuletzt aufgebaute vernindung
                this.br.setRequest(request);
                this.br.followConnection();

                // Fehlerbehanldung
                /*
                 * Achtung! keine Parsing arbeiten an diesem String!!!
                 */

                this.reportUnknownError(this.br.toString(), 6);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            this.dl.startDownload();
        } finally {
            if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                this.accName = null;
                // rs api does not return 416 error for bad ranges
                if ((JDL.L("download.error.message.rangeheaderparseerror", "Unexpected rangeheader format:") + "null").equals(downloadLink.getLinkStatus().getErrorMessage())) {
                    downloadLink.setChunksProgress(null);
                    throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, "Too many Chunks");
                }
            }
            if (account == Rapidshare.dummyAccount) {
                downloadLink.getLinkStatus().setStatusText(JDL.LF("plugins.host.rapidshare.loadedvia", "Loaded via %s", "DirectDownload"));
            } else {
                downloadLink.getLinkStatus().setStatusText(JDL.LF("plugins.host.rapidshare.loadedvia", "Loaded via %s", account.getUser()));
            }
        }
    }

    /**
     * DO NOT REMOVE, ANTI DDOS PROTECTION
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean isPremiumDownload() {
        /*
         * this plugin must take care of HOST_TEMP_UNAVAIL status even in
         * premium mode, can be removed with next major update TODO
         */
        if (DownloadWatchDog.getInstance().getRemainingTempUnavailWaittime(this.getHost()) > 0) { return false; }
        return super.isPremiumDownload();
    }

    private Browser login(final Account account, final boolean forceRefresh) throws Exception {
        synchronized (Rapidshare.LOCK) {
            final Browser br = new Browser();
            this.workAroundTimeOut(br);/* TODO: remove me after 0.9xx public */
            br.setDebug(true);
            br.setCookiesExclusive(true);
            br.clearCookies(this.getHost());

            boolean ssl = useSSL();
            final String prtotcol = ssl ? "https" : "http";

            /*
             * we can use cookie login if user does not want to get asked before
             * package upgrade. we dont need live traffic stats here
             */
            HashMap<String, String> cookies = account.getGenericProperty(Rapidshare.COOKIEPROP, (HashMap<String, String>) null);
            boolean cookieLogin = false;
            if (cookies != null && forceRefresh == false) {
                cookieLogin = true;
            }
            /* cookie login or not? */
            if (cookieLogin && cookies != null && cookies.get("enc") != null && cookies.get("enc").length() != 0) {
                logger.finer("Cookie Login");
                for (final Entry<String, String> cookie : cookies.entrySet()) {
                    br.setCookie(prtotcol + "://rapidshare.com", cookie.getKey(), cookie.getValue());
                }
                return br;
            } else {
                account.setProperty(Rapidshare.COOKIEPROP, null);
            }

            final String req = prtotcol + "://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=getaccountdetails&withcookie=1&type=prem&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
            this.queryAPI(br, req, account);
            final String error = br.getRegex("ERROR:(.*)").getMatch(0);
            if (error != null) {
                account.setProperty(Rapidshare.COOKIEPROP, null);
                logger.severe("10 " + br.toString());
                throw new PluginException(LinkStatus.ERROR_PREMIUM, error, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.containsHTML("Login failed")) {
                account.setProperty(Rapidshare.COOKIEPROP, null);
                logger.severe("1 " + br.toString());
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (br.containsHTML("access flood")) {
                logger.warning("RS API flooded! will not check again the next 15 minutes!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                logger.finer("API Login");
                final String cookie = br.getRegex("cookie=([A-Z0-9]+)").getMatch(0);
                br.setCookie("http://rapidshare.com", "enc", cookie);
            }
            final String cookie = br.getCookie("http://rapidshare.com", "enc");
            if (cookie == null) {
                account.setProperty(Rapidshare.COOKIEPROP, null);
                logger.severe("2 " + br.toString());
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                cookies = new HashMap<String, String>();
                cookies.put("enc", cookie);
                account.setProperty(Rapidshare.COOKIEPROP, cookies);
            }
            // put all accountproperties
            for (final String[] m : br.getRegex("(\\w+)=([^\r^\n]+)").getMatches()) {
                account.setProperty(m[0].trim(), m[1].trim());
            }
            this.updateAccountInfo(account, br);
            return br;
        }
    }

    /**
     * requests the API url req. if the http ip is blocked (UK-BT isp returns
     * 500 or 502 error) https is used.
     * 
     * @param br
     * @param req
     * @throws Exception
     */
    private void queryAPI(Browser br, String req, final Account account) throws Exception {

        if (br == null) {
            br = this.br;
        }
        this.workAroundTimeOut(br);/* TODO: remove me after 0.9xx public */
        br.forceDebug(true);

        if (account != null && useSSL()) {
            req = req.replaceFirst("http:", "https:");
        }
        boolean follow = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.getPage(req);
        } catch (final BrowserException e) {
            if (e.getConnection() != null && !req.startsWith("https")) {
                switch (e.getConnection().getResponseCode()) {
                case 500:
                case 502:
                    req = "https" + req.substring(4);
                    br.getPage(req);
                    break;
                default:
                    throw e;
                }
            } else {
                throw e;
            }
        } finally {
            br.setFollowRedirects(follow);
        }

    }

    private void reportUnknownError(final Object req, final int id) {
        logger.severe("Unknown error(" + id + "). Please add this HTML-Code to your Bugreport:\r\n" + req);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else {
            if (!downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        }
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void reset() {
        Rapidshare.RS_API_WAIT = 0;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    /**
     * Erzeugt den Configcontainer für die Gui
     */
    private void setConfigElements() {

        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), Rapidshare.SSL_CONNECTION, JDL.L("plugins.hoster.rapidshare.com.ssl2", "Use Secure Communication over SSL")).setDefaultValue(true));
        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), Rapidshare.HTTPS_WORKAROUND, JDL.L("plugins.hoster.rapidshare.com.https", "Use HTTPS workaround for ISP Block")).setDefaultValue(false));
        /* caused issues lately because it seems some ip's are sharedhosting */
        // this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // this.getPluginConfig(), Rapidshare.PRE_RESOLVE,
        // JDL.L("plugins.hoster.rapidshare.com.resolve",
        // "Use IP instead of hostname")).setDefaultValue(false));

        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), Rapidshare.WAIT_HOSTERFULL, JDL.L("plugins.hoster.rapidshare.com.waithosterfull", "Wait if all FreeUser Slots are full")).setDefaultValue(true));

    }

    /**
     * try the html filename workaround
     */
    private boolean tryWorkaround(final DownloadLink link) {
        if (link.getProperty("htmlworkaround", null) == null) {
            link.setProperty("htmlworkaround", Rapidshare.HTMLWORKAROUND);
            return true;
        }
        return false;
    }

    private void updateAccountInfo(final Account account, final Browser br) {
        if (account == null || br == null) { return; }
        AccountInfo ai = account.getAccountInfo();
        if (ai == null) {
            ai = new AccountInfo();
            account.setAccountInfo(ai);
        }
        /* let hoster report traffic limit reached! */
        // ai.setSpecialTraffic(true);
        /* reset expired flag */
        ai.setExpired(false);
        ai.setValidUntil(-1);
        account.setValid(true);
        ai.setUnlimitedTraffic();
        try {
            final String[][] matches = br.getRegex("(\\w+)=([^\r^\n]+)").getMatches();
            final HashMap<String, String> data = this.getMap(matches);
            final long rapids = Long.parseLong(data.get("rapids"));
            /* account infos */
            ai.setFilesNum(Long.parseLong(data.get("curfiles")));
            ai.setPremiumPoints(Long.parseLong(data.get("rapids")));
            ai.setUsedSpace(Long.parseLong(data.get("curspace")));
            boolean autoextend = "1".equals(data.get("autoextend"));
            final String billedUntilTime = data.get("billeduntil");
            final String serverTimeString = data.get("servertime");
            long nextBill = 0;
            if (billedUntilTime != null && serverTimeString != null) {
                /* next billing in */
                nextBill = Long.parseLong(billedUntilTime) - Long.parseLong(serverTimeString);
                if (nextBill <= 0) {
                    String possible = "";
                    if (autoextend) {
                        long days = (rapids / 495) * 30;
                        if (days > 0) {
                            possible = "(enough rapids for " + days + " days RapidPro left)";
                        }
                    }
                    ai.setStatus("No RapidPro" + possible);
                    ai.setExpired(true);
                    account.setValid(false);
                } else {
                    if (autoextend) {
                        long days = (rapids / 495) * 30;
                        if (days > 0) {
                            nextBill = nextBill + (days * 24 * 60);
                        }
                    }
                    final String left = Formatter.formatSeconds(nextBill, false);
                    ai.setValidUntil((Long.parseLong(serverTimeString) + nextBill) * 1000l);
                    ai.setStatus("Valid for " + left);
                }
            }
        } catch (final Throwable e) {
            logger.severe("RS-API change detected, please inform support!");
        }

    }

    private void workAroundTimeOut(final Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(30000);
                br.setReadTimeout(30000);
            }
        } catch (final Throwable e) {
        }
    }

}