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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mirorii.com" }, urls = { "http://(www\\.)?mirorii\\.com/fichier/\\d+/\\d+/" }, flags = { 0 })
public class MiroriiCom extends PluginForDecrypt {

    public MiroriiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCustomCharset("utf-8");
        br.getPage(parameter);
        if (br.containsHTML(">Désolé mais ce fichier n\\'est pas ou plus disponible")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String[] links = br.getRegex("\"><a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            if (br.containsHTML("/img/heb/miroriii\\.png\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            return null;
        }
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}