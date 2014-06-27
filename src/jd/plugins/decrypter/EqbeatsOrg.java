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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eqbeats.org" }, urls = { "https://(www\\.)?eqbeats\\.org/track/\\d+" }, flags = { 0 })
public class EqbeatsOrg extends PluginForDecrypt {

    public EqbeatsOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String artist = br.getRegex("artist: \"([^<>\"]*?)\"").getMatch(0);
        final String title = br.getRegex("title: \"([^<>\"]*?)\"").getMatch(0);
        final String dl_links_table = br.getRegex("<ul class=\"downloads\">(.*?)</ul>").getMatch(0);
        final String[] links = new Regex(dl_links_table, "\"(/track/\\d+/[a-z0-9]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links) {
            singleLink = "https://eqbeats.org" + singleLink;
            decryptedLinks.add(createDownloadlink("directhttp://" + singleLink));
        }

        if (artist != null && title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(artist + " - " + title);
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
