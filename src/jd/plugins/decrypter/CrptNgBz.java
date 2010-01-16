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
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crypting.bz" }, urls = { "http://[\\w\\.]*?crypting\\.bz/\\?id=[a-zA-Z0-9]+" }, flags = { 0 })
public class CrptNgBz extends PluginForDecrypt {

    public CrptNgBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String fpName = br.getRegex("ord_titel\"><br /><center><b>(.*?)</b>").getMatch(0);
        // container handling (if no containers found, use webprotection
        String containerRegex = br.getRegex("(getContainer\\.php\\?typ=.*?\\.dlc)\"").getMatch(0);
        if (containerRegex != null) {
            containerRegex = "http://crypting.bz/" + containerRegex;
            decryptedLinks = loadcontainer(br, containerRegex);
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        // Webprotection decryption
        decryptedLinks = new ArrayList<DownloadLink>();
        String[] links = br.getRegex("</td><td width=\"80\" valign=\"bottom\"><a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("(hide_url\\.php\\?url_id=.*?\\&uid=.*?)\"").getColumn(0);
        }
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            if (!link.contains("crypting.bz")) link = "http://crypting.bz/" + link;
            br.getPage(link);
            String finallink = br.getRegex("<iframe name=\".*?src=\"(.*?)\"").getMatch(0);
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        if (fpName != null && fpName.length() > 1) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    // By jiaz
    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String dlcName = new Regex(format, "\\?typ=(.*?\\.dlc)").getMatch(0);
        if (dlcName == null) return null;
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(format);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/cryptingbz/" + dlcName);
            if (file == null) return null;
            file.deleteOnExit();
            brc.downloadConnection(file, con);
        } else {
            con.disconnect();
            return null;
        }

        if (file != null && file.exists() && file.length() > 100) {
            ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            return null;
        }
        return null;
    }

}
