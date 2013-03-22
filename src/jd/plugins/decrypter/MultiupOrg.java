//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiup.org" }, urls = { "http://(www\\.)?multiup\\.org/([a-z]{2}/download/[a-z0-9]{32}/[^<> \"\\'\\&]+|fichiers/download/[a-z0-9]{32}[^<> \"\\'\\&]+)" }, flags = { 0 })
public class MultiupOrg extends PluginForDecrypt {

    // DEV NOTES:
    // /?lien=842fab872a0a9618f901b9f4ea986d47_bawls_doctorsdiary202.avi = url
    // doesn't exist on the provider any longer.. but is still transferable into
    // the new format!
    // /fichiers/download/d249b81f92d7789a1233e500a0319906_FIQHwASOOL_75_rar =
    // does and redirects to below rule
    // (/fr)/download/d249b81f92d7789a1233e500a0319906/FIQHwASOOL_75_rar
    // uid interchangeable, uid and filename are required to be a valid link.

    public MultiupOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">File not found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.getPage(br.getURL().replace("/download/", "/miror/"));

        String[] links = br.getRegex("style=\"width:97%;text\\-align:left\"[\t\n\r ]+href=\"(http[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.info("Could not find links, please report this to JDownloader Development Team. " + parameter);
            return null;
        }
        for (String singleLink : links) {
            decryptedLinks.add(createDownloadlink(singleLink.trim()));
        }

        return decryptedLinks;
    }
}
