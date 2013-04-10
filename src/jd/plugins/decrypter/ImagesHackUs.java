//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imageshack.us" }, urls = { "http://(www\\.)?(img[0-9]{1,4}\\.imageshack\\.us/(g/|my\\.php\\?image=[a-z0-9]+|i/[a-z0-9]+)\\.[a-zA-Z0-9]{2,4}|imageshack\\.us/photo/[^<>\"\\'/]+/\\d+/[^<>\"\\'/]+)" }, flags = { 0 })
public class ImagesHackUs extends PluginForDecrypt {

    public ImagesHackUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getURL().equals("http://imageshack.us/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getURL() != parameter) parameter = br.getURL();
        if (parameter.matches(".*?imageshack\\.us/photo/[^<>\"\\'/]+/\\d+/[^<>\"\\'/]+/?")) {
            String finallink = br.getRegex("<meta property=\"og:image\" content=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("<link rel=\"image_src\" href=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            /* Error handling */
            if (br.containsHTML(">Can not find album")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            String fpName = br.getRegex("<div style=\"float:left\">(.*?)</div>").getMatch(0);
            if (fpName == null || fpName.trim().equals("My Album")) fpName = new Regex(parameter, "img(\\d+)\\.imageshack\\.us").getMatch(0);
            String allPics[] = br.getRegex("<div onclick=\"window\\.location\\.href=\\'(http://.*?)\\'\"").getColumn(0);
            if (allPics == null || allPics.length == 0) allPics = br.getRegex("<input type=\"text\" value=\"(http://.*?)\"").getColumn(0);
            if (allPics == null || allPics.length == 0) allPics = br.getRegex("'\\[URL=(http://.*?)\\]").getColumn(0);
            if (allPics == null || allPics.length == 0) return null;
            for (String aPic : allPics)
                decryptedLinks.add(createDownloadlink(aPic));
            // The String "fpName" should never be null at this point
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}