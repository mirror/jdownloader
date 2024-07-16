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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "danbooru.donmai.us" }, urls = { "https?://(?:www\\.)?danbooru\\.donmai\\.us/posts\\?(?:page=\\d+\\&)?tags=[^<>\"\\&=\\?/]+" })
public class DanbooruDonmaiUsCrawler extends PluginForDecrypt {
    public DanbooruDonmaiUsCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null) {
            /* 2021-04-19: Loggedin users can add URLs containing more than 2 search-tags... */
            final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.DanbooruDonmaiUs) hostPlugin).loginAPI(acc, false);
            return crawlAPI(param, acc);
        } else {
            return crawlWebsite(param);
        }
    }

    private ArrayList<DownloadLink> crawlAPI(final CryptedLink param, final Account account) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final UrlQuery addedUrlParams = UrlQuery.parse(param.getCryptedUrl());
        final String paramTags = addedUrlParams.get("tags");
        final long accountQueryLimit = account.getLongProperty(jd.plugins.hoster.DanbooruDonmaiUs.PROPERTY_ACCOUNT_QUERY_LIMIT, 3);
        if (paramTags == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String paramTagsDecoded = Encoding.htmlDecode(paramTags);
        if (paramTagsDecoded.contains(",") && paramTagsDecoded.split(",").length > accountQueryLimit) {
            /* Too many tags for current accounts' limitations! */
            throw new AccountRequiredException();
        }
        final ArrayList<Long> dupes = new ArrayList<Long>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(paramTagsDecoded.trim());
        final int maxItemsPerPage = 200;
        final UrlQuery query = new UrlQuery();
        query.add("post[tags]", paramTagsDecoded);
        query.add("limit", Integer.toString(maxItemsPerPage));
        int page = 1;
        do {
            logger.info("Crawling page: " + page);
            query.addAndReplace("page", Integer.toString(page));
            /* 2021-04-19: API read requests are not limited! https://danbooru.donmai.us/forum_topics/13628 */
            br.getPage(jd.plugins.hoster.DanbooruDonmaiUs.API_BASE + "/posts.json?" + query.toString());
            final Object apiResponse = restoreFromString(br.toString(), TypeRef.OBJECT);
            if (!(apiResponse instanceof List)) {
                if (page == 1) {
                    /* Unsupported json response */
                    logger.warning("Unknown error happened");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    logger.info("Stopping because: Received error response");
                    break;
                }
            }
            final List<Object> ressourcelist = (List<Object>) apiResponse;
            if (ressourcelist.size() == 0) {
                if (page == 1) {
                    logger.info("Stopping because: Failed to find any items that match users' search queries");
                    break;
                } else {
                    /* Rare case but this can happen */
                    logger.info("Stopping because: Last page contained ZERO items");
                    break;
                }
            }
            int index = -1;
            boolean pageContainsNewItems = false;
            for (final Object postO : ressourcelist) {
                index += 1;
                final Map<String, Object> entries = (Map<String, Object>) postO;
                final Object idO = entries.get("id");
                if (idO == null) {
                    /* 2021-08-24: This may sometimes happen. Possible API bug. */
                    logger.warning("Possible API bug: Found item without id: Page: " + page + " | Index: " + index);
                    continue;
                }
                final long postID = ((Number) idO).longValue();
                if (!dupes.contains(postID)) {
                    pageContainsNewItems = true;
                    dupes.add(postID);
                    final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/posts/" + postID);
                    jd.plugins.hoster.DanbooruDonmaiUs.parseFileInformationAPI(dl, entries);
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    distribute(dl);
                    ret.add(dl);
                }
            }
            /* Check for stop conditions */
            if (!pageContainsNewItems) {
                logger.info("Stopping because: No new items on current page");
                break;
            } else if (ressourcelist.size() < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains only " + ressourcelist.size() + " of max " + maxItemsPerPage + " items.");
                break;
            } else {
                page++;
            }
        } while (!this.isAbort());
        logger.info("Total numbero found items: " + ret.size());
        return ret;
    }

    private ArrayList<DownloadLink> crawlWebsite(final CryptedLink param) throws AccountRequiredException, IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setAllowedResponseCodes(new int[] { 422 });
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 422) {
            /* 2021-04-19: "You cannot search for more than 2 tags at a time. Upgrade your account to search for more tags at once." */
            throw new AccountRequiredException();
        }
        final String tagsString = new Regex(parameter, "tags=(.+)$").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(tagsString).trim());
        final String url_part = parameter;
        int page_counter = 1;
        final int min_entries_per_page = 15;
        int entries_per_page_current = 0;
        final ArrayList<String> dupes = new ArrayList<String>();
        do {
            if (page_counter > 1) {
                this.br.getPage(url_part + "&page=" + page_counter);
            }
            logger.info("Decrypting: " + this.br.getURL());
            final String[] contentIDs = br.getRegex("id=\"post_(\\d+)\"").getColumn(0);
            if (contentIDs == null || contentIDs.length == 0) {
                logger.warning("Decrypter might be broken for link: " + parameter);
                break;
            }
            entries_per_page_current = contentIDs.length;
            boolean currentPageContainsNewItems = false;
            for (final String contentID : contentIDs) {
                if (!dupes.contains(contentID)) {
                    dupes.add(contentID);
                    final String link = "http://" + this.getHost() + "/posts/" + contentID;
                    final DownloadLink dl = createDownloadlink(link);
                    dl.setAvailable(true);
                    dl.setName(contentID);
                    dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    currentPageContainsNewItems = true;
                }
            }
            if (!currentPageContainsNewItems) {
                logger.info("Stopping because: Current page doesn't contain any new items");
                break;
            } else if (entries_per_page_current < min_entries_per_page) {
                /* 2021-08-23: Ignore this and crawl until we don't find new items anymore. */
                // logger.info("Stopping because: Current page only contained " + entries_per_page_current + " of minimum " +
                // min_entries_per_page + " items per page");
                // break;
                logger.info("Maybe reached end: Current page only contained " + entries_per_page_current + " of minimum " + min_entries_per_page + " items per page");
            }
            page_counter++;
        } while (!this.isAbort());
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Danbooru;
    }
}
