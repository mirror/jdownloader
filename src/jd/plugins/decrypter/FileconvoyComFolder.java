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
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FileconvoyComFolder extends PluginForDecrypt {
    public FileconvoyComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fileconvoy.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/dfl\\.php\\?id=([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] links = br.getRegex("(/gf\\.php\\?id=[^/\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        for (final String singleLink : links) {
            final DownloadLink file = createDownloadlink(br.getURL(singleLink).toString());
            file.setDefaultPlugin(plg);
            final String quotedRelativeURL = Pattern.quote(singleLink);
            final Regex finfo = br.getRegex(quotedRelativeURL + "\"[^>]*>([^<]+)</a></td><td>([^<]+)</td>");
            if (finfo.patternFind()) {
                file.setName(Encoding.htmlDecode(finfo.getMatch(0)).trim());
                file.setDownloadSize(SizeFormatter.getSize(finfo.getMatch(1)));
            }
            file.setAvailable(true);
            ret.add(file);
        }
        return ret;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*The file set you are looking for is no longer available")) {
            return true;
        } else if (br.containsHTML("Invalid retrieval request")) {
            /* 2020-12-03 */
            return true;
        } else {
            return false;
        }
    }
}
