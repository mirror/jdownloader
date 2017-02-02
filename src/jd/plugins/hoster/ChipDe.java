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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

    private static final String           type_chip_de_file         = "https?://(?:www\\.)?chip\\.de/downloads/[^/]+_\\d+\\.html";
    private static final String           type_chip_eu_file         = "https?://(?:www\\.)?download\\.chip\\.(?:eu|asia)/.+";
    public static final String            type_chip_de_video        = "https?://(?:www\\.)?chip\\.de/video/[^/]+_\\d+\\.html";
    public static final String            type_chip_de_pictures     = "https?://(?:www\\.)?chip\\.de/bildergalerie/[^/]+_\\d+\\.html";
    private static final String           type_chip_de_video_others = "https?://(?:[a-z0-9]+\\.)?chip\\.de/[^/]+/[^/]+_\\d+\\.html";

    private static final boolean          video_use_API             = true;

    /* Tags: kaltura player, medianac, api.medianac.com */
    /* Static values of their kaltura player configuration */
    private static final String           kaltura_partner_id        = "1741931";
    private static final String           kaltura_uiconf_id         = "30910812";
    private static final String           kaltura_sp                = "174193100";
    private static final String           host_chip_de              = "chip.de";

    private String                        DLLINK                    = null;
    private LinkedHashMap<String, Object> entries                   = null;

    /**
     * <b>Information for file (software)-downloads:</b> <br />
     * <b>Example URL:</b> <a
     * href="http://www.chip.de/downloads/Firefox-32-Bit_13014344.html">http://www.chip.de/downloads/Firefox-32-Bit_13014344.html</a> <br />
     * <b>1.</b> Links are language dependant. Unfortunately we cannot just force a specified language as content is different for each
     * country. <br />
     * That means we have to make RegExes that can handle all languages. <br />
     * <b>2.</b> Via website "automatic download" users will get adware-infested installers - via "manual download" we will get the original
     * installers (usually without any adware). <br />
     *
     * <b>Information for video downloads:</b> <br />
     * <b>Example URL:</b> <a
     * href="http://www.chip.de/video/DSLR-fuer-die-Hosentasche-DxO-One-im-Test-Video_85225530.html">http://www.chip.de
     * /video/DSLR-fuer-die-Hosentasche-DxO-One-im-Test-Video_85225530.html</a> <br />
     * <b>Videoid or how they call it "containerIdBeitrag": 85225530</b><br />
     * <b>1.</b> They use an external CDN for their videos called "kaltura video platform": <a
     * href="http://corp.kaltura.com/">kaltura.com</a> <br />
     * <b>2.</b> Information about their API: <br />
     * -https is usually possible via valid certificate even though it is not (always) possible via browser!<br />
     * -V1: <a href="http://apps-rest.chip.de/api/v1/?format=json">http://apps-rest.chip.de/api/v1/?format=json</a> --> Used by their
     * official (Android) App <br />
     * --> It is not possible for us to use this as the API uses different IDs which are neither in the URL our users add nor in their HTML
     * code!<br />
     * -V2: <a href="http://apps-rest.chip.de/api/v2/?format=json">http://apps-rest.chip.de/api/v2/?format=json</a> --> Used by us <br />
     * -V3-5 or higher: Seems like they re still under development - whatever, nothing we need!<br />
     * -Via V2 we can actually access videos via API: https://apps-rest.chip.de/api/v2/containerIdBeitrag/85225530/<br />
     * --> That will actually also contain an URL to the V1 API in the field "resource_uri": /api/v1/video/<b>136736</b>/<br />
     * ---> As you can see we are not able to (directly) use the V1 API because we do not have this ID: <b>136736</b><br />
     *
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        DLLINK = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("ISO-8859-1");
        br.setAllowedResponseCodes(410);

        boolean set_final_filename = false;
        String date = null;
        String date_formatted = null;
        String filename = null;
        String md5 = null;
        String filename_url = null;
        String description = null;
        long filesize = 0;
        final String linkid;
        final Regex linkinfo = new Regex(link.getDownloadURL(), "/([^/]+)_(\\d+)\\.html$");
        filename_url = linkinfo.getMatch(0);
        linkid = linkinfo.getMatch(1);
        /* Set name here in case the content is offline --> Users still have a nice filename. */
        link.setName(filename_url + "_" + linkid);
        /* Set linkid right away to avoid/identify duplicates! */
        link.setLinkID(linkid);
        if (link.getDownloadURL().matches(type_chip_de_file) || link.getDownloadURL().matches(type_chip_eu_file)) {

            set_final_filename = false;
            accessURL(this.br, link.getDownloadURL());
            if (link.getDownloadURL().matches(type_chip_eu_file) && !this.br.containsHTML("class=\"downloadnow_button")) {
                /* chip.eu url without download button --> No downloadable content --> URL is offline for us */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("\"pagetitle\":\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("\"dachzeile\":\"([^<>\"]*?)\"").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
            String filesize_str = br.getRegex(">Dateigr\\&ouml;\\&szlig;e:</p>[\t\n\r ]+<p class=\"col2\">([^<>\"]*?)<meta itemprop=\"fileSize\"").getMatch(0);
            if (filesize_str == null) {
                /* For the international chip websites! */
                filesize_str = br.getRegex("<dt>(?:File size:|Размер файла:|Dimensioni:|Dateigröße:|Velikost:|Fájlméret:|Bestandsgrootte:|Rozmiar pliku:|Mărime fişier:|Dosya boyu:|文件大小：)<br /></dt>[\t\n\r ]+<dd>(.*?)<br /></dd>").getMatch(0);
            }
            if (filesize_str == null) {
                filesize_str = br.getRegex("itemprop=\"fileSize\" content=\"([0-9\\.]+)\"").getMatch(0);
            }
            if (filesize_str != null) {
                filesize_str = filesize_str.replace("GByte", "GB");
                filesize = SizeFormatter.getSize(filesize_str);
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
            if (filename == null) {
                /* Last chance! */
                filename = filename_url + "_" + linkid;
            } else {
                filename += "_" + linkid;
            }

        } else {
            /* type_chip_de_video and type_chip_de_video_others */

            set_final_filename = true;
            String ext = null;
            if (video_use_API) {
                prepBRAPI(this.br);
                accesscontainerIdBeitrag(this.br, linkid);
                if (this.br.containsHTML("\"error_message\"")) {
                    /*
                     * Usually that should be covered already as API will return 404 on offline content but let's double-check by this
                     * errormessage-json-object.
                     */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                final String source_videoid = (String) JavaScriptEngineFactory.walkJson(entries, "videos/{0}/containerIdBeitrag");
                if (!link.getDownloadURL().matches(type_chip_de_video) && source_videoid != null) {
                    /* User added an article which may or may not contain one (or multiple) videos. */
                    accesscontainerIdBeitrag(this.br, source_videoid);
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                }
                /*
                 * The directlinks returned by their API are quality-wise not the best (middle) but in case everysthing fails, we still have
                 * them and our users can still download the video :)
                 */
                date = (String) entries.get("date");
                description = (String) entries.get("description");
                final String dllink_fallback = (String) entries.get("videoUrl");
                final String title = (String) entries.get("title");
                final String subtitle = (String) entries.get("headline");
                if (!inValidate(title)) {
                    filename = title;
                    if (!inValidate(subtitle)) {
                        filename += " - " + subtitle;
                    }
                }
                try {
                    DLLINK = videos_kaltura_getDllink();
                } catch (final Throwable e) {
                    /* Whatever happens, catch it - we might have a working fallback :) */
                }
                if (inValidate(DLLINK)) {
                    logger.warning("Failed to find highest quality final downloadlink via kaltura player --> Fallback to API downloadlink");
                    DLLINK = dllink_fallback;
                }
                if (!link.getDownloadURL().matches(type_chip_de_video) && inValidate(DLLINK)) {
                    /* Whatever the user added - there is no downloadable content! */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                accessURL(this.br, link.getDownloadURL());
                filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\"").getMatch(0);
                date = this.br.getRegex("\"publishDateTime\":\"(\\d{4}\\-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+\\d{2}:\\d{2})\"").getMatch(0);
                DLLINK = videos_kaltura_getDllink();
                // DLLINK = "http://video.chip.de/38396417/textzwei.flv";
            }
            try {
                ext = (String) entries.get("fileExt");
            } catch (final Throwable e) {
            }
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (filename == null) {
                /* Last chance! */
                filename = filename_url + "_" + linkid;
            }
            if (inValidate(ext)) {
                /* Fallback to chip standard video-extension */
                ext = "mp4";
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename += "." + ext;
            this.br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br.openHeadConnection(DLLINK);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
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
        filename = encodeUnicode(filename);
        if (set_final_filename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        if (filesize > -1) {
            link.setDownloadSize(filesize);
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
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(type_chip_de_video)) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors();
                br.followConnection();
                /* We use APIs which we can trust so retrying is okay ;) */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            }
        } else if (downloadLink.getDownloadURL().matches(type_chip_eu_file)) {
            /* TODO: Find a way around their .exe adware installers - then our CRC check will finally also work for chip.eu! */
            /* Re-Use saved downloadlinks */
            DLLINK = this.checkDirectLink(downloadLink, "free_directlink");
            if (DLLINK == null) {
                String getfile_url = br.getRegex("\"(/.{2}/download_getfile_[^<>\"]*?)\"").getMatch(0);
                if (getfile_url == null) {
                    getfile_url = this.br.getRegex("href=\"([^<>\"]*?)\" rel=\"nofollow\" class=\"dwnld\"").getMatch(0);
                }
                if (getfile_url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (getfile_url.startsWith("http") && !getfile_url.contains("chip.eu/")) {
                    /*
                     * Happens for software whos manufactors do not allow direct mirrors from chip servers e.g.
                     * http://download.chip.eu/en/Graffiti-Studio_1066113.html
                     */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Externer download - nicht per JDownloader ladbar!");
                }
                this.br.getPage(getfile_url);
                DLLINK = br.getRegex("If not, please click <a href=\"(http[^<>\"]*?)\"").getMatch(0);
                if (DLLINK == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }

            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors();
                br.followConnection();
                if (!this.br.getHost().contains("chip")) {
                    /*
                     * Happens for software whos manufactors do not allow direct mirrors from chip servers e.g.
                     * http://www.chip.de/downloads/Windows-10-64-Bit_72189999.html
                     */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "External download - not possible via JDownloader!");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
            downloadLink.setProperty("free_directlink", dl.getConnection().getURL().toString());
            dl.startDownload();

        } else {
            /* Re-Use saved downloadlinks */
            DLLINK = this.checkDirectLink(downloadLink, "free_directlink");
            if (DLLINK == null) {
                String step1 = br.getRegex("\"https?://x\\.chip\\.de/intern/dl/\\?url=(http[^<>\"]*?)\"").getMatch(0);
                if (step1 == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                step1 = Encoding.htmlDecode(step1);
                br.getPage(step1);
                String step2 = br.getRegex("\"(https?://(www\\.)?chip\\.de/downloads/c1_downloads_hs_getfile[^<>\"]*?)\"").getMatch(0);
                if (step2 != null) {
                    step2 = Encoding.htmlDecode(step2);
                    br.getPage(step2);
                }
                DLLINK = downloads_getDllink();
                if (DLLINK == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }

            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
            String etag = this.br.getRequest().getResponseHeader("ETag");
            if (etag != null) {
                /* chip.de servers will often | always return md5 hash via headers! */
                try {
                    etag = etag.replace("\"", "");
                    final String[] etagInfo = etag.split(":");
                    final String md5 = etagInfo[0];
                    if (md5.matches("[A-Fa-f0-9]{32}")) {
                        downloadLink.setMD5Hash(md5);
                    }
                } catch (final Throwable e) {
                }
            }
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors();
                br.followConnection();
                if (!this.br.getHost().contains("chip")) {
                    /*
                     * Happens for software whos manufactors do not allow direct mirrors from chip servers e.g.
                     * http://www.chip.de/downloads/Windows-10-64-Bit_72189999.html
                     */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "External download - not possible via JDownloader!");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        }
        downloadLink.setProperty("free_directlink", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    /* Handles general server errors. */
    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        }
    }

    private String downloads_getDllink() {
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String videos_kaltura_getDllink() throws Exception {
        String dllink_temp = null;
        /* First try to get informartion via website */
        String sp = this.br.getRegex("sp/(\\d+)/embedIframeJs").getMatch(0);
        final String entry_id = this.br.getRegex("/entry_id/([^/]*?)/").getMatch(0);
        String uiconf_id = this.br.getRegex("uiconf_id/(\\d+)").getMatch(0);
        String partner_id = this.br.getRegex("/partner_id/(\\d+)").getMatch(0);
        if (partner_id == null) {
            partner_id = this.br.getRegex("kaltura.com/p/(\\d+)").getMatch(0);
        }
        /* Then eventually fallback to static information */
        if (partner_id == null) {
            partner_id = kaltura_partner_id;
        }
        if (uiconf_id == null) {
            uiconf_id = kaltura_uiconf_id;
        }
        if (sp == null) {
            sp = kaltura_sp;
        }
        if (entry_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* They use waay more arguments via browser - we don't need them :) */
        final String postData = "cache_st=5&wid=_" + partner_id + "&uiconf_id=" + uiconf_id + "&entry_id=" + entry_id + "&urid=2.39";
        final Browser tempbr = new Browser();
        /* Beware of the Content-Type - it will not work using 'application/json; charset=utf-8' */
        tempbr.postPage("http://cdnapi.kaltura.com/html5/html5lib/v2.39/mwEmbedFrame.php", postData);
        final String json = tempbr.getRegex("window\\.kalturaIframePackageData = (\\{.*?\\});").getMatch(0);
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        long max_bitrate = 0;
        long max_bitrate_temp = 0;
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "entryResult/contextData/flavorAssets");
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String flavourid = (String) entries.get("id");
            if (flavourid == null) {
                continue;
            }
            max_bitrate_temp = JavaScriptEngineFactory.toLong(entries.get("bitrate"), 0);
            if (max_bitrate_temp > max_bitrate) {
                dllink_temp = "http://cdnapi.kaltura.com/p/" + partner_id + "/sp/" + sp + "/playManifest/entryId/" + entry_id + "/flavorId/" + flavourid + "/format/url/protocol/http/a.mp4";
                max_bitrate = max_bitrate_temp;
            }
        }
        return dllink_temp;
    }

    public static void accesscontainerIdBeitrag(final Browser br, final String containerIdBeitrag) throws PluginException, IOException {
        accessURL(br, "https://apps-rest.chip.de/api/v2/containerIdBeitrag/" + containerIdBeitrag + "/");
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

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
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

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KalturaVideoPlatform;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}