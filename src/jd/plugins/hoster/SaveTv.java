//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.tv" }, urls = { "https?://(www\\.)?(save\\.tv|free\\.save\\.tv)/STV/M/obj/user/usShowVideoArchiveDetail\\.cfm\\?TelecastID=\\d+" }, flags = { 2 })
public class SaveTv extends PluginForHost {

    private final String        PREMIUMPOSTPAGE                 = "https://www.save.tv/STV/M/Index.cfm?sk=PREMIUM";
    private static final String USEORIGINALFILENAME             = "USEORIGINALFILENAME";
    private static final String PREFERADSFREE                   = "PREFERADSFREE";
    private static final String DOWNLOADONLYADSFREE             = "DOWNLOADONLYADSFREE";
    private final String        ADSFREEAVAILABLETEXT            = JDL.L("plugins.hoster.SaveTv.AdsFreeAvailable", "Video ist werbefrei verfügbar");
    private final String        ADSFREEANOTVAILABLE             = JDL.L("plugins.hoster.SaveTv.AdsFreeNotAvailable", "Video ist nicht werbefrei verfügbar");
    private boolean             ISADSFREEAVAILABLE              = false;
    private static final String PREFERREDFORMATNOTAVAILABLETEXT = "Das bevorzugte Format (H.264 Mobile) ist (noch) nicht verfügbar. Warte oder ändere die Einstellung!";
    private final String        NOCUTAVAILABLETEXT              = "Für diese Sendung steht keine Schnittliste zur Verfügung";
    private static final String PREFERH264MOBILE                = "PREFERH264MOBILE";
    private final String        PREFERH264MOBILETEXT            = "H.264 Mobile Videos bevorzugen (diese sind kleiner)";
    private static final String USEAPI                          = "USEAPI";
    // If this != null, API is in use
    private String              SESSIONID                       = null;
    private final String        APIKEY                          = "Q0FFQjZDQ0YtMDdFNC00MDQ4LTkyMDQtOUU5QjMxOEU3OUIz";
    private final String        APIPAGE                         = "http://api.save.tv/v2/Api.svc?wsdl";
    private static Object       LOCK                            = new Object();
    private final String        COOKIE_HOST                     = "http://save.tv";
    private static final String NOCHUNKS                        = "NOCHUNKS";

    /** Custom filename stuff */
    // TODO: Add date via API, add genre, add start and end-times
    private static final String CUSTOM_DATE                     = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME2                = "CUSTOM_FILENAME2";

    private static final String CUSTOM_FILENAME_SERIES2         = "CUSTOM_FILENAME_SERIES2";

    // TODO general: Add setting "ausgefallene Sendungen nicht herunterladen"
    // TODO: Add decrypter to get the whole archive of a user - maybe add setting to enable/disable this function (disable = return empty
    // linklist in decrypter)

