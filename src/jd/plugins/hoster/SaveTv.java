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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
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

    private static final String PREMIUMPOSTPAGE         = "https://www.save.tv/STV/M/Index.cfm?sk=PREMIUM";
    private static final String NORANDOMNUMBERS         = "NORANDOMNUMBERS";
    private static final String USEORIGINALFILENAME     = "USEORIGINALFILENAME";
    private static final String PREFERADSFREE           = "PREFERADSFREE";
    private static final String ADSFREEAVAILABLE        = "for=\"archive-layer-adfree\">Schnittliste vor dem Download / Streaming anwenden<";
    private static final String ADSFREEAVAILABLETEXT    = "Video ist ohne Werbung verfügbar";
    private static final String ADSFREEANOTVAILABLETEXT = "Videos ohne Werbung werden bevorzugt, dieses ist aber nur mit Werbung verfügbar";
    private static final String PREFERH264MOBILE        = "PREFERH264MOBILE";
    private static final String PREFERH264MOBILETEXT    = "H.264 Mobile Videos bevorzugen (diese sind kleiner)";
    private static final String USEAPI                  = "USEAPI";
    private static String       SESSIONID               = null;
    private static final String APIKEY                  = "Q0FFQjZDQ0YtMDdFNC00MDQ4LTkyMDQtOUU5QjMxOEU3OUIz";
    private static final String APIPAGE                 = "http://api.save.tv/v2/Api.svc?wsdl";
    private static Object       LOCK                    = new Object();
    private static final String COOKIE_HOST             = "http://save.tv";

    public SaveTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.save.tv/stv/s/obj/registration/RegPage1.cfm");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://").replace("free.save.tv/", "save.tv/"));
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
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
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
        if (SaveTv.SESSIONID != null) {
            preferMobileVideos = "5";
            if (preferMobileVids) preferMobileVideos = "4";
            br.getHeaders().put("SOAPAction", "http://tempuri.org/IDownload/GetStreamingUrl");
            br.getHeaders().put("Content-Type", "text/xml");
            br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetStreamingUrl xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SaveTv.SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + telecastID + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified><recordingFormatId i:type=\"d:int\">" + preferMobileVideos + "</recordingFormatId><recordingFormatIdSpecified i:type=\"d:boolean\">true</recordingFormatIdSpecified><adFree i:type=\"d:boolean\">false</adFree><adFreeSpecified i:type=\"d:boolean\">" + downloadWithoutAds
                    + "</adFreeSpecified></GetStreamingUrl></v:Body></v:Envelope>");
            // Example request: http://jdownloader.net:8081/pastebin/110483
            dllink = br.getRegex("<a:DownloadUrl>(http://[^<>\"]*?)</a").getMatch(0);
        } else {
            br.getPage(downloadLink.getDownloadURL());
            preferMobileVideos = "c0-param1=number:0";
            if (preferMobileVids) preferMobileVideos = "c0-param1=number:1";
            br.postPage("http://www.save.tv/STV/M/obj/cRecordOrder/croGetDownloadUrl.cfm?null.GetDownloadUrl", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetDownloadUrl&c0-id=&c0-param0=number:" + telecastID + "&" + preferMobileVideos + "&c0-param2=boolean:" + downloadWithoutAds + "&xml=true&");
            dllink = br.getRegex("\\'OK\\',\\'(http://[^<>\"\\']+)\\'").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\\'(http://[^<>\"\\']+/\\?m=dl)\\'").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -4);
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
        dl.startDownload();
    }

    public void login(final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final boolean useAPI = getPluginConfig().getBooleanProperty(USEAPI);
        if (useAPI) {
            SaveTv.SESSIONID = account.getStringProperty("sessionid", null);
            final long lastUse = account.getLongProperty("lastuse", -1);
            // Only generate new sessionID if we have none or it's older than 6 hours
            if (SaveTv.SESSIONID == null || (System.currentTimeMillis() - lastUse) > 360000) {
                br.getHeaders().put("User-Agent", "kSOAP/2.0");
                br.getHeaders().put("Content-Type", "text/xml");
                br.getHeaders().put("SOAPAction", "http://tempuri.org/ISession/CreateSession");
                br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><CreateSession xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><apiKey i:type=\"d:string\">" + Encoding.Base64Decode(SaveTv.APIKEY) + "</apiKey></CreateSession></v:Body></v:Envelope>");
                SaveTv.SESSIONID = br.getRegex("<a:SessionId>([^<>\"]*?)</a:SessionId>").getMatch(0);
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
                account.setProperty("sessionid", SaveTv.SESSIONID);
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        prepBrowser(br);
        br.setFollowRedirects(true);

        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Kann Links ohne gültigen Account nicht überprüfen");
            return AvailableStatus.UNCHECKABLE;
        }
        br.setFollowRedirects(true);
        login(aa, false);
        boolean useOriginalFilename = getPluginConfig().getBooleanProperty(USEORIGINALFILENAME);
        boolean dontModifyFilename = getPluginConfig().getBooleanProperty(NORANDOMNUMBERS);
        boolean preferAdsFree = getPluginConfig().getBooleanProperty(PREFERADSFREE);
        boolean preferMobileVids = getPluginConfig().getBooleanProperty(PREFERH264MOBILE);
        String filename = null;
        String filesize = null;
        if (SaveTv.SESSIONID != null) {
            // Check adFree state: http://jdownloader.net:8081/pastebin/110484
            String preferMobileVideos = "5";
            if (preferMobileVids) preferMobileVideos = "4";
            br.getHeaders().put("SOAPAction", "http://tempuri.org/IDownload/GetStreamingUrl");
            br.getHeaders().put("Content-Type", "text/xml");
            br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetStreamingUrl xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SaveTv.SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + getTelecastId(link) + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified><recordingFormatId i:type=\"d:int\">" + preferMobileVideos + "</recordingFormatId><recordingFormatIdSpecified i:type=\"d:boolean\">true</recordingFormatIdSpecified><adFree i:type=\"d:boolean\">false</adFree><adFreeSpecified i:type=\"d:boolean\">false</adFreeSpecified></GetStreamingUrl></v:Body></v:Envelope>");
            filename = br.getRegex("<a:Filename>([^<>\"]*?)</a").getMatch(0);
            filesize = br.getRegex("<a:SizeMB>(\\d+)</a:SizeMB>").getMatch(0);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filesize += " KB";
        } else {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("(Leider ist ein Fehler aufgetreten|Bitte versuchen Sie es später noch einmal)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<h2 id=\"archive-detailbox-title\">(.*?)</h2>").getMatch(0);
            if (filename == null) filename = br.getRegex("id=\"telecast-detail\">.*?<h3>(.*?)</h2>").getMatch(0);
            filesize = br.getRegex("title=\"H\\.264 High Quality\"( )?/>[\t\n\r ]+</a>[\t\n\r ]+<p>[\t\n\r ]+<a class=\"archive-detail-link\" href=\"javascript:STV\\.Archive\\.Download\\.openWindow\\(\\d+, \\d+, \\d+, \\d+\\);\">Download</a>[\t\n\r ]+\\(ca\\.[ ]+(.*?)\\)").getMatch(1);
            if (preferMobileVids) filesize = br.getRegex("title=\"H\\.264 Mobile\"( )?/>[\t\n\r ]+</a>[\t\n\r ]+<p>[\t\n\r ]+<a class=\"archive-detail-link\" href=\"javascript:STV\\.Archive\\.Download\\.openWindow\\(\\d+, \\d+, \\d+, \\d+\\);\">Download</a>[\t\n\r ]+\\(ca\\.[ ]+(.*?)\\)").getMatch(1);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filesize = filesize.replace(".", "");
            if (preferAdsFree && br.containsHTML(ADSFREEAVAILABLE)) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SaveTv.AdsFreeAvailable", ADSFREEAVAILABLETEXT));
            } else if (preferAdsFree && !br.containsHTML(ADSFREEAVAILABLE)) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SaveTv.AdsFreeNotAvailable", ADSFREEANOTVAILABLETEXT));
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
        link.setName(filename + ".avi");
        link.setAvailable(true);
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(".", "")));
        return AvailableStatus.TRUE;
    }

    public void prepBrowser(final Browser br) {
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
    }

    private String getTelecastId(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), "TelecastID=(\\d+)").getMatch(0);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEAPI, JDL.L("plugins.hoster.SaveTv.UseAPI", "Mobile API verwenden (BETA!)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.NORANDOMNUMBERS, JDL.L("plugins.hoster.SaveTv.DontModifyFilename", "Keine Zufallszahlen an Dateinamen anhängen (kann Probleme verursachen)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEORIGINALFILENAME, JDL.L("plugins.hoster.SaveTv.UseOriginalFilename", "Original Dateinamen verwenden (erst beim Download sichtbar)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERADSFREE, JDL.L("plugins.hoster.SaveTv.PreferAdFreeVideos", "Geschnittene Videos (Videos ohne Werbung) bevorzugen")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERH264MOBILE, JDL.L("plugins.hoster.SaveTv.PreferH264MobileVideos", PREFERH264MOBILETEXT)).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, getPluginConfig(), SaveTv.REGEX_SAVETV_ID,
        // "fjhfgjhfhgfhgjfghjfvh").setDefaultValue("BLAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

    }
}