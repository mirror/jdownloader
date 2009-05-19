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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class BadongoCom extends PluginForDecrypt {
    static private String host = "badongo.com";

    public BadongoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setCookiesExclusive(true);
        br.clearCookies(host);
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        br.getPage(parameter);
        parameter = parameter.replaceFirst("badongo.com", "badongo.viajd");
        if (!br.containsHTML("Diese Datei wurde gesplittet")) {
            DownloadLink dlLink = createDownloadlink(Encoding.htmlDecode(parameter));
            dlLink.setProperty("type", "single");
            decryptedLinks.add(dlLink);
        } else {
            String[] links = br.getRegex("<div class=\"m\">Download Teil(.*?)</div>").getColumn(0);
            progress.setRange(links.length);
            for (String partId : links) {

                DownloadLink dlLink = createDownloadlink(Encoding.htmlDecode(parameter) + "?" + partId.trim());
                dlLink.setName(dlLink.getName() + "." + partId.trim());
                dlLink.setProperty("type", "split");
                dlLink.setProperty("part", Integer.parseInt(partId.trim()));
                dlLink.setProperty("parts", links.length);
                decryptedLinks.add(dlLink);
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}