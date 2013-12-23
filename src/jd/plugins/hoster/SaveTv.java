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

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
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

    /** Static information */
    private final String        INFOREGEX                                                   = "<h3>([^<>\"]*?)</h2>[\t\n\r ]+<p>([^<>]*?)</p>([\t\n\r ]+<p>([^<>]*?)</p>)?";
    private final String        GENERAL_REGEX                                               = "Kategorie:</label>[\t\n\r ]+Info.*?";
    private final String        SERIESINFORMATION                                           = "[A-Za-z]+ [A-Za-z]+ (\\d{4} / \\d{4}|\\d{4})";
    private final String        APIKEY                                                      = "Q0FFQjZDQ0YtMDdFNC00MDQ4LTkyMDQtOUU5QjMxOEU3OUIz";
    private final String        APIPAGE                                                     = "http://api.save.tv/v2/Api.svc?wsdl";
    private static Object       LOCK                                                        = new Object();
    private final String        COOKIE_HOST                                                 = "http://save.tv";

    /** Static information for improvised errorhandling */
    private static final String LASTFAILEDSTRING                                            = "lastfailed";
    private static final long   LASTFAILED_PLUGIN_DEFECT                                    = 1;
    private static final long   LASTFAILED_PLUGIN_TEMPORARILY_UNAVAILABLE_NOCUTAVAILABLE    = 2;
    private static final long   LASTFAILED_PLUGIN_TEMPORARILY_UNAVAILABLE_FORMATUNAVAILABLE = 3;

    /** Settings stuff */
    private static final String USEORIGINALFILENAME                                         = "USEORIGINALFILENAME";
    private static final String PREFERADSFREE                                               = "PREFERADSFREE";
    private static final String DOWNLOADONLYADSFREE                                         = "DOWNLOADONLYADSFREE";
    private final String        ADSFREEAVAILABLETEXT                                        = JDL.L("plugins.hoster.SaveTv.AdsFreeAvailable", "Video ist werbefrei verfügbar");
    private final String        ADSFREEANOTVAILABLE                                         = JDL.L("plugins.hoster.SaveTv.AdsFreeNotAvailable", "Video ist nicht werbefrei verfügbar");
    private static final String PREFERREDFORMATNOTAVAILABLETEXT                             = JDL.L("plugins.hoster.SaveTv.H264NotAvailable", "Das bevorzugte Format (H.264 Mobile) ist (noch) nicht verfügbar. Warte oder ändere die Einstellung!");
    private final String        NOCUTAVAILABLETEXT                                          = JDL.L("plugins.hoster.SaveTv.noCutAvailable", "Für diese Sendung steht keine Schnittliste zur Verfügung");
    private static final String PREFERH264MOBILE                                            = "PREFERH264MOBILE";
    private final String        PREFERH264MOBILETEXT                                        = "H.264 Mobile Videos bevorzugen (diese sind kleiner)?";
    private static final String USEAPI                                                      = "USEAPI";
    private static final String CRAWLER_ACTIVATE                                            = "CRAWLER_ACTIVATE";
    private static final String CRAWLER_ENABLE_FASTER                                       = "CRAWLER_ENABLE_FASTER";
    private static final String CRAWLER_DISABLE_DIALOGS                                     = "CRAWLER_DISABLE_DIALOGS";
    private static final String DISABLE_LINKCHECK                                           = "DISABLE_LINKCHECK";
    private static final String DELETE_TELECAST_ID_AFTER_DOWNLOAD                           = "DELETE_TELECAST_ID_AFTER_DOWNLOAD";

    /** Custom filename settings stuff */
    private static final String CUSTOM_DATE                                                 = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME2                                            = "CUSTOM_FILENAME2";
    private static final String CUSTOM_FILENAME_SERIES2                                     = "CUSTOM_FILENAME_SERIES2";
    private static final String CUSTOM_FILENAME_SERIES2_EPISODENAME_SEPERATION_MARK         = "CUSTOM_FILENAME_SERIES2_EPISODENAME_SEPERATION_MARK";
    private static final String CUSTOM_FILENAME_EMPTY_TAG_STRING                            = "CUSTOM_FILENAME_EMPTY_TAG_STRING";
    private static final String FORCE_ORIGINALFILENAME_SERIES                               = "FORCE_ORIGINALFILENAME_SERIES";
    private static final String FORCE_ORIGINALFILENAME_MOVIES                               = "FORCE_ORIGINALFILENAME_MOVIES";

    /** Variables */
    private boolean             FORCE_ORIGINAL_FILENAME                                     = false;
    private boolean             FORCE_LINKCHECK                                             = false;
    private boolean             ISADSFREEAVAILABLE                                          = false;
    // If this != null, API is in use
    private String              SESSIONID                                                   = null;
    private static final String NOCHUNKS                                                    = "NOCHUNKS";

    @SuppressWarnings("deprecation")
    public SaveTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.save.tv/stv/s/obj/registration/RegPage1.cfm");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://").replace("free.save.tv/", "save.tv/"));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /**
     * TODO: Known Bugs in API mode: API cannot differ between H.264 Mobile and normal videos -> Cannot show any error in case user chose H.264 but it's not
     * available. --> NO FATAL bugs ---> Plugin will work fine with them!
     */
    // TODO: Remove info-dialogs Feb, 2014 and add a single dialog which only shows up one time, first usage of the plugin

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        prepBrowser(br);
        br.setFollowRedirects(true);
        // Show id in case it is offline or plugin is broken
        if (link.getName() != null && (link.getName().contains(getTelecastId(link)) && !link.getName().endsWith(".mp4") || link.getName().contains("usShowVideoArchiveDetail.cfm"))) link.setName(getTelecastId(link) + ".mp4");
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Kann Links ohne gültigen Account nicht überprüfen");
            checkAccountNeededDialog();
            return AvailableStatus.UNCHECKABLE;
        }
        synchronized (LOCK) {
            checkFeatureDialog();
            checkFeatureDialog2();
            checkFeatureDialog3();
            checkFeatureDialog4();
        }
        if (this.getPluginConfig().getBooleanProperty(DISABLE_LINKCHECK, false) && !FORCE_LINKCHECK) {
            link.getLinkStatus().setStatusText("Linkcheck deaktiviert - korrekter Dateiname erscheint erst beim Downloadstart");
            return AvailableStatus.TRUE;
        }
        br.setFollowRedirects(true);
        login(this.br, aa, false);
        final boolean preferMobileVids = getPluginConfig().getBooleanProperty(PREFERH264MOBILE);
        String site_title = null;
        String filesize = null;
        long datemilliseconds = 0;
        String broadcastTime = null;
        String date = null;

        String episodenumber = null;
        String seriestitle = null;
        String episodename = null;

        String genre = null;
        String producecountry = null;
        String produceyear = null;
        if (SESSIONID != null || this.getPluginConfig().getBooleanProperty(USEAPI, false)) {
            if (SESSIONID == null) login(this.br, aa, true);
            // doSoapRequest("http://tempuri.org/ITelecast/GetTelecastDetail",
            // "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><GetTelecastDetail xmlns=\"http://tempuri.org/\"><sessionId>6f33f94f-13bb-4271-ab48-3339d2430d75</sessionId><telecastIds xmlns:a=\"http://schemas.microsoft.com/2003/10/Serialization/Arrays\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"/><detailLevel>1</detailLevel></GetTelecastDetail></s:Body></s:Envelope>");
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
            String apifilename = br.getRegex("<a:Filename>([^<>\"]*?)</a").getMatch(0);
            filesize = br.getRegex("<a:SizeMB>(\\d+)</a:SizeMB>").getMatch(0);
            if (apifilename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            final Regex fName = new Regex(apifilename, "((\\d{4}\\-\\d{2}\\-\\d{2})_\\d+_\\d+\\.mp4)");
            final String filenameReplace = fName.getMatch(0);
            if (filenameReplace != null) site_title = apifilename.replace(filenameReplace, "");

            date = fName.getMatch(1);
            // if (date != null) {
            // datemilliseconds = TimeFormatter.getMilliSeconds(date, "yyyy-MM-dd", Locale.ENGLISH);
            // final Regex originalDatePart = new Regex(apifilename, "(\\d{4}\\-\\d{2}\\-\\d{2}_(\\d{4}))_\\d+\\.mp4");
            // broadcastTime = originalDatePart.getMatch(1);
            // if (broadcastTime != null) {
            // // Add time to date
            // if (broadcastTime != null) {
            // DateFormat readFormat = new SimpleDateFormat("hhmm");
            // DateFormat writeFormat = new SimpleDateFormat("HH:mm");
            // Date date2 = null;
            // try {
            // date2 = readFormat.parse(broadcastTime);
            // } catch (ParseException e) {
            // e.printStackTrace();
            // }
            // String formattedDate = null;
            // if (date2 != null) {
            // formattedDate = writeFormat.format(date2);
            // }
            //
            // // Add missing time - Also add one hour because otherwise one is missing
            // datemilliseconds += TimeFormatter.getMilliSeconds(broadcastTime, "hhmm", Locale.ENGLISH) + (60 * 60 * 1000l);
            // }
            // }
            // String newDateString = null;
            // final String userDefinedDateFormat = "yyyy-MM-dd_HH_mm";
            // SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
            // Date theDate = new Date(datemilliseconds);
            // if (userDefinedDateFormat != null) {
            // try {
            // newDateString = formatter.format(theDate);
            // final String originalDateString = originalDatePart.getMatch(0);
            // apifilename = apifilename.replace(originalDateString, newDateString);
            // } catch (Exception e) {
            // // prevent user error killing plugin.
            // newDateString = "";
            // }
            // }
            // }
            link.setProperty("apiplainfilename", apifilename);
            filesize += " KB";
        } else {
            boolean isSeries = false;
            getPageSafe(link.getDownloadURL(), aa);
            if (br.containsHTML("(Leider ist ein Fehler aufgetreten|Bitte versuchen Sie es später noch einmal)") || !br.getURL().contains("TelecastID=")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            site_title = br.getRegex("<h2 id=\"archive-detailbox-title\">(.*?)</h2>").getMatch(0);
            if (site_title == null) site_title = br.getRegex("id=\"telecast-detail\">.*?<h3>(.*?)</h2>").getMatch(0);
            filesize = br.getRegex(">Download</a>[ \t\n\r]+\\(ca\\.[ ]+([0-9\\.]+ [A-Za-z]{1,5})\\)[ \t\n\r]+</p>").getMatch(0);
            if (preferMobileVids) filesize = br.getRegex("title=\"H\\.264 Mobile\"( )?/>[\t\n\r ]+</a>[\t\n\r ]+<p>[\t\n\r ]+<a class=\"archive\\-detail\\-link\" href=\"javascript:STV\\.Archive\\.Download\\.openWindow\\(\\d+, \\d+, \\d+, \\d+\\);\">Download</a>[\t\n\r ]+\\(ca\\.[ ]+(.*?)\\)").getMatch(1);
            if (site_title == null) {
                logger.warning("Save.tv: Availablecheck failed!");
                link.setProperty(LASTFAILEDSTRING, LASTFAILED_PLUGIN_DEFECT);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            site_title = correctSiteTitle(site_title);

            // Find custom filename stuff
            date = br.getRegex("<b>Datum:</b>[\t\n\r ]+[A-Za-z]{1,3}\\.,([^<>\"]*?)</p>").getMatch(0);
            if (date != null) {
                date = date.trim();
                date += +Calendar.getInstance().get(Calendar.YEAR);
                datemilliseconds = TimeFormatter.getMilliSeconds(date, "dd.MM.yyyy", Locale.GERMAN);
            }
            broadcastTime = br.getRegex("<b>Ausstrahlungszeitraum:</b>[\t\n\r ]+(\\d{2}:\\d{2}) \\-").getMatch(0);

            if (br.containsHTML(GENERAL_REGEX)) {
                // Find out if it's a series or if we should handle it like a movie
                final Regex seriesInfo = br.getRegex(INFOREGEX);
                String testtitle = seriesInfo.getMatch(0);
                if (testtitle != null) {
                    testtitle = testtitle.trim();
                    isSeries = isSeries(testtitle);
                }
            }

            if (br.containsHTML("Kategorie:</label>[\t\n\r ]+Serien.*?") || isSeries) {
                // For series
                link.setProperty("category", 2);
                episodenumber = br.getRegex("<strong>Folge:</strong> (\\d+)</p>").getMatch(0);
                final Regex seriesInfo = br.getRegex(INFOREGEX);
                seriestitle = seriesInfo.getMatch(0);
                episodename = seriesInfo.getMatch(1);
                if (episodename != null && episodename.contains("Originaltitel")) {
                    episodename = seriesInfo.getMatch(3);
                }
                if (seriestitle == null) seriestitle = site_title;
                if (episodename == null) episodename = this.getPluginConfig().getStringProperty(CUSTOM_FILENAME_EMPTY_TAG_STRING, defaultCustomStringForEmptyTags);
                seriestitle = Encoding.htmlDecode(seriestitle.trim());
                episodename = Encoding.htmlDecode(episodename.trim());
                if (episodename.matches(SERIESINFORMATION))
                    episodename = "-";
                else if (episodename.contains("Für diese Sendung stehen leider keine weiteren Informationen zur Verfügung")) episodename = "-";
                String seriesdata = seriesInfo.getMatch(3);
                if (seriesdata == null) seriesdata = seriesInfo.getMatch(2);
                if (seriesdata == null) seriesdata = seriesInfo.getMatch(1);
                if (seriesdata != null) {
                    seriesdata = seriesdata.trim();
                    final String[] dataArray = seriesdata.split(" ");
                    if (dataArray != null) {
                        genre = dataArray[0];
                        // Maybe the media was produced over multiple years
                        produceyear = new Regex(seriesdata, "(\\d{4} / \\d{4})").getMatch(0);
                        producecountry = getRegexSafe(seriesdata + genre, " ([\\p{L}/]+)", 0);
                        if (dataArray != null) {
                            if (dataArray.length >= 3) {
                                if (producecountry == null) producecountry = dataArray[1];
                                if (produceyear == null) produceyear = dataArray[2];
                            } else if (dataArray.length == 2) {
                                if (producecountry == null) producecountry = dataArray[1];
                            }
                        }

                    }
                }
            } else if (br.containsHTML("Kategorie:</label>[\t\n\r ]+Shows.*?") || br.containsHTML("src=\"/STV/IMG/global/TVCategorie/kat3\\.jpg\"")) {
                // For shows
                link.setProperty("category", 3);
                final Regex showInfo = br.getRegex(INFOREGEX);
                genre = showInfo.getMatch(3);
                if (genre == null) genre = showInfo.getMatch(1);
            } else if (br.containsHTML("Kategorie:</label>[\t\n\r ]+Musik.*?") || br.containsHTML("src=\"/STV/IMG/global/TVCategorie/kat7\\.jpg\"")) {
                // For music
                link.setProperty("category", 7);
                final Regex musicInfo = br.getRegex(INFOREGEX);
                final String music_title = musicInfo.getMatch(0);
                final String music_subtitle = musicInfo.getMatch(1);
                if (music_title != null && music_subtitle != null) {
                    site_title = Encoding.htmlDecode(music_title.trim()) + " - " + Encoding.htmlDecode(music_subtitle.trim());
                } else if (music_title != null) {
                    site_title = Encoding.htmlDecode(music_title.trim());
                }
                String musicdata = musicInfo.getMatch(2);
                if (musicdata != null) {
                    musicdata = musicdata.trim();
                    musicdata = musicdata.replace("<p>", "").replace("</p>", "");
                    genre = new Regex(musicdata, "([A-Za-z]+ / [A-Za-z, ]+)").getMatch(0);
                }
            } else if (br.containsHTML("src=\"/STV/IMG/global/TVCategorie/kat1\\.jpg\"") || br.containsHTML(GENERAL_REGEX)) {
                // For movies
                final Regex movieInfo = br.getRegex(INFOREGEX);
                String moviesdata = movieInfo.getMatch(1);
                if (moviesdata != null) {
                    moviesdata = moviesdata.trim();
                    final String[] dataArray = moviesdata.split(" ");
                    if (dataArray != null) {
                        genre = dataArray[0];
                        // Maybe the media was produced over multiple years
                        produceyear = new Regex(moviesdata, "(\\d{4} / \\d{4})").getMatch(0);
                        producecountry = new Regex(moviesdata + genre, " ([A-Za-z/]+)").getMatch(0);
                        if (dataArray != null) {
                            if (dataArray.length >= 3) {
                                if (producecountry == null) producecountry = dataArray[1];
                                if (produceyear == null) produceyear = dataArray[2];
                            } else if (dataArray.length == 2) {
                                if (producecountry == null) producecountry = dataArray[1];
                            }
                        }

                    }
                }
                link.setProperty("category", 1);
            } else {
                // For everything else
                link.setProperty("category", 0);
            }

            final Browser adsCheck = br.cloneBrowser();
            final DecimalFormat df = new DecimalFormat("0000");
            adsCheck.postPage("https://www.save.tv/STV/M/obj/cRecordOrder/croGetAdFreeAvailable.cfm?null.GetAdFreeAvailable", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetAdFreeAvailable&c0-id=" + df.format(new Random().nextInt(1000)) + "_" + System.currentTimeMillis() + "&c0-param0=number:" + getTelecastId(link) + "&xml=true&extend=function (object) {for (property in object) {this[property] = object[property];}return this;}&");
            if (adsCheck.containsHTML("= \\'3\\';")) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SaveTv.NoCutListAvailable", NOCUTAVAILABLETEXT));
            } else if (adsCheck.containsHTML("= \\'1\\';")) {
                link.getLinkStatus().setStatusText(ADSFREEAVAILABLETEXT);
                ISADSFREEAVAILABLE = true;
            } else {
                link.getLinkStatus().setStatusText(ADSFREEANOTVAILABLE);
                ISADSFREEAVAILABLE = false;
            }
            // Add time to date
            if (broadcastTime != null) {
                // Add missing time - Also add one hour because otherwise one is missing
                datemilliseconds += TimeFormatter.getMilliSeconds(broadcastTime, "HH:mm", Locale.GERMAN) + (60 * 60 * 1000l);
            }
        }
        link.setAvailable(true);
        if (filesize != null) {
            filesize = filesize.replace(".", "");
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(".", "")));
        }
        /** Set properties which are needed for filenames */
        // Add series information
        if (episodenumber != null) link.setProperty("episodenumber", Integer.parseInt(episodenumber));
        link.setProperty("seriestitle", seriestitle);
        if (episodename != null) {
            episodename = episodename.replace("/", getPluginConfig().getStringProperty(CUSTOM_FILENAME_SERIES2_EPISODENAME_SEPERATION_MARK, defaultCustomSeperationMark));
            link.setProperty("episodename", episodename);
        }
        // Add movie information
        if (produceyear != null) {
            produceyear = produceyear.replace("/", "-");
            link.setProperty("produceyear", produceyear);
        }
        if (genre == null) genre = "-";
        link.setProperty("genre", Encoding.htmlDecode(genre.trim()));
        if (producecountry == null) producecountry = "-";
        link.setProperty("producecountry", producecountry);
        // Add remaining information
        link.setProperty("plainfilename", site_title);
        link.setProperty("type", ".mp4");
        link.setProperty("originaldate", datemilliseconds);
        // No custom filename if not all required tags are given
        final boolean force_original_general = (datemilliseconds == 0 || getPluginConfig().getBooleanProperty(USEORIGINALFILENAME) || SESSIONID != null || getLongProperty(link, "category", 0l) == 0);
        boolean force_original_series = false;
        try {
            if (getLongProperty(link, "category", 0l) == 2 && seriestitle.matches(getPluginConfig().getStringProperty(FORCE_ORIGINALFILENAME_SERIES, ""))) force_original_series = true;
        } catch (final Throwable e) {
        }
        boolean force_original_movies = false;
        try {
            if (getLongProperty(link, "category", 0l) == 1 && site_title.matches(getPluginConfig().getStringProperty(FORCE_ORIGINALFILENAME_MOVIES, ""))) force_original_movies = true;
        } catch (final Throwable e) {
        }
        FORCE_ORIGINAL_FILENAME = (force_original_general || force_original_series || force_original_movies);
        if (FORCE_ORIGINAL_FILENAME) {
            final String originalfilename = getFakeOriginalFilename(link);
            link.setFinalFileName(null);
            link.setName(originalfilename);
        } else {
            final String formattedFilename = getFormattedFilename(link);
            link.setFinalFileName(formattedFilename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public String getAGBLink() {
        return "http://free.save.tv/STV/S/misc/miscShowTermsConditionsInMainFrame.cfm";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        // Improvised workaround for http://svn.jdownloader.org/issues/10306
        final int premiumError = (int) getLongProperty(downloadLink, LASTFAILEDSTRING, -1l);
        if (premiumError > 0) {
            switch (premiumError) {
            case 1:
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 2:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, NOCUTAVAILABLETEXT, 12 * 60 * 60 * 1000l);
            case 3:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, PREFERREDFORMATNOTAVAILABLETEXT, 4 * 60 * 60 * 1000l);
            default:
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

        }
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered/premium users");
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig("save.tv");
        FORCE_LINKCHECK = true;
        requestFileInformation(downloadLink);
        login(this.br, account, false);
        String dllink = null;
        // User wants ads-free but it's not available -> Wait 12 hours, status can still change but probably won't
        if (this.getPluginConfig().getBooleanProperty(DOWNLOADONLYADSFREE, false) && !this.ISADSFREEAVAILABLE) {
            downloadLink.setProperty(LASTFAILEDSTRING, LASTFAILED_PLUGIN_TEMPORARILY_UNAVAILABLE_NOCUTAVAILABLE);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, NOCUTAVAILABLETEXT, 12 * 60 * 60 * 1000l);
        }
        final boolean preferAdsFree = cfg.getBooleanProperty(PREFERADSFREE);
        final boolean preferMobileVids = cfg.getBooleanProperty(PREFERH264MOBILE);
        String downloadWithoutAds = "false";
        if (preferAdsFree) downloadWithoutAds = "true";
        String preferMobileVideos = null;
        if (SESSIONID != null) {
            preferMobileVideos = "5";
            if (preferMobileVids) preferMobileVideos = "4";
            postDownloadPageAPI(downloadLink, preferMobileVideos, downloadWithoutAds);
            dllink = br.getRegex("<a:DownloadUrl>(http://[^<>\"]*?)</a").getMatch(0);
        } else {
            preferMobileVideos = "c0-param1=number:0";
            if (preferMobileVids) preferMobileVideos = "c0-param1=number:1";
            postDownloadPage(downloadLink, preferMobileVideos, downloadWithoutAds);
            if (br.containsHTML("Die Aufnahme liegt nicht im gewünschten Format vor")) {
                downloadLink.setProperty(LASTFAILEDSTRING, LASTFAILED_PLUGIN_TEMPORARILY_UNAVAILABLE_FORMATUNAVAILABLE);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, PREFERREDFORMATNOTAVAILABLETEXT, 4 * 60 * 60 * 1000l);
            }
            // Ads-Free version not available - handle it
            if (br.containsHTML("\\'Leider enthält Ihre Aufnahme nur Werbung\\.\\'") && preferAdsFree) {
                this.ISADSFREEAVAILABLE = false;
                if (cfg.getBooleanProperty(DOWNLOADONLYADSFREE, false) && !this.ISADSFREEAVAILABLE) {
                    downloadLink.setProperty(LASTFAILEDSTRING, LASTFAILED_PLUGIN_TEMPORARILY_UNAVAILABLE_NOCUTAVAILABLE);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, NOCUTAVAILABLETEXT, 12 * 60 * 60 * 1000l);
                }
                postDownloadPage(downloadLink, preferMobileVideos, "false");
            }
            dllink = br.getRegex("\\'OK\\',\\'(http://[^<>\"\\']+)\\'").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\\'(http://[^<>\"\\']+/\\?m=dl)\\'").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            downloadLink.setProperty(LASTFAILEDSTRING, LASTFAILED_PLUGIN_DEFECT);
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
            logger.warning("Save.tv: Received HTML code instead of the file!");
            br.followConnection();
            downloadLink.setProperty(LASTFAILEDSTRING, LASTFAILED_PLUGIN_DEFECT);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (FORCE_ORIGINAL_FILENAME) {
            downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        } else {
            downloadLink.setFinalFileName(getFormattedFilename(downloadLink));
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) { return; }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getBooleanProperty(SaveTv.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(SaveTv.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                if (cfg.getBooleanProperty(DELETE_TELECAST_ID_AFTER_DOWNLOAD, false)) {
                    try {
                        br.postPage("https://www.save.tv/STV/M/obj/user/usShowVideoArchive.cfm?", "iFilterType=1&sText=&iTextSearchType=1&ChannelID=0&TVCategoryID=0&TVSubCategoryID=0&iRecordingState=1&iPageNumber=1&sSortOrder=&lTelecastID=" + getTelecastId(downloadLink));
                        logger.info("Successfully deleted telecastID:  " + getTelecastId(downloadLink));
                    } catch (final Throwable e) {
                        logger.info("Failed to delete telecastID: " + getTelecastId(downloadLink));
                    }
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(SaveTv.NOCHUNKS, false) == false) {
                downloadLink.setProperty(SaveTv.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    /**
     * @param dl
     *            DownloadLink
     * @param preferMobileVideos
     *            : Mobile Videos bevurzugen oder nicht
     * @param downloadWithoutAds
     *            : Videos mit angewandter Schnittliste bevorzugen oder nicht
     */
    private void postDownloadPage(final DownloadLink dl, final String preferMobileVideos, final String downloadWithoutAds) throws IOException {
        br.postPage("https://www.save.tv/STV/M/obj/cRecordOrder/croGetDownloadUrl.cfm?null.GetDownloadUrl", "ajax=true&clientAuthenticationKey=&callCount=1&c0-scriptName=null&c0-methodName=GetDownloadUrl&c0-id=&c0-param0=number:" + this.getTelecastId(dl) + "&" + preferMobileVideos + "&c0-param2=boolean:" + downloadWithoutAds + "&xml=true&");
    }

    /**
     * @param dl
     *            DownloadLink
     * @param preferMobileVideos
     *            : Mobile Videos bevurzugen oder nicht
     * @param downloadWithoutAds
     *            : Videos mit angewandter Schnittliste bevorzugen oder nicht
     */
    private void postDownloadPageAPI(final DownloadLink dl, final String preferMobileVideos, final String downloadWithoutAds) throws IOException {
        br.getHeaders().put("SOAPAction", "http://tempuri.org/IDownload/GetStreamingUrl");
        br.getHeaders().put("Content-Type", "text/xml");
        br.postPage(APIPAGE, "<?xml version=\"1.0\" encoding=\"utf-8\"?><v:Envelope xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:d=\"http://www.w3.org/2001/XMLSchema\" xmlns:c=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:v=\"http://schemas.xmlsoap.org/soap/envelope/\"><v:Header /><v:Body><GetStreamingUrl xmlns=\"http://tempuri.org/\" id=\"o0\" c:root=\"1\"><sessionId i:type=\"d:string\">" + SESSIONID + "</sessionId><telecastId i:type=\"d:int\">" + this.getTelecastId(dl) + "</telecastId><telecastIdSpecified i:type=\"d:boolean\">true</telecastIdSpecified><recordingFormatId i:type=\"d:int\">" + preferMobileVideos + "</recordingFormatId><recordingFormatIdSpecified i:type=\"d:boolean\">true</recordingFormatIdSpecified><adFree i:type=\"d:boolean\">false</adFree><adFreeSpecified i:type=\"d:boolean\">" + downloadWithoutAds
                + "</adFreeSpecified></GetStreamingUrl></v:Body></v:Envelope>");
    }

    /**
     * @param soapAction
     *            : The soap link which should be accessed
     * @param soapPost
     *            : The soap post data
     */
    private void doSoapRequest(final String soapAction, final String soapPost) throws IOException {
        System.out.println(soapPost);
        br.getHeaders().put("SOAPAction", soapAction);
        br.getHeaders().put("Content-Type", "text/xml");
        br.postPageRaw("http://api.save.tv/v2/Api.svc", soapAction);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (final PluginException e) {
            logger.info("save.tv Account ist ungültig!");
            account.setValid(false);
            throw e;
        } catch (final BrowserException eb) {
            for (int i = 1; i <= 3; i++) {
                try {
                    login(this.br, account, true);
                } catch (final BrowserException ebr) {
                    logger.info(i + "von 3: save.tv Login wegen Serverfehler (Timeout oder Serverfehler) fehlgeschlagen");
                    continue;
                }
                break;
            }
        }
        ai.setStatus("Premium save.tv User");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        final boolean useAPI = getPluginConfig().getBooleanProperty(USEAPI);
        if (useAPI) {
            SESSIONID = account.getStringProperty("sessionid", null);
            final long lastUse = getLongProperty(account, "lastuse", -1l);
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
            if (!br.containsHTML("<a:HasPremiumStatus>true</a:HasPremiumStatus>")) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
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
                                br.setCookie(COOKIE_HOST, key, value);
                            }
                            return;
                        }
                    }
                    final String postData = "sUsername=" + Encoding.urlEncode(account.getUser()) + "&sPassword=" + Encoding.urlEncode(account.getPass()) + "&bAutoLoginActivate=1";
                    br.postPage("https://www.save.tv/STV/M/Index.cfm?sk=PREMIUM", postData);
                    if (!br.containsHTML("/STV/M/obj/user/usEdit.cfm") || br.containsHTML("Bitte verifizieren Sie Ihre Logindaten")) {
                        final String lang = System.getProperty("user.language");
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    /** Save cookies */
                    final HashMap<String, String> cookies = new HashMap<String, String>();
                    final Cookies add = br.getCookies(COOKIE_HOST);
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

    // Sync this with the decrypter
    private void getPageSafe(final String url, final Account acc) throws Exception {
        // Limits made by me:
        // Max 6 logins possible
        // Max 3 accesses of the link possible
        // -> Max 9 total requests
        for (int i = 0; i <= 2; i++) {
            br.getPage(url);
            if (br.getURL().contains("Token=MSG_LOGOUT_B")) {
                for (int i2 = 0; i2 <= 1; i2++) {
                    logger.info("Link redirected to login page, logging in again to retry this: " + url);
                    logger.info("Try " + i2 + " of 1");
                    try {
                        login(this.br, acc, true);
                    } catch (final BrowserException e) {
                        logger.info("Login " + i2 + "of 1 failed, re-trying...");
                        continue;
                    }
                    logger.info("Re-Login " + i2 + "of 1 successful...");
                    break;
                }
                continue;
            }
            break;
        }
    }

    private void prepBrowser(final Browser br) {
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
    }

    private boolean isSeries(final String testTitle) {
        final String otherEpisodes = br.getRegex("Weitere Sendungen aus dieser Unterkategorie[\t\n\r ]+</h3>(.*?)</ul>").getMatch(0);
        if (otherEpisodes != null) {
            if (otherEpisodes.contains(testTitle)) return true;
        }
        return false;
    }

    private String correctSiteTitle(final String input) {
        String output = input.replace("_", " ");
        output = output.trim();
        output = output.replaceAll("(\r|\n)", "");
        final String[] unneededSpaces = new Regex(output, ".*?([ ]{2,}).*?").getColumn(0);
        if (unneededSpaces != null && unneededSpaces.length != 0) {
            for (String unneededSpace : unneededSpaces) {
                output = output.replace(unneededSpace, " ");
            }
        }
        return output;
    }

    private String getRegexSafe(final String input, final String regex, final int match) {
        final String regexFixedInput = input.replace("(", "65788jdclipopenjd4684").replace(")", "65788jdclipclosejd4684");
        String result = new Regex(regexFixedInput, regex).getMatch(match);
        if (result != null) result = result.replace("65788jdclipopenjd4684", "(").replace("65788jdclipclosejd4684", ")");
        return result;
    }

    private String getTelecastId(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), "TelecastID=(\\d+)").getMatch(0);
    }

    private String getRandomNumber() {
        final DecimalFormat df = new DecimalFormat("0000");
        return df.format(new Random().nextInt(10000));
    }

    @SuppressWarnings("deprecation")
    private String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("save.tv");
        String ext = downloadLink.getStringProperty("type", null);
        if (ext == null) ext = ".mp4";
        final String genre = downloadLink.getStringProperty("genre", null);
        final String producecountry = downloadLink.getStringProperty("producecountry", null);
        final String produceyear = downloadLink.getStringProperty("produceyear", null);
        final String randomnumber = getRandomNumber();
        final String telecastid = getTelecastId(downloadLink);

        final long date = getLongProperty(downloadLink, "originaldate", 0l);
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

        final String customStringForEmptyTags = this.getPluginConfig().getStringProperty(CUSTOM_FILENAME_EMPTY_TAG_STRING, defaultCustomStringForEmptyTags);
        String formattedFilename = null;
        if (belongsToCategoryMovie(downloadLink)) {
            final String title = downloadLink.getStringProperty("plainfilename", null);
            // For all other links
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME2, defaultCustomFilename);
            if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
            formattedFilename = formattedFilename.toLowerCase();
            if (!formattedFilename.contains("*endung*") || (!formattedFilename.contains("*videotitel*") && !formattedFilename.contains("*zufallszahl*") && !formattedFilename.contains("*telecastid*"))) formattedFilename = defaultCustomFilename;

            formattedFilename = formattedFilename.replace("*zufallszahl*", randomnumber);
            formattedFilename = formattedFilename.replace("*telecastid*", telecastid);
            formattedFilename = formattedFilename.replace("*datum*", formattedDate);
            if (produceyear != null) {
                formattedFilename = formattedFilename.replace("*produktionsjahr*", produceyear);
            } else {
                formattedFilename = formattedFilename.replace("*produktionsjahr*", customStringForEmptyTags);
            }
            formattedFilename = formattedFilename.replace("*endung*", ext);
            if (genre != null) {
                formattedFilename = formattedFilename.replace("*genre*", genre);
            } else {
                formattedFilename = formattedFilename.replace("*genre*", customStringForEmptyTags);
            }
            formattedFilename = formattedFilename.replace("*produktionsland*", producecountry);
            // Insert actual filename at the end to prevent errors with tags
            formattedFilename = formattedFilename.replace("*videotitel*", title);
        } else {
            // For series
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_SERIES2, defaultCustomSeriesFilename);
            if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
            formattedFilename = formattedFilename.toLowerCase();
            if (!formattedFilename.contains("*endung*") || (!formattedFilename.contains("*serientitel*") && !formattedFilename.contains("*episodenname*") && !formattedFilename.contains("*episodennummer*") && !formattedFilename.contains("*zufallszahl*") && !formattedFilename.contains("*telecastid*"))) formattedFilename = defaultCustomFilename;

            final String seriestitle = downloadLink.getStringProperty("seriestitle", null);
            final String episodename = downloadLink.getStringProperty("episodename", null);
            final String episodenumber = getEpisodeNumber(downloadLink);

            formattedFilename = formattedFilename.replace("*zufallszahl*", randomnumber);
            formattedFilename = formattedFilename.replace("*telecastid*", telecastid);
            formattedFilename = formattedFilename.replace("*datum*", formattedDate);
            if (produceyear != null) {
                formattedFilename = formattedFilename.replace("*produktionsjahr*", produceyear);
            } else {
                formattedFilename = formattedFilename.replace("*produktionsjahr*", customStringForEmptyTags);
            }
            formattedFilename = formattedFilename.replace("*episodennummer*", episodenumber);
            formattedFilename = formattedFilename.replace("*endung*", ext);
            if (genre != null) {
                formattedFilename = formattedFilename.replace("*genre*", genre);
            } else {
                formattedFilename = formattedFilename.replace("*genre*", customStringForEmptyTags);
            }
            if (producecountry != null) {
                formattedFilename = formattedFilename.replace("*produktionsland*", producecountry);
            } else {
                formattedFilename = formattedFilename.replace("*produktionsland*", customStringForEmptyTags);
            }
            // Insert actual filename at the end to prevent errors with tags
            formattedFilename = formattedFilename.replace("*serientitel*", seriestitle);
            formattedFilename = formattedFilename.replace("*episodenname*", episodename);
        }
        formattedFilename = encodeUnicode(formattedFilename);

        return formattedFilename;
    }

    public long getLongProperty(Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String getFakeOriginalFilename(final DownloadLink downloadLink) throws ParseException {
        String ext = downloadLink.getStringProperty("type", null);
        if (ext == null) ext = ".mp4";

        final long date = getLongProperty(downloadLink, "originaldate", 0l);
        String formattedDate = null;
        final String userDefinedDateFormat = "yyyy-MM-dd";
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

        final DecimalFormat df = new DecimalFormat("0000");
        final int random1 = new Random().nextInt(1000);
        final DecimalFormat df2 = new DecimalFormat("000000");
        final int random2 = new Random().nextInt(100000);
        String formattedFilename = downloadLink.getStringProperty("apiplainfilename", null);
        if (formattedFilename != null) {
        } else {
            if (belongsToCategoryMovie(downloadLink) || getLongProperty(downloadLink, "category", 0l) == 0l) {
                final String title = downloadLink.getStringProperty("plainfilename", null);
                formattedFilename = title.replace(" ", "_") + "_";
                formattedFilename += formattedDate + "_";
                formattedFilename += df.format(random1) + "_" + df2.format(random2);
            } else {
                final String seriestitle = downloadLink.getStringProperty("seriestitle", null);
                final String episodename = downloadLink.getStringProperty("episodename", null);
                final String episodenumber = getEpisodeNumber(downloadLink);
                // For series
                formattedFilename = seriestitle.replace(" ", "_") + "_";
                formattedFilename += episodename.replace(" ", "_") + "_";
                formattedFilename += "Folge" + episodenumber + "_" + formattedDate + "_";
                formattedFilename += df.format(random1) + "_" + df2.format(random2);
            }
            formattedFilename += ".mp4";
            formattedFilename = encodeUnicode(formattedFilename);
        }

        return formattedFilename;
    }

    private String getEpisodeNumber(final DownloadLink dl) {
        final long episodenumber = getLongProperty(dl, "episodenumber", 0l);
        if (episodenumber == 0) {
            return "-";
        } else {
            return Long.toString(episodenumber);
        }
    }

    private boolean belongsToCategoryMovie(final DownloadLink dl) {
        long cat = getLongProperty(dl, "category", 0l);
        final boolean belongsToCategoryMovie = (cat == 1 || cat == 3 || cat == 7);
        return belongsToCategoryMovie;
    }

    private String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public String getDescription() {
        return "JDownloader's Save.tv Plugin helps downloading videoclips from Save.tv. Save.tv provides different settings for its downloads.";
    }

    private final static String defaultCustomFilename           = "*videotitel**telecastid**endung*";
    private final static String defaultCustomSeriesFilename     = "*serientitel* ¦ *episodennummer* ¦ *episodenname**endung*";
    private final static String defaultCustomSeperationMark     = "+";
    private final static String defaultCustomStringForEmptyTags = "-";

    private void setConfigElements() {
        final ConfigEntry useMobileAPI = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEAPI, JDL.L("plugins.hoster.SaveTv.UseAPI", "API verwenden?\r\nWICHTIG: Aktiviert man die API, werden folgende Einstellungen ignoriert:\r\n-Benutzerdefinierte Dateinamen\r\n-Archiv-Crawler\r\n-Nur Aufnahmen mit angewandter Schnittliste laden\r\nAus technischen Gründen ist es (noch) nicht möglich, alle genannten Einstellungen beim aktivierter API auszugrauen um dem Benutzer visuelles Feedback zu geben, sorry!")).setDefaultValue(false);
        getConfig().addEntry(useMobileAPI);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DISABLE_LINKCHECK, JDL.L("plugins.hoster.SaveTv.DisableLinkcheck", "Linkcheck deaktivieren?\r\nVorteile:\r\n-Links landen schneller im Linkgrabber und können auch bei Serverproblemen oder wenn die save.tv Seite komplett offline ist gesammelt werden\r\nNachteile:\r\n-Im Linkgrabber werden zunächst nur die telecast-IDs als Dateinamen angezeigt\r\n-Korrekte Dateinamen werden erst beim Downloadstart angezeigt")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry grabArchives = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_ACTIVATE, JDL.L("plugins.hoster.SaveTv.grabArchive", "Archiv-Crawler aktivieren:\r\nKomplettes Archiv beim Hinzufügen folgender Adresse im Linkgrabber zeigen:\r\n'save.tv/STV/M/obj/user/usShowVideoArchive.cfm'?")).setDefaultValue(false).setEnabledCondidtion(useMobileAPI, false);
        getConfig().addEntry(grabArchives);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_ENABLE_FASTER, JDL.L("plugins.hoster.SaveTv.grabArchiveFaster", "Aktiviere schnellen Linkcheck für Archiv-Crawler?\r\nVorteil: Über den Archiv-Crawler hinzugefügte Links landen schneller im Linkgrabber\r\nNachteil: Korrekte Dateinamen werden erst beim Downloadstart angezeigt")).setDefaultValue(false).setEnabledCondidtion(grabArchives, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.CRAWLER_DISABLE_DIALOGS, JDL.L("plugins.hoster.SaveTv.crawlerDisableDialogs", "Info Dialoge des Archiv-Crawlers (Nach dem Crawlen oder im Fehlerfall) deaktivieren?")).setDefaultValue(false).setEnabledCondidtion(grabArchives, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry preferAdsFree = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERADSFREE, JDL.L("plugins.hoster.SaveTv.PreferAdFreeVideos", "Aufnahmen mit angewandter Schnittliste bevorzugen?")).setDefaultValue(true);
        getConfig().addEntry(preferAdsFree);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DOWNLOADONLYADSFREE, JDL.L("plugins.hoster.SaveTv.downloadOnlyAdsFree", "Nur Aufnahmen mit angewandter Schnittliste laden (zur Nutzung muss die Option\r\n'Aufnahmen mit angewandter Schnittliste bevorzugen' aktiviert sein)?")).setDefaultValue(false).setEnabledCondidtion(preferAdsFree, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.PREFERH264MOBILE, JDL.L("plugins.hoster.SaveTv.PreferH264MobileVideos", PREFERH264MOBILETEXT)).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Dateiname Einstellungen:"));
        final ConfigEntry origName = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.USEORIGINALFILENAME, JDL.L("plugins.hoster.SaveTv.UseOriginalFilename", "Original (Server) Dateinamen verwenden? [erst beim Downloadstart sichtbar]")).setDefaultValue(false).setEnabledCondidtion(useMobileAPI, false);
        getConfig().addEntry(origName);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.savetv.customdate", "Setze das Datumsformat:\r\nWichtige Information dazu:\r\nDas Datum erscheint im angegebenen Format im Dateinamen, allerdings nur,\r\nwenn man das *datum* Tag auch verwendet (siehe Benutzerdefinierte Dateinamen für Filme und Serien unten)")).setDefaultValue("dd.MM.yyyy").setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final StringBuilder sbinfo = new StringBuilder();
        sbinfo.append("Erklärung zur Nutzung eigener Dateinamen:\r\n");
        sbinfo.append("Eigene Dateinamen lassen sich unten über ein Tag-System (siehe weiter unten) nutzen.\r\n");
        sbinfo.append("Das bedeutet, dass man die Struktur seiner gewünschten Dateinamen definieren kann.\r\n");
        sbinfo.append("Dabei hat man Tags wie z.B. *telecastid*, die dann durch Daten ersetzt werden.\r\n");
        sbinfo.append("Wichtig dabei ist, dass Tags immer mit einem Stern starten und enden.\r\n");
        sbinfo.append("Man darf nichts zwischen ein Tag schreiben z.B. *-telecastid-*, da das\r\n");
        sbinfo.append("zu unschönen Dateinamen führt und das Tag nicht die Daten ersetzt werden kann.\r\n");
        sbinfo.append("Wenn man die Tags trennen will muss man die anderen Zeichen zwischen Tags\r\n");
        sbinfo.append("z.B. '-*telecastid*-*endung*' -> Der Dateiname würde dann in etwa so aussehen: '-7573789-.mp4' (ohne die '')\r\n");
        sbinfo.append("WICHTIG: Tags, zu denen die Daten fehlen , werden standardmäßig durch '-' (Bindestrich) ersetzt!\r\n");
        sbinfo.append("Fehlen z.B. die Daten zu *genre*, steht statt statt dem Genre dann ein Bindestrich ('-') an dieser Stelle im Dateinamen.");
        sbinfo.append("Gut zu wissen: Statt dem Bindestrich lässt sich hierfür unten auch ein anderes Zeichen bzw. Zeichenfolge definieren.\r\n");
        sbinfo.append("Gut zu wissen: Bedenke, dass es für Filme und Serien unterschiedliche Tags gibt.\r\n");
        sbinfo.append("Kaputtmachen kannst du mit den Einstellungen prinzipiell nichts also probiere es aus ;)\r\n");
        sbinfo.append("Tipp: Die Einstellungen lassen sich rechts oben wieder auf ihre Standardwerte zurücksetzen!\r\n");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbinfo.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME2, JDL.L("plugins.hoster.savetv.customfilenamemovies", "Eigener Dateiname für Filme/Shows:")).setDefaultValue(defaultCustomFilename).setEnabledCondidtion(origName, false));
        final StringBuilder sb = new StringBuilder();
        sb.append("Erklärung der verfügbaren Tags:\r\n");
        sb.append("*datum* = Datum der Ausstrahlung der aufgenommenen Sendung\r\n[Erscheint im oben definierten Format, wird von der save.tv Seite ausgelesen]\r\n");
        sb.append("*genre* = Das Genre\r\n");
        sb.append("*produktionsland* = Name des Produktionslandes\r\n");
        sb.append("*produktionsjahr* = Produktionsjahr\r\n");
        sb.append("*videotitel* = Name des Videos ohne Dateiendung\r\n");
        sb.append("*zufallszahl* = Eine vierstellige Zufallszahl\r\n[Nützlich um Dateinamenkollisionen zu vermeiden]\r\n");
        sb.append("*telecastid* = Die id, die in jedem save.tv Link steht: TelecastID=XXXXXXX\r\n[Nützlich um Dateinamenkollisionen zu vermeiden]\r\n");
        sb.append("*endung* = Die Dateiendung, in diesem Fall '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_SERIES2_EPISODENAME_SEPERATION_MARK, JDL.L("plugins.hoster.savetv.customseriesfilenameSeperationmark", "Trennzeichen für Episodennamen\r\nNötig, da save.tv hier oft '/' nutzt (ungültig in Dateinamen):")).setDefaultValue(defaultCustomSeperationMark).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_SERIES2, JDL.L("plugins.hoster.savetv.customseriesfilename", "Eigener Dateiname für Serien:")).setDefaultValue(defaultCustomSeriesFilename).setEnabledCondidtion(origName, false));
        final StringBuilder sbseries = new StringBuilder();
        sbseries.append("Erklärung der verfügbaren Tags:\r\n");
        sbseries.append("*datum* = Datum der Ausstrahlung der aufgenommenen Sendung\r\n[Erscheint im oben definierten Format, wird von der save.tv Seite ausgelesen]\r\n");
        sbseries.append("*genre* = Das Genre\r\n");
        sbseries.append("*produktionsland* = Name des Produktionslandes\r\n");
        sbseries.append("*produktionsjahr* = Produktionsjahr\r\n");
        sbseries.append("*serientitel* = Name der Serie\r\n");
        sbseries.append("*episodenname* = Name der Episode\r\n");
        sbseries.append("*episodennummer* = Episodennummer\r\n");
        sbseries.append("*zufallszahl* = Eine vierstellige Zufallszahl - nützlich um Dateinamenkollisionen zu vermeiden\r\n");
        sbseries.append("*telecastid* = Die id, die in jedem save.tv Link steht: TelecastID=XXXXXXX\r\n[Nützlich um Dateinamenkollisionen zu vermeiden]\r\n");
        sbseries.append("*endung* = Die Dateiendung, in diesem Fall '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbseries.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Erweiterte Einstellungen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SaveTv.DELETE_TELECAST_ID_AFTER_DOWNLOAD, JDL.L("plugins.hoster.SaveTv.deleteFromArchiveAfterDownload", "Erfolgreich geladene telecastIDs aus dem save.tv Archiv löschen?\r\n Warnung: Gelöschte telecast-IDs können nicht wiederhergestellt werden!\r\nFalls diese Funktion einen Fehler beinhaltet, ist Datenverlust möglich!\r\nWICHTIG: Bei aktivierter API kann man diese Einstellung nicht verwenden!")).setDefaultValue(false).setEnabledCondidtion(useMobileAPI, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_EMPTY_TAG_STRING, JDL.L("plugins.hoster.savetv.customEmptyTagsString", "Zeichen, mit dem Tags ersetzt werden sollen, deren Daten fehlen:")).setDefaultValue(defaultCustomStringForEmptyTags).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final StringBuilder sbmore = new StringBuilder();
        sbmore.append("Definiere Filme oder Serien, für die trotz obiger Einstellungen Originaldateinamen die\r\n");
        sbmore.append("genommen werden sollen.\r\n");
        sbmore.append("Manche mehrteiligen Filme haben dieselben Titel und bei manchen Serien fehlen die Episodennamen\r\n");
        sbmore.append("wodurch sie alle dieselben Dateinamen bekommen -> JDownloader denkt es seien Duplikate und lädt nur\r\n");
        sbmore.append("einen der scheinbar gleichen Dateien.\r\n");
        sbmore.append("Um dies zu verhindern,m kann man in den Eingabefeldern  die Namen der Filme/Serien eintragen,\r\n");
        sbmore.append("für die trotz obiger Einstellungen der Original Dateiname verwendet werden soll.\r\n");
        sbmore.append("Beispiel: 'serienname 1|serienname 2|usw.' (ohne die '')\r\n");
        sbmore.append("Auf die Eingaben wird ein RegEx angewandt. Wer nicht weiß was das ist -> Google");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbmore.toString()).setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), FORCE_ORIGINALFILENAME_SERIES, JDL.L("plugins.hoster.savetv.forceoriginalnameforspecifiedseries", "Original Dateinamen für folgende Serien erzwingen [Eingabe erfolgt in RegEx]:")).setDefaultValue("").setEnabledCondidtion(origName, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), FORCE_ORIGINALFILENAME_MOVIES, JDL.L("plugins.hoster.savetv.forcefilenameforspecifiedmovies", "Original Dateinamen für folgende Filme erzwingen [Eingabe erfolgt in RegEx]:")).setDefaultValue("").setEnabledCondidtion(origName, false));
    }

    private static String getMessageEnd() {
        String message = "";
        message += "\r\n\r\n";
        message += "Falls du Fehler findest oder Fragen hast, melde dich gerne jederzeit bei uns: board.jdownloader.org.\r\n";
        message += "\r\n";
        message += "Dieses Fenster wird nur einmal angezeigt.\r\nAlle wichtigen Informationen stehen auch in den save.tv Plugin Einstellungen.\r\n";
        message += "\r\n";
        message += "- Das JDownloader Team wünscht weiterhin viel Spaß mit JDownloader und save.tv! -";
        return message;
    }

    private void checkAccountNeededDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("accNeededShown", Boolean.FALSE) == false) {
                if (config.getProperty("accNeededShown2") == null) {
                    showAccNeededDialog();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("accNeededShown", Boolean.TRUE);
                config.setProperty("accNeededShown2", "shown");
                config.save();
            }
        }
    }

    private static void showAccNeededDialog() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Save.tv - Account benötigt";
                        message += "Hallo lieber save.tv Nutzer.\r\n";
                        message += "Um über JDownloader Videos aus deinem save.tv Archiv herunterladen zu können musst du\r\nzunächst deinen save.tv Account in JDownloader eintragen.";
                        message += "\r\n";
                        message += "In JDownloader 0.9.581 geht das unter:\r\n";
                        message += "Einstellungen -> Anbieter -> -> Premium -> Account hinzufügen save.tv\r\n";
                        message += "\r\n";
                        message += "In der JDownloader 2 BETA geht das unter:\r\n";
                        message += "Einstellungen -> Accountverwaltung -> Hinzufügen -> save.tv\r\n";
                        message += "\r\n";
                        message += "Sobald du deinen Account eingetragen hast kannst du aus deinem save.tv Archiv\r\n";
                        message += "Links dieses Formats in JDownloader einfügen und herunterladen:\r\n";
                        message += "save.tv/STV/M/obj/user/usShowVideoArchiveDetail.cfm?TelecastID=XXXXXXX";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkFeatureDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialogShown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialogShown2") == null) {
                    showFeatureDialog();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialogShown", Boolean.TRUE);
                config.setProperty("featuredialogShown2", "shown");
                config.save();
            }
        }
    }

    private static final short totalFeatureDialogNum = 4;

    private static void showFeatureDialog() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Save.tv - neue Features 1/" + totalFeatureDialogNum;
                        message += "Hallo lieber save.tv Nutzer.\r\n";
                        message += "Ab sofort gibt es folgende neue Features für das save.tv Plugin:\r\n";
                        message += "- Die Dateinamen lassen sich individualisieren \r\n";
                        message += "- Hinzufügen des kompletten Videoarchives über einen Klick\r\n  [Muss in den Einstellungen erst aktiviert werden!]\r\n";
                        message += "- Neue Option 'Aufnahmen mit angewandter Schnittliste bevorzugen'\r\n";
                        message += "--> Um diese Option nutzen zu können, muss die Option über dieser aktiviert sein.\r\n";
                        message += "--> Ist sie aktiviert, wird JDownloader nur noch Videos laden, auf die die Schnittliste angewandt wurde.\r\n";
                        message += "--> Alle anderen bekommen einen Warte-Status und werden nur geladen, falls die Schnittliste\r\n    nach der Wartezeit angewandt wurde.\r\n";
                        message += "\r\n";
                        message += "In JDownloader 0.9.581 findest du die Plugin Einstellungen unter:\r\n";
                        message += "Einstellungen -> Anbieter -> save.tv -> Doppelklick oder anklicken und links unten auf 'Einstellungen'\r\n";
                        message += "Diese sind aufgrund eines Fehlers abgeschnitten -> Ggf. auf die JDownloader 2 BETA ausweichen\r\n";
                        message += "\r\n";
                        message += "In der JDownloader 2 BETA findest du sie unter Einstellungen -> Plugin Einstellungen -> save.tv\r\n";
                        message += "\r\n";
                        message += "Bedenke bitte, das gewisse Einstellungen dafür sorgen, dass die eigenen\r\nDateinamen erst zum Downloadstart und nicht direkt im Linkgrabber angezeigt werden.\r\n";
                        message += "Dies steht im Normalfall bei den entsprechenden Einstellungen dabei und ist kein Bug.\r\n";
                        message += "Sollte es dennoch vorkommen, dass eigene Dateinamen bei bestimmten Links nicht funktionieren,\r\nkönnte es sich um einen Bug handeln.";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkFeatureDialog2() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog2Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog2Shown2") == null) {
                    showFeatureDialog2();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog2Shown", Boolean.TRUE);
                config.setProperty("featuredialog2Shown2", "shown");
                config.save();
            }
        }
    }

    private static void showFeatureDialog2() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Save.tv - neue Features 2/" + totalFeatureDialogNum;
                        message += "Hallo lieber save.tv Nutzer.\r\n";
                        message += "Ab sofort gibt es folgende neue Features für das save.tv Plugin:\r\n";
                        message += "- JDownloader sollte ab sofort das komplette Save.tv Archiv (bei aktivierter Einstellung) korrekt erkennen\r\n";
                        message += "- Die Plugin Einstellung 'Linkcheck deaktivieren':\r\n";
                        message += "--> Ist diese aktiviert, werden im Linkgrabber zunächst keine korrekten Dateinamen angezeigt.\r\n";
                        message += "--> Dafür kannst du Links schneller hinzufügen bzw. auch, wenn die save.tv Seite sehr langsam oder sogar nicht erreichbar ist.\r\n";
                        message += "--> Sobald du den Download startest, werden die korrekten Dateinamen angezeigt.\r\n";
                        message += "--> Fügt man das komplette Save.tv Archiv per JDownloader hinzu, werden trotz aktivierter Einstellung\r\n";
                        message += "    schönere Dateinamen angezeigt, die sich beim Downloadstart jedoch auch zu den gewünschten Dateinamen ändern.\r\n";
                        message += "\r\n";
                        message += "In JDownloader 0.9.581 findest du die Plugin Einstellungen unter:\r\n";
                        message += "Einstellungen -> Anbieter -> save.tv -> Doppelklick oder anklicken und links unten auf 'Einstellungen'\r\n";
                        message += "\r\n";
                        message += "In der JDownloader 2 BETA findest du sie unter Einstellungen -> Plugin Einstellungen -> save.tv\r\n";
                        message += "\r\n";
                        message += "Bedenke bitte, das gewisse Einstellungen dafür sorgen, dass die eigenen\r\nDateinamen erst zum Downloadstart und nicht direkt im Linkgrabber angezeigt werden.\r\n";
                        message += "Dies steht im Normalfall bei den entsprechenden Einstellungen dabei und ist kein Bug.\r\n";
                        message += "Sollte es dennoch vorkommen, dass eigene Dateinamen bei bestimmten Links nicht funktionieren,\r\nkönnte es sich um einen Bug handeln.";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkFeatureDialog3() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog3Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog3Shown2") == null) {
                    showFeatureDialog3();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog3Shown", Boolean.TRUE);
                config.setProperty("featuredialog3Shown2", "shown");
                config.save();
            }
        }
    }

    private static void showFeatureDialog3() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Save.tv - neue Features 3/" + totalFeatureDialogNum;
                        message += "Hallo lieber save.tv Nutzer.\r\n";
                        message += "Ab sofort gibt es folgende neue Features für das save.tv Plugin:\r\n";
                        message += "- Über die Plugin Einstellungen lassen sich Filme/Serien bestimmen, bei denen die Original Dateinamen\r\n";
                        message += "  unabhängig von den anderen Einstellungen erzwungen werden.\r\n";
                        message += "- Sind originale Dateinamen aktiviert, sehen die vorläufigen Dateinamen im Linkgrabber den finalen\r\n";
                        message += "  Dateinamen nun sehr ähnlich - wie zuvor auch ändern sich diese erst beim Downloadstart zu den Originalnamen.";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    private void checkFeatureDialog4() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("featuredialog4Shown", Boolean.FALSE) == false) {
                if (config.getProperty("featuredialog4Shown2") == null) {
                    showFeatureDialog4();
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog4Shown", Boolean.TRUE);
                config.setProperty("featuredialog4Shown2", "shown");
                config.save();
            }
        }
    }

    private static void showFeatureDialog4() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String message = "";
                        String title = null;
                        title = "Save.tv - neue Features 4/" + totalFeatureDialogNum;
                        message += "Hallo lieber save.tv Nutzer.\r\n";
                        message += "Ab sofort gibt es folgende neue Features für das save.tv Plugin:\r\n";
                        message += "- Über die Plugin Einstellungen kann man telecast-IDs nun nach erfolgreichem Download löschen lassen\r\n";
                        message += "- --> Entsprechende Warnhinweise dazu stehen nochmals in den save.tv Plugin Einstellungen\r\n";
                        message += "- Neue Einstellungsmöglichkeit: 'Zeichen, mit dem Tags ersetzt werden sollen, deren Daten fehlen'\r\n";
                        message += "- --> Bisher wurden Tags bei fehlenden Daten mit einem Bindestrich ('-') ersetzt.\r\n";
                        message += "- --> Dies lässt sich nun nach Belieben anpassen.\r\n";
                        message += "- Der Archiv Crawler funktioniert ab sofort vollständig und findet alle Links des Archivs.\r\n";
                        message += "- --> Nach dem Vorgang meldet er sich über ein Info-Dialog und zeigt an, wie viele Links gefunden wurden.\r\n";
                        message += "- ----> Diese Info-Dialoge lassen sich über 'Info Dialoge des Crawlers (Nach dem Crawlen oder im Fehlerfall) deaktivieren?' abschalten!";
                        message += getMessageEnd();
                        JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }
}