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
import java.util.Map;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.BiqleRuConfig;
import org.jdownloader.plugins.components.config.BiqleRuConfig.Quality;
import org.jdownloader.plugins.components.config.BiqleRuConfig.QualitySelectionMode;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "biqle.ru", "daxab.com", "divxcim.com", "daftsex.com", "artsporn.com" }, urls = { "https?://(?:www\\.)?biqle\\.(com|ru|org)/watch/(?:-)?\\d+_\\d+", "https?://(?:www\\.)?(daxab\\.com|dxb\\.to)/embed/(?:\\-)?\\d+_\\d+", "https?://(?:www\\.)?divxcim\\.com/video_ext\\.php\\?oid=(?:\\-)?\\d+\\&id=\\d+", "https?://(?:www\\.)?daftsex\\.com/watch/(?:-)?\\d+_\\d+", "https?://(?:www\\.)?artsporn\\.com/watch/(?:-)?\\d+_\\d+" })
public class BiqleRu extends PluginForDecrypt {
    public BiqleRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Converts embedded crap to vk.com video-urls. */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if ("biqle.ru".equals(getHost()) || "daftsex.com".equals(getHost()) || "artsporn.com".equals(getHost())) {
            final String decryptedhost = "biqledecrypted://";
            br.getPage(param.getCryptedUrl());
            br.followRedirect();
            if (br.getHttpConnection().getResponseCode() == 404) {
                ret.add(this.createOfflinelink(param.toString()));
                return ret;
            }
            final String title = Encoding.htmlOnlyDecode(br.getRegex("<title>\\s*(.*?)\\s*(—\\s*BIQLE.*?)?</title>").getMatch(0));
            FilePackage fp = null;
            if (title != null) {
                fp = FilePackage.getInstance();
                fp.setName(title);
            }
            final String DaxabPlayer = br.getRegex("DaxabPlayer\\.init\\(\\s*(\\{.*?\\})\\s*\\)\\s*;\\s*\\}\\s*</script").getMatch(0);
            String daxab = null;
            if (DaxabPlayer != null) {
                final String id = new Regex(param.getCryptedUrl(), "(-?\\d+_\\d+)").getMatch(0);
                final String hash = new Regex(DaxabPlayer, "id\\s*:\\s*'video" + id + "'.*?hash\\s*:\\s*\"(.*?)\"").getMatch(0);
                if (hash != null) {
                    daxab = "https://daxab.com/player/" + hash;
                }
            }
            if (daxab == null) {
                daxab = br.getRegex("((?:https?:)?//(?:daxab\\.com|dxb\\.to)/player/[a-zA-Z0-9_\\-]+)").getMatch(0);
            }
            if (daxab != null) {
                /* TODO: Apply quality setting also for other types of videos e.g. externally hosted/vk */
                final String userPreferredQuality = getUserPreferredqualityStr();
                final Map<String, DownloadLink> qualityMap = new HashMap<String, DownloadLink>();
                DownloadLink best = null;
                int highestQualityInt = -1;
                final Browser brc = br.cloneBrowser();
                sleep(1000, param);
                brc.getPage(daxab);
                if (brc.containsHTML("cdn_files")) {
                    final String server = Base64.decodeToString(new StringBuilder(brc.getRegex("server\\s*:\\s*\"(.*?)\"").getMatch(0)).reverse().toString());
                    final String cdn_id = brc.getRegex("cdn_id\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String cdn_filesString = brc.getRegex("cdn_files\\s*:\\s*(\\{.*?\\})").getMatch(0);
                    final Map<String, Object> cdn_files = JSonStorage.restoreFromString(cdn_filesString, TypeRef.HASHMAP);
                    for (Entry<String, Object> cdn_file : cdn_files.entrySet()) {
                        if (cdn_file.getKey().startsWith("mp4")) {
                            String heightStr = new Regex(cdn_file, "mp4_(\\d+)").getMatch(0);
                            if (heightStr == null) {
                                heightStr = "";
                            }
                            String fileName = (String) cdn_file.getValue();
                            fileName = fileName.replace(".", ".mp4?extra=");
                            final DownloadLink dl = createDownloadlink(decryptedhost + server + "/videos/" + cdn_id.replace("_", "/") + "/" + fileName);
                            if (title != null) {
                                dl.setFinalFileName(title + "_" + heightStr + ".mp4");
                            } else {
                                dl.setFinalFileName(cdn_id + "_" + heightStr + ".mp4");
                            }
                            if (fp != null) {
                                dl._setFilePackage(fp);
                            }
                            dl.setContainerUrl(param.getCryptedUrl());
                            if (!heightStr.isEmpty()) {
                                final int height = Integer.parseInt(heightStr);
                                if (height > highestQualityInt) {
                                    highestQualityInt = height;
                                    best = dl;
                                }
                                qualityMap.put(heightStr + "p", dl);
                            } else if (best == null) {
                                /* Assume video without quality modifier is BEST. */
                                best = dl;
                            }
                        }
                    }
                } else {
                    final String server = Base64.decodeToString(new StringBuilder(brc.getRegex("server\\s*:\\s*\"(.*?)\"").getMatch(0)).reverse().toString());
                    final String accessToken = brc.getRegex("access_token\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String videoId = brc.getRegex("id\\s*:\\s*\"(.*?)\"").getMatch(0);
                    // final String sig = brc.getRegex("sig\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String credentials = brc.getRegex("credentials\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String cKey = brc.getRegex("c_key\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String partialSig = brc.getRegex("\"sig\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String partialQualityString = brc.getRegex("\"quality\"\\s*:\\s*(\\{.*?\\})").getMatch(0);
                    final Map<String, Object> partialQuality = JSonStorage.restoreFromString(partialQualityString, TypeRef.HASHMAP);
                    final UrlQuery query = new UrlQuery();
                    query.add("token", accessToken);
                    query.add("videos", videoId);
                    query.add("ckey", cKey);
                    query.add("credentials", credentials);
                    final String path;
                    if (partialSig != null) {
                        path = "sig";
                        query.add("sig", partialSig);
                    } else {
                        path = "get";
                    }
                    String url = String.format("//%s/method/video.%s/%s", server, path, videoId);
                    url += "?" + query.toString();
                    brc.getPage(url);
                    if (brc.getHttpConnection().getResponseCode() == 404) {
                        ret.add(createOfflinelink(param.getCryptedUrl()));
                        return ret;
                    }
                    String titleName = PluginJSonUtils.getJsonValue(brc, "title");
                    if (titleName == null) {
                        titleName = title;
                    }
                    final String jsonFilesString = brc.getRegex("\"files\"\\s*:\\s*(\\{.*?\\})").getMatch(0);
                    final Map<String, Object> jsonFiles = JSonStorage.restoreFromString(jsonFilesString, TypeRef.HASHMAP);
                    for (Entry<String, Object> jsonFile : jsonFiles.entrySet()) {
                        if (jsonFile.getKey().startsWith("mp4")) {
                            String resolution = new Regex(jsonFile, "mp4_(\\d+)").getMatch(0);
                            String fileUrl = (String) jsonFile.getValue();
                            int insertPos = fileUrl.indexOf("://") + 3;
                            StringBuilder sb = new StringBuilder(fileUrl);
                            sb.insert(insertPos, server + "/");
                            sb.append(String.format("&extra_key=%s&videos=%s", partialQuality.get(resolution), videoId));
                            final DownloadLink dl = createDownloadlink(sb.toString().replaceAll("https?://", decryptedhost));
                            dl.setFinalFileName(titleName + "_" + resolution + ".mp4");
                            dl.setContainerUrl(param.getCryptedUrl());
                            if (!resolution.isEmpty()) {
                                final int height = Integer.parseInt(resolution);
                                if (height > highestQualityInt) {
                                    highestQualityInt = height;
                                    best = dl;
                                }
                                qualityMap.put(resolution + "p", dl);
                            } else if (best == null) {
                                /* Assume video without quality modifier is BEST. */
                                best = dl;
                            }
                        }
                    }
                }
                if (qualityMap.isEmpty() && best == null) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final QualitySelectionMode mode = PluginJsonConfig.get(BiqleRuConfig.class).getQualitySelectionMode();
                    if (mode == QualitySelectionMode.BEST) {
                        ret.add(best);
                    } else if (mode == QualitySelectionMode.SELECTED_ONLY) {
                        if (qualityMap.containsKey(userPreferredQuality)) {
                            logger.info("Adding user preferred quality: " + userPreferredQuality);
                            ret.add(qualityMap.get(userPreferredQuality));
                        } else {
                            logger.info("Adding best quality because user selected was not found");
                            ret.add(best);
                        }
                    } else {
                        /* Add ALL existant qualities */
                        for (final Entry<String, DownloadLink> entry : qualityMap.entrySet()) {
                            ret.add(entry.getValue());
                        }
                    }
                    return ret;
                }
            } else {
                // vk mode
                final String daxabExt = br.getRegex("((?:https?:)?//(?:daxab\\.com|dxb\\.to)/ext\\.php\\?oid=[0-9\\-]+&id=\\d+&hash=[a-zA-Z0-9]+)\"").getMatch(0);
                final Browser brc = br.cloneBrowser();
                brc.getPage(daxabExt);
                String escapeString = brc.getRegex("unescape\\('([^']+)'").getMatch(0);
                escapeString = Encoding.htmlDecode(escapeString);
                String base64String = new Regex(escapeString, "base64,(.+?)\"").getMatch(0);
                String decodeString = Encoding.Base64Decode(base64String);
                String action = new Regex(decodeString, "action=\"([^\"]+)\"").getMatch(0);
                String oid = new Regex(decodeString, "oid\" value=\"([^\"]+)\"").getMatch(0);
                String id = new Regex(decodeString, "id\" value=\"([^\"]+)\"").getMatch(0);
                String hash = new Regex(decodeString, "hash\" value=\"([^\"]+)\"").getMatch(0);
                String reqUrl = String.format("https:%s?oid=%s&id=%s&hash=%s&autoplay=0", action, oid, id, hash);
                final DownloadLink downloadLink = createDownloadlink(reqUrl);
                downloadLink.setFinalFileName(title + ".mp4");
                downloadLink.setContainerUrl(param.getCryptedUrl());
                ret.add(downloadLink);
                return ret;
            }
        } else {
            final String parameter = param.toString();
            final String videoid_part;
            if (parameter.matches("https?://(?:www\\.)?divxcim\\.com/video_ext\\.php\\?oid=(?:\\-)?\\d+\\&id=\\d+")) {
                final String oid = new Regex(parameter, "oid=((?:\\-)?\\d+)").getMatch(0);
                final String id = new Regex(parameter, "id=(\\d+)").getMatch(0);
                videoid_part = oid + "_:" + id;
            } else {
                videoid_part = new Regex(parameter, "((?:\\-)?\\d+_\\d+)").getMatch(0);
            }
            final String finallink = "https://vk.com/video" + videoid_part;
            ret.add(createDownloadlink(finallink));
        }
        return ret;
    }

    private String getUserPreferredqualityStr() {
        final Quality quality = PluginJsonConfig.get(BiqleRuConfig.class).getPreferredQuality();
        switch (quality) {
        case Q240:
            return "240p";
        case Q360:
            return "360p";
        case Q480:
            return "480p";
        case Q720:
            return "720p";
        case Q1080:
            return "1080p";
        default:
            /* Should never happen */
            return null;
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return BiqleRuConfig.class;
    }
}
