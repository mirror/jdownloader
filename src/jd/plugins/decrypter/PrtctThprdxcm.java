//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "Protect.Tehparadox.com" }, urls = { "http://[\\w\\.]*?protect\\.tehparadox\\.com/[\\w]+\\!" }, flags = { 0 })
public class PrtctThprdxcm extends PluginForDecrypt {

    public PrtctThprdxcm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.postPage("http://protect.tehparadox.com/getdata.php", "id=" + parameter.substring(parameter.lastIndexOf("/") + 1, parameter.length() - 1));
        if (br.containsHTML("requested cannot be found")) return decryptedLinks;
        String downloadlink = br.getRegex("<iframe name=\"ifram\" src=\"(.*?)\"").getMatch(0);
        if (downloadlink == null) return null;
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(downloadlink)));

        return decryptedLinks;
    }

    // @Override

}
