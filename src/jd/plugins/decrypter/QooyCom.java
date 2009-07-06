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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class QooyCom extends PluginForDecrypt {

    public QooyCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        String uid = br.getRegex("/status.php\\?uid=(.*?)\", true").getMatch(0);
        br.getPage("http://qooy.com/status.php?uid=" + uid);
        String[] links = br.getRegex("<a href=(.*?) target=_blank>").getColumn(0);
        progress.setRange(links.length);
        for (String data : links) {
            br.getPage("http://qooy.com/" + data);
            String dllink = br.getRegex("<frame name=\\\"main\\\" src=\\\"(.*?)\\\"").getMatch(0);
            decryptedLinks.add(createDownloadlink(dllink));
            progress.increase(1);
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 6424 $");
    }

}
