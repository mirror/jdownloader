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
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class AudioBeatsNet extends PluginForDecrypt {

    public AudioBeatsNet(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());

        logger.fine(br.toString());
        String[] linksa = br.getRegex("<b><a href=\"(/app/livesets/.*?)\">.*?</a></b><br>").getColumn(0);
        String[] linksb = br.getRegex("<a href=\"(/app/links/.*?)\" target=\"_blank\">.*?</a>").getColumn(0);
        progress.setRange(linksa.length + linksb.length);
        for (String data : linksa) {
            decryptedLinks.add(createDownloadlink("http://www.audiobeats.net" + data));
            progress.increase(1);
        }
        for (String data : linksb) {
            br.setFollowRedirects(false);
            br.getPage("http://www.audiobeats.net" + data);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            progress.increase(1);

        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
