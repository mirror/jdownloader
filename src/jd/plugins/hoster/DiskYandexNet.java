//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "disk.yandex.net", "video.yandex.ru", "yadi.sk" }, urls = { "http://yandexdecrypted\\.net/\\d+", "http://video\\.yandex\\.ru/(iframe/[A-Za-z0-9]+/[A-Za-z0-9]+\\.\\d+|users/[A-Za-z0-9]+/view/\\d+)", "https://yadi\\.sk/a/[A-Za-z0-9\\-_]+/[a-f0-9]{24}" })
public class DiskYandexNet extends PluginForHost {
    public DiskYandexNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://passport.yandex.ru/passport?mode=register&from=cloud&retpath=https%3A%2F%2Fdisk.yandex.ru%2F%3Fauth%3D1&origin=face.en");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://disk.yandex.net/";
    }

    /* Settings values */
    private final String          MOVE_FILES_TO_ACCOUNT              = "MOVE_FILES_TO_ACCOUNT";
    private final String          DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD = "EMPTY_TRASH_AFTER_DOWNLOAD";
    private static final String   NORESUME                           = "NORESUME";
    /* Some constants which they used in browser */
    public static final String    CLIENT_ID                          = "2784000881524006529778";
    public static String          VERSION_YANDEX_FILES               = "59.7";
    public static String          VERSION_YANDEX_PHOTO_ALBUMS        = "77.3";
    private static final String   STANDARD_FREE_SPEED                = "64 kbit/s";
    /* Different languages == different 'downloads' directory names */
    private static final String[] downloaddirs                       = { "Downloads", "%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8" };
    /* Connection limits */
    private final boolean         FREE_RESUME                        = true;
    private final int             FREE_MAXCHUNKS                     = 0;
    private static final int      FREE_MAXDOWNLOADS                  = 20;
    private final boolean         ACCOUNT_FREE_RESUME                = true;
    private final int             ACCOUNT_FREE_MAXCHUNKS             = 0;
    private static final int      ACCOUNT_FREE_MAXDOWNLOADS          = 20;
    /* Domains & other login stuff */
    private final String[]        cookie_domains                     = new String[] { "https://yandex.ru", "https://yandex.com", "https://disk.yandex.ru/", "https://disk.yandex.com/", "https://disk.yandex.net/" };
    public static final String[]  sk_domains                         = new String[] { "disk.yandex.com", "disk.yandex.ru", "disk.yandex.com.tr", "disk.yandex.ua", "disk.yandex.az", "disk.yandex.com.am", "disk.yandex.com.ge", "disk.yandex.co.il", "disk.yandex.kg", "disk.yandex.lt", "disk.yandex.lv", "disk.yandex.md", "disk.yandex.tj", "disk.yandex.tm", "disk.yandex.uz", "disk.yandex.fr", "disk.yandex.ee", "disk.yandex.kz", "disk.yandex.by" };
    private static Object         LOCK                               = new Object();
    /* Other constants */
    /* Important constant which seems to be unique for every account. It's needed for most of the requests when logged in. */
    private String                ACCOUNT_SK                         = null;
    private static final String   TYPE_VIDEO                         = "https?://video\\.yandex\\.ru/(iframe/[A-Za-z0-9]+/[A-Za-z0-9]+\\.\\d+|users/[A-Za-z0-9]+/view/\\d+)";
    private static final String   TYPE_VIDEO_USER                    = "https?://video\\.yandex\\.ru/users/[A-Za-z0-9]+/view/\\d+";
    private static final String   TYPE_DISK                          = "https?://yandexdecrypted\\.net/\\d+";
    private static final String   TYPE_ALBUM                         = "https://yadi\\.sk/a/.+";
    private static final String   ACCOUNTONLYTEXT                    = "class=\"nb-panel__warning aside\\-public__warning\\-speed\"|>File download limit exceeded";
    /* Properties */
    public static final String    PROPERTY_HASH                      = "hash_main";
    private Account               currAcc                            = null;
    /*
     * https://tech.yandex.com/disk/api/reference/public-docpage/ 2018-08-09: API(s) seem to work fine again - in case of failure, please
     * disable use_api_file_free_availablecheck ONLY!! This should work fine when enabled: use_api_file_free_download
     */
    private static final boolean  use_api_file_free_availablecheck   = true;
    private static final boolean  use_api_file_free_download         = true;

    /* Make sure we always use our main domain */
    private String getMainLink(final DownloadLink dl) throws Exception {
        String mainlink = dl.getStringProperty("mainlink", null);
        if (mainlink == null && getRawHash(dl) != null) {
            mainlink = String.format("https://yadi.sk/public/?hash=%s", URLEncode.encodeURIComponent(getRawHash(dl)));
        }
        if (mainlink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!StringUtils.contains(mainlink, "yadi")) {
            mainlink = "https://disk.yandex.com/" + new Regex(mainlink, "yandex\\.[^/]+/(.+)").getMatch(0);
        }
        return mainlink;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        /** TODO: Check linkid for other linktypes */
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(TYPE_ALBUM)) {
            return new Regex(link.getPluginPatternMatcher(), "/a/(.+)").getMatch(0);
        } else {
            return super.getLinkID(link);
        }
    }

    private void setConstants(final DownloadLink dl, final Account acc) {
        currAcc = acc;
    }

    /** Returns currently used domain */
    public static String getCurrentDomain() {
        return "disk.yandex.com";
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setConstants(link, null);
        setBrowserExclusive();
        this.br = prepBR(this.br);
        br.setFollowRedirects(true);
        String filename;
        String filesize_str;
        long filesize_long = -1;
        String final_filename = link.getStringProperty("plain_filename", null);
        if (final_filename == null) {
            final_filename = link.getFinalFileName();
        }
        if (link.getPluginPatternMatcher().matches(TYPE_VIDEO)) {
            getPage(link.getPluginPatternMatcher());
            if (link.getPluginPatternMatcher().matches(TYPE_VIDEO_USER)) {
                /* offline|empty|enything else (e.g. abuse) */
                if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>Ролик не найден</title>|>Здесь пока пусто<|class=\"error\\-container\"")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String iframe_url = br.getRegex("property=\"og:video:ifrаme\" content=\"(https?://video\\.yandex\\.ru/iframe/[^<>\"]*?)\"").getMatch(0);
                if (iframe_url == null) {
                    iframe_url = br.getRegex("class=\"video\\-frame\"><iframe src=\"(//video\\.yandex\\.ru/[^<>\"]*?)\"").getMatch(0);
                }
                if (iframe_url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (!iframe_url.startsWith("http:") && !iframe_url.startsWith("https:")) {
                    iframe_url = "http:" + iframe_url;
                }
                link.setUrlDownload(iframe_url);
                getPage(iframe_url);
            }
            if (br.containsHTML("<title>Яндекс\\.Видео</title>") || this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?) — Яндекс\\.Видео</title>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.htmlDecode(filename.trim()) + ".mp4";
            filename = encodeUnicode(filename);
            link.setName(filename);
        } else if (link.getPluginPatternMatcher().matches(TYPE_ALBUM)) {
            br.getPage(link.getPluginPatternMatcher());
            if (jd.plugins.decrypter.DiskYandexNetFolder.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (StringUtils.isEmpty(getRawHash(link))) {
                /* For urls which have not been added via crawler, this value is not set but we might need it later. */
                final String hash_long = jd.plugins.hoster.DiskYandexNet.getHashLongFromHTML(this.br);
                if (!StringUtils.isEmpty(hash_long)) {
                    setRawHash(link, hash_long);
                }
            }
            /* Find json object for current link ... */
            final ArrayList<Object> modelObjects = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(jd.plugins.decrypter.DiskYandexNetFolder.regExJSON(this.br));
            LinkedHashMap<String, Object> entries = jd.plugins.decrypter.DiskYandexNetFolder.findModel(modelObjects, "resources");
            if (entries == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final ArrayList<Object> mediaObjects = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "data/resources");
            boolean foundObject = false;
            final String item_id = this.albumGetID(link);
            for (final Object mediao : mediaObjects) {
                entries = (LinkedHashMap<String, Object>) mediao;
                final String item_id_current = (String) entries.get("item_id");
                if (item_id.equalsIgnoreCase(item_id_current)) {
                    foundObject = true;
                    break;
                }
            }
            if (foundObject) {
                /* Great - we found our object and can display filename information. */
                return parseInformationAPIAvailablecheckAlbum(this, link, entries);
            } else {
                /* We failed to find the details but a download should be possible nontheless! */
                return AvailableStatus.TRUE;
            }
        } else {
            if (getRawHash(link) == null || this.getPath(link) == null) {
                /* Errorhandling for old urls */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (use_api_file_free_availablecheck) {
                getPage("https://cloud-api.yandex.net/v1/disk/public/resources?public_key=" + URLEncode.encodeURIComponent(getRawHash(link)) + "&path=" + URLEncode.encodeURIComponent(this.getPath(link)));
                if (apiAvailablecheckIsOffline(br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                link.setProperty("premiumonly", false);
                return parseInformationAPIAvailablecheckFiles(this, link, (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString()));
            } else {
                getPage(getMainLink(link));
                if (br.containsHTML("(<title>The file you are looking for could not be found\\.|>Nothing found</span>|<title>Nothing found \\— Yandex\\.Disk</title>)") || br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = this.br.getRegex("class=\"file-name\" data-reactid=\"[^\"]*?\">([^<>\"]+)<").getMatch(0);
                if (StringUtils.isEmpty(filename)) {
                    /* Very unsafe method! */
                    final String json = br.getRegex("<script type=\"application/json\"[^<>]*?id=\"store\\-prefetch\">(.*?)<").getMatch(0);
                    if (json != null) {
                        filename = PluginJSonUtils.getJson(json, "name");
                    }
                }
                filesize_str = link.getStringProperty("plain_size", null);
                if (filesize_str == null) {
                    filesize_str = this.br.getRegex("class=\"item-details__name\">Size:</span> ([^<>\"]+)</div>").getMatch(0);
                }
                if (filesize_str == null) {
                    /* Language independant */
                    filesize_str = this.br.getRegex("class=\"item-details__name\">[^<>\"]+</span> ([\\d\\.]+ (?:B|KB|MB|GB))</div>").getMatch(0);
                }
                if (filesize_str != null) {
                    filesize_str = filesize_str.replace(",", ".");
                    filesize_long = SizeFormatter.getSize(filesize_str);
                }
                if (final_filename == null) {
                    final_filename = filename;
                }
                /* Important for account download handling */
                if (br.containsHTML(ACCOUNTONLYTEXT)) {
                    link.setProperty("premiumonly", true);
                } else {
                    // link.setProperty("premiumonly", false);
                    logger.info("Debug info: premiumonly: " + link.getBooleanProperty("premiumonly", false));
                }
            }
            if (final_filename == null && filename != null) {
                link.setFinalFileName(filename);
            } else if (final_filename != null) {
                link.setFinalFileName(final_filename);
            }
        }
        if (filesize_long > -1) {
            link.setDownloadSize(filesize_long);
        }
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus parseInformationAPIAvailablecheckFiles(final Plugin plugin, final DownloadLink dl, final LinkedHashMap<String, Object> entries) throws Exception {
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
        final String error = (String) entries.get("error");
        final String hash = (String) entries.get("public_key");
        String filename = (String) entries.get("name");
        final String path = (String) entries.get("path");
        final String md5 = (String) entries.get("md5");
        final String sha256 = (String) entries.get("sha256");
        if (error != null || StringUtils.isEmpty(filename) || StringUtils.isEmpty(path) || StringUtils.isEmpty(hash) || filesize == -1) {
            /* Whatever - our link is probably offline! */
            return AvailableStatus.FALSE;
        }
        filename = plugin.encodeUnicode(filename);
        if (md5 != null) {
            dl.setMD5Hash(md5);
        }
        if (sha256 != null) {
            dl.setSha256Hash(sha256);
        }
        dl.setProperty("path", path);
        dl.setProperty("hash_main", hash);
        dl.setFinalFileName(filename);
        dl.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    /** Parses file info for photo/video (ALBUM) urls e.g. /album/blabla:5fffffce1939c0c46ffffffe */
    public static AvailableStatus parseInformationAPIAvailablecheckAlbum(final Plugin plugin, final DownloadLink dl, final LinkedHashMap<String, Object> entries) throws Exception {
        LinkedHashMap<String, Object> entriesMeta = (LinkedHashMap<String, Object>) entries.get("meta");
        final List<Object> versions = (List<Object>) entriesMeta.get("sizes");
        final long filesize = JavaScriptEngineFactory.toLong(entriesMeta.get("size"), -1);
        final String error = (String) entries.get("error");
        String filename = (String) entries.get("name");
        if (error != null || StringUtils.isEmpty(filename) || filesize == -1) {
            /* Whatever - our link is probably offline! */
            return AvailableStatus.FALSE;
        }
        filename = plugin.encodeUnicode(filename);
        dl.setFinalFileName(filename);
        dl.setDownloadSize(filesize);
        String dllink = null, dllinkAlt = null;
        for (final Object versiono : versions) {
            entriesMeta = (LinkedHashMap<String, Object>) versiono;
            String url = (String) entriesMeta.get("url");
            final String versionName = (String) entriesMeta.get("name");
            if (StringUtils.isEmpty(url) || !url.startsWith("//") || StringUtils.isEmpty(versionName)) {
                continue;
            }
            url = "http:" + url;
            if (versionName.equalsIgnoreCase("ORIGINAL")) {
                dllink = url;
                break;
            } else if (StringUtils.isEmpty(dllinkAlt)) {
                /*
                 * 2018-08-09: List is sorted from best --> worst so let's use the highest one after ORIGINAL as alternative downloadurl.
                 */
                dllinkAlt = url;
            }
        }
        if (StringUtils.isEmpty(dllink) && !StringUtils.isEmpty(dllinkAlt)) {
            /* ORIGINAL not found? Download other version ... */
            dllink = dllinkAlt;
        }
        if (!StringUtils.isEmpty(dllink)) {
            dl.setProperty("directurl", dllink);
        }
        return AvailableStatus.TRUE;
    }

    public static boolean apiAvailablecheckIsOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie("http://disk.yandex.com/", "ys", "");
        return br;
    }

    private void getPage(final String url) throws IOException {
        getPage(this.br, url);
    }

    public static void getPage(final Browser br, final String url) throws IOException {
        br.getPage(url);
        /* 2017-03-30: New */
        final String jsRedirect = br.getRegex("(https?://[^<>\"]+force_show=1[^<>\"]*?)").getMatch(0);
        if (jsRedirect != null) {
            br.getPage(jsRedirect);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String dllink;
        if (downloadLink.getPluginPatternMatcher().matches(TYPE_ALBUM)) {
            handleDownloadAlbum(downloadLink);
        } else if (downloadLink.getPluginPatternMatcher().matches(TYPE_DISK)) {
            if (downloadableViaAccountOnly(downloadLink)) {
                /*
                 * link is only downloadable via account because the public overall download limit (traffic limit) is exceeded. In this case
                 * the user can only download the link by importing it into his account and downloading it "from there".
                 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            if (use_api_file_free_availablecheck) {
                /*
                 * 2018-08-09: Randomly found this during testing - this seems to be a good way to easily get downloadlinks PLUS via this
                 * way, it is possible to download files which otherwise require the usage of an account e.g. errormessage
                 * "Download limit reached. You can still save this file to your Yandex.Disk" [And then download it via own account]
                 */
                dllink = PluginJSonUtils.getJson(br, "file");
            }
            if (StringUtils.isEmpty(dllink)) {
                if (use_api_file_free_download) {
                    /**
                     * Download API:
                     *
                     * https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=public_key&path=/
                     */
                    /* Free API download. */
                    getPage("https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=" + URLEncode.encodeURIComponent(getRawHash(downloadLink)) + "&path=" + URLEncode.encodeURIComponent(this.getPath(downloadLink)));
                    if (this.br.containsHTML("DiskNotFoundError")) {
                        /* Inside key 'error' */
                        /*
                         * Usually this would mean that the file is offline but we checked it before so it has probably simply reached the
                         * traffic limit.
                         */
                        downloadLink.setProperty("premiumonly", true);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                    }
                    final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                    dllink = (String) entries.get("href");
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else {
                    if (StringUtils.isEmpty(dllink)) {
                        /* Free website download */
                        this.br = this.prepbrWebsite(new Browser());
                        getPage(getMainLink(downloadLink));
                        String sk = getSK(this.br);
                        if (sk == null) {
                            logger.warning("sk in account handling (without move) is null");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        /* 2018-04-18: Not required anymore */
                        // getPage("https://yadi.sk/tns.html");
                        br.getHeaders().put("Accept", "*/*");
                        br.getHeaders().put("Content-Type", "text/plain");
                        postPageRaw("/public-api-desktop/download-url", String.format("{\"hash\":\"%s\",\"sk\":\"%s\"}", getRawHash(downloadLink), sk));
                        /** TODO: Find out why we have the wrong SK here and remove this workaround! */
                        if (br.containsHTML("\"id\":\"WRONG_SK\"")) {
                            logger.warning("WRONG_SK --> This should not happen");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            // sk = getSK(this.br);
                            // if (sk == null || sk.equals("")) {
                            // logger.warning("sk in account handling (without move) is null");
                            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            // }
                            // br.postPage("/public-api-desktop/download-url", String.format("{\"hash\":\"%s\",\"sk\":\"%s\"}",
                            // this.currHash,
                            // sk));
                        }
                        handleErrorsFree();
                        dllink = PluginJSonUtils.getJsonValue(br, "url");
                        if (dllink == null) {
                            logger.warning("Failed to find final downloadurl");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        /* Don't do htmldecode because the link will be invalid then */
                        /* sure json will return url with htmlentities? */
                        dllink = HTMLEntities.unhtmlentities(dllink);
                    }
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors(downloadLink);
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            handleDownloadVideo(downloadLink);
        }
    }

    @SuppressWarnings("deprecation")
    private void handleDownloadVideo(final DownloadLink downloadLink) throws Exception {
        final String linkpart = new Regex(downloadLink.getDownloadURL(), "/iframe/(.+)").getMatch(0);
        final String width = br.getRegex("width\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
        final String height = br.getRegex("width\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
        String file = br.getRegex("\\&quot;file\\&quot;:\\&quot;([a-z0-9]+)\\&quot;").getMatch(0);
        if (file == null) {
            file = br.getRegex("name=\"twitter:image\" content=\"https?://static\\.video\\.yandex.ru/get/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_\\.]+/([A-Za-z0-9]+)\\.jpg\"").getMatch(0);
        }
        if (file == null && (width != null && height != null)) {
            file = "m" + width + "x" + height + ".flv";
            downloadLink.setFinalFileName(downloadLink.getName().replace(".mp4", ".flv"));
        } else if (file == null) {
            file = "0.flv";
            downloadLink.setFinalFileName(downloadLink.getName().replace(".mp4", ".flv"));
        } else {
            file += ".mp4";
            downloadLink.setFinalFileName(downloadLink.getName().replace(".flv", ".mp4"));
        }
        getPage("http://static.video.yandex.net/get-token/" + linkpart + "?nc=0." + System.currentTimeMillis());
        final String token = br.getRegex("<token>([^<>\"]*?)</token>").getMatch(0);
        if (token == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage("http://streaming.video.yandex.ru/get-location/" + linkpart + "/" + file + "?token=" + token + "&ref=video.yandex.ru");
        String dllink = br.getRegex("<video\\-location>(https?://[^<>\"]*?)</video\\-location>").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors(downloadLink);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /** Download single album objects (photo/video) */
    private void handleDownloadAlbum(final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, "directurl");
        if (StringUtils.isEmpty(dllink)) {
            final String id0 = albumGetID0(link);
            final String clientID = albumGetIdClient();
            /* Rare case - probably the user tries to download a video. */
            String sk = jd.plugins.hoster.DiskYandexNet.getSK(this.br);
            if (StringUtils.isEmpty(sk)) {
                /** TODO: Maybe keep SK throughout sessions to save that one request ... */
                logger.info("Getting new SK value ...");
                sk = jd.plugins.hoster.DiskYandexNet.getNewSK(this.br, "disk.yandex.ru", br.getURL());
            }
            if (StringUtils.isEmpty(sk) || id0 == null) {
                logger.warning("Failed to get SK value");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.postPage("https://disk.yandex.ru/album-models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=" + Encoding.urlEncode(id0) + "&idClient=" + clientID + "&version=" + jd.plugins.hoster.DiskYandexNet.VERSION_YANDEX_PHOTO_ALBUMS + "&sk=" + sk);
            dllink = PluginJSonUtils.getJson(br, "file");
            if (!StringUtils.isEmpty(dllink) && dllink.startsWith("//")) {
                dllink = "http:" + dllink;
            } else if (!dllink.startsWith("http")) {
                /* This should never happen! */
                logger.info("WTF bad downloadlink");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* More than 1 chunk is not necessary */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors(link);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directurl", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private void handleErrorsFree() throws PluginException {
        if (br.containsHTML("\"title\":\"invalid ckey\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'invalid ckey'", 5 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":21")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 21 'bad formed path'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":69")) {
            /* Usually this does not happen. Happens also if you actually try to download a "premiumonly" link via this method. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"code\":71")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 71 'Wrong path'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":77")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 77 'resource not found'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":88")) {
            /* Happens when we send a wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 88 'Decryption error'", 10 * 60 * 1000l);
        }
    }

    private String getIdClient() {
        return String.format("undefined", System.currentTimeMillis());
    }

    public static void setRawHash(final DownloadLink dl, final String hash_long) {
        dl.setProperty(PROPERTY_HASH, hash_long);
    }

    private String getRawHash(final DownloadLink dl) {
        final String hash = dl.getStringProperty(PROPERTY_HASH, null);
        return hash;
    }

    private String getPath(final DownloadLink dl) {
        return dl.getStringProperty("path", null);
    }

    private String getCkey() throws PluginException {
        final String ckey = PluginJSonUtils.getJsonValue(br, "ckey");
        if (ckey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ckey;
    }

    private String getUserID(final Account acc) {
        return acc.getStringProperty("account_userid");
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                prepbrWebsite(this.br);
                /*
                 * Login procedure can redirect multiple times! It should lead us to disk.yandex.com via the referer parameter in our login
                 * URL.
                 */
                br.setFollowRedirects(true);
                /* Always try to re-use cookies to avoid login captchas! */
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    for (final String domain : cookie_domains) {
                        br.setCookies(domain, cookies);
                    }
                    br.setCookies("passport.yandex.com", cookies);
                    if (!force) {
                        /* Trust cookies */
                        return;
                    }
                    br.getPage("https://passport.yandex.com/profile");
                    if (br.containsHTML("mode=logout")) {
                        /* Set new cookie timestamp */
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    }
                    /* Failed - we have to perform a full login! */
                    br.clearCookies(br.getHost());
                }
                boolean isLoggedIN = false;
                boolean requiresCaptcha;
                final Browser ajaxBR = br.cloneBrowser();
                ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                ajaxBR.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getPage("https://passport.yandex.com/auth?from=cloud&origin=disk_landing_web_signin_ru&retpath=https%3A%2F%2Fdisk.yandex.com%2F%3Fsource%3Dlanding_web_signin&backpath=https%3A%2F%2Fdisk.yandex.com");
                for (int i = 0; i <= 4; i++) {
                    final Form[] forms = br.getForms();
                    if (forms.length == 0) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Form loginform = forms[0];
                    loginform.remove("twoweeks");
                    loginform.put("source", "password");
                    loginform.put("login", Encoding.urlEncode(account.getUser()));
                    loginform.put("passwd", Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML("\\&quot;captchaRequired\\&quot;:true")) {
                        /** TODO: 2018-08-10: Fix captcha support */
                        /* 2018-04-18: Only required after 10 bad login attempts or bad IP */
                        requiresCaptcha = true;
                        final String csrf_token = loginform.hasInputFieldByName("csrf_token") ? loginform.getInputField("csrf_token").getValue() : null;
                        final String idkey = loginform.hasInputFieldByName("idkey") ? loginform.getInputField("idkey").getValue() : null;
                        if (csrf_token == null || idkey == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        ajaxBR.postPage("https://passport.yandex.com/registration-validations/textcaptcha", "csrf_token=" + csrf_token + "&track_id=" + idkey);
                        final String url_captcha = PluginJSonUtils.getJson(ajaxBR, "image_url");
                        final String id = PluginJSonUtils.getJson(ajaxBR, "id");
                        if (StringUtils.isEmpty(url_captcha) || StringUtils.isEmpty(id)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "https://" + account.getHoster(), true);
                        final String c = getCaptchaCode(url_captcha, dummyLink);
                        loginform.put("captcha_answer", c);
                        // loginform.put("idkey", id);
                    } else {
                        requiresCaptcha = false;
                    }
                    br.submitForm(loginform);
                    isLoggedIN = br.getCookie(br.getURL(), "yandex_login") != null;
                    if (!requiresCaptcha && i > 0) {
                        /* Probably wrong password */
                        break;
                    } else if (isLoggedIN) {
                        break;
                    }
                }
                if (!isLoggedIN) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (!br.getURL().contains("client/disk")) {
            getPage("https://" + getCurrentDomain() + "/client/disk/");
        }
        ACCOUNT_SK = br.getRegex("\"sk\":\"([a-z0-9]+)\"").getMatch(0);
        final String userID = PluginJSonUtils.getJson(br, "uid");
        if (ACCOUNT_SK == null || StringUtils.isEmpty(userID)) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        account.setProperty("saved_sk", ACCOUNT_SK);
        account.setProperty("account_userid", userID);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        ai.setStatus("Free Account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        ACCOUNT_SK = account.getStringProperty("saved_sk", null);
        requestFileInformation(link);
        login(account, false);
        setConstants(link, account);
        if (link.getPluginPatternMatcher().matches(TYPE_ALBUM) || link.getPluginPatternMatcher().matches(TYPE_VIDEO)) {
            /* These linktypes have only one download-handling! */
            doFree(link);
        } else {
            String dllink = checkDirectLink(link, "directlink_account");
            if (dllink == null) {
                final String userID = getUserID(account);
                final String hash = getRawHash(link);
                final String id0 = diskGetID0(link);
                /* This should never happen */
                if (ACCOUNT_SK == null || userID == null) {
                    logger.warning("ACCOUNT_SK is null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /*
                 * Move files into account and download them "from there" although user might not have selected this? --> Forced handling,
                 * only required if not possible via different way
                 */
                final boolean moveIntoAccHandlingActive = this.getPluginConfig().getBooleanProperty(MOVE_FILES_TO_ACCOUNT, false);
                final boolean downloadableViaAccountOnly = downloadableViaAccountOnly(link);
                if (!moveIntoAccHandlingActive && !downloadableViaAccountOnly) {
                    logger.info("MoveToAccount handling is inactive -> Starting free account download handling");
                    getPage(getMainLink(link));
                    br.postPage("https://" + getCurrentDomain() + "/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=%2Fpublic%2F" + URLEncode.encodeURIComponent(hash) + "&idClient=" + CLIENT_ID + "&version=" + VERSION_YANDEX_FILES + "&sk=" + this.ACCOUNT_SK);
                    dllink = siteGetDllink(link);
                }
                if (moveIntoAccHandlingActive || downloadableViaAccountOnly || dllink == null) {
                    if (downloadableViaAccountOnly) {
                        logger.info("forcedmoveIntoAccHandling active");
                    }
                    String internal_file_path = getInternalFilePath(link);
                    final boolean file_was_moved_before = internal_file_path != null;
                    logger.info("MoveFileIntoAccount: MoveFileIntoAccount handling is active");
                    if (internal_file_path == null) {
                        logger.info("MoveFileIntoAccount: No internal filepath available --> Trying to move file into account");
                        /*
                         * "name" pretty much doesn't matter as this is just the name the file gets inside our account. Of course this will
                         * be part of our <internal_file_path> but best is to use the original filename!
                         */
                        br.getHeaders().put("Content-Type", "text/plain");
                        postPageRaw("https://" + getCurrentDomain() + "/public/api/save", URLEncoder.encode(String.format("{\"hash\":\"%s\",\"name\":\"%s\",\"lang\":\"en\",\"source\":\"public_web_copy\",\"sk\":\"%s\",\"uid\":\"%s\"}", id0, link.getName(), this.ACCOUNT_SK, userID), "UTF-8"));
                        internal_file_path = PluginJSonUtils.getJson(br, "path");
                        if (br.containsHTML("\"code\":85")) {
                            logger.info("MoveFileIntoAccount: failed to move file to account: No free space available");
                            throw new PluginException(LinkStatus.ERROR_FATAL, "No free space available, failed to move file to account");
                        } else if (StringUtils.isEmpty(internal_file_path)) {
                            /* This should never happen! */
                            logger.info("MoveFileIntoAccount: Failed to move file into account: WTF");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        /* Save path for later. */
                        link.setProperty("path_internal", internal_file_path);
                        final String oid = PluginJSonUtils.getJson(br, "oid");
                        if (!StringUtils.isEmpty(oid)) {
                            /* Sometimes the process of 'moving' a file into a cloud account can take some seconds. */
                            logger.info("transfer-status: checking");
                            try {
                                for (int i = 1; i < 5; i++) {
                                    br.getHeaders().put("Content-Type", "text/plain");
                                    postPageRaw("https://" + getCurrentDomain() + "/public/api/get-operation-status", URLEncoder.encode(String.format("{\"oid\":\"%s\",\"sk\":\"%s\",\"uid\":\"%s\"}", oid, this.ACCOUNT_SK, userID), "UTF-8"));
                                    final String copyState = PluginJSonUtils.getJson(br, "state");
                                    logger.info("Copy state: " + copyState);
                                    if (copyState.equalsIgnoreCase("COMPLETED")) {
                                        break;
                                    } else if (copyState.equalsIgnoreCase("FAILED")) {
                                        logger.info("Possibly failed to copy file to account");
                                        break;
                                    }
                                    sleep(i * 1000l, link);
                                }
                            } catch (final Throwable e) {
                                logger.warning("transfer-status: Unknown failure occured");
                            }
                        } else {
                            logger.info("transfer-status: Cannot check - Failed to find oid");
                        }
                    } else {
                        logger.info("given/stored internal filepath: " + internal_file_path);
                    }
                    dllink = getDllinkFromFileInAccount(link, br);
                    if (dllink == null) {
                        if (file_was_moved_before) {
                            /*
                             * This could happen e.g. if the user has deleted or renamed the file --> Internal path changes --> We're not
                             * able to find it under the stored path
                             */
                            logger.info("Previously stored internal path was probably wrong --> Retrying");
                            link.setProperty("path_internal", Property.NULL);
                            throw new PluginException(LinkStatus.ERROR_RETRY, "Bad internal path");
                        }
                        logger.warning("MoveFileIntoAccount: Fatal failure");
                        /*
                         * Possible errors (which should never occur: "id":"HTTP_404 == File could not be found in the account --> Probably
                         * move handling failed or is broken
                         */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            if (getPluginConfig().getBooleanProperty(DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, false)) {
                /*
                 * It sounds crazy but we actually 'delete' the previously moved file before starting the download as cached links last long
                 * enough for us to download it PLUS this way we do not waste space on the users' account :)
                 */
                moveFileToTrash(link);
                emptyTrash();
            }
            boolean resume = ACCOUNT_FREE_RESUME;
            int maxchunks = ACCOUNT_FREE_MAXCHUNKS;
            if (link.getBooleanProperty(DiskYandexNet.NORESUME, false)) {
                logger.info("Resume is disabled for this try");
                resume = false;
                link.setProperty(DiskYandexNet.NORESUME, Boolean.valueOf(false));
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors(link);
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // final String server_filename = getFileNameFromHeader(dl.getConnection());
            // if (server_filename != null) {
            // link.setFinalFileName(server_filename.replace("+", " "));
            // }
            link.setProperty("directlink_account", dllink);
            dl.startDownload();
        }
    }

    private boolean downloadableViaAccountOnly(final DownloadLink dl) {
        return dl.getBooleanProperty("premiumonly", false);
    }

    private boolean isPartOfAFolder(final DownloadLink dl) {
        return dl.getBooleanProperty("is_part_of_a_folder", false);
    }

    @SuppressWarnings("deprecation")
    private void handleServerErrors(final DownloadLink link) throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(DiskYandexNet.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    private String getDllinkFromFileInAccount(final DownloadLink dl, final Browser br2) {
        final String filepath = getInternalFilePath(dl);
        if (filepath == null) {
            logger.info("Debug-info: filepath == null, can't throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); here");
            return null;
        }
        String dllink = null;
        final boolean newWay = true;
        if (newWay) {
            /* 2018-04-18 */
            try {
                postPage("https://" + getCurrentDomain() + "/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=" + Encoding.urlEncode(filepath) + "&idClient=" + CLIENT_ID + "&version=" + VERSION_YANDEX_FILES + "&sk=" + this.ACCOUNT_SK);
                dllink = siteGetDllink(dl);
            } catch (final Throwable e) {
                logger.warning("Failed to create dllink of link in account - Exception!");
            }
        } else {
            for (final String downloaddir : downloaddirs) {
                try {
                    br.setFollowRedirects(false);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Referer", "https://" + getCurrentDomain() + "/client/disk/Downloads");
                    postPage("https://" + getCurrentDomain() + "/models/?_m=do-get-resource-subresources,do-get-resource-url", "_model.0=do-get-resource-url&id.0=%2Fdisk%2F" + downloaddir + "%2F" + filepath + "&idClient=" + CLIENT_ID + "&version=" + VERSION_YANDEX_FILES + "&sk=" + this.ACCOUNT_SK);
                    /* 28 = file not found, 70 = folder not found */
                    if (br.containsHTML("\"code\":28") || br.containsHTML("\"code\":70")) {
                        logger.info("getLinkFromFileInAccount: Moved file was not found in directory: " + downloaddir);
                        continue;
                    }
                    dllink = br.getRedirectLocation();
                    if (dllink == null) {
                        dllink = siteGetDllink(dl);
                    }
                    br.setFollowRedirects(true);
                    /* Fix links - cookies sit on the other domain */
                    if (dllink != null) {
                        dllink = dllink.replace("disk.yandex.ru/", "disk.yandex.com/");
                    }
                    break;
                } catch (final Throwable e) {
                    logger.warning("Failed to create dllink of link in account - Exception!");
                    break;
                }
            }
        }
        return dllink;
    }

    private void moveFileToTrash(final DownloadLink dl) {
        final String filepath = getInternalFilePath(dl);
        if (!StringUtils.isEmpty(filepath)) {
            logger.info("Trying to move file to trash: " + filepath);
            final boolean newWay = true;
            if (newWay) {
                /* 2018-04-18 */
                try {
                    postPage("https://" + getCurrentDomain() + "/models/?_m=do-resource-delete", "_model.0=do-resource-delete&id.0=" + Encoding.urlEncode(filepath) + "&idClient=" + CLIENT_ID + "&sk=" + this.ACCOUNT_SK + "&version=" + VERSION_YANDEX_FILES);
                    final String error = PluginJSonUtils.getJson(br, "error");
                    if (!StringUtils.isEmpty(error)) {
                        logger.info("Possible failure on moving file into trash");
                    } else {
                        logger.info("Successfully moved file into trash");
                    }
                } catch (final Throwable e) {
                    logger.warning("Failed to move file to trash - Exception!");
                }
            } else {
                for (final String downloaddir : downloaddirs) {
                    try {
                        postPage("https://" + getCurrentDomain() + "/models/?_m=do-resource-delete", "_model.0=do-resource-delete&id.0=%2Fdisk%2F" + downloaddir + "%2F" + filepath + "&idClient=" + CLIENT_ID + "&sk=" + this.ACCOUNT_SK + "&version=" + VERSION_YANDEX_FILES);
                        /* 28 = file not found, 70 = folder not found */
                        if (br.containsHTML("\"code\":28") || br.containsHTML("\"code\":70")) {
                            logger.info("moveFileToTrash: ");
                            continue;
                        }
                        logger.info("Successfully moved file to trash");
                        break;
                    } catch (final Throwable e) {
                        logger.warning("Failed to move file to trash - Exception!");
                        break;
                    }
                }
            }
        } else {
            logger.info("Cannot move any file to trash as there is no stored internal path available");
        }
    }

    private String siteGetDllink(final DownloadLink dl) {
        String dllink = PluginJSonUtils.getJsonValue(br, "file");
        if (!StringUtils.isEmpty(dllink) && dllink.startsWith("//")) {
            /* 2018-04-18 */
            dllink = "https:" + dllink;
        }
        return dllink;
    }

    /**
     * Returns URL-encoded internal path for download/move/file POST-requests. This is the path which a specific file has inside an account.
     */
    private String getInternalFilePath(final DownloadLink dl) {
        String filepath = null;
        final boolean newWay = true;
        if (newWay) {
            /* 2018-04-18: New */
            filepath = dl.getStringProperty("path_internal", null);
        } else {
            final String plain_filename = dl.getStringProperty("plain_filename", null);
            filepath = Encoding.urlEncode(plain_filename);
            if (filepath == null) {
                logger.info("Debug-info: filepath == null, can't throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); here");
            }
        }
        return filepath;
    }

    /** Deletes all items inside users' Yandex trash folder. */
    private void emptyTrash() {
        try {
            logger.info("Trying to empty trash");
            postPage("https://" + getCurrentDomain() + "/models/?_m=do-clean-trash", "_model.0=do-clean-trash&idClient=" + CLIENT_ID + "&sk=" + this.ACCOUNT_SK + "&version=" + VERSION_YANDEX_FILES);
            logger.info("Successfully emptied trash");
        } catch (final Throwable e) {
            logger.warning("Failed to empty trash");
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    // private String diskGetID0Public(final DownloadLink dl) {
    // final String hash = this.currHash.replace("/", "_").replace("+", "-");
    // final String path = this.currPath;
    // final String postValue;
    // if (isPartOfAFolder(dl)) {
    // postValue = "%2Fpublic%2F" + Encoding.urlEncode(hash + ":" + path);
    // } else {
    // postValue = "%2Fpublic%2F" + Encoding.urlEncode(hash);
    // }
    // return postValue;
    // }
    private String diskGetID0(final DownloadLink dl) {
        final String hash = getRawHash(dl);
        final String path = this.getPath(dl);
        final String id0;
        if (isPartOfAFolder(dl)) {
            id0 = hash + ":" + path;
        } else {
            id0 = hash;
        }
        return id0;
    }

    private String albumGetID0(final DownloadLink dl) {
        final String hash_long = getRawHash(dl);
        final String id = albumGetID(dl);
        if (StringUtils.isEmpty(hash_long)) {
            /* This should never happen */
            return null;
        }
        return String.format("/album/%s:%s", hash_long, id);
    }

    private String albumGetID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), "/([a-f0-9]+)$").getMatch(0);
    }

    /** e.g. 'client365485934985' */
    public static String albumGetIdClient() {
        return "undefined" + System.currentTimeMillis();
    }

    public static String getHashLongFromHTML(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "public_key");
    }

    public static String getSK(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    /** Gets new 'SK' value via '/auth/status' request. */
    public static String getNewSK(final Browser br, final String domain, final String sourceURL) throws IOException {
        br.getPage("https://" + domain + "/auth/status?urlOrigin=" + Encoding.urlEncode(sourceURL) + "&source=album_web_signin");
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    private Browser prepbrWebsite(final Browser br) {
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.setFollowRedirects(true);
        return br;
    }

    public static Browser prepbrAPI(final Browser br) {
        return br;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private void postPage(final String url, final String data) throws Exception {
        br.postPage(url, data);
        if (br.containsHTML("\"id\":\"WRONG_SK\"")) {
            logger.info("Refreshing ACCOUNT_SK");
            this.login(this.currAcc, true);
        }
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private void postPageRaw(final String url, final String data) throws Exception {
        br.postPageRaw(url, data);
        if (br.containsHTML("\"id\":\"WRONG_SK\"")) {
            logger.info("Refreshing ACCOUNT_SK");
            this.login(this.currAcc, true);
        }
    }

    @Override
    public String getDescription() {
        return "JDownloader's Yandex.ru Plugin helps downloading files from Yandex.ru. It provides some settings for downloads via account.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Account settings:"));
        final ConfigEntry moveFilesToAcc = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), MOVE_FILES_TO_ACCOUNT, JDL.L("plugins.hoster.DiskYandexNet.MoveFilesToAccount", "1. Move files to account before downloading them to get higher download speeds?")).setDefaultValue(false);
        getConfig().addEntry(moveFilesToAcc);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, JDL.L("plugins.hoster.DiskYandexNet.EmptyTrashAfterSuccessfulDownload", "2. Delete moved files & empty trash after downloadlink-generation?")).setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}