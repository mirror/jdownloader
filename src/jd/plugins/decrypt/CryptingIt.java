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
import java.util.Vector;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class CryptingIt extends PluginForDecrypt {

    public CryptingIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String id = new Regex(parameter, "id=(\\d+)").getMatch(0);
        String password = br.getRegex("<td valign=\"top\" style=\"border-bottom: 1px dotted #C8C8C8;\"><div align=\"center\">(.*?)</div></td>").getMatch(0, 2);
        String dlcLink = "http://crypting.it/files/download.php?fileid=" + id + "-m1.dlc";

        File containerFile = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
        Vector<DownloadLink> links = null;
        Browser.download(containerFile, dlcLink);
        links = JDUtilities.getController().getContainerLinks(containerFile);
        containerFile.delete();

        if (links != null) {
            for (DownloadLink dLink : links) {
                dLink.addSourcePluginPassword(password);
                decryptedLinks.add(dLink);
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
