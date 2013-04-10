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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "skins.be" }, urls = { "http://(www\\.)?wallpaper\\.skins\\.be/[a-z\\-]+/\\d+/\\d{3,4}x\\d{3,4}/" }, flags = { 0 })
public class SkinsBe extends PluginForDecrypt {

    public SkinsBe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"/]+) Wallpaper\"/>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<meta name=\"title\" content=\"([^<>\"/]+) Wallpaper @ Skins\\.be\"").getMatch(0);
        if (fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        final String author = new Regex(parameter, "wallpaper\\.skins\\.be/([a-z\\-]+)/\\d+").getMatch(0);
        final String id = new Regex(parameter, "wallpaper\\.skins\\.be/[a-z\\-]+/(\\d+)").getMatch(0);
        String[] resolutions = br.getRegex("\"http://wallpaper\\.skins.be/" + author + "/" + id + "/(\\d{3,4}x\\d{3,4})/\"").getColumn(0);
        if (resolutions == null || resolutions.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String resolution : resolutions) {
            final DownloadLink dl = createDownloadlink("directhttp://http://wallpapers.skins.be/" + author + "/" + author + "-" + resolution + "-" + id + ".jpg");
            dl.setFinalFileName(fpName + " - " + resolution + ".jpg");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        /**
         * Add directlink for addedLink (only needed if they do major HTML changes)
         */
        // final String addedResolution = new Regex(parameter,
        // "wallpaper\\.skins\\.be/[a-z\\-]+/\\d+/(\\d{3,4}x\\d{3,4})/").getMatch(0);
        // final DownloadLink addedLink =
        // createDownloadlink("directhttp://http://wallpapers.skins.be/" +
        // author + "/" + author + "-" + addedResolution + "-" + id + ".jpg");
        // addedLink.setFinalFileName(fpName + " - " + addedResolution +
        // ".jpg");
        // addedLink.setAvailable(true);
        // decryptedLinks.add(addedLink);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName + " Wallpapers");
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}