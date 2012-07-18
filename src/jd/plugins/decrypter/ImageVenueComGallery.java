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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagevenue.com" }, urls = { "http://(www\\.)?img\\d+\\.imagevenue\\.com/galshow\\.php\\?gal=gallery_.+" }, flags = { 0 })
public class ImageVenueComGallery extends PluginForDecrypt {

    public ImageVenueComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        final String[] links = br.getRegex("<a href=\"(http://img\\d+\\.imagevenue\\.com/img\\.php\\?image=[^<>\"]*?)\" target=\"_blank\"><img src=\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            decryptedLinks.add(createDownloadlink(singleLink));
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(parameter, "gal=gallery_(.+)").getMatch(0));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
