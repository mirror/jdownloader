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

@DecrypterPlugin(revision = "$Revision: 18544 $", interfaceVersion = 2, names = { "revivelink.com" }, urls = { "http://(www\\.)?revivelink.com/.*" }, flags = { 0 })
public class ReviveLinkCom extends PluginForDecrypt {

    public ReviveLinkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // Logger logDebug = JDLogger.getLogger();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String strParameter = param.toString();

        // Get the package name
        int iPosition = strParameter.lastIndexOf("###");
        String strName = "";
        if (iPosition != -1) {
            strName = strParameter.substring(iPosition + 3);
            strParameter = strParameter.substring(0, iPosition);
        }
        // Reset the ? character that is remove from the link
        iPosition = strParameter.lastIndexOf('/');
        String strFirstPart = "";
        String strSecondPart = "";
        if (iPosition != -1) {
            strFirstPart = strParameter.substring(0, iPosition + 1);
            strSecondPart = strParameter.substring(iPosition + 1);
            if (strSecondPart.startsWith("?")) {
                strSecondPart = strSecondPart.substring(1);
            }
        }
        strParameter = strFirstPart + "liens.php?R=" + strSecondPart;

        br.setFollowRedirects(false);
        br.getPage(strParameter);

        if (br.containsHTML("(An error has occurred|The article cannot be found)")) {
            logger.info("Link offline: " + strParameter);
            return decryptedLinks;
        }

        jd.parser.Regex rTemp = br.getRegex("<a href=\"(.*?)\"");
        String[][] str = rTemp.getMatches();
        String[] links = new String[3];
        for (int iIndex = 0; iIndex < str.length; iIndex++) {
            links[iIndex] = str[iIndex][0];
        }
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + strParameter);
            return null;
        }

        // Added links
        for (String redirectlink : links) {
            decryptedLinks.add(createDownloadlink(redirectlink));
        }

        // Add all link in a package
        FilePackage fp = FilePackage.getInstance();
        if (strName != "") fp.setName(strName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}