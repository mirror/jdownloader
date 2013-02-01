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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiup.org" }, urls = { "http://(www\\.)?multiup\\.org/(fichiers/download/[a-z0-9]{32}_[^<> \"'&]+|([a-z]{2}/)?(download|miror)/[a-z0-9]{32}/[^<> \"'&]+)" }, flags = { 0 })
public class MultiupOrg extends PluginForDecrypt {

    // DEV NOTES:
    // /?lien=842fab872a0a9618f901b9f4ea986d47_bawls_doctorsdiary202.avi = format doesn't exist any longer..
    // /fichiers/download/d249b81f92d7789a1233e500a0319906_FIQHwASOOL_75_rar = does and redirects to below rule
    // (/fr)/download/d249b81f92d7789a1233e500a0319906/FIQHwASOOL_75_rar
    // uid interchangeable, uid and filename are required to be a valid link.

    public MultiupOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        String reg = "(org/fichiers/download/([0-9a-z]{32})_([^<> \"'&]+)?|org/([a-z]{2}/)?(download|miror)/([a-z0-9]{32})/([^<> \"'&]+))";
        String[][] matches = new Regex(parameter, reg).getMatches();
        String uid = matches[0][1];
        if (uid == null) {
            uid = matches[0][5];
            if (uid == null) {
                logger.info("URL is invalid, must contain 'uid' to be valid " + parameter);
                return decryptedLinks;
            }
        }
        String filename = matches[0][2];
        if (filename == null) {
            filename = matches[0][6];
            if (filename == null) {
                logger.info("URL is invalid, must contain 'filename' to be valid " + parameter);
                return decryptedLinks;
            }
        }
        parameter = new Regex(parameter, "(https?://[^/]+)").getMatch(0).replace("www.", "") + "/en/download/" + uid + "/" + filename;
        param.setCryptedUrl(parameter);
        br.getPage(parameter.replace("/en/download/", "/en/miror/"));
        if (br.containsHTML("The file does not exist any more\\.<|<h1>The server returned a \"404 Not Found\"\\.</h2>|<h1>Oops! An Error Occurred</h1>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        String[] links = br.getRegex("[\r\n\t ]{3,}href=\"([^\"]+)\"[\r\n\t ]{3,}").getColumn(0);
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
