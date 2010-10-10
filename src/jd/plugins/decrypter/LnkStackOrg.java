//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkstack.org" }, urls = { "http://[\\w\\.]*?linkstack\\.org/view/id/[a-z0-9]+" }, flags = { 0 })
public class LnkStackOrg extends PluginForDecrypt {

    public LnkStackOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("<h4>A PHP Error was encountered</h4>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String id = new Regex(parameter, "linkstack\\.org/view/id/(.+)").getMatch(0);
        // container handling (if no containers found, use webprotection
        decryptedLinks = loadcontainer("http://linkstack.org/dlc/download/" + id);
        if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;

        // Webprotection decryption
        decryptedLinks = new ArrayList<DownloadLink>();
        String[] links = br.getRegex("</span></td>[\n\r\t ]+<td><a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String aLink : links)
            decryptedLinks.add(createDownloadlink(aLink));

        return decryptedLinks;
    }

    private ArrayList<DownloadLink> loadcontainer(String dlclink) throws IOException, PluginException {
        ArrayList<DownloadLink> decryptedLinks = null;
        Browser brc = br.cloneBrowser();
        /*
         * walk from end to beginning, so we load the all in one container first
         */
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(dlclink);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/linkstack/" + dlclink.replaceAll("(:|/)", "") + ".dlc");
            if (file == null) return null;
            file.deleteOnExit();
            brc.downloadConnection(file, con);
            if (file != null && file.exists() && file.length() > 100) {
                decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            }
        } else {
            con.disconnect();
            return null;
        }

        if (file != null && file.exists() && file.length() > 100) {
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            return null;
        }
        return null;
    }

}
