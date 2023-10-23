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
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class OneTwoThreeEnjoy extends antiDDoSForDecrypt {
    public OneTwoThreeEnjoy(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "upmovies.to", "123enjoy.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/watch/.+\\.html?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String contenturl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String itemID = new Regex(contenturl, "/watch/\\w+-([^/.]+)").getMatch(0);
        final String[] sources = br.getRegex("\\(\\s*Base64\\.decode\\s*\\(\\s*\"([^\"]+)").getColumn(0);
        if (sources != null && sources.length > 0) {
            for (String source : sources) {
                source = Encoding.Base64Decode(source);
                ret.add(this.createDownloadlink(source));
            }
        }
        if (StringUtils.isNotEmpty(itemID)) {
            final String[] mirrorurls = br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+/watch/[^\"]+" + Pattern.quote(itemID) + "[^\"]*)\"").getColumn(0);
            final HashSet<String> dupes = new HashSet<String>();
            for (String mirrorurl : mirrorurls) {
                mirrorurl = br.getURL(mirrorurl).toExternalForm();
                if (mirrorurl.equals(br.getURL())) {
                    /* Skip mirror which we are currently processing. */
                    continue;
                } else if (!dupes.add(mirrorurl)) {
                    continue;
                }
                ret.add(this.createDownloadlink(mirrorurl));
            }
            logger.info("Number of detected other items/mirrors: " + dupes.size());
        }
        String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            title = title.replaceFirst("(?i)^Watch\\s*", "");
            title = title.replaceFirst("(?i)\\s*Online For Free$", "");
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.setAllowMerge(true);
            fp.setAllowInheritance(true);
            fp.addLinks(ret);
        }
        return ret;
    }
}