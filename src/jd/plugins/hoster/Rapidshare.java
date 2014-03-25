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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.RandomUserAgent;
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
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.com" }, urls = { "https?://[\\w\\.]*?rapidshare\\.com/(files/\\d+/.*?(\"|\r|\n|$|\\?)|(desktop/)?download/\\d+p\\d+/\\d+/[A-Za-z0-9=]+/\\d+/\\d+/\\d+/\\d+/[A-Fa-f0-9]{32}/refer|\\#\\!download(\\||%7C)\\d+.*?(\\||%7C)\\d+(\\||%7C).+?($|(\\||%7C)\\d+))" }, flags = { 2 })
public class Rapidshare extends PluginForHost {

    public static class RSLink {

        //
        public static RSLink parse(final DownloadLink link) {

            // test comment
            final RSLink ret = new RSLink(link);
            ret.id = Long.parseLong(new Regex(ret.url, "files/(\\d+)/").getMatch(0));
            if (ret.link.getProperty("htmlworkaround", null) == null) {
                /*
                 * remove html ending, because rs now checks the complete filename
                 */
                ret.name = new Regex(ret.url, "files/\\d+/(.*?/)?(.*?)(\\.html?|$|;$)").getMatch(1);
                ret.secMD5 = new Regex(ret.url, "files/\\d+/t(.*?)-(.*?)/").getMatch(1);
                ret.secTim = new Regex(ret.url, "files/\\d+/t(.*?)-(.*?)/").getMatch(0);
            } else {
                ret.name = new Regex(ret.url, "files/\\d+/(.*?/)?(.*?)($|;$)").getMatch(1);
                ret.secMD5 = new Regex(ret.url, "files/\\d+/t(.*?)-(.*?)/").getMatch(1);
                ret.secTim = new Regex(ret.url, "files/\\d+/t(.*?)-(.*?)/").getMatch(0);
            }
            if (ret.name != null) {
                ret.name = ret.name.replaceAll("&", "%26");
            }
            return ret;
        }

        private long               id;

        private String             name;

        private String             url;

        private String             secMD5;

        private String             secTim;
        private final DownloadLink link;

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

        /**
         * @return the secMD5
         */
        public String getSecMD5() {
            return this.secMD5;
        }

