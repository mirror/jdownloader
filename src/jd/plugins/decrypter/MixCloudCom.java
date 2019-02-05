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
            ArrayList<Object> audio_objects = new ArrayList<Object>();
            /* Find correct json ArrayList */
            if (page == 0) {
                String json = br.getRegex("id=\"relay-data\"[^>]*>(.*?)<").getMatch(0);
                json = Encoding.htmlOnlyDecode(json);
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                for (final Object audioO : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) audioO;
                    /* All items of a user? */
                    final Object userLookup = JavaScriptEngineFactory.walkJson(entries, "user/data/userLookup");
                    /* More likely a single item? */
                    final Object cloudcastLookup = JavaScriptEngineFactory.walkJson(entries, "cloudcast/data/cloudcastLookup");
                    if (cloudcastLookup != null) {
                        audio_objects.add(cloudcastLookup);
                    } else if (userLookup != null) {
                        entries = (LinkedHashMap<String, Object>) userLookup;
                        Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
                        while (iterator.hasNext()) {
                            final Entry<String, Object> entry = iterator.next();
                            final String keyName = entry.getKey();
                            if (keyName.matches("_stream.+")) {
                                entries = (LinkedHashMap<String, Object>) entry.getValue();
                                break;
                            }
                        }
                        final Object audio_objects_o = entries.get("edges");
                        if (audio_objects_o != null && audio_objects_o instanceof ArrayList) {
                            audio_objects = (ArrayList<Object>) audio_objects_o;
                            /* Only set this boolean if we at least found our array */
                            try {
                                hasMore = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "pageInfo/hasNextPage")).booleanValue();
                            } catch (final Throwable e) {
                            }
                            break;
                        }
                    }
                }
            } else {
                /* TODO */
                if (true) {
                    break;
                }
                String xcsrftoken = br.getCookie(br.getHost(), "csrftoken");
                // br.setCookie(br.getHost(), "previmpr", "");
                // final String authJson = "{\"id\":\"0\",\"query\":\"mutation UnknownFile($input_0:SetExperimentGoalMutationInput!)
                // {setExperimentGoal(input:$input_0)
                // {clientMutationId}}\",\"variables\":{\"input_0\":{\"goal\":\"page_view\",\"clientMutationId\":\"0\"}}}";
                // final PostRequest authReq = br.createJSonPostRequest("https://www." + this.getHost() + "/graphql", authJson);
                // authReq.getHeaders().put("accept", "application/json");
                // authReq.getHeaders().put("content-type", null);
                // authReq.getHeaders().put("content-type", "application/json");
                // authReq.getHeaders().put("origin", "https://www." + this.getHost() + "/");
                // authReq.getHeaders().put("x-csrftoken", xcsrftoken);
                // authReq.getHeaders().put("x-requested-with", "XMLHttpRequest");
                // authReq.getHeaders().put("accept-language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7");
                // br.openRequestConnection(authReq);
                // br.loadConnection(null);
                // br.setAllowedResponseCodes(new int[] { 400 });
                // xcsrftoken = br.getCookie(br.getHost(), "csrftoken");
                // if (xcsrftoken == null) {
                // logger.warning("Failed to find xcsrftoken");
                // break;
                // }
                final String requestJson = "{\"id\":\"q59\",\"query\":\"query UserStreamPageQuery($first_0:Int!,$lighten_1:Int!,$alpha_2:Float!) {_user1w5emR:user(id:\\\"VXNlcjoxMjIwNTM0NA==\\\") {id,...Fh}} fragment F0 on Picture {urlRoot,primaryColor} fragment F1 on User {id} fragment F2 on User {username,hasProFeatures,hasPremiumFeatures,isStaff,isSelect,id} fragment F3 on Cloudcast {isExclusive,isExclusivePreviewOnly,slug,owner {username,id},id} fragment F4 on CloudcastTag {tag {name,slug,isCategory,id},position} fragment F5 on Cloudcast {_tags4ruy33:tags {...F4},id} fragment F6 on Cloudcast {restrictedReason,owner {username,id},slug,id,isAwaitingAudio,isDraft,isPlayable,streamInfo {hlsUrl,dashUrl,url,uuid},audioLength,currentPosition,proportionListened,seekRestriction,previewUrl,isExclusivePreviewOnly,picture {primaryColor,isLight,_primaryColor2pfPSM:primaryColor(lighten:$lighten_1),_primaryColor3Yfcks:primaryColor(alpha:$alpha_2)}} fragment F7 on Cloudcast {id,name,slug,owner {id,username,displayName,isSelect,...F1,...F2},isUnlisted,isExclusive,...F3,...F5,...F6} fragment F8 on Cloudcast {isDraft,hiddenStats,plays,publishDate,qualityScore,listenerMinutes,id} fragment F9 on Cloudcast {id,isFavorited,isPublic,hiddenStats,favorites {totalCount},slug,owner {id,isFollowing,username,displayName,isViewer}} fragment Fa on Cloudcast {id,isReposted,isPublic,hiddenStats,reposts {totalCount},owner {isViewer,id}} fragment Fb on Cloudcast {id,isUnlisted,isPublic} fragment Fc on Cloudcast {id,isUnlisted,isPublic,slug,description,picture {urlRoot},owner {displayName,isViewer,username,id}} fragment Fd on Cloudcast {id,isPublic,isHighlighted,owner {isViewer,id}} fragment Fe on Cloudcast {id,isPublic,owner {isViewer,id},...F8,...F9,...Fa,...Fb,...Fc,...Fd} fragment Ff on Cloudcast {owner {quantcastTrackingPixel,id},id} fragment Fg on Cloudcast {id,slug,name,isAwaitingAudio,isDraft,isScheduled,restrictedReason,publishDate,waveformUrl,audioLength,owner {username,id},picture {...F0},...F7,...Fe,...Ff} fragment Fh on User {id,displayName,username,_streamvC1bh:stream(first:$first_0,after:\\\"2018-02-15 17:10:20+00:00|510659675\\\") {edges {cursor,repostedBy,node {id,...Fg}},pageInfo {endCursor,hasNextPage,hasPreviousPage}}}\",\"variables\":{\"first_0\":20,\"lighten_1\":15,\"alpha_2\":0.3}}";
                final PostRequest downloadReq = br.createJSonPostRequest("https://www." + this.getHost() + "/graphql", requestJson);
                downloadReq.getHeaders().put("accept", "application/json");
                downloadReq.getHeaders().put("content-type", null);
                downloadReq.getHeaders().put("content-type", "application/json");
                downloadReq.getHeaders().put("origin", "https://www." + this.getHost() + "/");
                downloadReq.getHeaders().put("x-csrftoken", xcsrftoken);
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
                final Object node = entries.get("node");
                if (node != null && node instanceof LinkedHashMap) {
                    /* E.g. decryption of multiple items (all items of user) */
                    entries = (LinkedHashMap<String, Object>) node;
                }
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