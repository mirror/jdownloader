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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.hoster.XiaoshenkeNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FullpornerCom extends PornEmbedParser {
    public FullpornerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fullporner.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/watch/([a-f0-9]{24})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = super.decryptIt(param, progress);
        String title = br.getRegex("class=\"single-video-title[^\"]*\">\\s*<h2>([^<]+)</h2>").getMatch(0);
        if (decryptedLinks.size() > 0 && title != null) {
            /* Most of all times we grab xiaoshenke.net URLs which need a special property. */
            title = Encoding.htmlDecode(title).trim();
            for (final DownloadLink link : decryptedLinks) {
                /* Set special properties for madaddy.cc URLs */
                final String host = Browser.getHost(link.getPluginPatternMatcher());
                if (host.equals(jd.plugins.hoster.XiaoshenkeNet.getPluginDomains().get(0)[0])) {
                    link.setProperty(XiaoshenkeNet.PROPERTY_TITLE, title);
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    protected boolean isOffline(Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }
}