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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.SankakucomplexCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SankakucomplexComCrawler extends PluginForDecrypt {
    public SankakucomplexComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sankakucomplex.com" });
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
            ret.add("https?://(?:(www|beta)\\.)?" + buildHostsPatternPart(domains) + "/[a-z]{2}/books/\\d+");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_BOOK = "https?://[^/]+/([a-z]{2})/books/(\\d+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        return crawlBook(param);
    }

    private ArrayList<DownloadLink> crawlBook(final CryptedLink param) throws Exception {
        final String bookID = new Regex(param.getCryptedUrl(), TYPE_BOOK).getMatch(1);
        if (bookID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage("https://capi-v2.sankakucomplex.com/pools/" + bookID + "?lang=de&includes[]=series&exceptStatuses[]=deleted");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> author = (Map<String, Object>) entries.get("author");
        String bookTitle = (String) entries.get("name_en");
        if (StringUtils.isEmpty(bookTitle)) {
            bookTitle = (String) entries.get("name_ja");
        }
        final List<Map<String, Object>> posts = (List<Map<String, Object>>) entries.get("posts");
        int page = 0;
        for (final Map<String, Object> post : posts) {
            final DownloadLink link = this.createDownloadlink("https://beta.sankakucomplex.com/de/post/show/" + post.get("id") + "?tags=pool%3A" + bookID + "&page=" + page);
            link.setProperty(SankakucomplexCom.PAGE_NUMBER, page);
            SankakucomplexCom.parseFileInfoAndSetFilenameAPI(link, post);
            ret.add(link);
            page++;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(author.get("name") + " - " + bookTitle);
        fp.addLinks(ret);
        return ret;
    }
}
