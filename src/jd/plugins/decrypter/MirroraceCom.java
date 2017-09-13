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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mirrorace.com" }, urls = { "https?://(?:www\\.)?mirrorace.com/m/[A-Za-z0-9]+" })
public class MirroraceCom extends PluginForDecrypt {
    public MirroraceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>\\s*(?:Download)?\\s*([^<]*?)\\s*(?:-\\s*MirrorAce)?\\s*</title>").getMatch(0);
        final String[] links = br.getRegex("\"(https?://mirrorace\\.com/m/[A-Za-z0-9]+/\\d+\\?t=[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            this.br.getPage(singleLink);
            final String finallink = this.br.getRegex("<a class=\"uk\\-button uk\\-button\\-primary\" href=\"(http[^<>\"]+)").getMatch(0);
            if (finallink == null) {
                return null;
            }
            final DownloadLink dl;
            if (finallink.contains("mirrorace.com")) {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                brc.getPage(finallink);
                if (brc.getRedirectLocation() != null) {
                    dl = createDownloadlink(brc.getRedirectLocation());
                } else {
                    continue;
                }
            } else {
                dl = createDownloadlink(finallink);
            }
            decryptedLinks.add(dl);
            distribute(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
