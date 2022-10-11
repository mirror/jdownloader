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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.JpgChurch;

import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { JpgChurch.class })
public class JpgChurchCrawler extends PluginForDecrypt {
    public JpgChurchCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return JpgChurch.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?!img/).+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String ogURL = HTMLSearch.searchMetaTag(br, "og:url");
        String seek = null;
        if (ogURL != null) {
            seek = UrlQuery.parse(Encoding.htmlDecode(ogURL)).get("seek");
            if (seek != null) {
                seek = Encoding.htmlDecode(seek);
            }
        }
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String token = br.getRegex("PF\\.obj.config.auth_token\\s*=\\s*\"([a-f0-9]+)\"").getMatch(0);
        final String apiurl = br.getRegex("PF\\.obj.config.json_api\\s*=\\s*\"(https?://[^\"]+)\"").getMatch(0);
        final String dataparamshidden = br.getRegex("data-params-hidden=\"([^\"]+)").getMatch(0);
        int page = 1;
        final UrlQuery query = dataparamshidden != null ? UrlQuery.parse(dataparamshidden) : new UrlQuery();
        query.add("action", "list");
        query.add("list", "images"); // contained in dataparamshidden
        query.add("sort", "date_desc");
        // query.add("page", "1"); // added later in do-while-loop
        // query.add("userid", ""); // contained in dataparamshidden
        // query.add("albumid", ""); // contained in dataparamshidden
        // query.add("from", "user"); // contained in dataparamshidden
        final String list = query.get("list");
        final String from = query.get("from");
        final String albumid = query.get("albumid");
        final String userid = query.get("userid");
        if (list != null) {
            query.add("params_hidden%5Blist%5D", list);
        }
        if (userid != null) {
            query.add("params_hidden%5Buserid%5D", userid);
        }
        if (from != null) {
            query.add("params_hidden%5Bfrom%5D", from);
        }
        if (albumid != null) {
            query.add("params_hidden%5Balbumid%5D", albumid);
        }
        query.add("params_hidden%5Bparams_hidden%5D", "");
        if (token != null) {
            query.add("auth_token", token);
        }
        final String siteTitle = HTMLSearch.searchMetaTag(br, "og:title", "twitter:title");
        FilePackage fp = null;
        if (siteTitle != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(siteTitle).trim());
        }
        final HashSet<String> dupes = new HashSet<String>();
        final int maxItemsPerPage = 42;
        do {
            final String[] htmls = br.getRegex("<div class=\"list-item [^\"]+\"(.*?)class=\"btn-lock fas fa-eye-slash\"[^>]*></div>").getColumn(0);
            int numberofNewItems = 0;
            for (final String html : htmls) {
                final String url = new Regex(html, "<a href=\"(https?://[^\"]+)\" class=\"image-container --media\">").getMatch(0);
                final String title = new Regex(html, "data-title=\"([^\"]+)\"").getMatch(0);
                final String filesizeBytesStr = new Regex(html, "data-size=\"(\\d+)\"").getMatch(0);
                if (url == null || title == null || filesizeBytesStr == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (!dupes.add(url)) {
                    continue;
                }
                numberofNewItems++;
                final DownloadLink link = this.createDownloadlink(url);
                link.setName(this.correctOrApplyFileNameExtension(Encoding.htmlDecode(title).trim(), ".jpg"));
                link.setVerifiedFileSize(Long.parseLong(filesizeBytesStr));
                link.setAvailable(true);
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                distribute(link);
                ret.add(link);
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (apiurl == null || token == null || seek == null) {
                /* This should never happen */
                logger.info("Stopping because: At least one mandatory pagination param is missing");
                break;
            } else if (numberofNewItems == 0) {
                logger.info("Stopping because: Current page contains no new items");
                break;
            } else if (numberofNewItems < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains only " + numberofNewItems + " new of max " + maxItemsPerPage + " items");
                break;
            } else {
                /* TODO: Fix pagination */
                page++;
                query.addAndReplace("page", Integer.toString(page));
                query.addAndReplace("seek", URLEncode.encodeURIComponent(seek));
                br.postPage(apiurl, query);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
                br.getRequest().setHtmlCode(entries.get("html").toString());
                seek = entries.get("seekEnd").toString();
            }
        } while (true);
        return ret;
    }
}
