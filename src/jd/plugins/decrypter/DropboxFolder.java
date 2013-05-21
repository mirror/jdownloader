//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropbox.com" }, urls = { "https?://(www\\.)?dropbox\\.com/gallery/\\d+/\\d+/[^?]+\\?h=[0-9a-f]+" }, flags = { 0 })
public class DropboxFolder extends PluginForDecrypt {

    private final String[] urlAttrs = { "video_url", "original", "extralarge", "large", "thumbnail" };

    public DropboxFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getPage(parameter);
        if (br.containsHTML("<div id=\"errorbox\">")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<h2>(?:Pictures|Fotos) in '(.*?)'</h2>").getMatch(0);
        final String[] links = br.getRegex("photos\\.push\\(\\{(.*?)\\}\\);").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        for (String singleLink : links) {
            // Finds the JSON attribute with one of the keys of the array, by
            // the order given (i.e. always highest quality)
            for (String urlAttr : urlAttrs) {
                String url = new Regex(singleLink, "'" + urlAttr + "': *'(.*?)'").getMatch(0);
                if (url != null && url.length() != 0) {
                    url = unescape(url);
                    decryptedLinks.add(createDownloadlink(url));
                    break;
                }
            }
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */

        final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
        if (plugin == null) throw new IllegalStateException("youtube plugin not found!");

        return jd.plugins.hoster.Youtube.unescape(s);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}