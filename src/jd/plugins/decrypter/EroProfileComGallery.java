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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eroprofile.com" }, urls = { "http://(www\\.)?eroprofile\\.com/m/photos/album/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class EroProfileComGallery extends PluginForDecrypt {

    public EroProfileComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> correctedpages = new ArrayList<String>();
        correctedpages.add("1");
        String parameter = param.toString();
        br.setCookie("http://eroprofile.com/", "lang", "en");
        br.getPage(parameter);
        final String fpName = br.getRegex("Browse photos from album \\&quot;([^<>\"]*?)\\&quot;<").getMatch(0);
        final String[] pages = br.getRegex("\\?pnum=(\\d+)\"").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String page : pages)
                if (!correctedpages.contains(page)) correctedpages.add(page);
        }
        for (final String page : correctedpages) {
            if (!page.equals("1")) br.getPage(parameter + "?pnum=" + page);
            String[][] links = br.getRegex("<table cellspacing=\"0\"><tr><td><a href=\"(/m/photos/view/([A-Za-z0-9\\-_]+))\"").getMatches();
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink[] : links) {
                final DownloadLink dl = createDownloadlink("http://www.eroprofile.com" + singleLink[0]);
                // final filename is set later in hosterplugin
                dl.setName(singleLink[1] + ".jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
