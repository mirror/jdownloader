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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.RumbleComConfig;
import org.jdownloader.plugins.components.config.RumbleComConfig.Quality;
import org.jdownloader.plugins.components.config.RumbleComConfig.QualitySelectionMode;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RumbleCom extends PluginForDecrypt {
    public RumbleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "rumble.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[^/]+\\.html|embedJS/[a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_EMBED  = "https?://[^/]+/embedJS/([a-z0-9]+)";
    private static final String TYPE_NORMAL = "https?://[^/]+/([^/]+)\\.html";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String videoID;
        if (param.getCryptedUrl().matches(TYPE_EMBED)) {
            videoID = new Regex(param.getCryptedUrl(), TYPE_EMBED).getMatch(0);
        } else {
            br.getPage(param.getCryptedUrl());
            videoID = br.getRegex("\"video\":\"([a-z0-9]+)\"").getMatch(0);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (videoID == null) {
                logger.info("Failed to find any downloadable content");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final RumbleComConfig cfg = PluginJsonConfig.get(RumbleComConfig.class);
        br.getPage("https://" + this.getHost() + "/embedJS/u3/?request=video&v=" + videoID);
        /* Double-check for offline content */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        String dateFormatted = null;
        final String dateStr = (String) root.get("pubDate");
        if (!StringUtils.isEmpty(dateStr)) {
            dateFormatted = new Regex(dateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        }
        final String uploaderName = (String) JavaScriptEngineFactory.walkJson(root, "author/name");
        String title = (String) root.get("title");
        String baseTitle = title;
        if (StringUtils.isEmpty(baseTitle)) {
            /* Fallback */
            baseTitle = videoID;
        }
        if (!StringUtils.isEmpty(uploaderName)) {
            baseTitle = uploaderName + " - " + baseTitle;
        }
        if (dateFormatted != null) {
            baseTitle = dateFormatted + "_" + baseTitle;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(baseTitle);
        final Map<String, Object> videoInfo = (Map<String, Object>) root.get("ua");
        final Iterator<Entry<String, Object>> iterator = videoInfo.entrySet().iterator();
        final QualitySelectionMode mode = cfg.getQualitySelectionMode();
        int bestQualityHeight = 0;
        DownloadLink best = null;
        int worstQualityHeight = 10000;
        DownloadLink worst = null;
        final int preferredHeight = getUserPreferredqualityHeight();
        DownloadLink selectedQuality = null;
        while (iterator.hasNext()) {
            final Entry<String, Object> entry = iterator.next();
            final String qualityHeightStr = entry.getKey();
            final int qualityHeight = Integer.parseInt(qualityHeightStr);
            final List<Object> qualityInfoArray = (List<Object>) entry.getValue();
            final String url = (String) qualityInfoArray.get(0);
            if (StringUtils.isEmpty(url) || StringUtils.isEmpty(qualityHeightStr)) {
                /* Skip invalid items */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink(url);
            /* Set this so when user copies URL of any video quality he'll get the URL to the main video. */
            dl.setContentUrl(param.getCryptedUrl());
            dl.setForcedFileName(baseTitle + "_" + qualityHeightStr + ".mp4");
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            if (qualityHeight > bestQualityHeight) {
                bestQualityHeight = qualityHeight;
                best = dl;
            }
            if (qualityHeight < worstQualityHeight) {
                worstQualityHeight = qualityHeight;
                worst = dl;
            }
            if (qualityHeight == preferredHeight) {
                selectedQuality = dl;
            }
            ret.add(dl);
        }
        if (mode == QualitySelectionMode.WORST && worst != null) {
            ret.clear();
            ret.add(worst);
            return ret;
        } else if (mode == QualitySelectionMode.BEST && best != null) {
            ret.clear();
            ret.add(best);
            return ret;
        } else if (mode == QualitySelectionMode.SELECTED_ONLY && selectedQuality != null) {
            ret.clear();
            ret.add(selectedQuality);
            return ret;
        } else {
            /* Return all if wanted by user and also as fallback. */
            return ret;
        }
    }

    private int getUserPreferredqualityHeight() throws PluginException {
        final Quality quality = PluginJsonConfig.get(RumbleComConfig.class).getPreferredQuality();
        switch (quality) {
        case Q240:
            return 240;
        case Q360:
            return 360;
        case Q480:
            return 480;
        case Q720:
            return 720;
        case Q1080:
            return 1080;
        default:
            /* Should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return RumbleComConfig.class;
    }
}
