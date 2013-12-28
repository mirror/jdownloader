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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloudzer.net" }, urls = { "http://(www\\.)?(cloudzer\\.net|clz\\.to)/(folder|f)/[a-z0-9]+" }, flags = { 0 })
public class CloudzerNetFolder extends PluginForDecrypt {

    public CloudzerNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("clz.to/", "cloudzer.net/").replace("/f/", "/folder/");
        // Prefer english language
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://cloudzer.net/404") || br.containsHTML("> us in case of a technical failure")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">doesn\\'t contain files<")) {
            logger.info("Link offline (folder empty): " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        final String[][] links = br.getRegex("\"(file/[a-z0-9]+)/from/[a-z0-9]+\" class=\"file\" onclick=\"visit\\(\\$\\(this\\)\\)\">([^<>\"]*?)</a></h2>[\t\n\r ]+<small class=\"cL\" style=\"font\\-size:13px\">([^<>\"]*?)</small>").getMatches();
        final String[] folders = br.getRegex("\"(/folder/[a-z0-9]+)\" onclick=\"visit\\(\\$\\(this\\)\\)\">").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (final String singleLink[] : links) {
                final DownloadLink dl = createDownloadlink("http://cloudzer.net/" + singleLink[0]);
                dl.setName(singleLink[1]);
                dl.setDownloadSize(SizeFormatter.getSize(singleLink[1].replace(",", ".")));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (folders != null && folders.length != 0) {
            for (final String folder : folders) {
                decryptedLinks.add(createDownloadlink("http://cloudzer.net" + folder));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}