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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fernsehkritik.tv", "massengeschmack.tv" }, urls = { "http://(couch\\.)?fernsehkritik\\.tv/(jdownloaderfolge(neu|alt)?\\d+|inline\\-video/postecke\\.php\\?(iframe=true\\&width=\\d+\\&height=\\d+\\&ep=|ep=)\\d+|dl/fernsehkritik\\d+\\.[a-z0-9]{1,4}|folge-\\d+.*|userbereich/archive#stream:\\d+)", "https?://massengeschmack\\.tv/play/\\d+/[a-z0-9\\-]+" }, flags = { 2, 2 })
public class FernsehkritikTv extends PluginForHost {

    // Refactored on the 02.07.2011, Rev. 14521,
    // http://svn.jdownloader.org/projects/jd/repository/revisions/14521
    public FernsehkritikTv(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://couch.fernsehkritik.tv/register.php");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://fernsehkritik.tv/datenschutzbestimmungen/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    private static final String TYPE_POSTECKE_GENERAL        = "http://(fernsehkritik\\.tv/inline\\-video/postecke\\.php\\?(iframe=true\\&width=\\d+\\&height=\\d+\\&ep=|ep=)|massengeschmack\\.tv/play/\\d+/postecke)\\d+";
    private static final String TYPE_POSTECKE_OLD            = "http://fernsehkritik\\.tv/inline\\-video/postecke\\.php\\?(iframe=true\\&width=\\d+\\&height=\\d+\\&ep=|ep=)\\d+";
    private static final String TYPE_POSTECKE_NEW            = "http://massengeschmack\\.tv/play/\\d+/postecke\\d+";
    private static final String TYPE_FOLGE_NEW               = "http://fernsehkritik\\.tv/jdownloaderfolgeneu\\d+";
    private static final String TYPE_FOLGE_OLD               = "http://fernsehkritik\\.tv/jdownloaderfolgealt\\d+";
    private static final String TYPE_COUCH                   = "http://couch\\.fernsehkritik\\.tv.*";
    private static final String TYPE_COUCHSTREAM             = "http://couch\\.fernsehkritik\\.tv/userbereich/archive#stream:.*";
    private static final String TYPE_MASSENGESCHMACK_GENERAL = "http://massengeschmack\\.tv/play/\\d+/[a-z0-9\\-]+";

    private static final String HOST_COUCH                   = "http://couch.fernsehkritik.tv";
    private static final String HOST_MASSENGESCHMACK         = "http://couch.fernsehkritik.tv";
    private static Object       LOCK                         = new Object();
    private static final String LOGIN_ERROR                  = "Login fehlerhaft";
    private String              DLLINK                       = null;
    private static final String DL_AS_MOV                    = "DL_AS_MOV";
    private static final String DL_AS_MP4                    = "DL_AS_MP4";
    private static final String DL_AS_FLV                    = "DL_AS_FLV";
    private static final String GRAB_POSTECKE                = "GRAB_POSTECKE";
    private static final String CUSTOM_DATE                  = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME_FKTV         = "CUSTOM_FILENAME_FKTV";
    private static final String CUSTOM_FILENAME_FKTVPOST     = "CUSTOM_FILENAME_FKTVPOST";
    private static final String CUSTOM_PACKAGENAME           = "CUSTOM_PACKAGENAME";
    private static final String FASTLINKCHECK                = "FASTLINKCHECK";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink, AccountController.getInstance().getValidAccount(this));
    }

    public AvailableStatus requestFileInformation(final DownloadLink downloadLink, Account account) throws Exception {
        DLLINK = null;
        String final_filename = null;
        if (downloadLink.getDownloadURL().matches(TYPE_POSTECKE_GENERAL)) {
            final String episodenumber = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
            if (downloadLink.getDownloadURL().matches(TYPE_POSTECKE_OLD)) {
                br.getPage("http://fernsehkritik.tv/folge-" + episodenumber + "/Start/");
                String date = br.getRegex("var flattr_tle = \\'Fernsehkritik\\-TV Folge \\d+ vom(.*?)\\'").getMatch(0);
                if (date == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                date = Encoding.htmlDecode(date.trim());
                String posteckeepisode = br.getRegex(">Zuschauerreaktionen: Postecke (\\d+)</a>").getMatch(0);
                // Only use episode number of fktv episode if postecke episodenumber is not available
                if (posteckeepisode == null) posteckeepisode = episodenumber;
                downloadLink.setProperty("directposteckeepisode", posteckeepisode);
                downloadLink.setProperty("directdate", date);
                downloadLink.setProperty("directepisodenumber", episodenumber);
                downloadLink.setProperty("directtype", ".flv");

                br.getPage(downloadLink.getDownloadURL());
                DLLINK = br.getRegex("playlist = \\[ \\{ url: \\'(http://[^<>\"]*?)\\'").getMatch(0);
                if (DLLINK == null) DLLINK = br.getRegex("\\'(http://dl\\d+\\.fernsehkritik\\.tv/postecke/postecke\\d+\\.flv)\\'").getMatch(0);
                if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                br.getPage(downloadLink.getDownloadURL());
                if (br.containsHTML(">Clip nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                getMassengeschmackDLLINK();
            }
            final_filename = getFKTVPostFormattedFilename(downloadLink);
        } else if (downloadLink.getDownloadURL().matches(TYPE_COUCHSTREAM)) {
            final String episodenumber = new Regex(downloadLink.getDownloadURL(), "archive#stream:(.*?)$").getMatch(0);
            br.getPage("http://fernsehkritik.tv/folge-" + episodenumber + "/Start/");
            final String date = br.getRegex("var flattr_tle = \\'Fernsehkritik\\-TV Folge \\d+ vom(.*?)\\'").getMatch(0);
            if (account != null) {
                try {
                    login(account, false);
                } catch (final PluginException e) {
                    return AvailableStatus.UNCHECKABLE;
                }
            } else {
                return AvailableStatus.UNCHECKABLE;
            }
            br.getPage(downloadLink.getDownloadURL());
            final Regex reg = br.getRegex("id:\"(\\d+)\", hash:\"([a-z0-9]+)\", stamp:\"([a-z0-9]+)\"");
            final String id = reg.getMatch(0);
            final String hash = reg.getMatch(1);
            final String stamp = reg.getMatch(2);
            br.getPage("http://couch.fernsehkritik.tv/dl/getData2.php?mode=stream&ep=" + episodenumber + "&id=" + id + "&hash=" + hash + "&stamp=" + stamp + "&j=0");
            DLLINK = "http://couch.fernsehkritik.tv" + br.getRegex("\'file\': \"(/dl/\\d+-[a-z0-9]+-[a-z0-9]+-\\d+\\.flv)\"").getMatch(0);

            if (date == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final_filename = "Fernsehkritik-TV Folge " + episodenumber + " vom " + date + ".flv";
        } else if (downloadLink.getDownloadURL().matches(TYPE_COUCH)) {
            final String episodenumber = new Regex(downloadLink.getDownloadURL(), "fernsehkritik(\\d+)\\..*?$").getMatch(0);
            br.getPage("http://fernsehkritik.tv/folge-" + episodenumber + "/Start/");
            final String date = br.getRegex("var flattr_tle = \\'Fernsehkritik\\-TV Folge \\d+ vom(.*?)\\'").getMatch(0);
            if (date == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (account != null) {
                try {
                    login(account, false);
                } catch (final PluginException e) {
                    return AvailableStatus.UNCHECKABLE;
                }
            } else {
                return AvailableStatus.UNCHECKABLE;
            }

            br.setFollowRedirects(true);
            String extension = new Regex(downloadLink.getDownloadURL(), "fernsehkritik(\\d+)\\.(.*?)$").getMatch(1);
            if (extension == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final_filename = "Fernsehkritik-TV Folge " + episodenumber + " vom " + date + "." + extension;
        } else if (downloadLink.getDownloadURL().matches(TYPE_FOLGE_NEW)) {
            br.getPage(downloadLink.getStringProperty("originallink", null));
            DLLINK = br.getRedirectLocation();
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(true);
            final_filename = getFKTVFormattedFilename(downloadLink);
        } else if (downloadLink.getDownloadURL().matches(TYPE_FOLGE_OLD)) {
            br.getPage(downloadLink.getStringProperty("originallink", null));
            DLLINK = br.getRedirectLocation();
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(true);
            final_filename = getFKTVFormattedFilename(downloadLink);
        } else if (downloadLink.getDownloadURL().matches(TYPE_MASSENGESCHMACK_GENERAL)) {
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML(">Clip nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            /* Sometimes different qualities are available - prefer webm */
            DLLINK = br.getRegex("type=\"video/webm\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) getMassengeschmackDLLINK();
            final String title = br.getRegex("<li><a href=\"/u/\\d+\">([^<>\"]*?)</a> <span class=\"divider\"").getMatch(0);
            final String episode = br.getRegex("<li class=\"active\">([^<>\"]*?)</li>").getMatch(0);
            if (title == null || episode == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final_filename = Encoding.htmlDecode(title).trim() + " - " + Encoding.htmlDecode(episode).trim() + DLLINK.substring(DLLINK.lastIndexOf("."));
        } else {
            downloadLink.getLinkStatus().setStatusText("Unknown linkformat");
            return AvailableStatus.UNCHECKABLE;
        }

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setFinalFileName(final_filename);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }

        return AvailableStatus.TRUE;
    }

    private void getMassengeschmackDLLINK() throws PluginException {
        final String base = br.getRegex("var base = \\'(http://[^<>\"]*?)\\';").getMatch(0);
        final String link = br.getRegex("playlist = \\[\\{url: base \\+ \\'([^<>\"]*?)\\'").getMatch(0);
        if (base == null || link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = base + link;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (!(account.getUser().matches(".+@.+"))) {
            ai.setStatus("Please enter your E-Mail adress as username!");
            account.setValid(false);
            return ai;
        }
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        String expire = br.getRegex("gültig bis zum:.*?<strong>(.*?)</strong>").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy hh:mm", Locale.UK));
        }
        return ai;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        AvailableStatus availStatus = requestFileInformation(downloadLink);
        if (AvailableStatus.UNCHECKABLE.equals(availStatus)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only");
            }
        }
        br.setFollowRedirects(false);
        // More chunks work but download will stop at random point then
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        AvailableStatus ret = requestFileInformation(link, account);
        if (AvailableStatus.UNCHECKABLE.equals(ret)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only");
            }
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
        // if (link.getDownloadURL().matches("http://fernsehkritik\\.tv/folge-.*")) {
        // /* TODO */
        // String folge = new Regex(link.getDownloadURL(), "http://fernsehkritik\\.tv/folge-(.*?)").getMatch(0);
        // link.setUrlDownload("http://couch.fernsehkritik.tv/fernsehkritik" + folge + ".mp4");
        // br.setFollowRedirects(true);
        // dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 1);
        // }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.getHeaders().put("Accept-Language", "de-de,de;q=0.8");
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(HOST_COUCH, key, value);
                        }
                        return;
                    }
                }
                br.getHeaders().put("Accept-Encoding", "gzip");
                br.setFollowRedirects(true);
                br.getPage(HOST_COUCH);
                br.postPage(HOST_COUCH + "/login.php", "location=&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&setCookie=set");
                if (br.containsHTML(LOGIN_ERROR)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                if (br.getCookie(HOST_COUCH, "couchlogin") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(HOST_COUCH);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    public String getFKTVFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("fernsehkritik.tv");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_FKTV, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
        if (!formattedFilename.contains("*episodenumber*") || !formattedFilename.contains("*ext*")) formattedFilename = defaultCustomFilename;

        final String ext = downloadLink.getStringProperty("directtype", null);
        final String date = downloadLink.getStringProperty("directdate", null);
        final String episodenumber = downloadLink.getStringProperty("directepisodenumber", null);
        final String partnumber = downloadLink.getStringProperty("directpartnumber", null);

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            SimpleDateFormat formatter = new SimpleDateFormat(inputDateformat, new Locale("de", "DE"));
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
        if (formattedFilename.contains("*episodenumber*") && episodenumber != null) {
            formattedFilename = formattedFilename.replace("*episodenumber*", episodenumber);
        }
        if (formattedFilename.contains("*partnumber*") && partnumber != null) {
            formattedFilename = formattedFilename.replace("*partnumber*", partnumber);
        } else if (partnumber == null && formattedFilename.contains("*partnumber*")) {
            formattedFilename = formattedFilename.replace("_Teil*partnumber*", "");
        }
        if (formattedFilename.contains("*date*") && formattedDate != null) {
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);

        return formattedFilename;
    }

    public String getFKTVPostFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("fernsehkritik.tv");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_FKTVPOST, defaultCustomFilename_fktvpost);
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename_fktvpost;
        if (!formattedFilename.contains("*episodenumber*") || !formattedFilename.contains("*ext*")) formattedFilename = defaultCustomFilename_fktvpost;

        final String ext = downloadLink.getStringProperty("directtype", null);
        final String date = downloadLink.getStringProperty("directdate", null);
        final String episodenumber = downloadLink.getStringProperty("directepisodenumber", null);
        final String posteckeepisodenumber = downloadLink.getStringProperty("directposteckeepisode", null);

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            SimpleDateFormat formatter = new SimpleDateFormat(inputDateformat, new Locale("de", "DE"));
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
        if (formattedFilename.contains("*episodenumber*") && episodenumber != null) {
            formattedFilename = formattedFilename.replace("*episodenumber*", episodenumber);
        }
        if (formattedFilename.contains("*posteckeepisodenumber*") && posteckeepisodenumber != null) {
            formattedFilename = formattedFilename.replace("*posteckeepisodenumber*", posteckeepisodenumber);
        }
        if (formattedFilename.contains("*date*") && formattedDate != null) {
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);

        return formattedFilename;
    }

    @Override
    public String getDescription() {
        return "JDownloader's Fernsehkritik Plugin helps downloading videoclips from fernsehkritik.tv. Fernsehkritik provides different video formats and other settings to chose from.";
    }

    private final static String defaultCustomFilename          = "Fernsehkritik-TV Folge *episodenumber* vom *date*_Teil*partnumber**ext*";
    private final static String defaultCustomFilename_fktvpost = "Fernsehkritik-TV Postecke *posteckeepisodenumber* zur Episode *episodenumber* vom *date**ext*";
    private final static String defaultCustomPackagename       = "Fernsehkritik.tv Folge *episodenumber* vom *date*";
    private final static String defaultCustomDate              = "dd MMMMM yyyy";
    private static final String inputDateformat                = "dd. MMMMM yyyy";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for downloads in general:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_POSTECKE, JDL.L("plugins.hoster.fernsehkritik.grabpostecke", "Grab Postecke if available?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, JDL.L("plugins.hoster.fernsehkritik.fastLinkcheck", "Fast linkcheck (filesize won't be shown in linkgrabber)?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Settings for downloads via account:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DL_AS_MOV, JDL.L("plugins.hoster.fernsehkritik.mov", "Load Free Streams as Premium .mov")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DL_AS_MP4, JDL.L("plugins.hoster.fernsehkritik.mp4", "Load Free Streams as Premium .mp4")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DL_AS_FLV, JDL.L("plugins.hoster.fernsehkritik.flv", "Load Free Streams as Premium .flv")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename/packagename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.fernsehkritiktv.customdate", "Define how the date should look.")).setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename for fktv episodes!\r\nExample: 'Fernsehkritik-TV Folge *episodenumber* vom *date*_Teil*partnumber**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_FKTV, JDL.L("plugins.hoster.fernsehkritiktv.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*episodenumber* = number of the fktv episode\r\n");
        sb.append("*partnumber* = number of the part - only used if a video consists of multiple parts\r\n");
        sb.append("*date* = date when the link was posted - appears in the user-defined format above\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.flv'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename for fktv Postecke links!\r\nExample: 'Fernsehkritik-TV Postecke *posteckeepisodenumber* zur Episode *episodenumber* vom *date**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_FKTVPOST, JDL.L("plugins.hoster.fernsehkritiktv.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename_fktvpost));
        final StringBuilder sbpost = new StringBuilder();
        sbpost.append("Explanation of the available tags:\r\n");
        sbpost.append("*episodenumber* = number of the fktv episode\r\n");
        sbpost.append("*posteckeepisodenumber* = number of the Postecke episode\r\n");
        sbpost.append("*date* = date when the link was posted - appears in the user-defined format above\r\n");
        sbpost.append("*ext* = the extension of the file, in this case usually '.flv'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbpost.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the packagename!\r\nExample: 'Fernsehkritik.tv Folge *episodenumber* vom *date*':"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME, JDL.L("plugins.hoster.fernsehkritiktv.custompackagename", "Define how the packagenames should look:")).setDefaultValue(defaultCustomPackagename));
        final StringBuilder sbpack = new StringBuilder();
        sbpack.append("Explanation of the available tags:\r\n");
        sbpack.append("*episodenumber* = number of the fktv episode\r\n");
        sbpack.append("*date* = date when the linklist was created - appears in the user-defined format above\r\n");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbpack.toString()));
    }
}