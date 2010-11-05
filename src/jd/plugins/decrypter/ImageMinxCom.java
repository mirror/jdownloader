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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imageminx.com" }, urls = { "http://(www\\.)?imageminx\\.com/public(/view(/full)?/|/viewset/)\\d+" }, flags = { 0 })
public class ImageMinxCom extends PluginForDecrypt {

    public ImageMinxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String FINALLINKREGEX  = "<p id=\"image_container\"><img src=\"(http://.*?)\"";
    private static final String FINALLINKREGEX2 = "\"(http://imageminx\\.com/pfiles/\\d+/.*?)\"";
    private static final String OFFLINETEXT     = "<title>Invalid (file|set) - ImageMinx";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("imageminx.com/public/view/")) {
            logger.info("Decrypting a single link...");
            if (!parameter.contains("/full")) parameter = parameter.replace("view/", "/view/full/");
            br.getPage(parameter);
            if (br.containsHTML(OFFLINETEXT)) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            String finallink = br.getRegex(FINALLINKREGEX).getMatch(0);
            if (finallink == null) finallink = br.getRegex(FINALLINKREGEX2).getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        } else {
            logger.info("Decrypting gallery link...");
            br.getPage(parameter);
            if (br.containsHTML(OFFLINETEXT)) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            String fpName = br.getRegex("<p>Uploaded by <strong>(.*?)</strong>").getMatch(0);
            String[] links = br.getRegex("\\'(http://imageminx\\.com/public/view/full/\\d+)\\'").getColumn(0);
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String singleLink : links) {
                br.getPage(singleLink);
                String finallink = br.getRegex(FINALLINKREGEX).getMatch(0);
                if (finallink == null) finallink = br.getRegex(FINALLINKREGEX2).getMatch(0);
                if (finallink == null) return null;
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                progress.increase(1);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

}
