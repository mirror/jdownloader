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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RelinkaNet extends PluginForDecrypt {

    public RelinkaNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        String links[] = br.getRegex("<a href=\"http://relinka.net\\/out\\/[a-z0-9]{8}-[a-z0-9]{4}\\/(.*?)\" class=").getColumn(0);
        String folderId = new Regex(parameter, "http://relinka.net\\/folder\\/([a-z0-9]{8}-[a-z0-9]{4})").getMatch(0);

        progress.setRange(links.length);
        for (String element : links) {
            String encodedLink = Encoding.htmlDecode(new Regex(br.getPage("http://relinka.net/out/" + folderId + "/" + element), "<iframe src=\"(.*)\" marginhe").getMatch(0));

            decryptedLinks.add(createDownloadlink(encodedLink));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
