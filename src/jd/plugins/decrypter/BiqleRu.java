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
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.Base64;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "biqle.ru", "daxab.com", "divxcim.com" }, urls = { "https?://(?:www\\.)?biqle\\.(com|ru|org)/watch/(?:\\-)?\\d+_\\d+", "https?://(?:www\\.)?daxab\\.com/embed/(?:\\-)?\\d+_\\d+", "https?://(?:www\\.)?divxcim\\.com/video_ext\\.php\\?oid=(?:\\-)?\\d+\\&id=\\d+" })
public class BiqleRu extends PluginForDecrypt {
    public BiqleRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Converts embedded crap to vk.com video-urls. */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if ("biqle.ru".equals(getHost())) {
            br.getPage(param.getCryptedUrl());
            br.followRedirect();
            final String title = br.getRegex("<title>\\s*(.*?)\\s*(— BIQLE Video)?</title>").getMatch(0);
            final String daxab = br.getRegex("((?:https?:)?//daxab\\.com/player/[a-zA-Z0-9_\\-]+)\"").getMatch(0);
            if (daxab != null) {
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
                            String resolution = new Regex(cdn_file, "mp4_(\\d+)").getMatch(0);
                            if (resolution == null) {
                                resolution = "";
                            }
                            String fileName = (String) cdn_file.getValue();
                            fileName = fileName.replace(".", ".mp4?extra=");
                            final DownloadLink downloadLink = createDownloadlink("directhttp://http://" + server + "/videos/" + cdn_id.replace("_", "/") + "/" + fileName);
                            if (title != null) {
                                downloadLink.setFinalFileName(title + "_" + resolution + ".mp4");
                            } else {
                                downloadLink.setFinalFileName(cdn_id + "_" + resolution + ".mp4");
                            }
                            downloadLink.setContainerUrl(param.getCryptedUrl());
                            ret.add(downloadLink);
                        }
                    }
                    return ret;
                } else {
                    final String server = Base64.decodeToString(new StringBuilder(brc.getRegex("server\\s*:\\s*\"(.*?)\"").getMatch(0)).reverse().toString());
                    final String accessToken = brc.getRegex("access_token\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String videoId = brc.getRegex("id\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String sig = brc.getRegex("sig\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String cKey = brc.getRegex("c_key\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String partialSig = brc.getRegex("\"sig\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                    final String partialQualityString = brc.getRegex("\"quality\"\\s*:\\s*(\\{.*?\\})").getMatch(0);
                    final Map<String, Object> partialQuality = JSonStorage.restoreFromString(partialQualityString, TypeRef.HASHMAP);
                    String path, extraQuery;
                    if (partialSig != null) {
                        path = "sig";
                        extraQuery = String.format("&sig=%s", partialSig);
                    } else {
                        path = "get";
                        extraQuery = "";
                    }
                    brc.getPage(String.format("//%s/method/video.%s?callback=jQuery&token=%s&videos=%s&extra_key=%s&ckey=%s%s", server, path, accessToken, videoId, sig, cKey, extraQuery));
                    if (brc.getHttpConnection().getResponseCode() == 404) {
                        ret.add(createOfflinelink(param.getCryptedUrl()));
                        return ret;
                    }
                    final String titleName = PluginJSonUtils.getJsonValue(brc, "title");
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
                            final DownloadLink downloadLink = createDownloadlink("directhttp://" + sb.toString());
                            downloadLink.setFinalFileName(titleName + "_" + resolution + ".mp4");
                            downloadLink.setContainerUrl(param.getCryptedUrl());
                            ret.add(downloadLink);
                        }
                    }
                    return ret;
                }
            } else {
                // vk mode
                final String daxabExt = br.getRegex("((?:https?:)?//daxab\\.com/ext\\.php\\?oid=[0-9\\-]+&id=\\d+&hash=[a-zA-Z0-9]+)\"").getMatch(0);
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
}
