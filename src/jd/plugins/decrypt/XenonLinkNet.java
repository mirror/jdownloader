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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class XenonLinkNet extends PluginForDecrypt {

    public XenonLinkNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String dataCode = getUserInput("Data-Code?", param);

        br.getPage("http://www.xenonlink.net/index.php?p=2&dg=" + dataCode);

        String password = br.getRegex("Passwort:.*<br>(.*?)</font>").getMatch(0);
        // String name =
        // br.getRegex("<font color=white size=4>(.*?)</font>").getMatch(0);
        // Will diesen Namen als Namen für das Package benutzen.

        String[][] links = br.getRegex("<br>(http://.*?)\\s").getMatches();

        progress.setRange(links.length);
        for (String[] element : links) {
            DownloadLink dLink = createDownloadlink(element[0]);
            dLink.addSourcePluginPassword(password);
            decryptedLinks.add(dLink);

            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision:$");
    }
}
