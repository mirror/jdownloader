//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tv.adobe.com" }, urls = { "http://(www\\.)?tv\\.adobe\\.com/watch/[a-z0-9\\-]+/[a-z0-9\\-]+/?" }, flags = { 0 })
public class TvAdbCm extends PluginForDecrypt {

    // dev notes
    // final links seem to not have any session info bound, nor restricted to IP and are hotlinkable, hoster plugin not required.

    /**
     * @author raztoki
     * */
    public TvAdbCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            // No need to randomise URL when exporting offline links!
            // Please always use directhttp && property OFFLINE from decrypters! Using 'OFFLINE' property, ensures when the user checks
            // online status again it will _always_ return offline status.
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "([a-z0-9\\-]+)/?$").getMatch(0) + ".mp4");
            offline.setProperty("OFFLINE", true);
            offline.setAvailable(false);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String html5player = br.getRegex(",html5player:[^\r\n]+").getMatch(-1);
        if (html5player == null) return null;
        // parse for qualities
        String[] qualities = new Regex(html5player, "\\{\"quality\".*?\\}").getColumn(-1);
        final String name = getJson(html5player, "title");
        if (qualities == null || name == null) return null;
        for (final String qual : qualities) {
            final String q = getJson(qual, "quality");
            final String u = getJson(qual, "src");
            if (q == null || u == null) continue;
            final DownloadLink dl = createDownloadlink("directhttp://" + u);
            dl.setFinalFileName(name + " - " + q + u.substring(u.lastIndexOf(".")));
            decryptedLinks.add(dl);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     * 
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        if (result != null) result = result.replaceAll("\\\\/", "/");
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from default 'br' Browser.
     * 
     * @author raztoki
     * */
    private String getJson(final String key) {
        return getJson(br.toString(), key);
    }

    /**
     * Tries to return value of key from JSon response, from provided Browser.
     * 
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return getJson(ibr.toString(), key);
    }

}