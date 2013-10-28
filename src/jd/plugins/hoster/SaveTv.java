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
import java.util.Date;
import java.util.HashMap;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.tv" }, urls = { "https?://(www\\.)?(save\\.tv|free\\.save\\.tv)/STV/M/obj/user/usShowVideoArchiveDetail\\.cfm\\?TelecastID=\\d+" }, flags = { 2 })
public class SaveTv extends PluginForHost {

    private final String        PREMIUMPOSTPAGE                 = "https://www.save.tv/STV/M/Index.cfm?sk=PREMIUM";
    private static final String NORANDOMNUMBERS                 = "NORANDOMNUMBERS";
    private static final String USEORIGINALFILENAME             = "USEORIGINALFILENAME";
    private static final String PREFERADSFREE                   = "PREFERADSFREE";
    private final String        ADSFREEAVAILABLETEXT            = JDL.L("plugins.hoster.SaveTv.AdsFreeAvailable", "Video ist werbefrei verfügbar");
    private final String        ADSFREEANOTVAILABLE             = JDL.L("plugins.hoster.SaveTv.AdsFreeNotAvailable", "Video ist nicht werbefrei verfügbar");
    private static final String PREFERREDFORMATNOTAVAILABLETEXT = "Das bevorzugte Format (H.264 Mobile) ist (noch) nicht verfügbar. Warte oder ändere die Einstellung!";
    private final String        NOCUTAVAILABLETEXT              = "Für diese Sendung steht keine Schnittliste zur Verfügung";
    private static final String PREFERH264MOBILE                = "PREFERH264MOBILE";
    private final String        PREFERH264MOBILETEXT            = "H.264 Mobile Videos bevorzugen (diese sind kleiner)";
    private static final String USEAPI                          = "USEAPI";
    private String              SESSIONID                       = null;
    private final String        APIKEY                          = "Q0FFQjZDQ0YtMDdFNC00MDQ4LTkyMDQtOUU5QjMxOEU3OUIz";
    private final String        APIPAGE                         = "http://api.save.tv/v2/Api.svc?wsdl";
    private static Object       LOCK                            = new Object();
    private final String        COOKIE_HOST                     = "http://save.tv";
    private static final String NOCHUNKS                        = "NOCHUNKS";

    /** Custom filename stuff */
    // TODO: Add date via API, add genre, add start and end-times
    private static final String CUSTOM_DATE                     = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME                 = "CUSTOM_FILENAME";

