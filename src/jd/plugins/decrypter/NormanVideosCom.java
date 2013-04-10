//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
//
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 18544 $", interfaceVersion = 2, names = { "normanfaitdesvideos.com" }, urls = { "http://(www\\.)?normanfaitdesvideos\\.com/.*" }, flags = { 0 })
public class NormanVideosCom extends PluginForDecrypt {

    public NormanVideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // Logger logDebug = JDLogger.getLogger();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String strParameter = param.toString();

        br.setFollowRedirects(false);
        br.getPage(strParameter);

        String[] links = br.getRegex("http://(www\\.)?youtube.com/embed/(.*?)\\?").getColumn(1);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + strParameter);
            return null;
        }

        // Added links
        for (String redirectlink : links) {
            redirectlink = "http://www.youtube.com/embed/" + redirectlink;
            DownloadLink DLLink = createDownloadlink(redirectlink);
            // if (strDate != null && strDate != "") DLLink.setFinalFileName(strDate + " " + DLLink.getFinalFileName());
            decryptedLinks.add(DLLink);
        }

        // Add all link in a package
        FilePackage fp = FilePackage.getInstance();
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}