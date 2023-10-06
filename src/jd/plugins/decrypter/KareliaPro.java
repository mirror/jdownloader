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
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "disk.karelia.pro" }, urls = { "http://(www\\.)?(disk\\.karelia\\.pro/fast/[A-Za-z0-9]+|fast\\.karelia\\.pro/[A-Za-z0-9]+/[^<>\"/]*?/)" })
public class KareliaPro extends PluginForDecrypt {
    public KareliaPro(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        if (parameter.matches("http://(www\\.)?disk\\.karelia\\.pro/fast/[A-Za-z0-9]+")) {
            br.getPage(parameter);
            final String[] links = br.getRegex("18px center no\\-repeat;\">[\t\n\r ]+<a href=\"(http://disk\\.karelia\\.pro/fast/[^<>\"]*?)\"").getColumn(0);
            if ((links == null || links.length == 0) && !br.containsHTML("\"diskFile\"")) {
                logger.info("Link offline: " + parameter);
                return ret;
            }
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links) {
                ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(singleLink)));
            }
        } else {
            ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(parameter.replaceAll("(/)$", "").replace("fast.karelia.pro/", "disk.karelia.pro/fast/"))));
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}