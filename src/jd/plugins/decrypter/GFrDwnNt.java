//    jDownloader - Downloadmanager
//    Copyright (C) 2009 JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision: 7185 $", interfaceVersion = 2, names = { "go4Down.net" }, urls = { "http://[\\w\\.]*?(short\\.)?go4down\\.(com|net)/(short/)\\d+"}, flags = { 0 })


public class GFrDwnNt extends PluginForDecrypt {

    public GFrDwnNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String linkid = new Regex(parameter, ".*?short\\.go4down\\.(net|com)/([^/]*)").getMatch(1);
        br.getPage("http://short.go4down.net/m1.php?id=" + linkid);
        String declink = br.getRegex("onclick=\"NewWindow\\('(.*?)','name'").getMatch(0);
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(declink)));

        return decryptedLinks;
    }

    // @Override
    
}
