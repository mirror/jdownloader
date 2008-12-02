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
import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class LinksaveIn extends PluginForDecrypt {

    public LinksaveIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (br.containsHTML("<title>LinkSave.in - Error 404</title>")) {
            logger.info("LinkSave.in Folder not found!");
            return decryptedLinks;
        }
        parameter = parameter + ".dlc";
        URL url = new URL(parameter);
        File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
        HTTPConnection dlc_con = new HTTPConnection(url.openConnection());
        // Api ! nicht in org tauschen!
        dlc_con.setRequestProperty("Referer", "jdownloader.org");
        Browser.download(container, dlc_con);
        decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
        container.delete();

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
