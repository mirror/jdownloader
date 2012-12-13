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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedload.org" }, urls = { "http://(www\\.)?speedload\\.org/(?!faq|register|login|terms|report|index|earn_money|upgrade|checker|sitemap|dmca|tos)[a-z0-9]+(~f)?" }, flags = { 0 })
public class SpeedLoadOrgFolder extends PluginForDecrypt {

    public SpeedLoadOrgFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        if (parameter.contains("~f")) {
            br.getPage("http://speedload.org/api/folder_link.php?id=" + new Regex(parameter, "/(\\d+)~f$").getMatch(0));
            if ("0".equals("total_files")) {
                logger.info("This folder is empty: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("No htmlCode read")) {
                logger.info("This folderlink is invalid: " + parameter);
                return decryptedLinks;
            }
            final String[] links = br.getRegex("\"(http:\\\\/\\\\/speedload\\.org\\\\/[A-Za-z0-9]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink(singleLink.replace("\\", "").replace("speedload.org/", "speedload.orgdecrypted/")));
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(getJson("folderName"));
            fp.addLinks(decryptedLinks);
        } else {
            decryptedLinks.add(createDownloadlink(parameter.replace("speedload.org/", "speedload.orgdecrypted/")));
        }

        return decryptedLinks;
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }
}
