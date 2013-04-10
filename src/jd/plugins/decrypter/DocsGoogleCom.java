//  jDownloader - Downloadmanager
//  Copyright (C) 2013  JD-Team support@jdownloader.org
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: 20105 $", interfaceVersion = 2, names = { "docs.google.com" }, urls = { "https?://(www\\.)?docs\\.google\\.com/folder/d/[a-zA-Z0-9\\-_]+" }, flags = { 0 })
public class DocsGoogleCom extends PluginForDecrypt {

    /**
     * @author raztoki
     * */
    public DocsGoogleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // DEV NOTES
    // https://docs.google.com/folder/d/0B4lNqBSBfg_dbEdISXAyNlBpLUk/edit?pli=1 :: folder view of dir and files, can't seem to view dir
    // unless edit present.
    // https://docs.google.com/folder/d/0B4lNqBSBfg_dOEVERmQzcU9LaWc/edit?pli=1&docId=0B4lNqBSBfg_deEpXNjJrZy1MSGM :: above sub dir of docs
    // they don't provide data constistantly.
    // - with /edit?pli=1 they provide via javascript section partly escaped
    // - with /list?rm=whitebox&hl=en_GB&forcehl=1&pref=2&pli=1"; - not used and commented out, supported except for scanLinks
    // language determined by the accept-language
    // user-agent required to use new ones otherwise blocks with javascript notice.

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        JDUtilities.getPluginForDecrypt("docs.google.com");
        jd.plugins.hoster.DocsGoogleCom.prepBrowser(br);

        br.getPage(parameter + "/edit?pli=1");
        // Browser br2 = br.cloneBrowser();

        if (br.containsHTML("<p class=\"errorMessage\" style=\"padding\\-top: 50px\">Sorry, the file you have requested does not exist\\.</p>")) {
            logger.info("Link is offline or Invalid URL been provided " + parameter);
            return decryptedLinks;
        }

        String fpName = br.getRegex("\"title\":\"([^\"]+)\",\"urlPrefix\"").getMatch(0);
        if (fpName == null) fpName = br.getRegex("").getMatch(0);

        String[] results = br.getRegex("(\\{\"description\":[^\\}]+),").getColumn(0);
        // if (results == null || results.length == 0) {
        // br2.getPage(parameter + "/list?rm=whitebox&hl=en_GB&forcehl=1&pref=2&pli=1");
        // results = br2.getRegex("(<td class=\"list\\-entry\\-title\".+</a></td></tr>)").getColumn(0);
        // }
        if (results != null && results.length != 0) {
            for (String result : results) {
                String link = new Regex(result, "\"openURL\":\"(http.+(\\\\/|/)folder(\\\\/|/)d(\\\\/|/)[^\"]+)").getMatch(0);
                // if (link == null) link = new Regex(result, "(.+)").getMatch(0);
                if (link != null) {
                    // return folder links back into the plugin again.
                    decryptedLinks.add(createDownloadlink(link.replaceAll("\\\\/", "/")));
                } else {
                    // folder could just contain /file/ links
                    scanLinks(result, decryptedLinks);
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Found nothing to download: " + parameter);
            return decryptedLinks;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void scanLinks(String result, ArrayList<DownloadLink> ret) {
        if (result != null) {
            String link = new Regex(result, "\"openURL\":\"(http.+(\\\\/|/)file(\\\\/|/)d(\\\\/|/)[^\"]+)").getMatch(0);
            String filename = new Regex(result, "\"name\":\"([^\"]+)").getMatch(0);
            if (filename == null) filename = new Regex(result, ">(.*?)</a>").getMatch(0);
            if (link != null && filename != null) {
                DownloadLink dl = createDownloadlink(link.replaceAll("\\\\/", "/"));
                dl.setName(filename);
                dl.setAvailable(true);
                ret.add(dl);
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}