        /**
         * @return the secTim
         */
        public String getSecTimout() {
            return this.secTim;
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

    private String                 WAIT_HOSTERFULL   = "WAIT_HOSTERFULL";

    private String                 SSL_CONNECTION    = "SSL_CONNECTION2";

    private String                 HTTPS_WORKAROUND  = "HTTPS_WORKAROUND";

    private static Object          LOCK              = new Object();

    private static AtomicLong      RS_API_WAIT       = new AtomicLong(0);

    private String                 COOKIEPROP        = "cookiesv2";
    private String                 COOKIEPROPENC     = "cookiesv2enc";

    private static Account         dummyAccount      = new Account("TRAFSHARE", "TRAFSHARE");

    private String                 PRE_RESOLVE       = "PRE_RESOLVE2";

    private static StringContainer UA                = new StringContainer(RandomUserAgent.generate());

    private char[]                 FILENAMEREPLACES  = new char[] { '_', ' ' };

    private static AtomicBoolean   ReadTimeoutHotFix = new AtomicBoolean(false);

    private String                 dllink            = null;

    private static final String    LINKTYPE_DOWNLOAD = "https?://(www\\.)?rapidshare\\.com/(desktop/)?download/\\d+p\\d+/\\d+/[A-Za-z0-9=]+/\\d+/\\d+/\\d+/\\d+/[A-Z0-9]{32}/refer";

    public static class StringContainer {
        public String string = null;

        public StringContainer(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    /* returns file id of link */
    private static String getID(final String link) {
        String ret = new Regex(link, "files/(\\d+)/").getMatch(0);
        if (ret == null) {
            ret = new Regex(link, "\\#\\!download(\\||%7C)(\\d+.*?)(\\||%7C)(\\d+)(\\||%7C)(.+?)($|\\||%7C)").getMatch(3);
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
     * Bietet der hoster eine Möglichkeit mehrere links gleichzeitig zu prüfen, kann das über diese Funktion gemacht werden.
     */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {

        if (urls == null || urls.length == 0) { return false; }
        try {
            if (Rapidshare.RS_API_WAIT.get() > System.currentTimeMillis()) {
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
                if (u.getBooleanProperty("fastAdd", false)) {
                    u.setProperty("fastAdd", Property.NULL);
                    continue;
                }
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

    public String filterPackageID(String packageIdentifier) {
        return packageIdentifier.replaceAll("([^a-zA-Z0-9]+)", "");
    }

    public char[] getFilenameReplaceMap() {
        return FILENAMEREPLACES;
    }

    public boolean isHosterManipulatesFilenames() {
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
                    namelist.append(",").append(encodeFilename(getName(u)));
                }
                final String req = "https://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=checkfiles&files=" + idlist.toString().substring(1) + "&filenames=" + namelist.toString().substring(1) + "&incmd5=1";

                this.queryAPI(null, req);

                if (this.br.containsHTML("access flood")) {
                    logger.warning("RS API flooded! Will not check again the next 5 minutes!");
                    Rapidshare.RS_API_WAIT.set(System.currentTimeMillis() + 5 * 60 * 1000l);
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
                    long size;
                    u.setDownloadSize(size = Long.parseLong(matches[i][2]));
                    if (size > 0) {
                        u.setProperty("VERIFIEDFILESIZE", size);
                        if (matches[i][6].trim().length() == 32) {
                            u.setMD5Hash(matches[i][6]);
                        } else {
                            u.setMD5Hash(null);
                        }
                    } else {
                        /* Don't check hash if filesize is 0 */
                        u.setMD5Hash(null);
                    }
                    String suggestedName = null;
                    String finalFilename = matches[i][1];
                    /* special filename modding if we have a suggested filename */
                    if ((suggestedName = (String) u.getProperty("SUGGESTEDFINALFILENAME", (String) null)) != null) {
                        String finalFilename2 = suggestedName.replaceAll("(-| |\\[|\\])", "_");
                        if (finalFilename2.equalsIgnoreCase(finalFilename)) {
                            finalFilename = suggestedName;
                        }
                    }
                    u.setFinalFileName(finalFilename);
                    // 0=File not found 1=File OK 2=File OK (direct download)
                    // 3=Server down 4=File abused 5
                    switch (Integer.parseInt(matches[i][4])) {
                    case 0:
                        if (new Regex(u.getDownloadURL(), ".*?(html?)$").matches() && this.tryWorkaround(u)) {
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
                Rapidshare.RS_API_WAIT.set(System.currentTimeMillis() + 5 * 60 * 1000l);
            }
            return false;
        }
    }

    private String encodeFilename(String filename) {
        /* Encode some things here so that they're decoded back to their original and can be encoded correctly below */
        filename = filename.replace("%2C", "%252C");
        filename = filename.replace("+", "%2B");
        filename = Encoding.htmlDecode(filename);
        filename = filename.replace("%", "%25");
        filename = filename.replace("+", "%2B");
        filename = filename.replace(" ", "%20");
        filename = filename.replace("&", "%26");
        filename = filename.replace(",", "%2C");
        return filename;
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
        final String originalURL = link.getDownloadURL();
        String correctedURL = null;
        if (originalURL.matches(LINKTYPE_DOWNLOAD)) {
            final Regex type_download_regex = new Regex(originalURL, "download/\\d+p\\d+/(\\d+)/([A-Za-z0-9=]+)/.+([A-Z0-9]{32})/refer");
            final String fid = type_download_regex.getMatch(0);
            final String filename = Encoding.Base64Decode(type_download_regex.getMatch(1));
            final String shareid = type_download_regex.getMatch(2);
            link.setProperty("shareid", shareid);
            link.setName(filename);
            correctedURL = "http://rapidshare.com/files/" + fid + "/" + filename;
        } else {
            correctedURL = this.getCorrectedURL(link.getDownloadURL());
        }
        link.setUrlDownload(correctedURL);
    }

    private RAFDownload createHackedDownloadInterface(PluginForHost plugin, final Browser br, final DownloadLink downloadLink, final String url) throws IOException, PluginException, Exception {
        Request r = br.createRequest(url);
        RAFDownload dl = this.createHackedDownloadInterface2(plugin, downloadLink, r);
        try {
            dl.connect(br);
        } catch (final PluginException e) {
            if (e.getValue() == -1) {

                int maxRedirects = 10;
                while (maxRedirects-- > 0) {
                    dl = this.createHackedDownloadInterface2(plugin, downloadLink, r = br.createGetRequestRedirectedRequest(r));
                    try {
                        dl.connect(br);
                        break;
                    } catch (final PluginException e2) {
                        continue;
                    }
                }
                if (maxRedirects <= 0) { throw new PluginException(LinkStatus.ERROR_FATAL, "Redirectloop"); }

            }
        }
        if (plugin.getBrowser() == br) {
            plugin.setDownloadInterface(dl);
        }
        return dl;
    }

    private RAFDownload createHackedDownloadInterface2(PluginForHost plugin, final DownloadLink downloadLink, final Request request) throws IOException, PluginException {
        request.getHeaders().put("Accept-Encoding", "");
        final RAFDownload dl = new RAFDownload(plugin, downloadLink, request) {

            long lastWrite = -1;

            //

            @Override
            protected void addChunk(final Chunk chunk) {
                final Chunk newChunk = new Chunk(chunk.getStartByte(), chunk.getEndByte(), this.connection, this) {
                    final int max   = 30 * 1024;
                    int       speed = 30 * 1024;

                    public int getMaximalSpeed() {
                        if (speed >= max) return max;
                        if (speed <= 0) return max;
                        return speed;
                    }

                    public void setMaximalSpeed(final int i) {
                        this.speed = i;
                    }

                };

                try {
                    final Class<?> c = Chunk.class;
                    Field f = null;
                    if (f == null) {
                        f = c.getDeclaredField("MAX_BUFFERSIZE");
                    }
                    f.setAccessible(true);
                    f.setLong(newChunk, 30 * 1024l);
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
                super.addChunk(newChunk);
            }

            @Override
            protected boolean writeChunkBytes(final Chunk chunk) {
                final boolean ret = super.writeChunkBytes(chunk);
                if (this.lastWrite == -1) {
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                    }
                    this.lastWrite = System.currentTimeMillis();
                } else {
                    long diff = System.currentTimeMillis() - this.lastWrite;
                    try {
                        if (diff < 1000) {
                            diff = 1000 - diff;
                            Thread.sleep(diff);
                        }
                    } catch (final InterruptedException e) {
                    }
                    this.lastWrite = System.currentTimeMillis();
                }
                return ret;
            }

        };
        plugin.setDownloadInterface(dl);
        dl.setResume(true);
        dl.setChunkNum(1);
        return dl;
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

        String filename = new Regex(link, "https?://[\\w\\.]*?rapidshare\\.com/files/\\d+/?(.*?)(\\?|\"|\r|$)").getMatch(0);
        if (filename == null) {
            filename = new Regex(link, "\\#\\!download(\\||%7C)(\\d+.*?)(\\||%7C)(\\d+)(\\||%7C)(.+?)($|\\||%7C)").getMatch(5);

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
        return -1;
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

        return name.replaceAll("&", "%26");
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 100;
    }

    private void handleErrors(final DownloadLink link, final Browser br) throws PluginException {
        String error = null;
        if (this.br.toString().startsWith("ERROR: ")) {
            error = this.br.getRegex("ERROR: ([^\r\n]+)").getMatch(0);
            final int index = error.lastIndexOf("(");
            if (index > 0) {
                error = error.substring(0, index).trim();
            }
            final String ipwait = new Regex(error, "You need to wait (\\d+) seconds until you can download another file without having RapidPro.").getMatch(0);
            if (ipwait != null) {
                logger.info(error);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, Long.parseLong(ipwait) * 1000l);
            }
            if (error.startsWith("Download permission denied by")) {
                boolean nowdownloadable = true;
                try {
                    logger.info(" Trying workaround for the rapidshare.com serverside permissions bug");
                    br.getPage("https://api.rapidshare.com/cgi-bin/rsapi.cgi?rsource=web&sub=checkfiles&files=" + getID(link.getDownloadURL()) + "&filenames=" + Encoding.urlEncode(link.getName()) + "&cbid=4&cbf=rsapi.system.jsonp.callback&callt=" + System.currentTimeMillis());
                    br.getRequest().setHtmlCode(br.toString().replace("\\n", ""));
                    if (!br.containsHTML("Download permission denied by") && br.containsHTML(link.getName())) {
                        final String apitext = br.getRegex("rsapi\\.system\\.jsonp\\.callback\\(4,\"(.*?)\"\\)").getMatch(0);
                        final String[] data = apitext.split(",");
                        final String shareid = link.getStringProperty("shareid", null);
                        if (shareid != null) {
                            dllink = "https://rs" + data[3] + data[5] + ".rapidshare.com/cgi-bin/rsapi.cgi?sub=download&share=" + shareid + "&fileid=" + getID(link.getDownloadURL()) + "&filename=" + Encoding.urlEncode(link.getName()) + "&bin=1";
                        } else {
                            dllink = "https://rs" + data[3] + data[5] + ".rapidshare.com/cgi-bin/rsapi.cgi?sub=download&fileid=" + getID(link.getDownloadURL()) + "&filename=" + Encoding.urlEncode(link.getName()) + "&bin=1";
                        }
                        nowdownloadable = false;
                    }
                } catch (final Throwable e) {
                }
                if (nowdownloadable) {
                    // Errorhandling before: File not found
                    try {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                    } catch (final Throwable e) {
                        if (e instanceof PluginException) throw (PluginException) e;
                    }
                    throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by its uploader");
                }
            } else if (error.startsWith("This server's filesystem is in maintenance")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This server's filesystem is in maintenance.", 2 * 60 * 60 * 1000l);
            } else if ("RapidPro expired.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("File not found.".equals(error) || "Folder not found.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if ("All free download slots are full. Please try again later.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("jd.plugins.hoster.Rapidshare.handleErrors.message.nofreeslots", "Download as freeuser currently not possible"), 5 * 60 * 1000l);
            } else if ("Please stop flooding our download servers.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("jd.plugins.hoster.Rapidshare.handleErrors.message.stopflood", "Please stop flooding our download servers"), 10 * 60 * 1000l);

            } else if ("No traffic left.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if ("You need RapidPro to download more files from your IP address.".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            } else if (error.contains("traffic exhausted")) {
                /* anti ddos protection */
                sleep(Math.max(2, new Random().nextInt(10)) * 60 * 1000, link);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File owner's traffic exhausted", 60 * 60 * 1000l);
            } else if (error.contains("Filename invalid")) {
                /* Probably offline */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Filename invalid or file offline");
            } else {
                logger.fine(br.toString());
                throw new PluginException(LinkStatus.ERROR_FATAL, error);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.readTimeoutHotFix();
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
            String query = "http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=download&try=1&fileid=" + link.getId() + "&filename=" + encodeFilename(link.getName());
            /* needed for secured links */
            if (link.getSecMD5() != null) {
                query += "&seclinkmd5=" + link.getSecMD5();
            }
            if (link.getSecTimout() != null) {
                query += "&seclinktimeout=" + link.getSecTimout();
            }
            this.queryAPI(this.br, query);
            this.handleErrors(downloadLink, this.br);

            // Check if we already have the final downloadling because of the permissions bug workaround
            if (dllink == null) {
                // RS URL wird aufgerufen
                // this.br.getPage(link);
                final String host = this.br.getRegex("DL:(.*?),").getMatch(0);
                final String auth = this.br.getRegex("DL:(.*?),(.*?),").getMatch(1);
                final String wait = this.br.getRegex("DL:(.*?),(.*?),(\\d+)").getMatch(2);
                if (wait == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                this.sleep(Long.parseLong(wait) * 1000l, downloadLink);
                dllink = "http://" + host + "/cgi-bin/rsapi.cgi?sub=download&dlauth=" + auth + "&bin=1&noflvheader=1&fileid=" + link.getId() + "&filename=" + encodeFilename(link.getName());
                /* needed for secured links */
                if (link.getSecMD5() != null) {
                    dllink += "&seclinkmd5=" + link.getSecMD5();
                }
                if (link.getSecTimout() != null) {
                    dllink += "&seclinktimeout=" + link.getSecTimout();
                }
                logger.finest("Direct-Download: Server-Selection not available!");

                this.br.setFollowRedirects(true);
                try {
                    br.setVerbose(true);
                } catch (final Throwable e) {
                    /* only available after 0.9xx version */
                }

                // if (this.getPluginConfig().getBooleanProperty("notifyShown",
                // false) == false) {
                // this.getPluginConfig().setProperty("notifyShown", true);
                // try {
                // this.getPluginConfig().save();
                // } catch (final Throwable e) {
                // }
                // UserIO.getInstance().requestMessageDialog(UserIO.NO_COUNTDOWN,
                // "Rapidshare Speed Limitation",
                // "Rapidshare disabled the ability to resume downloads that were stopped for free users and also limited the average download speed to 30 kb/s.\r\nBecause of the way they are doing this, it may look like the download is frozen!\r\n\r\nDon't worry - it's not. It's just waiting for the next piece of the file to be transferred.\r\n\r\nThe pauses in between are added by Rapidshare in order to make the overall average speed slower for free-users.");
                // }
            }
            if (downloadLink.getDownloadSize() > 30 * 1024 * 1024 && oldStyle() && false) {
                this.dl = this.createHackedDownloadInterface(this, this.br, downloadLink, dllink);
            } else {
                this.dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            }
            final URLConnectionAdapter urlConnection = this.dl.getConnection();
            if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
                try {
                    this.br.followConnection();
                } catch (final Throwable e) {
                }
                if (br.containsHTML("Download permission denied by uploader"))
                    throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by its uploader");
                else if (br.containsHTML("File ID invalid"))
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                else if (br.containsHTML("Filename invalid")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
                logger.severe(this.br.toString());
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            }

            this.dl.startDownload();
        } finally {
            if (!downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                this.accName = null;
            }
        }
    }

    private boolean oldStyle() {
        String style = System.getProperty("ftpStyle", null);
        if ("new".equalsIgnoreCase(style)) return false;
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 10000) return true;
        return false;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.readTimeoutHotFix();
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

            this.accName = account.getUser();
            /* synchronized check of account, package handling */
            String cookie = null;
            synchronized (Rapidshare.LOCK) {
                this.br = this.login(account, false);
                cookie = "" + account.getProperty(COOKIEPROPENC);
            }
            this.br.setFollowRedirects(false);

            RSLink link = null;
            try {
                link = RSLink.parse(downloadLink);
            } catch (final NumberFormatException e) {
                /* invalid link format */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            String query = "https://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=download&try=1&fileid=" + link.getId() + "&filename=" + link.getName() + "&cookie=" + cookie;
            /* needed for secured links */
            if (link.getSecMD5() != null) {
                query += "&seclinkmd5=" + link.getSecMD5();
            }
            if (link.getSecTimout() != null) {
                query += "&seclinktimeout=" + link.getSecTimout();
            }
            this.queryAPI(this.br, query);
            this.handleErrors(downloadLink, this.br);
            boolean retry = true;
            String host = this.br.getRegex("DL:(.*?),").getMatch(0);
            while (true) {

                // this might bypass some isps limit restrictions on rapidshare
                // host
                // names
                if (this.getPluginConfig().getBooleanProperty(PRE_RESOLVE, false)) {
                    try {
                        logger.fine("Try to resolve adress " + host);
                        final InetAddress inetAddress = InetAddress.getByName(host);
                        host = inetAddress.getHostAddress();
                    } catch (final Throwable e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
                String directurl = "https://" + host + "/cgi-bin/rsapi.cgi?sub=download&bin=1&noflvheader=1&fileid=" + link.getId() + "&filename=" + link.getName() + "&cookie=" + cookie;
                if (dllink != null) {
                    directurl = dllink + "&cookie=" + cookie;
                } else if (host == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                /* needed for secured links */
                if (link.getSecMD5() != null) {
                    directurl += "&seclinkmd5=" + link.getSecMD5();
                }
                if (link.getSecTimout() != null) {
                    directurl += "&seclinktimeout=" + link.getSecTimout();
                }

                this.br.setFollowRedirects(true);
                this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, directurl, true, 0);
                final URLConnectionAdapter urlConnection = this.dl.getConnection();
                /*
                 * Download starten prüft ob ein content disposition header geschickt wurde. Falls nicht, ist es eintweder eine Bilddatei
                 * oder eine Fehlerseite. BIldfiles haben keinen Cache-Control Header
                 */
                if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
                    // Lädt die zuletzt aufgebaute vernindung
                    this.br.followConnection();
                    if (br.containsHTML("Download permission denied by uploader")) { throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by its uploader"); }
                    if (retry) {
                        /* in case we get anther DL hoster */
                        host = this.br.getRegex("DL:(.*?),").getMatch(0);
                        if (host != null) {
                            retry = false;
                            continue;
                        }
                    }
                    // Fehlerbehanldung
                    /*
                     * Achtung! keine Parsing arbeiten an diesem String!!!
                     */

                    this.reportUnknownError(this.br.toString(), 6);
                    synchronized (Rapidshare.LOCK) {
                        /* we retry and fetch new cookies */
                        account.setProperty(COOKIEPROP, null);
                        account.setProperty(COOKIEPROPENC, null);
                    }
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                break;
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

    private Browser login(final Account account, final boolean forceRefresh) throws Exception {
        synchronized (Rapidshare.LOCK) {
            final Browser br = new Browser();
            this.workAroundTimeOut(br);/* TODO: remove me after 0.9xx public */
            br.setDebug(true);
            br.setCookiesExclusive(true);
            br.clearCookies(this.getHost());

            final boolean ssl = this.useSSL();
            final String prtotcol = ssl ? "https" : "http";

            /*
             * we can use cookie login if user does not want to get asked before package upgrade. we dont need live traffic stats here
             */
            Object cookiesRet = account.getProperty(COOKIEPROP);
            Object cookieEnc = account.getProperty(COOKIEPROPENC);
            Map<String, String> cookies = null;
            if (cookiesRet != null && cookiesRet instanceof Map) {
                cookies = (Map<String, String>) cookiesRet;
            }
            boolean cookieLogin = false;
            if (cookies != null && forceRefresh == false) {
                cookieLogin = true;
            }
            if (cookieEnc == null || !(cookieEnc instanceof String)) {
                cookieLogin = false;
            }
            /* cookie login or not? */
            if (cookieLogin && cookies != null && cookies.get("enc") != null && cookies.get("enc").length() != 0) {
                logger.finer("Cookie Login");
                for (final Entry<String, String> cookie : cookies.entrySet()) {
                    br.setCookie(prtotcol + "://rapidshare.com", cookie.getKey(), cookie.getValue());
                }
                return br;
            } else {
                account.setProperty(COOKIEPROP, null);
                account.setProperty(COOKIEPROPENC, null);
            }
            try {
                final String req = prtotcol + "://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=getaccountdetails&withcookie=1&type=prem&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                this.queryAPI(br, req);
                final String error = br.getRegex("ERROR:(.*)").getMatch(0);
                if (error != null) {
                    logger.severe("10 " + br.toString());
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, error, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.containsHTML("Login failed")) {
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
                    logger.severe("2 " + br.toString());
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    cookies = new HashMap<String, String>();
                    cookies.put("enc", cookie);
                    account.setProperty(COOKIEPROP, cookies);
                    account.setProperty(COOKIEPROPENC, cookie);
                }
                // put all accountproperties
                for (final String[] m : br.getRegex("(\\w+)=([^\r^\n]+)").getMatches()) {
                    account.setProperty(m[0].trim(), m[1].trim());
                }
                this.updateAccountInfo(account, br);
                return br;
            } catch (PluginException e) {
                account.setProperty(COOKIEPROP, null);
                account.setProperty(COOKIEPROPENC, null);
                throw e;
            }
        }
    }

    /**
     * requests the API url req. if the http ip is blocked (UK-BT isp returns 500 or 502 error) https is used.
     * 
     * @param br
     * @param req
     * @throws Exception
     */
    private void queryAPI(Browser br, String req) throws Exception {

        if (br == null) {
            br = this.br;
        }
        this.workAroundTimeOut(br);/* TODO: remove me after 0.9xx public */
        br.forceDebug(true);
        // ssl works in free mode, too we have connection timeouts for many
        // calls without ssl
        if (this.useSSL()) {
            req = req.replaceFirst("http:", "https:");
        }
        final boolean follow = br.isFollowingRedirects();
        try {
            br.getHeaders().put("Accept-Language", "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");
            br.getHeaders().put("User-Agent", Rapidshare.UA.toString());
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

    private void readTimeoutHotFix() {
        if (ReadTimeoutHotFix.get() == true) { return; }
        synchronized (Rapidshare.LOCK) {
            if (ReadTimeoutHotFix.get() == true) { return; }
            Rapidshare.ReadTimeoutHotFix.set(true);
            try {
                final Class<?> c = Class.forName("sun.net.NetworkClient");
                Field field = null;
                if (field == null) {
                    try {
                        field = c.getField("defaultSoTimeout");
                    } catch (final NoSuchFieldException e) {
                    }
                }
                if (field == null) {
                    try {
                        field = c.getDeclaredField("defaultSoTimeout");
                    } catch (final NoSuchFieldException e) {
                    }
                }
                if (field != null) {
                    field.setAccessible(true);
                    try {
                        final Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                    final int newValue = 100000;
                    field.setInt(null, newValue);
                    final int after = field.getInt(null);
                    if (after == newValue) {
                        System.out.println("ReadTimeout Hotfix!YEAH!!!!");
                    } else {
                        System.out.println("ReadTimeout Hotfix!FAILED!!!!");
                    }
                } else {
                    System.out.println("ReadTimeout Hotfix!FAILED!!!!");
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                System.out.println("ReadTimeout Hotfix!FAILED!!!!");
            }
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
        return getAvailableStatus(downloadLink);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) return (AvailableStatus) ret;
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public void reset() {
        Rapidshare.RS_API_WAIT.set(0);
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    /**
     * Erzeugt den Configcontainer für die Gui
     */
    private void setConfigElements() {

        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, JDL.L("plugins.hoster.rapidshare.com.ssl2", "Use Secure Communication over SSL")).setDefaultValue(true));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), HTTPS_WORKAROUND, JDL.L("plugins.hoster.rapidshare.com.https", "Use HTTPS workaround for ISP Block")).setDefaultValue(false));
        /* caused issues lately because it seems some ip's are sharedhosting */
        // this.config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // this.getPluginConfig(), Rapidshare.PRE_RESOLVE,
        // JDL.L("plugins.hoster.rapidshare.com.resolve",
        // "Use IP instead of hostname")).setDefaultValue(false));

        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), WAIT_HOSTERFULL, JDL.L("plugins.hoster.rapidshare.com.waithosterfull", "Wait if all FreeUser Slots are full")).setDefaultValue(true));

    }

    /**
     * try the html filename workaround
     */
    private boolean tryWorkaround(final DownloadLink link) {
        if (link.getProperty("htmlworkaround", null) == null) {
            link.setProperty("htmlworkaround", "done");
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
            final boolean autoextend = "1".equals(data.get("autoextend"));
            final String billedUntilTime = data.get("billeduntil");
            final String serverTimeString = data.get("servertime");
            long nextBill = 0;
            if (billedUntilTime != null && serverTimeString != null) {
                /* next billing in */
                nextBill = Long.parseLong(billedUntilTime) - Long.parseLong(serverTimeString);
                if (nextBill <= 0) {
                    String possible = "";
                    if (autoextend) {
                        final long days = rapids / 495 * 30;
                        if (days > 0) {
                            possible = "(enough rapids for " + days + " days RapidPro left)";
                        }
                    }
                    ai.setStatus("No RapidPro" + possible);
                    ai.setExpired(true);
                    account.setValid(false);
                } else {
                    if (autoextend) {
                        final long days = rapids / 495 * 30;
                        if (days > 0) {
                            nextBill = nextBill + days * 24 * 60;
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

    private boolean useSSL() {
        return this.getPluginConfig().getBooleanProperty(SSL_CONNECTION, true) || this.getPluginConfig().getBooleanProperty(HTTPS_WORKAROUND, false);
    }

    private void workAroundTimeOut(final Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(30000);
                br.setReadTimeout(120000);
            }
        } catch (final Throwable e) {
        }
    }

}