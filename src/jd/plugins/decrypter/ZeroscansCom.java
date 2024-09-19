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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ZeroscansCom extends PluginForDecrypt {
    public ZeroscansCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "zscans.com", "zeroscans.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/comics/([a-z0-9\\-]+)(/(\\d+))?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("^http://", "https://");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex urlinfo = new Regex(contenturl, this.getSupportedLinks());
        final String comicSlug = urlinfo.getMatch(0);
        final String chapterID = urlinfo.getMatch(2);
        if (chapterID != null) {
            /* Crawl all pages of a single comic */
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.getRequest().getHtmlCode()));
            String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
                title = title.replaceFirst("(?i) • Zero Scans$", "");
            }
            final String urlsJson = br.getRegex("good_quality:\\[([^\\]]+)").getMatch(0);
            final String[] links = PluginJSonUtils.unescape(urlsJson).replace("\"", "").split(",");
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final int padLength = StringUtils.getPadLength(links.length);
            int position = 1;
            for (String singleLink : links) {
                if (!singleLink.startsWith("http") && !singleLink.startsWith("/")) {
                    singleLink = "https://" + br.getHost() + "/storage/" + singleLink;
                }
                final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(singleLink));
                dl.setFinalFileName(StringUtils.formatByPadLength(padLength, position) + "_" + Plugin.getFileNameFromURL(new URL(singleLink)));
                dl.setAvailable(true);
                ret.add(dl);
                position++;
            }
            final FilePackage fp = FilePackage.getInstance();
            if (title != null) {
                fp.setName(title);
            }
            fp.setPackageKey("zeroscans://chapter/" + chapterID);
            fp.addLinks(ret);
        } else {
            /* Crawl all chapters of a comic-series */
            final String comicSeriesID = br.getRegex("id:(\\d+)").getMatch(0);
            if (comicSeriesID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int page = 1;
            final HashSet<String> dupes = new HashSet<String>();
            do {
                br.getPage("/swordflake/comic/" + comicSeriesID + "/chapters?sort=desc&page=" + page);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> data = (Map<String, Object>) entries.get("data");
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) data.get("data");
                int numberofNewItems = 0;
                for (final Map<String, Object> ressource : ressourcelist) {
                    final String thisChapterID = ressource.get("id").toString();
                    if (!dupes.add(thisChapterID)) {
                        continue;
                    }
                    numberofNewItems++;
                    final DownloadLink dlchapter = this.createDownloadlink("https://" + this.getHost() + "/comics/" + comicSlug + "/" + thisChapterID);
                    ret.add(dlchapter);
                    distribute(dlchapter);
                }
                final String next_page_url = (String) data.get("next_page_url");
                logger.info("Crawled page " + page + " | Found items: " + ret.size());
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else if (StringUtils.isEmpty(next_page_url)) {
                    logger.info("Stopping because: Reached last page");
                    break;
                } else if (numberofNewItems == 0) {
                    logger.info("Stopping because: Failed to find any new items on current page");
                    break;
                } else {
                    /* Continue to next page */
                    br.getPage(next_page_url);
                }
            } while (!this.isAbort());
        }
        return ret;
    }
}
