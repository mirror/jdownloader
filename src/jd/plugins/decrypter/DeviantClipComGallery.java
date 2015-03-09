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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantclip.com" }, urls = { "http://(www\\.)?(deviantclip|dagay|dachix)\\.com/watch/[A-Za-z0-9\\-]+" }, flags = { 0 })
public class DeviantClipComGallery extends PluginForDecrypt {

    public DeviantClipComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String currentdomain_full = new Regex(parameter, "((deviantclip|dagay|dachix)\\.com)").getMatch(0);
        final String currentdomain = new Regex(parameter, "(deviantclip|dagay|dachix)").getMatch(0);
        final String decrypterdomain = currentdomain + "decrypted.com";
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final Exception e) {
            decryptedLinks.add(createDownloadlink(parameter.replace(currentdomain_full, decrypterdomain)));
            return decryptedLinks;
        }
        if (br.containsHTML(">PICTURE GALLERY<")) {
            String fpName = getfpName();
            if (fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpName = Encoding.htmlDecode(fpName.trim());
            final String[] picLinks = br.getRegex("\"(/watch/[a-z0-9\\-]+\\?fileid=[A-Za-z0-9]+)\"").getColumn(0);
            if (picLinks == null || picLinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            int counter = 1;
            final DecimalFormat df = new DecimalFormat("0000");
            for (final String picLink : picLinks) {
                final DownloadLink dl = createDownloadlink("http://" + decrypterdomain + picLink);
                dl.setName(fpName + "_" + df.format(counter));
                decryptedLinks.add(dl);
                counter++;
            }
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        } else {
            if (br.containsHTML("flvplayer\\.swf\"")) {
                decryptedLinks.add(createDownloadlink(parameter.replace(currentdomain_full, decrypterdomain)));
            } else {
                logger.info("Found no valid link for: " + parameter);
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
        }
        return decryptedLinks;
    }

    private String getfpName() throws NumberFormatException, PluginException {
        String fpName = br.getRegex("class=\"main\\-sectioncontent\"><p class=\"footer\">.*?<b>(.*?)</b>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("name=\"DC\\.title\" content=\"(.*?)\">").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
        }
        return fpName;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}