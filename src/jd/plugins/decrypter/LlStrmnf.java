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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "all-stream.info" }, urls = { "http://(www\\.)?all-stream\\.info/\\?id=\\d+" }, flags = { 0 })
public class LlStrmnf extends PluginForDecrypt {

    public LlStrmnf(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("normal; margin-left: 0x; margin-right: 0px;\"><br><b>(.*?)</b><br><br>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("\"Cover: (.*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<TITLE>All-Stream - (.*?)</TITLE>").getMatch(0);
            }
        }
        String password = br.getRegex("<b>Passwort:</b><a href=\"\" onClick=\"CopyToClipboard\\(this\\); return\\(false\\);\">(.*?)</a></P>").getMatch(0);
        if (password != null && (password.equals(" keine Angabe"))) password = null;
        String[] additionalStreamlinks = br.getRegex("name=\"movie\" value=\"(http://.*?)\"").getColumn(0);
        if (additionalStreamlinks != null && additionalStreamlinks.length != 0) {
            for (String streamlink : additionalStreamlinks) {
                DownloadLink dl = createDownloadlink(streamlink);
                if (password != null) dl.addSourcePluginPassword(password);
                decryptedLinks.add(dl);
            }
        }
        String[] postVars = br.getRegex("NAME=\"m\" VALUE=\"(.*?)\"").getColumn(0);
        if (postVars == null || postVars.length == 0) return null;
        progress.setRange(postVars.length);
        for (String postVar : postVars) {
            br.postPage(parameter + "&location=mirror", "m=" + postVar);
            String[] links = br.getRegex("<FORM ACTION=\"(.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String link : links) {
                DownloadLink dl = createDownloadlink(link);
                if (password != null) dl.addSourcePluginPassword(password);
                decryptedLinks.add(dl);
            }
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
