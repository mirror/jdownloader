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
//    along with this program.  If not, see <http://www.gnu.org/licenses

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadjockey.com" }, urls = { "http://[\\w\\.]*?uploadjockey\\.com/((download/[\\w]+/(.*))|redirect\\.php\\?url=.+)" }, flags = { 0 })
public class Pldckcm extends PluginForDecrypt {

    public Pldckcm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("redirect.php")) {
            String link = new Regex(parameter, "redirect\\.php\\?url=(.+)").getMatch(0);
            if (link == null) return null;
            decryptedLinks.add(createDownloadlink(Encoding.Base64Decode(link)));
        } else {
            br.getPage(parameter);
            String links[] = br.getRegex("<a href=\"http://www\\.uploadjockey\\.com/redirect\\.php\\?url=(.*?)\"").getColumn(0);
            progress.setRange(links.length);
            for (String element : links) {
                String link = Encoding.Base64Decode(element);
                String damnedStuff = new Regex(link, "(\\&key=[0-9]+\\&ref=.+)").getMatch(0);
                if (damnedStuff != null) link = link.replace(damnedStuff, "");
                decryptedLinks.add(createDownloadlink(link));
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }

}
