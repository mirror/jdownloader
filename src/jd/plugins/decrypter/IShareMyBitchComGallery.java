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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "isharemybitch.com" }, urls = { "http://(www\\.)?isharemybitch\\.com/galleries/\\d+/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class IShareMyBitchComGallery extends PluginForDecrypt {

    public IShareMyBitchComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<div id=\"v_header\">([^<>\"]*?)</div>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>([^<>\"]*?) at IShareMyBitch\\.com Naked Girls in Homemade Porn Sextapes and Amateur Pictures</title>").getMatch(0);
        String galleryID = br.getRegex("isharemybitch\\-gallery\\-(\\d+)\"").getMatch(0);
        if (galleryID == null) galleryID = br.getRegex("id=\"unit_long(\\d+)\"").getMatch(0);
        if (fpName == null || galleryID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        br.getPage("http://www.isharemybitch.com/gallery-widget/widget.php?id=" + galleryID);
        final String[] links = br.getRegex("\"(http://media\\.isharemybitch\\.com:\\d+/galleries/[a-z0-9]+/[a-z0-9\\-_]+\\.jpg)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        int counter = 1;
        final DecimalFormat df = new DecimalFormat("0000");
        for (String singleLink : links) {
            final DownloadLink dl = createDownloadlink("directhttp://" + singleLink);
            dl.setFinalFileName(fpName + "_" + df.format(counter) + ".jpg");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            counter++;
        }
        return decryptedLinks;
    }

}
