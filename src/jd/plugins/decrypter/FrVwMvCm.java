//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freeviewmovies.com" }, urls = { "http://(www\\.)?freeviewmovies\\.com/(porn|video)/\\d+" }, flags = { 0 })
public class FrVwMvCm extends PluginForDecrypt {

    public FrVwMvCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // old /porn/ links redirect
        br.setFollowRedirects(true);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("<div id=\"player\">\\s+This video has no trailer\\.\\s+</div>")) {
            logger.info("No public trailer!: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("No htmlCode read") || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link broken: " + parameter);
            return decryptedLinks;
        }

        // this supports freeviewmovies hosted files
        String result = br.getRegex("file: \'(http[^']+)").getMatch(0);
        if (result != null) {
            decryptedLinks.add(createDownloadlink(parameter.replace("freeviewmovies.com/", "freeviewmoviesdecrypted/")));
        } else {
            // this should cover most of the other sites, at least links I have tested.
            result = br.getRegex("<iframe[^\r\n]+src=\"(https?://([^\"]+)?(spankwire|tube8|pornhub|pornhost|xhamster|xvideos)[^\"]+)").getMatch(0);
            if (result != null) {
                decryptedLinks.add(createDownloadlink(result.replace("freeviewmovies.com/", "freeviewmoviesdecrypted/")));
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info("No links found, maybe broken plugin, please report to JD Development Team : " + parameter);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}