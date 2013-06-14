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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "issuu.com" }, urls = { "http://(www\\.)?issuu\\.com/[a-z0-9\\-_\\.]+/docs/[a-z0-9\\-_\\.]+" }, flags = { 0 })
public class IssuuCom extends PluginForDecrypt {

    public IssuuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://www.", "http://");
        br.getPage(parameter);
        if (br.containsHTML(">We can\\'t find what you\\'re looking for")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String documentID = br.getRegex("\"documentId\":\"([^<>\"]*?)\"").getMatch(0);
        if (documentID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage("http://s3.amazonaws.com/document.issuu.com/" + documentID + "/document.xml");
        final String username = br.getRegex("username=\"([^<>\"]*?)\"").getMatch(0);
        final String rareTitle = br.getRegex("title=\"([^<>\"]*?)\"").getMatch(0);
        String originalDocName = br.getRegex("orgDocName=\"([^<>\"]*?)\"").getMatch(0);
        if (username == null || rareTitle == null || originalDocName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        originalDocName = Encoding.htmlDecode(originalDocName.trim());
        final DecimalFormat df = new DecimalFormat("0000");
        final String[] pageInfos = br.getRegex("<page(.*?)</page>").getColumn(0);
        if (pageInfos == null || pageInfos.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String general_naming = Encoding.htmlDecode(rareTitle.trim()) + " by " + Encoding.htmlDecode(username.trim()) + " [" + originalDocName + "] (" + pageInfos.length + " pages)";
        if (br.containsHTML("downloadable=\"true\"")) {
            final DownloadLink mainDownloadlink = createDownloadlink(parameter.replace("issuu.com/", "issuudecrypted.com/"));
            mainDownloadlink.setAvailable(true);
            final String pdfName = general_naming + ".pdf";
            mainDownloadlink.setFinalFileName(pdfName);
            mainDownloadlink.setProperty("finalname", pdfName);
            decryptedLinks.add(mainDownloadlink);
        } else {
            for (int i = 1; i <= pageInfos.length; i++) {
                final DownloadLink dl = createDownloadlink("http://image.issuu.com/" + documentID + "/jpg/page_" + i + ".jpg");
                dl.setFinalFileName("page_" + df.format(i) + ".jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(general_naming);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
