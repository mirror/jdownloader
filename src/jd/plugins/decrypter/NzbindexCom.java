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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class NzbindexCom extends PluginForDecrypt {
    public NzbindexCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "nzbindex.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:(?:search/)?\\?q=.+|collection/\\d+/[^/]+\\.nzb)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_SINGLE_SEARCH = "https?://[^/]+/(?:search/)?\\?q=.+";
    private static final String TYPE_SINGLE_NZB    = "https?://[^/]+/collection/((\\d+)/([^/]+\\.nzb))";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches(TYPE_SINGLE_SEARCH)) {
            /* URL containing search-query -> Crawl first page of search-results and return direct-URLs to NZB files. */
            final UrlQuery query = UrlQuery.parse(param.getCryptedUrl());
            final int page;
            final String pageStr = query.get("p");
            if (pageStr != null && pageStr.matches("\\d+")) {
                page = Integer.parseInt(pageStr);
            } else {
                query.addAndReplace("p", "0");
                page = 0;
            }
            final String searchString = query.get("q");
            final String url = query.toString();
            br.getPage("https://" + this.getHost() + "/search/json?" + url.substring(url.lastIndexOf("?") + 1) + "&p=0");
            final Map<String, Object> root = restoreFromString(br.toString(), TypeRef.MAP);
            final List<Map<String, Object>> results = (List<Map<String, Object>>) root.get("results");
            final Map<String, Object> stats = (Map<String, Object>) root.get("stats");
            final int max_page = ((Number) stats.get("max_page")).intValue();
            if (max_page > 0) {
                logger.info("Multiple pages available! We'll only crawl page " + (page + 1) + " of " + (max_page + 1));
            }
            if (results.isEmpty()) {
                /* User was looking for search query with zero results. */
                final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "NO_SEARCH_RESULTS_FOR_" + searchString, "No results for search query: " + searchString);
                decryptedLinks.add(dummy);
                return decryptedLinks;
            }
            int skippedIncompleteElements = 0;
            for (final Map<String, Object> result : results) {
                if ((Boolean) result.get("complete") == Boolean.FALSE) {
                    /* Skip incomplete search-results */
                    skippedIncompleteElements++;
                    continue;
                }
                final String name = (String) result.get("name");
                /* According to: https://nzbindex.com/js/nzbindex-all.min.js */
                final String nameForURL = name.replaceAll("[^a-zA-Z0-9\\.\\-\\s]", "").replaceAll("(?i)yenc", "").trim().replaceAll("\\s", "-");
                final String nzbURL = "https://nzbindex.com/download/" + result.get("id") + "/" + nameForURL + ".nzb";
                decryptedLinks.add(this.createDownloadlink(nzbURL));
            }
            if (decryptedLinks.isEmpty()) {
                final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "ALL_SEARCH_RESULTS_INVOMPLETE_FOR_" + searchString, "Found only incomplete results for search query: " + searchString);
                decryptedLinks.add(dummy);
                return decryptedLinks;
            }
            if (skippedIncompleteElements > 0) {
                logger.info("Number of skipped incomplete elements: " + skippedIncompleteElements);
            }
        } else if (param.getCryptedUrl().matches(TYPE_SINGLE_NZB)) {
            /* TODO: Make sure this somehow gets handled before "genericautocontainer" plugin. */
            /* Single nzb file --> Modify URL so we get a direct URL -> Let generic parser do the rest */
            final String directURTL = "https://" + this.getHost() + "/download/" + new Regex(param.getCryptedUrl(), TYPE_SINGLE_NZB).getMatch(0);
            decryptedLinks.add(this.createDownloadlink(directURTL));
        } else {
            /* Developer mistake -> Unsupported URL */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return decryptedLinks;
    }
}
