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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chip.de" }, urls = { "http://(www\\.)?chip\\.de/bildergalerie/.*?_\\d+\\.html" }, flags = { 0 })
public class ChipDeGallerie extends PluginForDecrypt {

    public ChipDeGallerie(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(parameter);
            if (con.getResponseCode() == 410) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }

        String fpName = br.getRegex("<meta property=\"og:title\" content=\"(.*?) \\- Bildergalerie\"/>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>(.*?) \\- Bilder \\-").getMatch(0);
        }
        if (fpName == null) {
            logger.warning("Decrypter broken for link:" + parameter);
            return null;
        }
        fpName = fpName.trim();
        String[] pictureNames = br.getRegex("bGrossversion\\[\\d+\\] = \"(.*?)\";").getColumn(0);
        if (pictureNames == null || pictureNames.length == 0) pictureNames = br.getRegex("url \\+= \"/ii/grossbild_v2\\.html\\?grossbild=(.*?)\";").getColumn(0);
        if (pictureNames == null || pictureNames.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        DecimalFormat df = new DecimalFormat("000");
        int counter = 1;
        for (String picName : pictureNames) {
            // Skip invalid links, most times only the last link is invalid
            if (picName.equals("")) continue;
            DownloadLink dl = createDownloadlink("directhttp://http://www.chip.de/ii/" + picName.trim());
            dl.setFinalFileName(fpName + "_" + df.format(counter) + picName.substring(picName.lastIndexOf(".")));
            decryptedLinks.add(dl);
            counter++;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}