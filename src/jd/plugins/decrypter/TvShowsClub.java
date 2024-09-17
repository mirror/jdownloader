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
import java.util.Collections;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class TvShowsClub extends PluginForDecrypt {
    public TvShowsClub(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "tvshows.club", "tvshows.show", "tvshows.me", "tvshows.ac" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[^/]+(/[^/]+)?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String urlSlug = br._getURL().getPath().split("/")[1];
        String title = br.getRegex("<title>(?:TV Show\\s+)?([^<]+)(?:\\s+(?:Today&#039\\;s TV Series|TV show. List of all seasons))").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = urlSlug.replace("-", " ").trim();
        }
        final ArrayList<String> episodes = new ArrayList<String>();
        Collections.addAll(episodes, br.getRegex("<div[^>]+itemprop\\s*=\\s*\"containsSeason\"[^>]+>\\s*<div[^>]+class\\s*=\\s*\"card\"[^>]*>\\s*<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0));
        final String[] base64Strings = br.getRegex("<script defer src=\"data:text/javascript;base64,([a-zA-Z0-9_/\\+\\=\\-%]+)").getColumn(0);
        for (final String base64String : base64Strings) {
            final String decodedStr = Encoding.Base64Decode(base64String);
            final String[] encodedLinks = new Regex(decodedStr, "arr\\s*\\[\\s*\"[^\"]+\"\\s*\\]\\s*=\\s*\"([^\"]+)\"").getColumn(0);
            if (encodedLinks != null && encodedLinks.length > 0) {
                for (final String encodedLink : encodedLinks) {
                    String decodedLink = Encoding.Base64Decode(encodedLink);
                    /* Check for double-encoded data */
                    if (StringUtils.startsWithCaseInsensitive(decodedLink, "aHR")) {
                        decodedLink = Encoding.Base64Decode(decodedLink);
                    }
                    if (StringUtils.isNotEmpty(decodedLink)) {
                        ret.add(this.createDownloadlink(decodedLink));
                    }
                }
                break;
            }
        }
        if (episodes.size() > 0) {
            logger.info("Found episode-URLs which will go into this crawler again: " + episodes.size());
            for (String link : episodes) {
                link = Encoding.htmlDecode(link).replaceAll("^//", "https://");
                if (link.startsWith("/")) {
                    link = br.getURL(link).toExternalForm();
                }
                ret.add(createDownloadlink(link));
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.isNotEmpty(title)) {
            title = Encoding.htmlDecode(title).trim();
            title = title.replaceFirst("(?i) Download.?$", "");
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(ret);
        }
        return ret;
    }
}