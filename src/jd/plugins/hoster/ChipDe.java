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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.ChipDeConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chip.de" }, urls = { "https?://(?:www\\.)?(?:chip\\.de/downloads|download\\.chip\\.(?:eu|asia)/.{2})/[A-Za-z0-9_\\-]+_\\d+\\.html|https?://(?:[a-z0-9]+\\.)?chip\\.de/[^/]+/[^/]+_\\d+\\.html" })
public class ChipDe extends PluginForHost {
    public ChipDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.chip.de/s_specials/c1_static_special_index_13162756.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String type_chip_de_file         = "(?i)https?://(?:www\\.)?chip\\.de/downloads/[^/]+_\\d+\\.html";
    private static final String type_chip_eu_file         = "(?i)https?://(?:www\\.)?download\\.chip\\.(?:eu|asia)/.+";
    public static final String  type_chip_de_video        = "(?i)https?://(?:www\\.)?chip\\.de/video/[^/]+_(\\d+)\\.html";
    public static final String  type_chip_de_pictures     = "(?i)https?://(?:www\\.)?chip\\.de/bildergalerie/[^/]+_\\d+\\.html";
    private static final String type_chip_de_video_others = "(?i)https?://(?:[a-z0-9]+\\.)?chip\\.de/[^/]+/[^/]+_\\d+\\.html";
    private static final String host_chip_de              = "chip.de";
    private String              dllink                    = null;
    private static final String PROPERTY_DOWNLOAD_TARGET  = "download_target";

