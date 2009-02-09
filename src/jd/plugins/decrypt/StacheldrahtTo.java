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

import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.HTTPConnecter;
import jd.http.HTTPConnection;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class StacheldrahtTo extends PluginForDecrypt {

    public StacheldrahtTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String links[] = br.getRegex("var InputVars = \"(.*?)\"").getColumn(0);

        progress.setRange(links.length / 2);
        for (int i = 0; i < links.length; i = i + 2) {
            /**
             * TODO: böse art eine Connection zu öffnen!!
             */
            HTTPConnection httpConnection = (HTTPConnection)new URL("jdp://www.stacheldraht.to/php_docs/ajax/link_get.php?" + links[i]).openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setInstanceFollowRedirects(true);
            httpConnection.setRequestProperty("Host", "stacheldraht.to");
            httpConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            httpConnection.setRequestProperty("X-Prototype-Version", "1.6.0.2");

            decryptedLinks.add(createDownloadlink(br.loadConnection(httpConnection).trim()));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}