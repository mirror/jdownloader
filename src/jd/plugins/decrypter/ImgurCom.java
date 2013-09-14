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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgur.com" }, urls = { "https?://((www|i)\\.)?imgur\\.com(/gallery|/a|/download)?/(?!register|contact|removalrequest|stats|https)[A-Za-z0-9]{5,}" }, flags = { 0 })
public class ImgurCom extends PluginForDecrypt {

    public ImgurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("https://", "http://").replace("/all$", "");

        if (parameter.contains("imgur.com/a/")) {
            final String albumID = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
            br.getPage("http://api.imgur.com/2/album/" + albumID + "/images");
            if (br.containsHTML("<message>Album not found</message>")) {
                logger.info("Link offline OR empty album: " + parameter);
                return decryptedLinks;
            }
            final String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            // using links (i.imgur.com/imgUID(s)?.extension) seems to be problematic, it can contain 's' (imgUID + s +
            // .extension), but not always! imgUid.endswith("s") is also a valid uid, so you can't strip them!
            String[] items = br.getRegex("<item>(.*?)</item>").getColumn(0);
            if (items == null || items.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String item : items) {
                final String filesize = new Regex(item, "<size>(\\d+)</size>").getMatch(0);
                final String imgUID = new Regex(item, "<hash>([A-Za-z0-9]+)</hash>").getMatch(0);
                final String directlink = new Regex(item, "<original>(https?://i\\.imgur\\.com/[^<>\"]*?)</original>").getMatch(0);
                if (imgUID == null || filesize == null || directlink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final String filename = new Regex(directlink, "i\\.imgur\\.com/(.+)").getMatch(0);
                final DownloadLink dl = createDownloadlink("http://imgurdecrypted.com/download/" + imgUID);
                dl.setName(filename);
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setAvailable(true);
                dl.setProperty("imgUID", imgUID);
                dl.setProperty("directlink", directlink);
                decryptedLinks.add(dl);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        } else if (parameter.matches("https?://(((www|i)\\.)?imgur\\.com/[A-Za-z0-9]{5,}|(www\\.)?imgur\\.com/(download|gallery)/[A-Za-z0-9]{5,})")) {
            String imgUID = new Regex(parameter, "([A-Za-z0-9]{5,})$").getMatch(0);
            final DownloadLink dl = createDownloadlink("http://imgurdecrypted.com/download/" + imgUID);
            dl.setProperty("imgUID", imgUID);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

}
