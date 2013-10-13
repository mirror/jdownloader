//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/((gallery|favourites)/\\d+(\\?offset=\\d+)?|(gallery|favourites)/(\\?offset=\\d+|\\?catpath=[^\r\n\t ]+)?)" }, flags = { 0 })
public class DevArtCm extends PluginForDecrypt {

    /**
     * @author raztoki
     */
    public DevArtCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This plugin grabs range of content depending on parameter.
    // profile.devart.com/gallery/uid*
    // profile.devart.com/favorites/uid*
    // profile.devart.com/gallery/*
    // profile.devart.com/favorites/*
    // * = ?offset=\\d+
    //
    // All of the above formats should support spanning pages, but when
    // parameter contains '?offset=x' it will not span.
    //
    // profilename.deviantart.com/art/uid/ == grabs the 'download image' (best
    // quality available).
    //
    // I've created the plugin this way to allow users to grab as little or as
    // much, content as they wish. Hopefully this wont create any
    // issues.

    private static final String FASTLINKCHECK = "FASTLINKCHECK";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        if (br.containsHTML("The page you were looking for doesn\\'t exist\\.")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }

        // only non /art/ requires packagename
        if (parameter.contains("/gallery/") || parameter.contains("/favourites/")) {
            // find and set username
            String username = br.getRegex("name=\"username\" value=\"([^<>\"]*?)\"").getMatch(0);
            // find and set page type
            String pagetype = "";
            if (parameter.contains("/favourites/")) pagetype = "Favourites";
            if (parameter.contains("/gallery/")) pagetype = "Gallery";
            // find and set pagename
            String pagename = br.getRegex("<span class=\"folder\\-title\">(.*?)</span>").getMatch(0);
            // set packagename
            String fpName = "";
            if ((username != null) && (pagetype != null) && (pagename != null))
                fpName = username + " - " + pagetype + " - " + pagename;
            else if ((username != null) && (pagename != null))
                fpName = username + " - " + pagename;
            else if ((username != null) && (pagetype != null))
                fpName = username + " - " + pagetype;
            else if ((pagetype != null) && (pagename != null)) fpName = pagetype + " - " + pagename;

            // now we find and crawl!
            parsePage(decryptedLinks, parameter);

            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName);
                fp.setProperty("ALLOW_MERGE", true);
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String parameter) throws Exception {
        final boolean fastcheck = SubConfiguration.getConfig("deviantart.com").getBooleanProperty(FASTLINKCHECK, false);
        final String grab = br.getRegex("<smoothie q=(.*?)(class=\"folderview-bottom\"></div>|div id=\"gallery_pager\")").getMatch(0);
        String[] artlinks = new Regex(grab, "\"(https?://[\\w\\.\\-]*?deviantart\\.com/art/[\\w\\-]+)\"").getColumn(0);
        // href="/gallery/?set=46236989&amp;offset=24">Next</a></li><
        String nextPage = br.getRegex("href=\"(/(gallery|favourites)/[^<>\"]*?((\\?|&amp;|&)offset=\\d+))\">Next</a>").getMatch(0);
        if (artlinks == null || artlinks.length == 0) {
            logger.warning("Possible Plugin error, with finding /art/ links: " + parameter);
            return;
        }
        if (artlinks != null && artlinks.length != 0) {
            for (final String al : artlinks) {
                final DownloadLink fina = createDownloadlink(al);
                if (fastcheck) fina.setAvailable(true);
                ret.add(fina);
            }
        }
        if (nextPage != null && !parameter.contains("offset=")) {
            br.getPage(HTMLEntities.unhtmlentities(nextPage));
            parsePage(ret, parameter);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}