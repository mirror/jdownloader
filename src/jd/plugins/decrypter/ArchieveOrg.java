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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "archive.org" }, urls = { "https?://(www\\.)?archive\\.org/(?:details|download)/(?!copyrightrecords)[A-Za-z0-9_\\-\\.]+$" }) 
public class ArchieveOrg extends PluginForDecrypt {

    public ArchieveOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("://www.", "://").replace("/details/", "/download/");
        br.getPage(parameter);
        if (br.containsHTML(">The item is not available")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (!br.containsHTML("\"/download/")) {
            logger.info("Maybe invalid link or nothing there to download: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<h1>Index of [^<>\"]+/([^<>\"/]+)/?</h1>").getMatch(0);
        br.setFollowRedirects(true);
        // New way
        final String[][] finfo = br.getRegex("<a href=\"([^<>\"]*?)\">[^<>\"]*?</a>[^<>\"]*?(\\d+\\.\\d+(?:K|M|G|B))").getMatches();
        for (final String[] finfosingle : finfo) {
            final String filename = finfosingle[0];
            String fsize = finfosingle[1];
            final DownloadLink fina = createDownloadlink("directhttp://" + br.getURL() + "/" + filename);
            fsize += "b";
            fina.setDownloadSize(SizeFormatter.getSize(fsize));
            fina.setAvailable(true);
            fina.setFinalFileName(Encoding.urlDecode(filename, false));
            decryptedLinks.add(fina);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (fpName != null) {
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}