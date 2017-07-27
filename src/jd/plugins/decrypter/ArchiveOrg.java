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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "archive.org" }, urls = { "https?://(?:www\\.)?archive\\.org/details/(?!copyrightrecords)[A-Za-z0-9_\\-\\.]+$" })
public class ArchiveOrg extends PluginForDecrypt {

    public ArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("://www.", "://").replace("/details/", "/download/");
        /*
         * 2017-01-25: We do not (yet) have to be logged in here. We can always see all items and their information but some may be limited
         * to premium users only.
         */
        // final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        // if (aa != null) {
        // jd.plugins.hoster.ArchiveOrg.login(this.br, aa, false);
        // }
        br.getPage(parameter);
        if (br.containsHTML(">The item is not available")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (!br.containsHTML("\"/download/")) {
            logger.info("Maybe invalid link or nothing there to download: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<h1>Index of [^<>\"]+/([^<>\"/]+)/?</h1>").getMatch(0);
        br.setFollowRedirects(true);
        // New way
        final String[][] finfo = br.getRegex("<a href=\"([^<>\"]*?)\">[^<>\"]*?</a>[^<>\"]*?(\\d+\\.\\d+(?:K|M|G|B))").getMatches();
        for (final String[] finfosingle : finfo) {
            final String filename = finfosingle[0];
            String fsize = finfosingle[1];
            final DownloadLink fina = createDownloadlink(br.getURL() + "/" + filename);
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