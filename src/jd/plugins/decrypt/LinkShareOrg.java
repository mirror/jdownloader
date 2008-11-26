//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.io.JDIO;

public class LinkShareOrg extends PluginForDecrypt {

    public LinkShareOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String containerId = new Regex(parameter, "\\?url=([a-zA-Z0-9]{32})").getMatch(0);

        // Prüfen ob Containerfiles vorhanden sind
        if (!br.getPage("http://www.link-share.org/container/download.php?id=" + containerId + ".dlc").toString().contains("Die Datei wurde nicht gefunden")) {
            File container = JDIO.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            Browser.download(container, "http://www.link-share.org/container/download.php?id=" + containerId + ".dlc");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }
        if (decryptedLinks.size() == 0 && !br.getPage("http://www.link-share.org/container/download.php?id=" + containerId + ".ccf").toString().contains("Die Datei wurde nicht gefunden")) {
            File container = JDIO.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
            Browser.download(container, "http://www.link-share.org/container/download.php?id=" + containerId + ".ccf");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }
        if (decryptedLinks.size() == 0 && !br.getPage("http://www.link-share.org/container/download.php?id=" + containerId + ".rsdf").toString().contains("Die Datei wurde nicht gefunden")) {
            File container = JDIO.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
            Browser.download(container, "http://www.link-share.org/container/download.php?id=" + containerId + ".rsdf");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }
        if (decryptedLinks.size() == 0) {
            // 150 Links können maximal verwendet werden
            // Hier läuft es durch und wartet bis encodedLink nichtmehr
            // vorhanden ist und somit die Links "zuende"

            for (int i = 0; i < 150; i++) {
                String encodedLink = Encoding.htmlDecode(new Regex(br.getPage("http://link-share.org/show.php?url=" + containerId + "&id=" + i), "<iframe src=\"(.*?)\" allow").getMatch(0));
                if (encodedLink.equals("")) break;

                decryptedLinks.add(createDownloadlink(encodedLink));

            }
        }
        
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
