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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.NewgroundsCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "newgrounds.com" }, urls = { "https?://(?:\\w+\\.)?newgrounds\\.com/(?:art|audio|movies|games)(/view/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+)?/?$" })
public class NewgroundsComDecrypter extends PluginForDecrypt {
    public NewgroundsComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_ART   = ".+/art/?$";
    private static final String TYPE_AUDIO = ".+/audio/?$";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    @Override
    public void init() {
        try {
            /* 2020-10-26: They have quite strict rate-limits ... */
            Browser.setRequestIntervalLimitGlobal(getHost(), 1000);
        } catch (final Throwable e) {
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            /* Login whenever possible */
            final NewgroundsCom hosterPlugin = (NewgroundsCom) this.getNewPluginForHostInstance(this.getHost());
            hosterPlugin.login(account, false);
        }
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Invalid item/user-profile */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*You must be logged in, and at least 18 years")) {
            throw new AccountRequiredException();
        }
        final Regex singleItem = new Regex(param.getCryptedUrl(), "/view/(.+)");
        if (singleItem.matches()) {
            /* Single item */
            /*
             * 2020-02-03: New: Such URLs may contain multiple URLs --> Crawl all of them. This linktype was initially handled in the
             * hosterplugin.
             */
            final String itemSlug = singleItem.getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(itemSlug);
            final String[] pics = br.getRegex("\"(https?://art\\.ngfiles\\.com/(?:comments|images)/[^<>\"]+)\"").getColumn(0);
            if (pics == null || pics.length == 0) {
                return null;
            }
            for (final String pic : pics) {
                final DownloadLink dl = this.createDownloadlink("directhttp://" + pic);
                dl.setAvailable(true);
                ret.add(dl);
                dl._setFilePackage(fp);
            }
        } else {
            /* Crawl all items of a user */
            final String username = new Regex(param.getCryptedUrl(), "https?://([^/]+)\\.newgrounds\\.com/").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(username);
            int page = 1;
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            String nextPageURL = "https://" + username + ".newgrounds.com/art?page=" + page + "&isAjaxRequest=1";
            do {
                br.getPage(nextPageURL);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    /* Invalid profile */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.containsHTML("(?)does not have any art submissions\\.?\\s*</p>")) {
                    /* Profile that does not contain any items. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                // nextPage = (String) entries.get("more");
                final Map<String, List<String>> years = (Map<String, List<String>>) entries.get("items");
                final Set<Entry<String, List<String>>> yearsEntrySet = years.entrySet();
                for (final Entry<String, List<String>> entry : yearsEntrySet) {
                    // final String yearStr = entry.getKey();
                    final List<String> items = entry.getValue();
                    for (final String html : items) {
                        String title = new Regex(html, "title\\s*=\\s*\"([^<>\"]+)\"").getMatch(0);
                        if (title == null) {
                            title = new Regex(html, "alt\\s*=\\s*\"([^<>\"]+)\"").getMatch(0);
                        }
                        String url = new Regex(html, "((?:https?:)?//(?:\\w+\\.)?newgrounds\\.com/(?:(?:art|portal)/view|audio/listen)/[^<>\"\\']+)").getMatch(0);
                        if (StringUtils.isEmpty(url)) {
                            logger.warning("Found unsupported item: " + html);
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        /* Convert possible crippled/relative URLs to absolute URLs. */
                        url = br.getURL(url).toString();
                        final DownloadLink dl = createDownloadlink(url);
                        if (title != null) {
                            title = Encoding.htmlDecode(title).trim();
                            dl.setName(title);
                        }
                        /* Allow these items to go back info crawler as each link can lead to multiple media files. */
                        // dl.setAvailable(true);
                        dl._setFilePackage(fp);
                        // if (url.matches(TYPE_AUDIO)) {
                        // dl.setMimeHint(CompiledFiletypeFilter.AudioExtensions.MP3);
                        // } else if (url.matches(TYPE_ART)) {
                        // dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
                        // } else {
                        // /* Movies & games */
                        // dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                        // }
                        ret.add(dl);
                        distribute(dl);
                    }
                }
                logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
                if (page == 1 && ret.isEmpty()) {
                    /* This profile does not contain any items */
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, username);
                }
                final String load_more_html = (String) entries.get("load_more");
                nextPageURL = (String) entries.get("url");
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else if (!StringUtils.containsIgnoreCase(load_more_html, "page=" + (page + 1)) || StringUtils.isEmpty(nextPageURL)) {
                    logger.info("Stopping because: Reached last page: " + page);
                    break;
                } else {
                    /* Continue to next page */
                    page++;
                }
            } while (true);
        }
        return ret;
    }
}
