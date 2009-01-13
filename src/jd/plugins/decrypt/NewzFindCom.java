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

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.Vector;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class NewzFindCom extends PluginForDecrypt {

    public NewzFindCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        Vector<String> passwords = HTMLParser.findPasswords(br.getPage(parameter));

        br.getPage("http://newzfind.com/ajax/links.html?a=" + parameter.substring(parameter.lastIndexOf("/") + 1));
        String links[] = br.getRegex("<link title=\"(.*?)\"").getColumn(0);
        progress.setRange(links.length);
        for (String element : links) {
            DownloadLink dl_link = createDownloadlink(element);
            dl_link.addSourcePluginPasswords(passwords);
            decryptedLinks.add(dl_link);
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}