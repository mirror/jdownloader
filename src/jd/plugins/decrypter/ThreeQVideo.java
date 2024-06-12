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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.ThreeQVideoConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ThreeQVideo extends PluginForDecrypt {
    public ThreeQVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "playout.3qsdn.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://" + buildHostsPatternPart(domains) + "/(?:(?:config|embed)/)?([a-f0-9\\-]+(\\?[^/]+)?)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (contentID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept", "*/*");
        try {
            final String refererURL = param.getDownloadLink().getReferrerUrl();
            /*
             * 2023-09-04: Origin header is required for some items. If it is missing but required, responsecode down below will be code
             * 400.
             */
            br.getHeaders().put("Origin", "https://" + Browser.getHost(refererURL, false));
        } catch (final Exception ignore) {
        }
        br.getPage("https://" + this.getHost() + "/config/" + contentID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String contentType = root.get("streamContent").toString();
        if (StringUtils.equalsIgnoreCase(contentType, "live")) {
            logger.info("Livestreams are not supported");
            throw new DecrypterRetryException(RetryReason.UNSUPPORTED_LIVESTREAM);
        }
        final String title = root.get("title").toString();
        final String description = (String) root.get("description");
        final String date = root.get("upload_date").toString();
        final String dateFormatted = new Regex(date, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(dateFormatted + "_" + title);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        final ThreeQVideoConfig cfg = PluginJsonConfig.get(ThreeQVideoConfig.class);
        /* Crawl audio/video items */
        final Map<String, Object> sources = (Map<String, Object>) root.get("sources");
        final List<Map<String, Object>> sourcesProgressive = (List<Map<String, Object>>) sources.get("progressive");
        int qualityValueMax = -1;
        DownloadLink maxQuality = null;
        final Map<String, Integer> audioqualitymap = new HashMap<String, Integer>();
        audioqualitymap.put("audio/mp3", 100);
        audioqualitymap.put("audio/ogg", 200);
        audioqualitymap.put("audio/aac", 300);
        for (final Map<String, Object> sourceProgressive : sourcesProgressive) {
            final String directurl = sourceProgressive.get("src").toString();
            String ext = null;
            final String mimetype = (String) sourceProgressive.get("type");
            if (mimetype != null) {
                ext = getExtensionFromMimeType(mimetype);
            }
            if (ext != null) {
                ext = "." + ext;
            } else {
                /* Fallback 1 */
                ext = Plugin.getFileNameExtensionFromURL(directurl);
            }
            if (ext == null) {
                /* Fallback 2 */
                ext = ".mp4";
            }
            final int height = ((Number) sourceProgressive.get("height")).intValue();
            final String filename = dateFormatted + "_" + title + "_" + height + "p" + ext;
            final DownloadLink media = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
            media.setFinalFileName(filename);
            media.setProperty(DirectHTTP.FIXNAME, filename);
            media.setAvailable(true);
            media._setFilePackage(fp);
            int valueForQualitySelection = 0;
            if (height > 0) {
                valueForQualitySelection = height;
            } else if (audioqualitymap.containsKey(mimetype)) {
                valueForQualitySelection = audioqualitymap.get(mimetype);
            }
            if (valueForQualitySelection > qualityValueMax) {
                qualityValueMax = valueForQualitySelection;
                maxQuality = media;
            }
            ret.add(media);
        }
        /* Check if user wants best quality only. */
        if (cfg.isOnlyGrabBestQuality()) {
            /* Clear list of collected items */
            ret.clear();
            /* Add best quality only */
            ret.add(maxQuality);
        }
        return ret;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ThreeQVideoConfig.class;
    }
}