    /**
     * <b>Information for file (software)-downloads:</b> <br />
     * <b>Example URL:</b>
     * <a href="http://www.chip.de/downloads/Firefox-32-Bit_13014344.html">http://www.chip.de/downloads/Firefox-32-Bit_13014344.html</a>
     * <br />
     * <b>1.</b> Links are language dependant. Unfortunately we cannot just force a specified language as content is different for each
     * country. <br />
     * That means we have to make RegExes that can handle all languages. <br />
     * <b>2.</b> Via website "automatic download" users will get adware-infested installers - via "manual download" we will get the original
     * installers (usually without any adware). <br />
     *
     * <b>Information for video downloads:</b> <br />
     * <b>Example URL:</b>
     * <a href="http://www.chip.de/video/DSLR-fuer-die-Hosentasche-DxO-One-im-Test-Video_85225530.html">http://www.chip.de
     * /video/DSLR-fuer-die-Hosentasche-DxO-One-im-Test-Video_85225530.html</a> <br />
     * <b>Videoid or how they call it "containerIdBeitrag": 85225530</b><br />
     *
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        boolean set_final_filename = false;
        String date = null;
        String date_formatted = null;
        String filename = null;
        String md5 = null;
        String title_URL = null;
        String description = null;
        final String contentID_URL;
        final Regex linkinfo = new Regex(link.getPluginPatternMatcher(), "(?i)/([^/]+)_(\\d+)\\.html$");
        title_URL = linkinfo.getMatch(0);
        contentID_URL = linkinfo.getMatch(1);
        /* Set name here in case the content is offline --> Users still have a nice filename. */
        if (!link.isNameSet() && title_URL != null && contentID_URL != null) {
            link.setName(title_URL + "_" + contentID_URL);
        }
        if (link.getPluginPatternMatcher().matches(type_chip_de_file) || link.getPluginPatternMatcher().matches(type_chip_eu_file)) {
            set_final_filename = false;
            accessURL(this.br, link.getPluginPatternMatcher());
            if (link.getPluginPatternMatcher().matches(type_chip_eu_file) && !this.br.containsHTML("class=\"downloadnow_button")) {
                /* chip.eu url without download button --> No downloadable content --> URL is offline for us */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filesizeStr = null;
            final String json1 = br.getRegex("var digitalData = (\\{.*?\\});").getMatch(0);
            final String json2 = br.getRegex("var utag_data = (\\{.*?\\});").getMatch(0);
            String title = null;
            if (json1 != null) {
                /* 2021-07-22 */
                final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json1);
                final Object downloadO = entries.get("download");
                if (downloadO == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> download = (Map<String, Object>) downloadO;
                final String productID = (String) download.get("productID");
                if (productID != null) {
                    link.setLinkID(this.getHost() + "://application/" + productID);
                }
                title = (String) download.get("name");
                /* Sometimes filesize is not given --> "n/a" */
                final String filesizeTmp = (String) download.get("fileSize");
                if (filesizeTmp.matches("\\d+(\\.\\d+)")) {
                    filesizeStr = filesizeTmp + "MB";
                }
                link.setProperty(PROPERTY_DOWNLOAD_TARGET, download.get("Target").toString());
            } else if (json2 != null) {
                /* 2023-10-18 */
                final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json2);
                final String productID = (String) entries.get("downloadProductId");
                if (productID != null) {
                    link.setLinkID(this.getHost() + "://application/" + productID);
                }
                title = (String) entries.get("downloadName");
                /* Sometimes filesize is not given --> "n/a" */
                final String filesizeTmp = (String) entries.get("downloadFileSize");
                if (filesizeTmp.matches("\\d+(\\.\\d+)")) {
                    filesizeStr = filesizeTmp + "MB";
                }
            }
            /* Sometimes contains a comma and trash after that (wtf?) */
            final String filesizeBytesStr = br.getRegex("itemprop=\"fileSize\" content=\"(\\d+)(\\.\\d+)?\"").getMatch(0);
            if (StringUtils.isEmpty(title)) {
                title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (StringUtils.isEmpty(filesizeStr)) {
                filesizeStr = br.getRegex(">Dateigr\\&ouml;\\&szlig;e:</p>[\t\n\r ]+<p class=\"col2\">([^<>\"]*?)<meta itemprop=\"fileSize\"").getMatch(0);
            }
            if (filesizeStr == null) {
                /* For the international chip websites! */
                filesizeStr = br.getRegex("<dt>(?:File size:|Размер файла:|Dimensioni:|Dateigröße:|Velikost:|Fájlméret:|Bestandsgrootte:|Rozmiar pliku:|Mărime fişier:|Dosya boyu:|文件大小：)<br /></dt>[\t\n\r ]+<dd>(.*?)<br /></dd>").getMatch(0);
            }
            if (filesizeBytesStr != null) {
                link.setVerifiedFileSize(Long.parseLong(filesizeBytesStr));
            } else if (filesizeStr != null) {
                filesizeStr = filesizeStr.replace("GByte", "GB");
                link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            /* Checksum is usually only available for chip.eu downloads! */
            md5 = br.getRegex("<dt>(?:Контрольная сумма \\(MD 5\\):|Checksum:|Prüfsumme:|Kontrolní součet:|Szumma:|Suma kontrolna|Checksum|Kontrol toplamı:|校验码：)<br /></dt>[\t\n\r ]+<dd>(.*?)<br /></dd>").getMatch(0);
            date = this.br.getRegex("itemprop=\"datePublished\" datetime=\"(\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2})\"").getMatch(0);
            description = this.br.getRegex("description:\"([^<>\"]*?)\"").getMatch(0);
            /*
             * Include linkid in this case because otherwise links could be identified as duplicates / mirrors wrongly e.g.
             *
             * http://download.chip.eu/en/Firefox_115074.html
             *
             * http://download.chip.eu/en/Firefox_106534.html
             */
            if (title == null) {
                /* Last chance! */
                filename = title_URL + "_" + contentID_URL;
            } else {
                filename = title + "_" + contentID_URL;
            }
        } else {
            /* 2021-07-23: This seems to be broken */
            /* type_chip_de_video and type_chip_de_video_others */
            if (contentID_URL != null) {
                link.setLinkID(this.getHost() + "://video/" + contentID_URL);
            }
            set_final_filename = true;
            accessURL(this.br, link.getPluginPatternMatcher());
            filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\"").getMatch(0);
            date = this.br.getRegex("\"publishDateTime\":\"(\\d{4}\\-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+\\d{2}:\\d{2})\"").getMatch(0);
            /*
             * 2021-07-23: Their website obtains this URL via a 3rd party CDN/API but they also expose their direct-video-URLs like this
             * (maybe a legacy way?) so we'll use the easy way for now.
             */
            dllink = PluginJSonUtils.getJson(this.br, "contentUrl");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (StringUtils.isEmpty(filename)) {
                /* Last chance! */
                filename = title_URL + "_" + contentID_URL;
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename += ".mp4";
            this.br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        date_formatted = formatDate(date);
        if (date_formatted != null) {
            filename = date_formatted + "_chip_" + filename;
        } else {
            filename = "chip_" + filename;
        }
        if (!link.getDownloadURL().contains(host_chip_de + "/")) {
            /* Basically a warning for chip.eu downloads as there is no way around their adware installers! */
            filename = "THIS_FILE_CONTAINS_ADWARE_" + filename + ".exe";
        }
        filename = Encoding.htmlDecode(filename).trim();
        if (set_final_filename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        /*
         * Do not allow chip.eu hashes because at the moment we can only download their adware installers so even though they give us the
         * hash, we can never check it against the original file so it makes no sense to set it here.
         */
        if (md5 != null && link.getDownloadURL().contains(host_chip_de + "/")) {
            link.setMD5Hash(md5);
        }
        if (description != null) {
            description = Encoding.htmlDecode(description);
            link.setComment(description);
        }
        if (PluginJsonConfig.get(ChipDeConfig.class).isDisplayExternalDownloadsAsOffline() && this.isExternalDownload(link)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final String directlinkproperty = "directlink";
        if (link.getPluginPatternMatcher().matches(type_chip_de_video)) {
            requestFileInformation(link);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                handleServerErrors();
                br.followConnection(true);
                /* We use APIs which we can trust so retrying is okay ;) */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            }
        } else if (link.getDownloadURL().matches(type_chip_eu_file)) {
            /* Re-Use saved direct-downloadlinks */
            final boolean resume = false;
            final int maxchunks = 1;
            if (!attemptStoredDownloadurlDownload(link, directlinkproperty, resume, maxchunks)) {
                requestFileInformation(link);
                if (isExternalDownload(link)) {
                    errorExternalDownloadImpossible();
                }
                String getfileUrl = br.getRegex("\"(/.{2}/download_getfile_[^<>\"]*?)\"").getMatch(0);
                if (getfileUrl == null) {
                    getfileUrl = this.br.getRegex("href=\"([^<>\"]*?)\" rel=\"nofollow\" class=\"dwnld\"").getMatch(0);
                }
                if (getfileUrl == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (getfileUrl.startsWith("http") && !getfileUrl.contains("chip.eu/")) {
                    errorExternalDownloadImpossible();
                }
                this.br.getPage(getfileUrl);
                dllink = br.getRegex("If not, please click <a href=\"(http[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                handleServerErrors();
                if (!this.br.getHost().contains("chip")) {
                    errorExternalDownloadImpossible();
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
            dl.startDownload();
        } else {
            /* Normal chip.de downloads (application downloads) */
            final boolean resume = true;
            final int maxchunks = 1;
            /* Re-Use saved downloadlinks */
            if (!attemptStoredDownloadurlDownload(link, directlinkproperty, resume, maxchunks)) {
                requestFileInformation(link);
                if (isExternalDownload(link)) {
                    errorExternalDownloadImpossible();
                }
                final String step1 = br.getRegex("class=\"[^\"]*download_button[^\"]+\"[^>]*><a rel=\"nofollow\"[^>]*href=\"(https?://[^\"]+)").getMatch(0);
                if (step1 == null) {
                    /*
                     * 2021-07-22: Treat such files as non-downloadable although this could also mean there is a plugin failure. Some items
                     * are just "dummy" items which don't have any download options e.g.
                     * https://www.chip.de/downloads/WordPress-Android-App_54778552.html
                     */
                    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    throw new PluginException(LinkStatus.ERROR_FATAL, "No download button available");
                }
                final boolean useStep1Better = false;
                final String step1decoded = URLEncode.decodeURIComponent(step1);
                final String step1better = new Regex(step1decoded, "(https?://[^/]+/downloads/c1_downloads_auswahl_.*s=[^&]+)").getMatch(0);
                if (step1better != null && useStep1Better) {
                    br.getPage(step1better);
                } else {
                    br.getPage(step1decoded);
                }
                String step2 = br.getRegex("(/downloads/c1_downloads_hs_getfile[^<>\"]+)\"").getMatch(0);
                if (step2 != null) {
                    br.getPage(step2);
                }
                dllink = applicationDownloadsGetDllink();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
                String etag = this.br.getRequest().getResponseHeader("ETag");
                if (etag != null) {
                    /* chip.de servers will often | always return md5 hash via headers! */
                    try {
                        etag = etag.replace("\"", "");
                        final String[] etagInfo = etag.split(":");
                        final String md5 = etagInfo[0];
                        if (md5.matches("[A-Fa-f0-9]{32}")) {
                            link.setMD5Hash(md5);
                        }
                    } catch (final Throwable ignore) {
                    }
                }
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    handleServerErrors();
                    if (!this.br.getHost().contains("chip")) {
                        /*
                         * Happens for software whos manufactors do not allow direct mirrors from chip servers e.g.
                         * http://www.chip.de/downloads/Windows-10-64-Bit_72189999.html
                         */
                        throw new PluginException(LinkStatus.ERROR_FATAL, "External download - not possible via JDownloader!");
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.setFilenameFix(true);
                // link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            link.removeProperty(directlinkproperty);
            return false;
        }
    }

    @Deprecated
    public static void accesscontainerIdBeitrag(final Browser br, final String containerIdBeitrag) throws PluginException, IOException {
        accessURL(br, "https://apps-rest.chip.de/api/v2/containerIdBeitrag/" + containerIdBeitrag + "/");
    }

    private void errorExternalDownloadImpossible() throws PluginException {
        /**
         * Happens for software whose manufacturers do not allow direct mirrors from chip servers e.g.
         * http://www.chip.de/downloads/Windows-10-64-Bit_72189999.html </br>
         * https://www.chip.de/downloads/WordPress-Android-App_54778552.html
         */
        throw new PluginException(LinkStatus.ERROR_FATAL, "External download - not possible via JDownloader!");
    }

    /** Externally hosted content is not directly downloadable via chip.de servers thus cannot be downloaded via this plugin! */
    private boolean isExternalDownload(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_DOWNLOAD_TARGET)) {
            if (link.getStringProperty(PROPERTY_DOWNLOAD_TARGET).equalsIgnoreCase("intern")) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /* Handles general server errors. */
    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
        }
    }

    private String applicationDownloadsGetDllink() {
        String dllink = br.getRegex("Falls der Download nicht beginnt,\\&nbsp;<a class=\"b\" href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("class=\"dl\\-btn\"><a href=\"(http.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("</span></a></div><a href=\"(http.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("var adtech_dl_url = \\'(https?://[^<>\"]*?)\\';").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("(?:\"|\\')(https?://dl\\.cdn\\.chip\\.de/downloads/\\d+/.*?)(?:\"|\\')").getMatch(0);
        }
        return dllink;
    }

    public static void accessURL(final Browser br, final String url) throws PluginException, IOException {
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(url);
            final long responsecode = con.getResponseCode();
            if (responsecode == 404 || responsecode == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    public static final Browser prepBRAPI(final Browser br) {
        /* Needed! */
        br.setCustomCharset("UTF-8");
        /* Not necessarily needed! */
        br.getHeaders().put("Content-Type", "application/json; charset=utf-8");
        br.getHeaders().put("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 5.0; Nexus 5 Build/LRX21O)");
        return br;
    }

    public static String formatDate(String input) {
        if (input == null) {
            return null;
        }
        final long date;
        if (input.matches("\\d{4}\\-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
            /* Typically for videos via API */
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY);
        } else if (input.matches("\\d{4}\\-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+\\d{2}:\\d{2}")) {
            /* Typically for videos via website */
            /* First lets correct that date so we can easily parse it. */
            input = input.substring(0, input.lastIndexOf(":")) + "00";
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ssZ", Locale.GERMANY);
        } else if (input.matches("\\d{4}\\-\\d{2}\\-\\d{2}\\d{2}:\\d{2}:\\d{2}")) {
            /* E.g. software/file downloads */
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY);
        } else {
            /* Unsupported input date format */
            return null;
        }
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ChipDeConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}