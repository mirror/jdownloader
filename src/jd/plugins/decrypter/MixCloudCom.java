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
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDHexUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mixcloud.com" }, urls = { "https?://(?:www\\.)?mixcloud\\.com/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_%]+/" })
public class MixCloudCom extends antiDDoSForDecrypt {
    public MixCloudCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> tempLinks = new ArrayList<String>();
        final String parameter = param.toString().replace("http://", "https://");
        if (parameter.matches("https?://(?:www\\.)?mixcloud\\.com/((developers|categories|media|competitions|tag|discover)/.+|[\\w\\-]+/(playlists|activity|followers|following|listens|favourites).+)")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        getPage(parameter);
        if (br.getRedirectLocation() != null) {
            logger.info("Unsupported or offline link: " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML("<title>404 Error page|class=\"message-404\"|class=\"record-error record-404") || br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String url_thumbnail = br.getRegex("class=\"album-art\"\\s*?src=\"(http[^<>\"\\']+)\"").getMatch(0);
        String theName = br.getRegex("class=\"cloudcast-name\" itemprop=\"name\">(.*?)</h1>").getMatch(0);
        if (theName == null) {
            theName = br.getRegex("data-resourcelinktext=\"(.*?)\"").getMatch(0);
            if (theName == null) {
                theName = br.getRegex("property=\"og:title\"[^>]* content=\"([^<>\"]*?)\"").getMatch(0);
                if (theName == null) {
                    theName = br.getRegex("<title>(.*?) \\| Mixcloud</title>").getMatch(0);
                    if (theName == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                }
            }
        }
        theName = Encoding.htmlDecode(theName).trim();
        String comment = "";
        int page = 0;
        boolean hasMore = false;
        do {
            /* Find Array with stream-objects */
            LinkedHashMap<String, Object> entries = null;
            ArrayList<Object> audio_objects = null;
            /* Find correct json ArrayList */
            if (page == 0) {
                String json = br.getRegex("id=\"relay-data\"[^>]*>(.*?)<").getMatch(0);
                json = Encoding.htmlOnlyDecode(json);
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                for (final Object audioO : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) audioO;
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "user/data/userLookup");
                    if (entries == null) {
                        continue;
                    }
                    Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
                    while (iterator.hasNext()) {
                        final Entry<String, Object> entry = iterator.next();
                        final String keyName = entry.getKey();
                        if (keyName.matches("_stream.+")) {
                            entries = (LinkedHashMap<String, Object>) entry.getValue();
                            break;
                        }
                    }
                    audio_objects = (ArrayList<Object>) entries.get("edges");
                    if (audio_objects == null) {
                        continue;
                    }
                    /* Only set this boolean if we at least found our array */
                    hasMore = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "pageInfo/hasNextPage")).booleanValue();
                    break;
                }
            } else {
                /* TODO */
                if (true) {
                    break;
                }
                final String requestJson = "TODO";
                final PostRequest downloadReq = br.createJSonPostRequest("https://www." + this.getHost() + "/graphql", requestJson);
                downloadReq.getHeaders().put("accept", "application/json");
                downloadReq.getHeaders().put("content-type", "application/json");
                downloadReq.getHeaders().put("origin", "https://www." + this.getHost() + "/");
                downloadReq.getHeaders().put("x-csrftoken", "TODO");
                downloadReq.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.openRequestConnection(downloadReq);
                br.loadConnection(null);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("data");
                Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Entry<String, Object> entry = iterator.next();
                    final String keyName = entry.getKey();
                    if (keyName.matches("_user.+")) {
                        entries = (LinkedHashMap<String, Object>) entry.getValue();
                        break;
                    }
                }
                iterator = entries.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Entry<String, Object> entry = iterator.next();
                    final String keyName = entry.getKey();
                    if (keyName.matches("_stream.+")) {
                        entries = (LinkedHashMap<String, Object>) entry.getValue();
                        break;
                    }
                }
                audio_objects = (ArrayList<Object>) entries.get("edges");
                if (audio_objects == null) {
                    return null;
                }
                /* Only set this boolean if we at least found our array */
                hasMore = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "pageInfo/hasNextPage")).booleanValue();
            }
            for (final Object edgeO : audio_objects) {
                entries = (LinkedHashMap<String, Object>) edgeO;
                entries = (LinkedHashMap<String, Object>) entries.get("node");
                if (entries == null) {
                    /* Skip invalid objects */
                    continue;
                }
                final String title = (String) entries.get("name");
                final Object cloudcastStreamInfo = entries.get("streamInfo");
                if (StringUtils.isEmpty(title) || cloudcastStreamInfo == null) {
                    /* Skip invalid objects */
                    continue;
                }
                /* We should have found the correct object here! */
                // final String url_mp3_preview = (String) entries.get("previewUrl");
                entries = (LinkedHashMap<String, Object>) cloudcastStreamInfo;
                /*
                 * 2017-11-15: We can chose between dash, http or hls
                 */
                String downloadurl = (String) entries.get("url");
                if (downloadurl == null) {
                    /* Skip objects without streams */
                    continue;
                }
                downloadurl = decode(downloadurl);
                if (StringUtils.isEmpty(downloadurl) || downloadurl.contains("test")) {
                    /* Skip teststreams */
                    continue;
                }
                final String ext = getFileNameExtensionFromString(downloadurl, ".mp3");
                if (!StringUtils.endsWithCaseInsensitive(ext, ".mp3") && !StringUtils.endsWithCaseInsensitive(ext, ".m4a")) {
                    /* Skip unsupported extensions. */
                    continue;
                }
                final DownloadLink dlink = createDownloadlink(downloadurl);
                if (!StringUtils.isEmpty(comment)) {
                    dlink.setComment(comment);
                }
                dlink.setFinalFileName(title + ext);
                dlink.setAvailable(true);
                decryptedLinks.add(dlink);
            }
            page++;
        } while (hasMore);
        /* Add thumbnail if possible. */
        if (!StringUtils.isEmpty(url_thumbnail)) {
            final DownloadLink dlink = createDownloadlink(url_thumbnail);
            if (!StringUtils.isEmpty(comment)) {
                dlink.setComment(comment);
            }
            dlink.setFinalFileName(theName + ".jpeg");
            decryptedLinks.add(dlink);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(theName));
        fp.addLinks(decryptedLinks);
        if ((decryptedLinks == null || decryptedLinks.size() == 0) && tempLinks.size() > 0) {
            logger.info("Found urls but all of them were offline");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private String decode(String playInfo) {
        final byte[] key = JDHexUtils.getByteArray(JDHexUtils.getHexString("IFYOUWANTTHEARTISTSTOGETPAIDDONOTDOWNLOADFROMMIXCLOUD"));
        final byte[] enc = jd.nutils.encoding.Base64.decode(playInfo);
        byte[] plain = new byte[enc.length];
        int keyLen = key.length;
        for (int i = 0; i < enc.length; i++) {
            plain[i] = (byte) (enc[i] ^ key[i % keyLen]);
        }
        return new String(plain);
    }

    private ArrayList<String> siht(String playInfo, String... object) {
        final String[] keys = object != null ? object : new String[] { "cGxlYXNlZG9udGRvd25sb2Fkb3VybXVzaWN0aGVhcnRpc3Rzd29udGdldHBhaWQ=", "ZGVmZXJyZWQucmVzb2x2ZSA9IGRlZmVycmVkLnJlamVjdCA9IGZ1bmN0aW9uKCkge307", "cmV0dXJuIHsgcmVxdWVzdEFuaW1hdGlvbkZyYW1lOiBmdW5jdGlvbihjYWxsYmFjaykgeyBjYWxsYmFjaygpOyB9LCBpbm5lckhlaWdodDogNTAwIH07", "d2luZG93LmFkZEV2ZW50TGlzdGVuZXIgPSB3aW5kb3cuYWRkRXZlbnRMaXN0ZW5lciB8fCBmdW5jdGlvbigpIHt9Ow==" };
        final ArrayList<String> tempLinks = new ArrayList<String>();
        for (final String key : keys) {
            String result = null;
            try {
                result = decrypt(playInfo, key);
            } catch (final Throwable e) {
            }
            final String[] links = new Regex(result, "\"(http.*?)\"").getColumn(0);
            if (links != null && links.length != 0) {
                for (final String temp : links) {
                    tempLinks.add(temp);
                }
            }
        }
        return tempLinks;
    }

    private String decrypt(final String e, final String k) {
        final byte[] key = JDHexUtils.getByteArray(JDHexUtils.getHexString(Encoding.Base64Decode(k)));
        final byte[] enc = jd.crypt.Base64.decode(e);
        byte[] plain = new byte[enc.length];
        int count = enc.length;
        int i = 0;
        int j = 0;
        while (i < count) {
            if (j > key.length - 1) {
                j = 0;
            }
            plain[i] = (byte) (0xff & (enc[i] ^ key[j]));
            i++;
            j++;
        }
        return new String(plain);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}