    // TODO general: Add setting "only download cut (geschnittene) Videos" and "ausgefallene Sendungen nicht herunterladen"

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
     * TODO: Known Bugs in API mode: 1. Not all filename settings are applied because API returns another filename in general than site does
     * 2. API cannot differ between H.264 Mobile and normal videos -> Cannot show any error in case user chose H.264 but it's not available.
     * --> These are NO FATAL bugs ---> Plugin will work fine with them!
     */

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
        final boolean dontModifyFilename = getPluginConfig().getBooleanProperty(NORANDOMNUMBERS);
        final boolean preferMobileVids = getPluginConfig().getBooleanProperty(PREFERH264MOBILE);
        String filename = null;
        String filesize = null;
        String date = null;
        if (SESSIONID != null) {
            br.getHeaders().put("SOAPAction", "http://tempuri.org/IVideoArchive/GetAdFreeState");
            br.getHeaders().put("Content-Type", "text/xml");
            br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetAdFreeState xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + getTelecastId(link) + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified></GetAdFreeState></v:Body></v:Envelope>");
            if (br.containsHTML("<a:IsAdFreeAvailable>false</a:IsAdFreeAvailable>")) {
                link.getLinkStatus().setStatusText(ADSFREEANOTVAILABLE);
            } else {
                link.getLinkStatus().setStatusText(ADSFREEAVAILABLETEXT);
            }
            String preferMobileVideosString = "5";
            if (preferMobileVids) preferMobileVideosString = "4";
            br.getHeaders().put("SOAPAction", "http://tempuri.org/IDownload/GetStreamingUrl");
            br.getHeaders().put("Content-Type", "text/xml");
            br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetStreamingUrl xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + getTelecastId(link) + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified><recordingFormatId i:type=\"d:int\">" + preferMobileVideosString + "</recordingFormatId><recordingFormatIdSpecified i:type=\"d:boolean\">true</recordingFormatIdSpecified><adFree i:type=\"d:boolean\">false</adFree><adFreeSpecified i:type=\"d:boolean\">false</adFreeSpecified></GetStreamingUrl></v:Body></v:Envelope>");
            filename = br.getRegex("<a:Filename>([^<>\"]*?)</a").getMatch(0);
            filesize = br.getRegex("<a:SizeMB>(\\d+)</a:SizeMB>").getMatch(0);
            if (filename == null || filesize == null) {
                link.setName(new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
            final DecimalFormat df = new DecimalFormat("0000");

            // Find custom filename stuff
            date = br.getRegex("<b>Datum:</b>[\t\n\r ]+[A-Za-z]{1,3}\\.,([^<>\"]*?)</p>").getMatch(0);

            br.postPage("https://www.save.tv/STV/M/obj/cRecordOrder/croGetAdFreeAvailable.cfm?null.GetAdFreeAvailable", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetAdFreeAvailable&c0-id=" + df.format(new Random().nextInt(1000)) + "_" + System.currentTimeMillis() + "&c0-param0=number:" + getTelecastId(link) + "&xml=true&extend=function (object) {for (property in object) {this[property] = object[property];}return this;}&");
            if (br.containsHTML("= \\'3\\';")) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SaveTv.NoCutListAvailable", NOCUTAVAILABLETEXT));
            } else if (br.containsHTML("= \\'1\\';")) {
                link.getLinkStatus().setStatusText(ADSFREEAVAILABLETEXT);
            } else {
                link.getLinkStatus().setStatusText(ADSFREEANOTVAILABLE);
            }
        }
        filename = filename.trim();
        filename = filename.replaceAll("(\r|\n)", "");
        final String[] unneededSpaces = new Regex(filename, ".*?([ ]{2,}).*?").getColumn(0);
        if (unneededSpaces != null && unneededSpaces.length != 0) {
            for (String unneededSpace : unneededSpaces) {
                filename = filename.replace(unneededSpace, " ");
            }
        }
        if (!dontModifyFilename || useOriginalFilename) filename = filename + new Random().nextInt(1000);
        link.setName(filename + ".mp4");
        link.setAvailable(true);
        if (filesize != null) {
            filesize = filesize.replace(".", "");
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(".", "")));
        }
        // Set properties which are needed for custom filenames
        link.setProperty("plainfilename", filename);
        if (date != null) {
            date = date.trim();
            // TODO: Add current year instead of hardcoded 2013!
            if (!date.endsWith(".2013")) date += "2013";
            link.setProperty("originaldate", date);
        }
        link.setProperty("type", ".mp4");
        String lol = getFormattedFilename(link);
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
            // Example request: http://jdownloader.net:8081/pastebin/110483
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
        if (downloadLink.getFinalFileName() == null) {
            if (useOriginalFilename)
                downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
            else
                downloadLink.setFinalFileName(downloadLink.getName());
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

    private String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        String songTitle = downloadLink.getStringProperty("plainfilename", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("save.tv");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
        if (!formattedFilename.contains("*videotitle*") || !formattedFilename.contains("*ext*")) formattedFilename = defaultCustomFilename;
        String ext = downloadLink.getStringProperty("type", null);
        if (ext == null) ext = ".mp4";

        final String date = downloadLink.getStringProperty("originaldate", null);

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy");
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            Date dateStr = formatter.parse(date);

            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null)
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            else
                formattedFilename = formattedFilename.replace("*date*", "");
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*videotitle*", songTitle);

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

    private final static String defaultCustomFilename = "*videotitle**ext*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEAPI, JDL.L("plugins.hoster.SaveTv.UseAPI", "Mobile API verwenden (BETA!)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.NORANDOMNUMBERS, JDL.L("plugins.hoster.SaveTv.DontModifyFilename", "Keine Zufallszahlen an Dateinamen anhängen (kann Probleme verursachen)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEORIGINALFILENAME, JDL.L("plugins.hoster.SaveTv.UseOriginalFilename", "Original Dateinamen verwenden (erst beim Download sichtbar)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERADSFREE, JDL.L("plugins.hoster.SaveTv.PreferAdFreeVideos", "Geschnittene Videos (Videos ohne Werbung) bevorzugen")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERH264MOBILE, JDL.L("plugins.hoster.SaveTv.PreferH264MobileVideos", PREFERH264MOBILETEXT)).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename/packagename properties:"));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE,
        // JDL.L("plugins.hoster.savetv.customdate", "Define how the date should look.")).setDefaultValue("dd.MM.yyyy"));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties:"));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*videotitle**ext*'"));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME,
        // JDL.L("plugins.hoster.savetv.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        // final StringBuilder sb = new StringBuilder();
        // sb.append("Explanation of the available tags:\r\n");
        // sb.append("*date* = date when the link was posted - appears in the user-defined format above\r\n");
        // sb.append("*videotitle* = name of the video without extension\r\n");
        // sb.append("*ext* = the extension of the file, in this case usually '.mp4'");
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
    }
}