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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantclip.com" }, urls = { "http://(www\\.)?deviantclip\\.com/watch/[a-z0-9\\-]+" }, flags = { 0 })
public class DeviantClipComGallery extends PluginForDecrypt {

    public DeviantClipComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">PICTURE GALLERY<")) {
            String fpName = getfpName();
            if (fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpName = Encoding.htmlDecode(fpName.trim());
            if (parameter.matches("http://(www\\.)?deviantclip\\.com/watch/[a-z0-9\\-]+")) {
                final String[] picLinks = br.getRegex("\"(/watch/[a-z0-9\\-]+\\?fileid=[A-Za-z0-9]+)\"").getColumn(0);
                if (picLinks == null || picLinks.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                int counter = 1;
                DecimalFormat df = new DecimalFormat("0000");
                for (final String picLink : picLinks) {
                    final DownloadLink dl = createDownloadlink("http://deviantclipdecrypted.com" + picLink);
                    dl.setName(fpName + "_" + df.format(counter));
                    decryptedLinks.add(dl);
                    counter++;
                }
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            } else {
                logger.info("Found no valid link for: " + parameter);
            }
        } else {
            if (br.containsHTML("flvplayer\\.swf\"")) {
                decryptedLinks.add(createDownloadlink(parameter.replace("deviantclip.com", "deviantclipdecrypted.com")));
            } else {
                logger.info("Found no valid link for: " + parameter);
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
}
