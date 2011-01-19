//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nareey.com" }, urls = { "http://[\\w\\.]*?nareey\\.com/(\\d\\.php\\?\\d+|\\d+/)" }, flags = { 0 })
public class NrCm extends PluginForDecrypt {

    public NrCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String id;
        String parameter = param.toString();
        if (parameter.contains("php?")) {
            id = new Regex(parameter, "php\\?(\\d+)").getMatch(0);
        } else {
            id = new Regex(parameter, "nareey\\.com/(\\d+)/").getMatch(0);
        }
        br.getPage("http://www.nareey.com/2.php?" + id);
        String link = br.getRedirectLocation();
        decryptedLinks.add(createDownloadlink(link));
        return decryptedLinks;
    }

    // @Override

}
