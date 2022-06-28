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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KemonoPartyCrawler extends PluginForDecrypt {
    public KemonoPartyCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "coomer.party" }); // onlyfans.com content
        ret.add(new String[] { "kemono.party" }); // content of other websites such as patreon.com
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[^/]+/user/([^/]+)/post/\\d+");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_PROFILE = "https?://[^/]+/([^/]+)/user/(\\d+)$";
    private final String TYPE_POST    = "https?://[^/]+/([^/]+)/user/([^/]+)/post/(\\d+)$";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_POST)) {
            return this.crawlPost(param, progress);
        } else {
            /* Unsupported URL --> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> crawlPost(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_POST);
        if (!urlinfo.matches()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String portal = urlinfo.getMatch(0);
        final String userID = urlinfo.getMatch(1);
        final String postID = urlinfo.getMatch(2);
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().matches(TYPE_POST)) {
            /* E.g. redirect to main page of user because single post does not exist */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String postTitle = br.getRegex("class=\"post__title\">\\s*<span>([^<]+)</span>").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (postTitle != null) {
            fp.setName(portal + " - " + userID + " - " + postID + " - " + Encoding.htmlDecode(postTitle));
        } else {
            /* Fallback */
            fp.setName(portal + " - " + userID + " - " + postID);
        }
        final String[] directURLs = br.getRegex("\"[^\"]*(/data/[^\"]+)").getColumn(0);
        for (String directURL : directURLs) {
            directURL = br.getURL(directURL).toString();
            final DownloadLink media = this.createDownloadlink("directhttp://" + directURL);
            media.setAvailable(true);
            ret.add(media);
        }
        fp.addLinks(ret);
        return ret;
    }
}
