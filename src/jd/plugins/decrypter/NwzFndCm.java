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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "newzfind.com" }, urls = { "http://[\\w\\.]*?newzfind\\.com/(video|music|games|software|mac|graphics|unix|magazines|e-books|xxx|other)/.+" }, flags = { 0 })
public class NwzFndCm extends PluginForDecrypt {

    public NwzFndCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        ArrayList<String> passwords = HTMLParser.findPasswords(br.getPage(parameter));

        br.getPage("http://newzfind.com/ajax/links.html?a=" + parameter.substring(parameter.lastIndexOf("/") + 1));
        String links[] = br.getRegex("<link title=\"(.*?)\"").getColumn(0);
        progress.setRange(links.length);
        for (String element : links) {
            DownloadLink dl_link = createDownloadlink(element);
            if (passwords != null && passwords.size() > 0) dl_link.setSourcePluginPasswordList(passwords);
            decryptedLinks.add(dl_link);
            progress.increase(1);
        }

        return decryptedLinks;
    }

    // @Override

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}