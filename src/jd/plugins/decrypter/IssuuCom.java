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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "issuu.com" }, urls = { "http://(www\\.)?issuu\\.com/[a-z0-9\\-_]+/docs/[a-z0-9\\-_]+" }, flags = { 0 })
public class IssuuCom extends PluginForDecrypt {

    public IssuuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
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
        final DecimalFormat df = new DecimalFormat("0000");
        final String[] pageInfos = br.getRegex("<page(.*?)</page>").getColumn(0);
        if (pageInfos == null || pageInfos.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String fpName = new Regex(parameter, "issuu\\.com/[a-z0-9\\-_]+/docs/([a-z0-9\\-_]+)").getMatch(0);
        int counter = 1;
        for (final String swfInfo : pageInfos) {
            final DownloadLink dl = createDownloadlink("http://page.issuu.com/" + documentID + "/swf/page_" + counter + ".swf");
            final String filesize = new Regex(swfInfo, "<swf size=\"(\\d+)\"").getMatch(0);
            dl.setDownloadSize(Long.parseLong(filesize));
            dl.setFinalFileName(fpName + "_page_" + df.format(counter) + ".swf");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            counter++;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
