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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.TiktokConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { jd.plugins.hoster.TiktokCom.class })
public class TiktokCom extends PluginForDecrypt {
    public TiktokCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.hoster.TiktokCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_USER = "https?://[^/]+/(?:@|share/user/\\d+)(.+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        if (param.getCryptedUrl().matches("https?://vm\\..+")) {
            /* Single redirect URLs */
            br.setFollowRedirects(false);
            br.getPage(param.getCryptedUrl().replaceFirst("http://", "https://"));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (plg.canHandle(param.getCryptedUrl())) {
            /* Single video URL --> Is handled by host plugin */
            decryptedLinks.add(this.createDownloadlink(param.getCryptedUrl()));
            return decryptedLinks;
        } else if (param.getCryptedUrl().matches(TYPE_USER)) {
            crawlProfileWebsite(param, decryptedLinks);
        } else {
            logger.info("Unsupported URL: " + param.getCryptedUrl());
        }
        return decryptedLinks;
    }

    public ArrayList<DownloadLink> crawlProfileWebsite(final CryptedLink param, final ArrayList<DownloadLink> ret) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String usernameSlug = new Regex(br.getURL(), TYPE_USER).getMatch(0);
        if (usernameSlug == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (jd.plugins.hoster.TiktokCom.isBotProtectionActive(this.br)) {
            throw new DecrypterRetryException(RetryReason.CAPTCHA, "Bot protection active, cannot crawl any items of user " + usernameSlug, null, null);
        }
        /* 2022-01-07: New simple handling */
        // TODO: Implement pagination, see: https://svn.jdownloader.org/issues/86758
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        FilePackage fp = null;
        String username = null;
        try {
            /* First try the "hard" way */
            String json = br.getRegex("window\\['SIGI_STATE'\\]\\s*=\\s*(\\{.*?\\});").getMatch(0);
            if (json == null) {
                json = br.getRegex("<script\\s*id\\s*=\\s*\"SIGI_STATE\"[^>]*>\\s*(\\{.*?\\});?\\s*</script>").getMatch(0);
            }
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final Map<String, Map<String, Object>> itemModule = (Map<String, Map<String, Object>>) entries.get("ItemModule");
            final Map<String, Object> userPost = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "ItemList/user-post");
            final List<Map<String, Object>> preloadList = (List<Map<String, Object>>) userPost.get("preloadList");
            /* Typically we get up to 30 items per page. In some cases we get only 28 or 29 for some reason. */
            final Collection<Map<String, Object>> videos = itemModule.values();
            int index = 0;
            for (final Map<String, Object> video : videos) {
                final Map<String, Object> preloadInfo = preloadList.get(index);
                final Map<String, Object> stats = (Map<String, Object>) video.get("stats");
                final Map<String, Object> streamInfo = (Map<String, Object>) video.get("video");
                final String author = video.get("author").toString();
                final String videoID = (String) video.get("id");
                final String createTimeStr = (String) video.get("createTime");
                final String description = (String) video.get("desc");
                String directurl = (String) streamInfo.get("downloadAddr");
                if (StringUtils.isEmpty(directurl)) {
                    directurl = (String) streamInfo.get("playAddr");
                }
                if (StringUtils.isEmpty(directurl)) {
                    directurl = preloadInfo.get("url").toString();
                }
                if (fp == null) {
                    username = author;
                    fp = FilePackage.getInstance();
                    fp.setName(username);
                }
                final DownloadLink dl = this.createDownloadlink("https://www.tiktok.com/@" + author + "/video/" + videoID);
                final String dateFormatted = formatDate(Long.parseLong(createTimeStr));
                dl.setFinalFileName(dateFormatted + "_@" + author + "_" + videoID + ".mp4");
                dl.setAvailable(true);
                jd.plugins.hoster.TiktokCom.setDescriptionAndHashtags(dl, description);
                dl.setProperty(jd.plugins.hoster.TiktokCom.PROPERTY_USERNAME, author);
                dl.setProperty(jd.plugins.hoster.TiktokCom.PROPERTY_USER_ID, video.get("authorId"));
                dl.setProperty(jd.plugins.hoster.TiktokCom.PROPERTY_DATE, dateFormatted);
                jd.plugins.hoster.TiktokCom.setLikeCount(dl, (Number) stats.get("diggCount"));
                jd.plugins.hoster.TiktokCom.setPlayCount(dl, (Number) stats.get("playCount"));
                jd.plugins.hoster.TiktokCom.setShareCount(dl, (Number) stats.get("shareCount"));
                jd.plugins.hoster.TiktokCom.setCommentCount(dl, (Number) stats.get("commentCount"));
                if (!StringUtils.isEmpty(directurl)) {
                    dl.setProperty(jd.plugins.hoster.TiktokCom.PROPERTY_DIRECTURL, directurl);
                }
                dl._setFilePackage(fp);
                ret.add(dl);
                // distribute(dl);
                index++;
            }
            if ((Boolean) userPost.get("hasMore") && cfg.isAddDummyURLProfileCrawlerWebsiteModeMissingPagination()) {
                final DownloadLink dummy = createLinkCrawlerRetry(getCurrentLink(), new DecrypterRetryException(RetryReason.FILE_NOT_FOUND));
                dummy.setFinalFileName("CANNOT_CRAWL_MORE_THAN_" + videos.size() + "_ITEMS_OF_PROFILE_" + usernameSlug);
                dummy.setComment("This crawler plugin cannot handle pagination yet thus it is currently impossible to crawl more than " + videos.size() + " items. Check our forum for more info: https://board.jdownloader.org/showthread.php?t=79982");
                dummy._setFilePackage(fp);
                // distribute(dummy);
                ret.add(dummy);
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        if (ret.isEmpty()) {
            /* Last chance fallback */
            logger.warning("Fallback to plain html handling");
            final String[] videoIDs = br.getRegex(usernameSlug + "/video/(\\d+)\"").getColumn(0);
            for (final String videoID : videoIDs) {
                final DownloadLink dl = this.createDownloadlink("https://www.tiktok.com/@" + usernameSlug + "/video/" + videoID);
                dl.setName("@" + usernameSlug + "_" + videoID + ".mp4");
                dl.setAvailable(cfg.isEnableFastLinkcheck());
                dl._setFilePackage(fp);
                ret.add(dl);
            }
        }
        return ret;
    }

    public static String formatDate(final long date) {
        if (date <= 0) {
            return null;
        }
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date * 1000);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date);
        }
        return formattedDate;
    }
}
