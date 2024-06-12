//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abc.net.au" }, urls = { "https?://(?:www\\.)?abc\\.net\\.au/.+" })
public class AbcNtAu extends PluginForDecrypt {
    public AbcNtAu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String lastMediaTitle = null;
        final String json = br.getRegex("id=\"__NEXT_DATA__\" type=\"application/json\"[^>]*>(\\{.*?\\})</script>").getMatch(0);
        final HashSet<String> dupesMediaids = new HashSet<String>();
        if (json != null) {
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final ArrayList<Map<String, Object>> audiomaps = new ArrayList<Map<String, Object>>();
            this.findAudioMaps(entries, audiomaps);
            for (final Map<String, Object> audiomap : audiomaps) {
                if (!dupesMediaids.add(audiomap.get("id").toString())) {
                    continue;
                }
                final String title = audiomap.get("title").toString().trim();
                lastMediaTitle = title;
                final String caption = (String) audiomap.get("caption");
                final List<Map<String, Object>> renditions = (List<Map<String, Object>>) audiomap.get("renditions");
                for (final Map<String, Object> rendition : renditions) {
                    final String mimetype = rendition.get("MIMEType").toString();
                    String ext = getExtensionFromMimeType(mimetype);
                    if (ext == null) {
                        ext = "mp3";
                    } else {
                        logger.warning("Failed to find ext for mimetype: " + mimetype);
                    }
                    final String url = rendition.get("url").toString();
                    final DownloadLink dl = createDownloadlink(url);
                    if (!StringUtils.isEmpty(caption)) {
                        dl.setComment(caption);
                    }
                    if (renditions.size() == 1) {
                        /* Only one item? Don't include quality information in filename. */
                        dl.setFinalFileName(title + "." + ext);
                    } else {
                        dl.setFinalFileName(title + rendition.get("bitrate") + "b." + ext);
                    }
                    dl.setVerifiedFileSize(((Number) rendition.get("fileSize")).longValue());
                    dl.setAvailable(true);
                    ret.add(dl);
                }
            }
            final ArrayList<Map<String, Object>> videomaps = new ArrayList<Map<String, Object>>();
            this.findVideoMaps(entries, videomaps);
            for (final Map<String, Object> videomap : videomaps) {
                final Map<String, Object> document = (Map<String, Object>) JavaScriptEngineFactory.walkJson(videomap, "props/document");
                if (!dupesMediaids.add(document.get("id").toString())) {
                    continue;
                }
                final String title = document.get("title").toString();
                lastMediaTitle = title;
                final List<Map<String, Object>> renditions = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(document, "media/video/renditions/files");
                final ArrayList<DownloadLink> thisResults = new ArrayList<DownloadLink>();
                final ArrayList<Integer> qual = new ArrayList<Integer>();
                final HashMap<Integer, DownloadLink> abc = new HashMap<Integer, DownloadLink>();
                String urlPattern = null;
                for (final Map<String, Object> rendition : renditions) {
                    final String mimetype = rendition.get("MIMEType").toString();
                    if (!mimetype.equals("video/mp4")) {
                        logger.info("Skipping mimetype: " + mimetype);
                        continue;
                    }
                    final String url = rendition.get("url").toString();
                    final String q = new Regex(url, "(\\d+)k\\.mp4").getMatch(0);
                    if (q != null) {
                        // get qual
                        qual.add(Integer.parseInt(q));
                        if (urlPattern == null) {
                            urlPattern = url;
                        }
                    }
                    final DownloadLink dl = createDownloadlink(url);
                    dl.setFinalFileName(title + rendition.get("height") + "p.mp4");
                    dl.setVerifiedFileSize(((Number) rendition.get("size")).longValue());
                    dl.setAvailable(true);
                    if (q != null) {
                        abc.put(Integer.parseInt(q), dl);
                    }
                    thisResults.add(dl);
                }
                /* Old handling */
                boolean foundAndAddedBetterResult = false;
                // analyse the results, because they don't include all qualities at times.. yet they're available.
                if (urlPattern != null && !qual.isEmpty()) {
                    // 1000k, 512k, 256k,
                    final int[] k = new int[] { 1000, 512, 256 };
                    for (final int kk : k) {
                        if (!qual.contains(kk)) {
                            ret.add(createDownloadlink(urlPattern.replaceFirst("\\d+(k\\.mp4)", kk + "$1")));
                            foundAndAddedBetterResult = true;
                        }
                    }
                }
                // only add best
                if (!abc.isEmpty()) {
                    final int[] k = new int[] { 1000, 512, 256 };
                    for (final int kk : k) {
                        if (abc.containsKey(kk)) {
                            ret.add(abc.get(kk));
                            foundAndAddedBetterResult = true;
                            break;
                        }
                    }
                }
                if (!foundAndAddedBetterResult) {
                    ret.addAll(thisResults);
                }
            }
        }
        // lets look for external links? youtube etc.
        final String[] externlinks = br.getRegex("<div class=\"inline-content (?:interactive )?(?:full|left|right)\">(.*?)</div>").getColumn(0);
        if (externlinks != null) {
            for (final String externlink : externlinks) {
                final String[] links = HTMLParser.getHttpLinks(externlink, null);
                for (final String link : links) {
                    ret.add(createDownloadlink(link));
                }
            }
        }
        String packageTitle = null;
        if (dupesMediaids.size() == 1) {
            packageTitle = lastMediaTitle;
        } else {
            packageTitle = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        }
        if (!StringUtils.isEmpty(packageTitle)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(packageTitle).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    private void findAudioMaps(final Object o, final ArrayList<Map<String, Object>> results) {
        if (o instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) o;
            final String docType = (String) map.get("docType");
            if (StringUtils.equalsIgnoreCase(docType, "audiosegment") && map.containsKey("duration")) {
                results.add(map);
            } else {
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    // final String key = entry.getKey();
                    final Object value = entry.getValue();
                    if (value instanceof List || value instanceof Map) {
                        findAudioMaps(value, results);
                    }
                }
            }
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    findAudioMaps(arrayo, results);
                }
            }
            return;
        } else {
            return;
        }
    }

    private void findVideoMaps(final Object o, final ArrayList<Map<String, Object>> results) {
        if (o instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) o;
            final String type = (String) map.get("type");
            final String keyy = (String) map.get("key");
            if (StringUtils.equalsIgnoreCase(type, "embed") && StringUtils.equalsIgnoreCase(keyy, "video")) {
                results.add(map);
            } else {
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    // final String key = entry.getKey();
                    final Object value = entry.getValue();
                    if (value instanceof List || value instanceof Map) {
                        findVideoMaps(value, results);
                    }
                }
            }
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    findVideoMaps(arrayo, results);
                }
            }
            return;
        } else {
            return;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}