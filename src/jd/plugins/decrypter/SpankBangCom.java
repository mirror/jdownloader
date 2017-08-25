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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spankbang.com" }, urls = { "https?://(www\\.)?([a-z]{2}\\.)?spankbang\\.com/([a-z0-9]+/video/\\?quality=[\\w\\d]+|[a-z0-9]+/(?:video|embed)/)" })
public class SpankBangCom extends PluginForDecrypt {
    public SpankBangCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String           DOMAIN         = "spankbang.com";
    private LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
    private String                        parameter      = null;
    /** Settings stuff */
    private static final String           FASTLINKCHECK  = "FASTLINKCHECK";
    private static final String           ALLOW_BEST     = "ALLOW_BEST";
    private static final String           ALLOW_240p     = "ALLOW_240p";
    private static final String           ALLOW_480p     = "ALLOW_480p";
    private static final String           ALLOW_720p     = "ALLOW_720p";
    private static final String           ALLOW_1080p    = "ALLOW_1080p";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private void getPage(final String page) throws Exception {
        final PluginForHost plugin = JDUtilities.getPluginForHost("spankbang.com");
        if (plugin == null) {
            throw new IllegalStateException("Plugin not found!");
        }
        // set cross browser support
        ((jd.plugins.hoster.SpankBangCom) plugin).setBrowser(br);
        ((jd.plugins.hoster.SpankBangCom) plugin).getPage(page);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean fastcheck = cfg.getBooleanProperty(FASTLINKCHECK, false);
        parameter = param.toString().replaceAll("http://(www\\.)?([a-z]{2}\\.)?spankbang\\.com/", "http://spankbang.com/").replace("/embed/", "/video/");
        br.setFollowRedirects(true);
        br.setCookie("http://spankbang.com/", "country", "GB");
        br.getHeaders().put("Accept-Language", "en");
        getPage(parameter);
        logger.info(br.toString());
        if (isOffline(this.br)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Decrypt start */
        final FilePackage fp = FilePackage.getInstance();
        /* Decrypt qualities START */
        String title = br.getRegex("<title>([^<>\"]*?)( free HD Porn Video)? - SpankBang.*?</title>").getMatch(0);
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
        boolean q480p = cfg.getBooleanProperty(ALLOW_480p, true);
        boolean q720p = cfg.getBooleanProperty(ALLOW_720p, true);
        boolean q1080p = cfg.getBooleanProperty(ALLOW_1080p, true);
        if (!q240p && !q480p && !q720p && !q1080p) {
            // user has made error and disabled them all, so we will treat as all enabled.
            q240p = true;
            q480p = true;
            q720p = true;
            q1080p = true;
        }
        final boolean best = cfg.getBooleanProperty(ALLOW_BEST, true);
        // needs to be in reverse order
        if (q1080p) {
            selectedQualities.add("1080p");
        }
        if (q720p) {
            selectedQualities.add("720p");
        }
        if (q480p) {
            selectedQualities.add("480p");
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
                final DownloadLink dl = createDownloadlink("http://spankbangdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
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

    public static LinkedHashMap<String, String> findQualities(final Browser br, final String source_url) throws DecrypterException, PluginException {
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        final String fid = getFid(source_url);
        final String streamkey = br.getRegex("var stream_key  = \\'([^<>\"]*?)\\'").getMatch(0);
        // qualities 'super = 1080p', 'high = 720p', 'medium = 480p', 'low = 240p' they do this in javascript
        String[] qualities = br.getRegex("class=\"q_(\\w+)\"").getColumn(0);
        if (qualities == null || qualities.length == 0) {
            // this is typically within <source
            final String source = br.getRegex("<source src=\"(.*?)\"").getMatch(0);
            if (source != null) {
                qualities = new String[1];
                qualities[0] = parseSingleQuality(source);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (qualities == null || qualities.length == 0 || streamkey == null) {
            return null;
        }
        for (final String q : qualities) {
            final String quality = getQuality(q);
            final String directlink = "http://spankbang.com/_" + fid + "/" + streamkey + "/title/" + quality + "__mp4";
            foundQualities.put(quality, directlink);
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
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">this video is (no longer available|private|under review)|>este vídeo já não está disponível") || !br.getURL().contains("/video");
    }

    /**
     * 'super = 1080p', 'high = 720p', 'medium = 480p', 'low = 240p' they do this in javascript
     *
     * @param q
     * @return
     * @throws DecrypterException
     */
    public static String getQuality(final String q) throws DecrypterException {
        if ("super".equalsIgnoreCase(q)) {
            return "1080p";
        } else if ("high".equalsIgnoreCase(q)) {
            return "720p";
        } else if ("medium".equalsIgnoreCase(q)) {
            return "480p";
        } else if ("low".equalsIgnoreCase(q)) {
            return "240p";
        }
        throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}