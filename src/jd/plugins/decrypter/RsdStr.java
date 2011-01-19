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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rsd-store.com" }, urls = { "http://[\\w\\.]*?rsd-store\\.com/\\d+\\.html" }, flags = { 0 })
public class RsdStr extends PluginForDecrypt {

    public RsdStr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.toString();
        this.setBrowserExclusive();
        br.getPage(url);
        sleep(4000, parameter);
        String dlc = new Regex(url, "/(\\d+)\\.html").getMatch(0);
        File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
        br.getDownload(container, "http://rsd-store.com/" + dlc + ".dlc");
        decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
        return decryptedLinks;
    }

}
