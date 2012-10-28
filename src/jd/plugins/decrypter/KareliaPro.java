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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "disk.karelia.pro" }, urls = { "http://(www\\.)?(disk\\.karelia\\.pro/fast/[A-Za-z0-9]+|fast\\.karelia\\.pro/[A-Za-z0-9]+/[^<>\"/]*?/)" }, flags = { 0 })
public class KareliaPro extends PluginForDecrypt {

    public KareliaPro(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches("http://(www\\.)?disk\\.karelia\\.pro/fast/[A-Za-z0-9]+")) {
            br.getPage(parameter);
            final String[] links = br.getRegex("18px center no\\-repeat;\">[\t\n\r ]+<a href=\"(http://disk\\.karelia\\.pro/fast/[^<>\"]*?)\"").getColumn(0);
            if ((links == null || links.length == 0) && !br.containsHTML("\"diskFile\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links)
                decryptedLinks.add(createDownloadlink("directhttp://" + singleLink));
        } else {
            decryptedLinks.add(createDownloadlink("directhttp://" + parameter.replaceAll("(/)$", "").replace("fast.karelia.pro/", "disk.karelia.pro/fast/")));
        }

        return decryptedLinks;
    }

}
