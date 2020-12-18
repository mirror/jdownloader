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
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Cookies;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mixcloud.com" }, urls = { "https?://(?:www\\.)?mixcloud\\.com/(widget/iframe/\\?.+|[^/]+/?([^/]+/)?)" })
public class MixCloudCom extends antiDDoSForDecrypt {
    public MixCloudCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private static final String TYPE_SINGLE_AUDIO         = "^https?://[^/]+/[^/]+/[^/]+/?$";
    private static final String TYPE_SINGLE_AUDIO_IFRAME_ = "https?://[^/]+/widget/iframe/\\?.*feed=.+";
    private static final String TYPE_CHANNEL              = "^https?://[^/]+/([^/]+)/?$";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("http://", "https://");
        if (parameter.matches("https?://(?:www\\.)?mixcloud\\.com/((developers|categories|media|competitions|tag|discover)/.+|[\\w\\-]+/(playlists|activity|followers|following|listens|favourites).+)")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        } else if (parameter.matches(TYPE_SINGLE_AUDIO_IFRAME_)) {
            /* Correct URL leading to embedded content --> Normal URL */
            final UrlQuery query = new UrlQuery().parse(parameter);
            String urlpart = query.get("feed");
            if (urlpart == null) {
                return null;
            }
            if (Encoding.isUrlCoded(urlpart)) {
                urlpart = Encoding.htmlDecode(urlpart);
            }
            if (urlpart.startsWith("/")) {
                parameter = "https://www." + this.getHost() + urlpart;
            } else {
                parameter = urlpart;
            }
        }
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.MixCloudCom) plg).login(account, false);
        }
        if (parameter.matches(TYPE_SINGLE_AUDIO)) {
            return this.crawlSingleAudio(param, parameter);
        } else if (parameter.matches(TYPE_CHANNEL)) {
            return this.crawlUsername(param);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> crawlUsername(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final String username = new Regex(param.getCryptedUrl(), TYPE_CHANNEL).getMatch(0);
        if (username == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.getPage(param.getCryptedUrl());
        final String csrftoken = br.getCookie(br.getHost(), "csrftoken", Cookies.NOTDELETEDPATTERN);
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        br.getHeaders().put("x-csrftoken", csrftoken);
        br.getHeaders().put("accept", "application/json");
        br.getHeaders().put("content-type", "application/json");
        br.postPageRaw("/graphql",
                "{\"query\":\"query UserProfileHeaderQuery(  $lookup: UserLookup!) {  user: userLookup(lookup: $lookup) {    id    displayName    username    isBranded    isStaff    isFollowing    isViewer    followers {      totalCount    }    hasCoverPicture    hasPremiumFeatures    hasProFeatures    picture {      primaryColor      ...UGCImage_picture    }    coverPicture {      urlRoot    }    ...UserBadge_user    ...ProfileNavigation_user    ...ShareUserButton_user    ...ProfileRegisterUpsellComponent_user    ...FollowButton_user    ...SelectUpsellButton_user  }  viewer {    ...ProfileRegisterUpsellComponent_viewer    ...FollowButton_viewer    id  }}fragment FollowButton_user on User {  id  isFollowed  isFollowing  isViewer  followers {    totalCount  }  username  displayName}fragment FollowButton_viewer on Viewer {  me {    id  }}fragment ProfileNavigation_user on User {  id  username  stream {    totalCount  }  favorites {    totalCount  }  listeningHistory {    totalCount  }  uploads {    totalCount  }  posts {    totalCount  }  profileNavigation {    menuItems {      __typename      ... on NavigationItemInterface {        __isNavigationItemInterface: __typename        inDropdown      }      ... on HideableNavigationItemInterface {        __isHideableNavigationItemInterface: __typename        hidden      }      ... on PlaylistNavigationItem {        count        playlist {          id          name          slug        }      }    }  }}fragment ProfileRegisterUpsellComponent_user on User {  id  displayName  followers {    totalCount  }}fragment ProfileRegisterUpsellComponent_viewer on Viewer {  me {    id  }}fragment SelectUpsellButton_user on User {  username  isSelect  isSubscribedTo  selectUpsell {    planInfo {      displayAmount    }  }}fragment ShareUserButton_user on User {  biog  username  displayName  id  isUploader  picture {    urlRoot  }}fragment UGCImage_picture on Picture {  urlRoot  primaryColor}fragment UserBadge_user on User {  username  hasProFeatures  hasPremiumFeatures  isStaff  isSelect}\",\"variables\":{\"lookup\":{\"username\":\""
                        + username + "\"}}}");
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/user");
        final String userIDb64 = (String) entries.get("id");
        final int totalItems = ((Number) JavaScriptEngineFactory.walkJson(entries, "uploads/totalCount")).intValue();
        if (totalItems <= 0) {
            logger.info("This user does not own any uploads");
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        } else if (StringUtils.isEmpty(userIDb64)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<String> dupes = new ArrayList<String>();
        final String query = "query UserUploadsQuery(  $lookup: UserLookup!  $orderBy: CloudcastOrderByEnum) {  user: userLookup(lookup: $lookup) {    id    username    displayName    ...UserUploadsPage_user_7FfCv    ...Drafts_user  }  viewer {    ...UserUploadsPage_viewer    ...Drafts_viewer    id  }}fragment AudioCardActions_cloudcast on Cloudcast {  id  isPublic  isExclusive  owner {    id    username    isViewer    isSubscribedTo  }  ...AudioCardFavoriteButton_cloudcast  ...AudioCardRepostButton_cloudcast  ...AudioCardShareButton_cloudcast  ...AudioCardAddToButton_cloudcast  ...AudioCardHighlightButton_cloudcast  ...AudioCardBoostButton_cloudcast  ...AudioCardStats_cloudcast}fragment AudioCardActions_viewer on Viewer {  ...AudioCardFavoriteButton_viewer  ...AudioCardRepostButton_viewer  ...AudioCardHighlightButton_viewer}fragment AudioCardAddToButton_cloudcast on Cloudcast {  id  isUnlisted  isPublic}fragment AudioCardBoostButton_cloudcast on Cloudcast {  id  isPublic  owner {    id    isViewer  }}fragment AudioCardFavoriteButton_cloudcast on Cloudcast {  id  isFavorited  isPublic  hiddenStats  favorites {    totalCount  }  slug  owner {    id    isFollowing    username    isSelect    displayName    isViewer  }}fragment AudioCardFavoriteButton_viewer on Viewer {  me {    id  }  experiments {    experiment(name: \\\"growth_chained_actions\\\") {      name      alternative    }  }}fragment AudioCardHighlightButton_cloudcast on Cloudcast {  id  isPublic  isHighlighted  owner {    isViewer    id  }}fragment AudioCardHighlightButton_viewer on Viewer {  me {    id    hasProFeatures    highlighted {      totalCount    }  }}fragment AudioCardPlayButton_cloudcast on Cloudcast {  id  restrictedReason  owner {    displayName    country    username    isSubscribedTo    isViewer    id  }  slug  isAwaitingAudio  isDraft  isPlayable  streamInfo {    hlsUrl    dashUrl    url    uuid  }  audioLength  currentPosition  proportionListened  repeatPlayAmount  hasPlayCompleted  seekRestriction  previewUrl  isExclusivePreviewOnly  isExclusive}fragment AudioCardProgress_cloudcast on Cloudcast {  id  proportionListened}fragment AudioCardRepostButton_cloudcast on Cloudcast {  id  isReposted  isPublic  hiddenStats  reposts {    totalCount  }  owner {    isViewer    id  }}fragment AudioCardRepostButton_viewer on Viewer {  me {    id  }}fragment AudioCardShareButton_cloudcast on Cloudcast {  id  isUnlisted  isPublic  slug  description  picture {    urlRoot  }  owner {    displayName    isViewer    username    id  }}fragment AudioCardStats_cloudcast on Cloudcast {  isDraft  hiddenStats  plays  publishDate  qualityScore  listenerMinutes  tags(country: \\\"GLOBAL\\\") {    tag {      name      slug      id    }  }  ...AudioCardTags_cloudcast}fragment AudioCardTags_cloudcast on Cloudcast {  tags(country: \\\"GLOBAL\\\") {    tag {      name      slug      id    }  }}fragment AudioCardTitle_cloudcast on Cloudcast {  id  slug  name  isUnlisted  isLiveRecording  isExclusive  owner {    id    displayName    username    ...Hovercard_user    ...RebrandUserBadge_user  }  ...AudioCardPlayButton_cloudcast  ...ExclusiveCloudcastBadgeContainer_cloudcast}fragment AudioCard_cloudcast on Cloudcast {  id  slug  name  isAwaitingAudio  isDraft  isScheduled  restrictedReason  publishDate  waveformUrl  audioLength  isLiveRecording  owner {    username    id  }  picture {    ...UGCImage_picture  }  ...AudioCardTitle_cloudcast  ...AudioCardProgress_cloudcast  ...AudioCardActions_cloudcast  ...QuantcastCloudcastTracking_cloudcast}fragment AudioCard_viewer on Viewer {  ...AudioCardActions_viewer}fragment Drafts_user on User {  drafts(first: 100) {    edges {      node {        ...AudioCard_cloudcast        id      }    }  }}fragment Drafts_viewer on Viewer {  ...AudioCard_viewer}fragment ExclusiveCloudcastBadgeContainer_cloudcast on Cloudcast {  isExclusive  isExclusivePreviewOnly  slug  id  owner {    username    id  }}fragment Hovercard_user on User {  id}fragment QuantcastCloudcastTracking_cloudcast on Cloudcast {  owner {    quantcastTrackingPixel    id  }}fragment RebrandUserBadge_user on User {  username  hasProFeatures  isSelect}fragment UGCImage_picture on Picture {  urlRoot  primaryColor}fragment UserUploadsPage_user_7FfCv on User {  id  displayName  username  uploads(first: 10, orderBy: $orderBy) {    edges {      node {        ...AudioCard_cloudcast        id        __typename      }      cursor    }    pageInfo {      endCursor      hasNextPage    }  }}fragment UserUploadsPage_viewer on Viewer {  ...AudioCard_viewer}";
        final String queryPagination = "query UserUploadsPageQuery(  $count: Int!  $cursor: String  $orderBy: CloudcastOrderByEnum  $userID: ID!) {  user(id: $userID) {    ...UserUploadsPage_user_32czeo    id  }}fragment AudioCardActions_cloudcast on Cloudcast {  id  isPublic  isExclusive  owner {    id    username    isViewer    isSubscribedTo  }  ...AudioCardFavoriteButton_cloudcast  ...AudioCardRepostButton_cloudcast  ...AudioCardShareButton_cloudcast  ...AudioCardAddToButton_cloudcast  ...AudioCardHighlightButton_cloudcast  ...AudioCardBoostButton_cloudcast  ...AudioCardStats_cloudcast}fragment AudioCardAddToButton_cloudcast on Cloudcast {  id  isUnlisted  isPublic}fragment AudioCardBoostButton_cloudcast on Cloudcast {  id  isPublic  owner {    id    isViewer  }}fragment AudioCardFavoriteButton_cloudcast on Cloudcast {  id  isFavorited  isPublic  hiddenStats  favorites {    totalCount  }  slug  owner {    id    isFollowing    username    isSelect    displayName    isViewer  }}fragment AudioCardHighlightButton_cloudcast on Cloudcast {  id  isPublic  isHighlighted  owner {    isViewer    id  }}fragment AudioCardPlayButton_cloudcast on Cloudcast {  id  restrictedReason  owner {    displayName    country    username    isSubscribedTo    isViewer    id  }  slug  isAwaitingAudio  isDraft  isPlayable  streamInfo {    hlsUrl    dashUrl    url    uuid  }  audioLength  currentPosition  proportionListened  repeatPlayAmount  hasPlayCompleted  seekRestriction  previewUrl  isExclusivePreviewOnly  isExclusive}fragment AudioCardProgress_cloudcast on Cloudcast {  id  proportionListened}fragment AudioCardRepostButton_cloudcast on Cloudcast {  id  isReposted  isPublic  hiddenStats  reposts {    totalCount  }  owner {    isViewer    id  }}fragment AudioCardShareButton_cloudcast on Cloudcast {  id  isUnlisted  isPublic  slug  description  picture {    urlRoot  }  owner {    displayName    isViewer    username    id  }}fragment AudioCardStats_cloudcast on Cloudcast {  isDraft  hiddenStats  plays  publishDate  qualityScore  listenerMinutes  tags(country: \\\"GLOBAL\\\") {    tag {      name      slug      id    }  }  ...AudioCardTags_cloudcast}fragment AudioCardTags_cloudcast on Cloudcast {  tags(country: \\\"GLOBAL\\\") {    tag {      name      slug      id    }  }}fragment AudioCardTitle_cloudcast on Cloudcast {  id  slug  name  isUnlisted  isLiveRecording  isExclusive  owner {    id    displayName    username    ...Hovercard_user    ...RebrandUserBadge_user  }  ...AudioCardPlayButton_cloudcast  ...ExclusiveCloudcastBadgeContainer_cloudcast}fragment AudioCard_cloudcast on Cloudcast {  id  slug  name  isAwaitingAudio  isDraft  isScheduled  restrictedReason  publishDate  waveformUrl  audioLength  isLiveRecording  owner {    username    id  }  picture {    ...UGCImage_picture  }  ...AudioCardTitle_cloudcast  ...AudioCardProgress_cloudcast  ...AudioCardActions_cloudcast  ...QuantcastCloudcastTracking_cloudcast}fragment ExclusiveCloudcastBadgeContainer_cloudcast on Cloudcast {  isExclusive  isExclusivePreviewOnly  slug  id  owner {    username    id  }}fragment Hovercard_user on User {  id}fragment QuantcastCloudcastTracking_cloudcast on Cloudcast {  owner {    quantcastTrackingPixel    id  }}fragment RebrandUserBadge_user on User {  username  hasProFeatures  isSelect}fragment UGCImage_picture on Picture {  urlRoot  primaryColor}fragment UserUploadsPage_user_32czeo on User {  id  displayName  username  uploads(first: $count, after: $cursor, orderBy: $orderBy) {    edges {      node {        ...AudioCard_cloudcast        id        __typename      }      cursor    }    pageInfo {      endCursor      hasNextPage    }  }}";
        // final Map<String, Object> varLookup = new HashMap<String, Object>();
        // varLookup.put("username", username);
        // final Map<String, Object> variables = new HashMap<String, Object>();
        // variables.put("lookup", varLookup);
        // variables.put("orderBy", "LATEST");
        // final Map<String, Object> jsonData = new HashMap<String, Object>();
        // jsonData.put("query", query);
        // jsonData.put("variables", variables);
        br.postPageRaw("/graphql", "{\"query\":\"" + query + "\",\"variables\":{\"lookup\":{\"username\":\"" + username + "\"},\"orderBy\":\"LATEST\"}}");
        final int maxItemsPerPage = 10;
        int page = 0;
        do {
            logger.info("Crawling page: " + page);
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/user/uploads");
            final Map<String, Object> pageInfo = (Map<String, Object>) entries.get("pageInfo");
            final List<Object> audioObjects = (List<Object>) entries.get("edges");
            this.crawlAudioObjects(decryptedLinks, audioObjects, dupes);
            final String lastCursor = (String) ((Map<String, Object>) audioObjects.get(audioObjects.size() - 1)).get("cursor");
            if (audioObjects.size() < maxItemsPerPage) {
                logger.info("Stopping because current page contains less than " + maxItemsPerPage + " items");
                break;
            } else if (!((Boolean) pageInfo.get("hasNextPage")).booleanValue()) {
                logger.info("Stopping because current page is the last page");
                break;
            } else if (StringUtils.isEmpty(lastCursor)) {
                logger.info("Stopping because endCursor is missing");
                break;
            }
            br.postPageRaw("/graphql", "{\"query\":\"" + queryPagination + "\",\"variables\":{\"count\":20,\"cursor\":\"" + lastCursor + "\",\"orderBy\":\"LATEST\",\"userID\":\"" + userIDb64 + "\"}}");
            page++;
        } while (!this.isAbort());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlSingleAudio(final CryptedLink param, final String parameter) throws Exception {
        final ArrayList<String> tempLinks = new ArrayList<String>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        getPage(parameter);
        if (br.getRedirectLocation() != null) {
            logger.info("Unsupported or offline link: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>404 Error page|class=\"message-404\"|class=\"record-error record-404")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* TODO: Fix thumbnail support */
        final String url_thumbnail = br.getRegex("class=\"album-art\"\\s*?src=\"(http[^<>\"\\']+)\"").getMatch(0);
        final Regex urlregex = new Regex(parameter, "https?://[^/]+/([^/]+)/([^/]+)/?");
        String username = urlregex.getMatch(0);
        String slug = urlregex.getMatch(1);
        if (Encoding.isUrlCoded(username)) {
            username = Encoding.urlDecode(username, false);
        }
        if (Encoding.isUrlCoded(slug)) {
            slug = Encoding.urlDecode(slug, false);
        }
        String theName = slug;
        final String csrftoken = br.getCookie(br.getHost(), "csrftoken", Cookies.NOTDELETEDPATTERN);
        if (csrftoken == null || username == null || slug == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        br.getHeaders().put("x-csrftoken", csrftoken);
        br.getHeaders().put("accept", "application/json");
        br.getHeaders().put("content-type", "application/json");
        final String postdata = String.format(
                "{\"id\":\"q13\",\"query\":\"query HeaderQuery($lookup_0:CloudcastLookup!,$lighten_1:Int!,$alpha_2:Float!) {cloudcastLookup(lookup:$lookup_0) {id,...Fn}} fragment F0 on Cloudcast {picture {urlRoot,primaryColor},id} fragment F1 on Cloudcast {id,name,slug,owner {username,id}} fragment F2 on Cloudcast {owner {id,displayName,followers {totalCount}},id} fragment F3 on Cloudcast {restrictedReason,owner {displayName,country,username,isSubscribedTo,isViewer,id},slug,id,isAwaitingAudio,isDraft,isPlayable,streamInfo {hlsUrl,dashUrl,url,uuid},audioLength,currentPosition,proportionListened,repeatPlayAmount,hasPlayCompleted,seekRestriction,previewUrl,isExclusivePreviewOnly,isExclusive,picture {primaryColor,isLight,_primaryColor2pfPSM:primaryColor(lighten:$lighten_1),_primaryColor3Yfcks:primaryColor(alpha:$alpha_2)}} fragment F4 on Node {id,__typename} fragment F5 on Cloudcast {id,isFavorited,isPublic,hiddenStats,favorites {totalCount},slug,owner {id,isFollowing,username,isSelect,displayName,isViewer}} fragment F6 on Cloudcast {id,isUnlisted,isPublic} fragment F7 on Cloudcast {id,isReposted,isPublic,hiddenStats,reposts {totalCount},owner {isViewer,id}} fragment F8 on Cloudcast {id,isUnlisted,isPublic,slug,description,picture {urlRoot},owner {displayName,isViewer,username,id}} fragment F9 on Cloudcast {id,slug,isSpam,owner {username,isViewer,id}} fragment Fa on Cloudcast {owner {isViewer,isSubscribedTo,username,hasProFeatures,isBranded,id},sections {__typename,...F4},id,slug,isExclusive,isUnlisted,isShortLength,...F5,...F6,...F7,...F8,...F9} fragment Fb on Cloudcast {qualityScore,listenerMinutes,id} fragment Fc on Cloudcast {slug,plays,publishDate,hiddenStats,owner {username,id},id,...Fb} fragment Fd on User {id} fragment Fe on User {username,hasProFeatures,hasPremiumFeatures,isStaff,isSelect,id} fragment Ff on User {id,isFollowed,isFollowing,isViewer,followers {totalCount},username,displayName} fragment Fg on Cloudcast {isExclusive,isExclusivePreviewOnly,slug,id,owner {username,id}} fragment Fh on Cloudcast {isExclusive,owner {id,username,displayName,...Fd,...Fe,...Ff},id,...Fg} fragment Fi on Cloudcast {id,streamInfo {uuid,url,hlsUrl,dashUrl},audioLength,seekRestriction,currentPosition} fragment Fj on Cloudcast {owner {displayName,isSelect,username,id},seekRestriction,id} fragment Fk on Cloudcast {id,waveformUrl,previewUrl,audioLength,isPlayable,streamInfo {hlsUrl,dashUrl,url,uuid},restrictedReason,seekRestriction,currentPosition,...Fj} fragment Fl on Cloudcast {__typename,isExclusivePreviewOnly,isExclusive,owner {isSelect,isSubscribedTo,username,displayName,isViewer,id},id} fragment Fm on Cloudcast {owner {username,displayName,isSelect,id},id} fragment Fn on Cloudcast {id,name,picture {isLight,primaryColor,urlRoot,primaryColor},owner {displayName,isViewer,isBranded,selectUpsell {text},id},repeatPlayAmount,restrictedReason,seekRestriction,...F0,...F1,...F2,...F3,...Fa,...Fc,...Fh,...Fi,...Fk,...Fl,...Fm}\",\"variables\":{\"lookup_0\":{\"username\":\"%s\",\"slug\":\"%s\"},\"lighten_1\":15,\"alpha_2\":0.3}}",
                username, slug);
        this.postPageRaw("/graphql", postdata);
        int page = 0;
        boolean hasMore = false;
        final ArrayList<String> dupes = new ArrayList<String>();
        do {
            /* Find Array with stream-objects */
            Map<String, Object> entries = null;
            List<Object> audioObjects = new ArrayList<Object>();
            /* Find correct json ArrayList */
            if (page == 0) {
                String json = br.toString();
                // json = Encoding.htmlOnlyDecode(json);
                final Object jsonO = JavaScriptEngineFactory.jsonToJavaObject(json);
                if (jsonO instanceof Map) {
                    /* 2020-06-02 */
                    entries = (Map<String, Object>) jsonO;
                    final Object cloudcastLookupO = JavaScriptEngineFactory.walkJson(entries, "data/cloudcastLookup");
                    if (cloudcastLookupO == null) {
                        decryptedLinks.add(this.createOfflinelink(parameter));
                        return decryptedLinks;
                    } else {
                        audioObjects.add(cloudcastLookupO);
                    }
                } else {
                    final List<Object> ressourcelist = (List<Object>) jsonO;
                    for (final Object audioO : ressourcelist) {
                        entries = (Map<String, Object>) audioO;
                        /* All items of a user? */
                        Object userLookup = JavaScriptEngineFactory.walkJson(entries, "user/data/userLookup");
                        if (userLookup == null) {
                            /* 2019-11-10 */
                            userLookup = JavaScriptEngineFactory.walkJson(entries, "userLookup/data/userLookup");
                        }
                        /* More likely a single item? */
                        Object cloudcastLookupO = JavaScriptEngineFactory.walkJson(entries, "data/cloudcastLookup");
                        if (cloudcastLookupO == null) {
                            /* 2019-11-10 */
                            cloudcastLookupO = JavaScriptEngineFactory.walkJson(entries, "data/cloudcastLookup");
                        }
                        if (cloudcastLookupO != null) {
                            audioObjects.add(cloudcastLookupO);
                        } else if (userLookup != null) {
                            entries = (Map<String, Object>) userLookup;
                            Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
                            while (iterator.hasNext()) {
                                final Entry<String, Object> entry = iterator.next();
                                final String keyName = entry.getKey();
                                if (keyName.matches("_stream.+")) {
                                    entries = (Map<String, Object>) entry.getValue();
                                    break;
                                }
                            }
                            final Object audio_objects_o = entries.get("edges");
                            if (audio_objects_o != null && audio_objects_o instanceof List) {
                                audioObjects = (List<Object>) audio_objects_o;
                                /* Only set this boolean if we at least found our array */
                                try {
                                    hasMore = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "pageInfo/hasNextPage")).booleanValue();
                                } catch (final Throwable e) {
                                }
                                break;
                            }
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
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                entries = (Map<String, Object>) entries.get("data");
                Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Entry<String, Object> entry = iterator.next();
                    final String keyName = entry.getKey();
                    if (keyName.matches("_user.+")) {
                        entries = (Map<String, Object>) entry.getValue();
                        break;
                    }
                }
                iterator = entries.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Entry<String, Object> entry = iterator.next();
                    final String keyName = entry.getKey();
                    if (keyName.matches("_stream.+")) {
                        entries = (Map<String, Object>) entry.getValue();
                        break;
                    }
                }
                audioObjects = (ArrayList<Object>) entries.get("edges");
                if (audioObjects == null) {
                    return null;
                }
                /* Only set this boolean if we at least found our array */
                hasMore = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "pageInfo/hasNextPage")).booleanValue();
            }
            crawlAudioObjects(decryptedLinks, audioObjects, dupes);
            page++;
        } while (hasMore);
        /* Add thumbnail if possible. */
        if (!StringUtils.isEmpty(url_thumbnail)) {
            final DownloadLink dlink = createDownloadlink(url_thumbnail);
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

    private void crawlAudioObjects(final ArrayList<DownloadLink> decryptedLinks, final List<Object> audio_objects, final ArrayList<String> dupes) {
        Map<String, Object> entries;
        for (final Object edgeO : audio_objects) {
            entries = (Map<String, Object>) edgeO;
            final Object node = entries.get("node");
            if (node != null && node instanceof Map) {
                /* E.g. decryption of multiple items (all items of user) */
                entries = (Map<String, Object>) node;
            }
            if (entries == null) {
                /* Skip invalid objects */
                continue;
            }
            final Map<String, Object> ownerInfo = (Map<String, Object>) entries.get("owner");
            final String uploaderName = (String) ownerInfo.get("displayName");
            final String id = (String) entries.get("id");
            final String title = (String) entries.get("name");
            final String url_preview = (String) entries.get("previewUrl");
            // final Object isExclusiveO = entries.get("isExclusive");
            // final boolean isExclusive = isExclusiveO != null ? ((Boolean) isExclusiveO).booleanValue() : false;
            if (StringUtils.isEmpty(id) || StringUtils.isEmpty(title) || StringUtils.isEmpty(uploaderName)) {
                /* Skip invalid objects */
                continue;
            } else if (dupes.contains(id)) {
                logger.info("Skip dupe object: " + id);
                continue;
            }
            final Object cloudcastStreamInfo = entries.get("streamInfo");
            if (cloudcastStreamInfo == null && StringUtils.isEmpty(url_preview)) {
                /* Skip invalid objects */
                continue;
            }
            dupes.add(id);
            String filename_prefix = "";
            String downloadurl = null;
            if (cloudcastStreamInfo == null && !StringUtils.isEmpty(url_preview)) {
                downloadurl = url_preview;
                filename_prefix = "[preview] ";
            } else {
                /* We should have found the correct object here! */
                // final String url_mp3_preview = (String) entries.get("previewUrl");
                entries = (Map<String, Object>) cloudcastStreamInfo;
                /*
                 * 2017-11-15: We can chose between dash, http or hls
                 */
                downloadurl = (String) entries.get("url");
                if (downloadurl == null) {
                    /* Skip objects without streams */
                    continue;
                }
                downloadurl = decode(downloadurl);
                if (StringUtils.isEmpty(downloadurl) || downloadurl.contains("test")) {
                    /* Skip teststreams */
                    continue;
                }
            }
            final String ext = getFileNameExtensionFromString(downloadurl, ".mp3");
            if (!StringUtils.endsWithCaseInsensitive(ext, ".mp3") && !StringUtils.endsWithCaseInsensitive(ext, ".m4a")) {
                /* Skip unsupported extensions. */
                continue;
            }
            final DownloadLink dlink = createDownloadlink(downloadurl);
            dlink.setFinalFileName(filename_prefix + uploaderName + " - " + title + ext);
            /* Packagizer tags */
            dlink.setProperty("title", title);
            dlink.setProperty("uploader", uploaderName);
            dlink.setAvailable(true);
            decryptedLinks.add(dlink);
        }
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