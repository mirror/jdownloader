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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animea.net" }, urls = { "http://[\\w\\.]*?animea\\.net/download/[\\d]+/(.*?)\\.html|http://[\\w\\.]*?animea\\.net/download/[\\d]+-[\\d]+/(.*?)\\.html" }, flags = { 0 })
public class NmNt extends PluginForDecrypt {

    static public final String DECRYPTER_ANIMEANET_SERIES = "http://[\\w\\.]*?animea\\.net/download/[\\d]+/(.*?)\\.html";

    public NmNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceAll(" ", "+");

        br.getPage(parameter);
        if (parameter.matches(DECRYPTER_ANIMEANET_SERIES)) {
            String[] links = br.getRegex("<a href=\"/download/(.*?)\\.html\"").getColumn(0);
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink("http://www.animea.net/download/" + element + ".html"));
                progress.increase(1);
            }
        } else {
            String[] links = br.getRegex("<td><a href=\"(.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink(element));
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }
}
