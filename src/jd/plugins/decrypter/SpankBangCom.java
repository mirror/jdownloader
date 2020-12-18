//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spankbang.com" }, urls = { "https?://(?:www\\.)?(?:[a-z]{2}\\.)?spankbang\\.com/(?:[a-z0-9]+/video/\\?quality=[\\w\\d]+|[a-z0-9]+/(?:video|embed)/([^/]+)?)" })
public class SpankBangCom extends PluginForDecrypt {
    public SpankBangCom(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 250);
    }

    private static final String           DOMAIN         = "spankbang.com";
    private LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
    private String                        parameter      = null;
    /** Settings stuff */
    private static final String           FASTLINKCHECK  = "FASTLINKCHECK";
    private static final String           ALLOW_BEST     = "ALLOW_BEST";
    private static final String           ALLOW_240p     = "ALLOW_240p";
    private static final String           ALLOW_320p     = "ALLOW_320p";
    private static final String           ALLOW_480p     = "ALLOW_480p";
    private static final String           ALLOW_720p     = "ALLOW_720p";
    private static final String           ALLOW_1080p    = "ALLOW_1080p";
    private static final String           ALLOW_4k       = "ALLOW_4k";
    private PluginForHost                 plugin         = null;

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private void getPage(final String page) throws Exception {
        if (plugin == null) {
            plugin = getNewPluginForHostInstance(getHost());
            if (plugin == null) {
                throw new IllegalStateException("Plugin not found!");
            }
        }
        ((jd.plugins.hoster.SpankBangCom) plugin).getPage(page);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean fastcheck = cfg.getBooleanProperty(FASTLINKCHECK, true);
        parameter = param.toString().replace("/embed/", "/video/");
        br.setFollowRedirects(true);
        /* www = English language */
        br.setCookie(this.getHost(), "language", "www");
        br.getHeaders().put("Accept-Language", "en");
        br.setAllowedResponseCodes(new int[] { 503 });
        getPage(parameter);
        logger.info(br.toString());
        if (isOffline(this.br)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        } else if (isPrivate(this.br)) {
            decryptedLinks.add(createOfflinelink(parameter, "PRIVATE_VIDEO"));
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 503) {
            logger.info("Server error 503: Cannot crawl new URLs at the moment");
            decryptedLinks.add(createOfflinelink(parameter, "SERVER_ERROR_503"));
            return decryptedLinks;
        }
        /* Decrypt start */
        final FilePackage fp = FilePackage.getInstance();
        /* Decrypt qualities START */
        /* 2020-05-11: Prefer filenames from inside URL as they are always 'good'. */
        String title = br.getRegex("\"name\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        if (title == null) {
            title = br.getRegex("<meta\\s*name\\s*=\\s*\"twitter:description\"\\s*content\\s*=\\s*\"(?:\\s*Watch\\s*)?([^<>\"]+)\\s+on SpankBang now").getMatch(0);
            if (title == null) {
                title = br.getRegex("<h1\\s*title\\s*=\\s*\"(.*?)\"").getMatch(0);
                if (title == null) {
                    title = new Regex(parameter, "/video/(.+)").getMatch(0);
                    title = Encoding.urlDecode(title, false);
                }
            }
        }
        final String fid = getFid(parameter);
        foundQualities = findQualities(this.br, parameter);
        if (foundQualities == null || foundQualities.size() == 0 || title == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        title = Encoding.htmlDecode(title.trim());
        fp.setName(title);
        /* Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        boolean q240p = cfg.getBooleanProperty(ALLOW_240p, true);
        boolean q320p = cfg.getBooleanProperty(ALLOW_320p, true);
        boolean q480p = cfg.getBooleanProperty(ALLOW_480p, true);
        boolean q720p = cfg.getBooleanProperty(ALLOW_720p, true);
        boolean q1080p = cfg.getBooleanProperty(ALLOW_1080p, true);
        boolean q4k = cfg.getBooleanProperty(ALLOW_4k, true);
        if (!q240p && !q320p && !q480p && !q720p && !q1080p) {
            // user has made error and disabled them all, so we will treat as all enabled.
            q240p = true;
            q320p = true;
            q480p = true;
            q720p = true;
            q1080p = true;
            q4k = true;
        }
        final boolean best = cfg.getBooleanProperty(ALLOW_BEST, true);
        // needs to be in reverse order
        if (q4k) {
            selectedQualities.add("4k");
        }
        if (q1080p) {
            selectedQualities.add("1080p");
        }
        if (q720p) {
            selectedQualities.add("720p");
        }
        if (q480p) {
            selectedQualities.add("480p");
        }
        if (q320p) {
            selectedQualities.add("320p");
        }
        if (q240p) {
            selectedQualities.add("240p");
        }
        String predefinedVariant = new Regex(param.getCryptedUrl(), "\\?quality=([\\w\\d]+)").getMatch(0);
        for (final String selectedQualityValue : selectedQualities) {
            // if quality marker is in the url. skip all others
            if (predefinedVariant != null && !predefinedVariant.equalsIgnoreCase(selectedQualityValue)) {
                continue;
            }
            final String directlink = foundQualities.get(selectedQualityValue);
            if (directlink != null) {
                final String finalname = title + "_" + selectedQualityValue + ".mp4";
                final DownloadLink dl = createDownloadlink("http://spankbangdecrypted.com/" + UniqueAlltimeID.create());
                dl.setFinalFileName(finalname);
                dl.setContentUrl("http://spankbang.com/" + fid + "/video/?quality=" + selectedQualityValue);
                if (fastcheck) {
                    dl.setAvailable(true);
                }
                dl.setLinkID("spankbangcom_" + fid + "_" + selectedQualityValue);
                dl.setProperty("plain_filename", finalname);
                dl.setProperty("plain_directlink", directlink);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("quality", selectedQualityValue);
                fp.add(dl);
                decryptedLinks.add(dl);
                if (best) {
                    break;
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected qualities were found, decrypting done...");
        }
        return decryptedLinks;
    }

    public static LinkedHashMap<String, String> findQualities(final Browser br, final String source_url) throws DecrypterException, PluginException, IOException {
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        // final String fid = getFid(source_url);
        final String dataStreamKey = br.getRegex("data-streamkey\\s*=\\s*\"(.*?)\"").getMatch(0);
        if (dataStreamKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String x_csrftoken = br.getCookie(br.getURL(), "sb_csrf_session");
        Request request = br.createPostRequest("/api/videos/stream", "data=0&id=" + dataStreamKey + "&sb_csrf_session=" + x_csrftoken);
        request.getHeaders().put("accept", "application/json, text/javascript, */*; q=0.01");
        request.getHeaders().put("x-requested-with", "XMLHttpRequest");
        if (x_csrftoken != null) {
            request.getHeaders().put("x-csrftoken", x_csrftoken);
        }
        final String page = br.getPage(request);
        final String[] qualities = new String[] { "4k", "1080p", "720p", "480p", "320p", "240p" };
        if (page.matches("(?s)^\\s*\\{.*") && page.matches("(?s).*\\}\\s*$")) {
            final Map<String, Object> map = JSonStorage.restoreFromString(page, TypeRef.HASHMAP);
            final String stream_url_m3u8 = String.valueOf(map.get("m3u8"));
            for (final String quality : qualities) {
                final String qualityID = getQuality(quality);
                final Object entry = map.get(quality);
                String value = null;
                if (entry instanceof String) {
                    value = (String) entry;
                } else if (entry instanceof List && ((List) entry).size() != 0) {
                    value = ((List<String>) entry).get(0);
                }
                System.out.println("value: " + value);
                if (StringUtils.isEmpty(value) && StringUtils.isNotEmpty(stream_url_m3u8)) {
                    final String one_stream_url_m3u8 = new Regex(stream_url_m3u8, "(http.*?d=\\d)").getMatch(0);
                    // System.out.println("one_stream_url_m3u8: " + one_stream_url_m3u8);
                    final String page_m3u8 = br.getPage(one_stream_url_m3u8);
                    final String[] single_url_m3u8s = new Regex(page_m3u8, "(http.*?d=\\d)\\s").getColumn(0);
                    // System.out.println("single_url_m3u8s.length: " + single_url_m3u8s.length);
                    for (final String single_url_m3u8 : single_url_m3u8s) {
                        // System.out.println("single_url_m3u8: " + single_url_m3u8);
                        if (single_url_m3u8.contains(quality)) {
                            value = single_url_m3u8;
                            System.out.println("value: " + value);
                        }
                    }
                } else {
                    // continue;
                }
                if (StringUtils.isNotEmpty(value)) {
                    foundQualities.put(qualityID, value);
                }
            }
        } else {
            // final String streamkey = br.getRegex("var stream_key = \\'([^<>\"]*?)\\'").getMatch(0);
            for (final String q : qualities) {
                final String quality = getQuality(q);
                // final String directlink = "http://spankbang.com/_" + fid + "/" + streamkey + "/title/" + quality + "__mp4";
                final String directlink = PluginJSonUtils.getJson(br, "stream_url_" + q);
                if (StringUtils.isEmpty(directlink)) {
                    continue;
                }
                foundQualities.put(quality, directlink);
            }
        }
        return foundQualities;
    }

    private static String parseSingleQuality(String source) {
        if (source == null) {
            return null;
        }
        /* 'super = 1080p', 'high = 720p', 'medium = 480p', 'low = 240p' they do this in javascript */
        if (source.contains("240p")) {
            return "low";
        } else if (source.contains("480p")) {
            return "medium";
        } else if (source.contains("hi")) {
            return "medium";
        } else if (source.contains("720p")) {
            return "high";
        } else if (source.contains("1080p")) {
            return "super";
        }
        return null;
    }

    public static String getFid(final String source_url) {
        return new Regex(source_url, "spankbang\\.com/([a-z0-9]+)/video/").getMatch(0);
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*this video is (no longer available|private|under review)|>\\s*este vídeo já não está disponível|video_removed_page") || !br.getURL().contains("/video");
    }

    public static boolean isPrivate(final Browser br) {
        return br.containsHTML("this video is private\\.?\\s*<");
    }

    /**
     * 'super = 1080p', 'high = 720p', 'medium = 480p', 'low = 240p' they do this in javascript
     *
     * @param q
     * @return
     * @throws DecrypterException
     */
    public static String getQuality(final String q) throws PluginException {
        if ("4k".equalsIgnoreCase(q)) {
            return "4k";
        } else if ("super".equalsIgnoreCase(q) || "1080p".equalsIgnoreCase(q)) {
            return "1080p";
        } else if ("high".equalsIgnoreCase(q) || "720p".equalsIgnoreCase(q)) {
            return "720p";
        } else if ("medium".equalsIgnoreCase(q) || "480p".equalsIgnoreCase(q)) {
            return "480p";
        } else if ("320p".equalsIgnoreCase(q)) {
            return "320p";
        } else if ("low".equalsIgnoreCase(q) || "240p".equalsIgnoreCase(q)) {
            return "240p";
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /**
     * JD2 CODE: DO NOT USE OVERRIDE FOR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}