    public SaveTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.save.tv/stv/s/obj/registration/RegPage1.cfm");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://").replace("free.save.tv/", "save.tv/"));
    }

    /**
     * TODO: Known Bugs in API mode: API cannot differ between H.264 Mobile and normal videos -> Cannot show any error in case user chose
     * H.264 but it's not available. --> These are NO FATAL bugs ---> Plugin will work fine with them!
     */

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        prepBrowser(br);
        br.setFollowRedirects(true);
        // Show id in case it is offline or plugin is broken
        link.setName(new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.setName(new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
            link.getLinkStatus().setStatusText("Kann Links ohne gültigen Account nicht überprüfen");
            return AvailableStatus.UNCHECKABLE;
        }
        br.setFollowRedirects(true);
        login(aa, false);
        final boolean useOriginalFilename = getPluginConfig().getBooleanProperty(USEORIGINALFILENAME);
        final boolean preferMobileVids = getPluginConfig().getBooleanProperty(PREFERH264MOBILE);
        String filename = null;
        String filesize = null;
        long datemilliseconds = 0;
        String date = null;
        String episodenumber = null;
        String seriestitle = null;
        String episodename = null;
        String broadcastTime = null;
        if (SESSIONID != null) {
            br.getHeaders().put("SOAPAction", "http://tempuri.org/IVideoArchive/GetAdFreeState");
            br.getHeaders().put("Content-Type", "text/xml");
            br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetAdFreeState xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + getTelecastId(link) + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified></GetAdFreeState></v:Body></v:Envelope>");
            if (br.containsHTML("<a:IsAdFreeAvailable>false</a:IsAdFreeAvailable>")) {
                link.getLinkStatus().setStatusText(ADSFREEANOTVAILABLE);
                ISADSFREEAVAILABLE = false;
            } else {
                link.getLinkStatus().setStatusText(ADSFREEAVAILABLETEXT);
                ISADSFREEAVAILABLE = true;
            }
            String preferMobileVideosString = "5";
            if (preferMobileVids) preferMobileVideosString = "4";
            br.getHeaders().put("SOAPAction", "http://tempuri.org/IDownload/GetStreamingUrl");
            br.getHeaders().put("Content-Type", "text/xml");
            br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetStreamingUrl xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + getTelecastId(link) + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified><recordingFormatId i:type=\"d:int\">" + preferMobileVideosString + "</recordingFormatId><recordingFormatIdSpecified i:type=\"d:boolean\">true</recordingFormatIdSpecified><adFree i:type=\"d:boolean\">false</adFree><adFreeSpecified i:type=\"d:boolean\">false</adFreeSpecified></GetStreamingUrl></v:Body></v:Envelope>");
            filename = br.getRegex("<a:Filename>([^<>\"]*?)</a").getMatch(0);
            filesize = br.getRegex("<a:SizeMB>(\\d+)</a:SizeMB>").getMatch(0);
            if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            final Regex fName = new Regex(filename, "((\\d{4}\\-\\d{2}\\-\\d{2})_\\d+_\\d+\\.mp4)");
            final String filenameReplace = fName.getMatch(0);
            if (filenameReplace != null) filename = filename.replace(filenameReplace, "");

            date = fName.getMatch(1);
            if (date != null) {
                datemilliseconds = TimeFormatter.getMilliSeconds(date, "yyyy-MM-dd", Locale.ENGLISH);
            }

            filesize += " KB";
        } else {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("(Leider ist ein Fehler aufgetreten|Bitte versuchen Sie es später noch einmal)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<h2 id=\"archive-detailbox-title\">(.*?)</h2>").getMatch(0);
            if (filename == null) filename = br.getRegex("id=\"telecast-detail\">.*?<h3>(.*?)</h2>").getMatch(0);
            filesize = br.getRegex(">Download</a>[ \t\n\r]+\\(ca\\.[ ]+([0-9\\.]+ [A-Za-z]{1,5})\\)[ \t\n\r]+</p>").getMatch(0);
            if (preferMobileVids) filesize = br.getRegex("title=\"H\\.264 Mobile\"( )?/>[\t\n\r ]+</a>[\t\n\r ]+<p>[\t\n\r ]+<a class=\"archive\\-detail\\-link\" href=\"javascript:STV\\.Archive\\.Download\\.openWindow\\(\\d+, \\d+, \\d+, \\d+\\);\">Download</a>[\t\n\r ]+\\(ca\\.[ ]+(.*?)\\)").getMatch(1);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            // Find custom filename stuff
            date = br.getRegex("<b>Datum:</b>[\t\n\r ]+[A-Za-z]{1,3}\\.,([^<>\"]*?)</p>").getMatch(0);
            if (date != null) {
                date = date.trim();
                if (!date.endsWith(".2013")) date += +Calendar.getInstance().get(Calendar.YEAR);
                datemilliseconds = TimeFormatter.getMilliSeconds(date, "dd.MM.yyyy", Locale.GERMAN);
            }
            broadcastTime = br.getRegex("<b>Ausstrahlungszeitraum:</b>[\t\n\r ]+(\\d{2}:\\d{2}) \\-").getMatch(0);
            if (br.containsHTML("src=\"/STV/IMG/global/TVCategorie/kat2\\.jpg\"")) {
                // For series
                link.setProperty("category", 2);
                episodenumber = br.getRegex("<strong>Folge:</strong> (\\d+)</p>").getMatch(0);
                final Regex seriesInfo = br.getRegex("<h3>([^<>\"]*?)</h2>[\t\n\r ]+<p>([^<>\"]*?)</p>[\t\n\r ]+<p>([^<>\"]*?)</p>");
                seriestitle = seriesInfo.getMatch(0);
                episodename = seriesInfo.getMatch(1);
                if (episodename != null && episodename.contains("Originaltitel")) {
                    episodename = seriesInfo.getMatch(2);
                }
            } else {
                // For everything else
                link.setProperty("category", 0);
            }

            final DecimalFormat df = new DecimalFormat("0000");
            br.postPage("https://www.save.tv/STV/M/obj/cRecordOrder/croGetAdFreeAvailable.cfm?null.GetAdFreeAvailable", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetAdFreeAvailable&c0-id=" + df.format(new Random().nextInt(1000)) + "_" + System.currentTimeMillis() + "&c0-param0=number:" + getTelecastId(link) + "&xml=true&extend=function (object) {for (property in object) {this[property] = object[property];}return this;}&");
            if (br.containsHTML("= \\'3\\';")) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SaveTv.NoCutListAvailable", NOCUTAVAILABLETEXT));
            } else if (br.containsHTML("= \\'1\\';")) {
                link.getLinkStatus().setStatusText(ADSFREEAVAILABLETEXT);
                ISADSFREEAVAILABLE = true;
            } else {
                link.getLinkStatus().setStatusText(ADSFREEANOTVAILABLE);
                ISADSFREEAVAILABLE = false;
            }
        }
        filename = filename.replace("_", " ");
        filename = filename.trim();
        filename = filename.replaceAll("(\r|\n)", "");
        final String[] unneededSpaces = new Regex(filename, ".*?([ ]{2,}).*?").getColumn(0);
        if (unneededSpaces != null && unneededSpaces.length != 0) {
            for (String unneededSpace : unneededSpaces) {
                filename = filename.replace(unneededSpace, " ");
            }
        }
        link.setAvailable(true);
        if (filesize != null) {
            filesize = filesize.replace(".", "");
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(".", "")));
        }
        // Set properties which are needed for custom filenames
        link.setProperty("plainfilename", filename);
        // Add time to date
        if (broadcastTime != null) {
            // Add missing time - Also add one hour because otherwise one is missing
            datemilliseconds += TimeFormatter.getMilliSeconds(broadcastTime, "HH:mm", Locale.GERMAN) + (60 * 60 * 1000l);
        }
        link.setProperty("originaldate", datemilliseconds);
        if (episodenumber != null) link.setProperty("episodenumber", Integer.parseInt(episodenumber));
        link.setProperty("seriestitle", seriestitle);
        link.setProperty("episodename", episodename);
        link.setProperty("type", ".mp4");
        if (useOriginalFilename || SESSIONID != null) {
            link.setName(filename + ".mp4");
        } else {
            final String formattedFilename = getFormattedFilename(link);
            link.setName(formattedFilename);
        }
        return AvailableStatus.TRUE;
    }

    public void extendedLogin(String accessSite, String postPage, String user, String password) throws Exception {
        br.getPage(accessSite);
        String postData = "sUsername=" + Encoding.urlEncode_light(user) + "&sPassword=" + Encoding.urlEncode_light(password) + "&image.x=" + new Random().nextInt(100) + "&image.y=" + new Random().nextInt(100) + "&image=Login&bAutoLoginActivate=1";
        br.postPage(postPage, postData);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Premium save.tv User");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://free.save.tv/STV/S/misc/miscShowTermsConditionsInMainFrame.cfm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered/premium users");
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        String dllink = null;
        // User wants ads-free but it's not available -> Wait 12 hours, status can still change but probably won't
        if (this.getPluginConfig().getBooleanProperty(DOWNLOADONLYADSFREE, false) && !this.ISADSFREEAVAILABLE) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Dieses Video ist nicht mit angewandter Schnittliste verfügbar!", 12 * 60 * 60 * 1000l);
        final String telecastID = getTelecastId(downloadLink);
        final boolean preferAdsFree = getPluginConfig().getBooleanProperty(PREFERADSFREE);
        final boolean preferMobileVids = getPluginConfig().getBooleanProperty(PREFERH264MOBILE);
        String downloadWithoutAds = "false";
        if (preferAdsFree) downloadWithoutAds = "true";
        String preferMobileVideos = null;
        if (SESSIONID != null) {
            preferMobileVideos = "5";
            if (preferMobileVids) preferMobileVideos = "4";
            br.getHeaders().put("SOAPAction", "http://tempuri.org/IDownload/GetStreamingUrl");
            br.getHeaders().put("Content-Type", "text/xml");
            br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetStreamingUrl xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + telecastID + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified><recordingFormatId i:type=\"d:int\">" + preferMobileVideos + "</recordingFormatId><recordingFormatIdSpecified i:type=\"d:boolean\">true</recordingFormatIdSpecified><adFree i:type=\"d:boolean\">false</adFree><adFreeSpecified i:type=\"d:boolean\">" + downloadWithoutAds
                    + "</adFreeSpecified></GetStreamingUrl></v:Body></v:Envelope>");
            // Example request streaming url: http://jdownloader.net:8081/pastebin/110483
            dllink = br.getRegex("<a:DownloadUrl>(http://[^<>\"]*?)</a").getMatch(0);
        } else {
            br.getPage(downloadLink.getDownloadURL());
            preferMobileVideos = "c0-param1=number:0";
            if (preferMobileVids) preferMobileVideos = "c0-param1=number:1";
            br.postPage("http://www.save.tv/STV/M/obj/cRecordOrder/croGetDownloadUrl.cfm?null.GetDownloadUrl", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetDownloadUrl&c0-id=&c0-param0=number:" + telecastID + "&" + preferMobileVideos + "&c0-param2=boolean:" + downloadWithoutAds + "&xml=true&");
            if (br.containsHTML("Die Aufnahme liegt nicht im gewünschten Format vor")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, PREFERREDFORMATNOTAVAILABLETEXT, 4 * 60 * 60 * 1000l); }
            dllink = br.getRegex("\\'OK\\',\\'(http://[^<>\"\\']+)\\'").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\\'(http://[^<>\"\\']+/\\?m=dl)\\'").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        int maxChunks = -4;
        boolean resume = true;
        if (downloadLink.getBooleanProperty(SaveTv.NOCHUNKS, false) || resume == false) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean useOriginalFilename = getPluginConfig().getBooleanProperty(USEORIGINALFILENAME);
        if (useOriginalFilename) {
            downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        } else {
            downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            /* unknown error, we disable multiple chunks */
            if (downloadLink.getBooleanProperty(SaveTv.NOCHUNKS, false) == false) {
                downloadLink.setProperty(SaveTv.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    public void login(final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final boolean useAPI = getPluginConfig().getBooleanProperty(USEAPI);
        if (useAPI) {
            SESSIONID = account.getStringProperty("sessionid", null);
            final long lastUse = account.getLongProperty("lastuse", -1);
            // Only generate new sessionID if we have none or it's older than 6 hours
            if (SESSIONID == null || (System.currentTimeMillis() - lastUse) > 360000) {
                br.getHeaders().put("User-Agent", "kSOAP/2.0");
                br.getHeaders().put("Content-Type", "text/xml");
                br.getHeaders().put("SOAPAction", "http://tempuri.org/ISession/CreateSession");
                br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><CreateSession xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><apiKey i:type=\"d:string\">" + Encoding.Base64Decode(APIKEY) + "</apiKey></CreateSession></v:Body></v:Envelope>");
                SESSIONID = br.getRegex("<a:SessionId>([^<>\"]*?)</a:SessionId>").getMatch(0);
                if (SESSIONID == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** TODO: Save and use session expiry date */
                // // Get expiry date
                // br.getHeaders().put("Content-Type", "text/xml");
                // br.getHeaders().put("SOAPAction", "http://tempuri.org/ISession/GetSessionExpiry");
                // br.postPage(APIPAGE,
                // "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetSessionExpiry xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">"
                // + SaveTv.SESSIONID + "</sessionId></GetSessionExpiry></v:Body></v:Envelope>");
                // // Example: 2013-05-07T21:39:21.07275Z
                // final String sessionExpiry = br.getRegex("<GetSessionExpiryResult>([^<>\"]*?)</GetSessionExpiryResult>").getMatch(0);
                account.setProperty("lastuse", System.currentTimeMillis());
                account.setProperty("sessionid", SESSIONID);
            }
            br.getHeaders().put("SOAPAction", "http://tempuri.org/IUser/Login");
            br.getHeaders().put("Content-Type", "text/xml");
            final String postData = "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><Login xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SESSIONID + "</sessionId><username i:type=\"d:string\">" + account.getUser() + "</username><password i:type=\"d:string\">" + account.getPass() + "</password></Login></v:Body></v:Envelope>";
            br.postPage(APIPAGE, postData);
            if (!br.containsHTML("<a:HasPremiumStatus>true</a:HasPremiumStatus>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            synchronized (LOCK) {
                try {
                    /** Load cookies */
                    br.setCookiesExclusive(true);
                    final Object ret = account.getProperty("cookies", null);
                    boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                    if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                    if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                        final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                        if (account.isValid()) {
                            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                                final String key = cookieEntry.getKey();
                                final String value = cookieEntry.getValue();
                                this.br.setCookie(COOKIE_HOST, key, value);
                            }
                            return;
                        }
                    }
                    br.getPage("http://www.save.tv/STV/S/misc/home.cfm");
                    extendedLogin("http://www.save.tv/STV/S/misc/home.cfm?", PREMIUMPOSTPAGE, Encoding.urlEncode(account.getUser()), Encoding.urlEncode(account.getPass()));
                    if (!br.containsHTML("/STV/M/obj/user/usEdit.cfm") || br.containsHTML("Bitte verifizieren Sie Ihre Logindaten")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    /** Save cookies */
                    final HashMap<String, String> cookies = new HashMap<String, String>();
                    final Cookies add = this.br.getCookies(COOKIE_HOST);
                    for (final Cookie c : add.getCookies()) {
                        cookies.put(c.getKey(), c.getValue());
                    }
                    account.setProperty("name", Encoding.urlEncode(account.getUser()));
                    account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                    account.setProperty("cookies", cookies);
                } catch (final PluginException e) {
                    account.setProperty("cookies", Property.NULL);
                    throw e;
                }
            }
        }
    }

    public void prepBrowser(final Browser br) {
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
    }

    private String getTelecastId(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), "TelecastID=(\\d+)").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    private String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("save.tv");
        final DecimalFormat df = new DecimalFormat("0000");
        String ext = downloadLink.getStringProperty("type", null);
        if (ext == null) ext = ".mp4";
        final long date = downloadLink.getLongProperty("originaldate", 0);
        String formattedDate = null;
        final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy");
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        Date theDate = new Date(date);

        if (userDefinedDateFormat != null) {
            try {
                formatter = new SimpleDateFormat(userDefinedDateFormat);
                formattedDate = formatter.format(theDate);
            } catch (Exception e) {
                // prevent user error killing plugin.
                formattedDate = "";
            }
        }

        String formattedFilename = null;
        if (downloadLink.getLongProperty("category", 0) == 2) {
            // For series
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_SERIES2, defaultCustomSeriesFilename);
            if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
            if (!formattedFilename.contains("*endung*") || (!formattedFilename.contains("*serientitel*") && !formattedFilename.contains("*episodenname*") && !formattedFilename.contains("*episodennummer*"))) formattedFilename = defaultCustomFilename;

            final String seriestitle = downloadLink.getStringProperty("seriestitle", null);
            final String episodename = downloadLink.getStringProperty("episodename", null);
            final long episodenumber = downloadLink.getLongProperty("episodenumber", 0);

            formattedFilename = formattedFilename.replace("*zufallszahl*", df.format(new Random().nextInt(10000)));
            formattedFilename = formattedFilename.replace("*datum*", formattedDate);
            if (episodenumber == 0) {
                formattedFilename = formattedFilename.replace("*episodennummer*", "-");
            } else {
                formattedFilename = formattedFilename.replace("*episodennummer*", Long.toString(episodenumber));
            }
            formattedFilename = formattedFilename.replace("*endung*", ext);
            // Insert actual filename at the end to prevent errors with tags
            formattedFilename = formattedFilename.replace("*serientitel*", seriestitle);
            formattedFilename = formattedFilename.replace("*episodenname*", episodename);
        } else {
            String title = downloadLink.getStringProperty("plainfilename", null);
            // For all other links
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME2, defaultCustomFilename);
            if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
            if (!formattedFilename.contains("*videotitel*") || !formattedFilename.contains("*endung*")) formattedFilename = defaultCustomFilename;

            formattedFilename = formattedFilename.replace("*zufallszahl*", df.format(new Random().nextInt(10000)));
            formattedFilename = formattedFilename.replace("*datum*", formattedDate);
            formattedFilename = formattedFilename.replace("*endung*", ext);
            // Insert actual filename at the end to prevent errors with tags
            formattedFilename = formattedFilename.replace("*videotitel*", title);
        }

        return formattedFilename;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public String getDescription() {
        return "JDownloader's Save.tv Plugin helps downloading Videoclips from Save.tv. Save.tv provides different settings for its downloads.";
    }

    private final static String defaultCustomFilename       = "*videotitel**zufallszahl**endung*";
    private final static String defaultCustomSeriesFilename = "*serientitel* ¦ *episodennummer* ¦ *episodenname**endung*";

    private void setConfigElements() {
        final ConfigEntry useMobileAPI = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEAPI, JDL.L("plugins.hoster.SaveTv.UseMobileAPI", "Mobile API verwenden (BETA! Benutzerdefinierte Dateinamen werden deaktiviert!)")).setDefaultValue(false);
        getConfig().addEntry(useMobileAPI);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry origName = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEORIGINALFILENAME, JDL.L("plugins.hoster.SaveTv.UseOriginalFilename", "Original Dateinamen verwenden (erst beim Download sichtbar)")).setDefaultValue(false).setEnabledCondidtion(useMobileAPI, false);
        getConfig().addEntry(origName);
        final ConfigEntry preferAdsFree = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERADSFREE, JDL.L("plugins.hoster.SaveTv.PreferAdFreeVideos", "Aufnahmen mit angewandter Schnittliste bevorzugen")).setDefaultValue(true);
        getConfig().addEntry(preferAdsFree);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DOWNLOADONLYADSFREE, JDL.L("plugins.hoster.SaveTv.downloadOnlyAdsFree", "Nur Aufnahmen mit angewandter Schnittliste laden (zum aktivieren obere Option aktivieren)")).setDefaultValue(false).setEnabledCondidtion(preferAdsFree, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERH264MOBILE, JDL.L("plugins.hoster.SaveTv.PreferH264MobileVideos", PREFERH264MOBILETEXT)).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Dateiname Einstellungen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.savetv.customdate", "Setze das Datumsformat:")).setDefaultValue("dd.MM.yyyy").setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Individualisiere die Dateinamen!"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME2, JDL.L("plugins.hoster.savetv.customfilename", "Eigener Dateiname für Filme:")).setDefaultValue(defaultCustomFilename).setEnabledCondidtion(origName, false));
        final StringBuilder sb = new StringBuilder();
        sb.append("Erklärung der verfügbaren Tags:\r\n");
        sb.append("*datum* = Datum der Ausstrahlung der aufgenommenen Sendung - erscheint im oben definierten Format\r\n");
        sb.append("*videotitel* = Name des Videos ohne Dateiendung\r\n");
        sb.append("*zufallszahl* = Eine vierstellige Zufallszahl - nützlich um Dateinamenkollisionen zu vermeiden\r\n");
        sb.append("*endung* = Die Dateiendung, in diesem Fall '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_SERIES2, JDL.L("plugins.hoster.savetv.customseriesfilename", "Eigener Dateiname für Serien:")).setDefaultValue(defaultCustomSeriesFilename).setEnabledCondidtion(origName, false));
        final StringBuilder sbseries = new StringBuilder();
        sbseries.append("Erklärung der verfügbaren Tags:\r\n");
        sbseries.append("*date* = Datum der Ausstrahlung der aufgenommenen Sendung - erscheint im oben definierten Format\r\n");
        sbseries.append("*serientitel* = Name der Serie\r\n");
        sbseries.append("*episodenname* = Name der Episode\r\n");
        sbseries.append("*episodennummer* = Episodennummer - falls nicht gegeben entspricht das '-' (Bindestrich)\r\n");
        sbseries.append("*zufallszahl* = Eine vierstellige Zufallszahl - nützlich um Dateinamenkollisionen zu vermeiden\r\n");
        sbseries.append("*endung* = Die Dateiendung, in diesem Fall '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbseries.toString()).setEnabledCondidtion(origName, false));
    }
    // private void checkFeatureDialog() {
    // SubConfiguration config = null;
    // try {
    // config = getPluginConfig();
    // if (config.getBooleanProperty("ftDialogShown", Boolean.FALSE) == false) {
    // if (config.getProperty("ftDialogShown2") == null) {
    // showFeatureDialog();
    // } else {
    // config = null;
    // }
    // } else {
    // config = null;
    // }
    // } catch (final Throwable e) {
    // } finally {
    // if (config != null) {
    // config.setProperty("ftDialogShown", Boolean.TRUE);
    // config.setProperty("ftDialogShown2", "shown");
    // config.save();
    // }
    // }
    // }
    //
    // private static void showFeatureDialog() {
    // try {
    // SwingUtilities.invokeAndWait(new Runnable() {
    //
    // @Override
    // public void run() {
    // try {
    // String message = null;
    // String title = null;
    // title = "save.tv - neue Features";
    // message = "Hallo liebe save.tv User.\r\n";
    // message = "Ab sofort gibt es Neue Features (Plugineinstellungen):\r\n";
    // message = "- Die Dateinamen lassen sich individualisieren \r\n";
    // message = "- 'Aufnahmen mit angewandter Schnittliste bevorzugen' ist nun als Option verfügbar\r\n";
    // message = "- Verbesserungen an Dateinamen, die über die API kommen\r\n";
    // message = "- Weitere Features (Download des kompletten Videoarchives über einen Klick) werden bis zum 10.11.13 folgen :)\r\n";
    // message = "\r\n";
    // message = "In JDownloader 0.9.581 findet ihr die Plugin Einstellungen unter:\r\n";
    // message = "Einstellungen -> Hoster -> save.tv -> Doppelklick oder anklicken und links unten auf 'Einstellungen'\r\n";
    // message = "In der JDownloader 2 BETA findet ihr sie unter Einstellungen -> Plugin Einstellungen -> save.tv\r\n";
    // message = "- Das JDownloader Team wünscht weiterhin viel Spaß mit JDownloader! -\r\n";
    // message = "- Bugs bitte unter board.jdownloader.org melden, danke :)\r\n";
    // JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION,
    // JOptionPane.INFORMATION_MESSAGE, null);
    // } catch (Throwable e) {
    // }
    // }
    // });
    // } catch (Throwable e) {
    // }
    // }
}