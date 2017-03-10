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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "updup.net" }, urls = { "https?://updup\\.net/reDirect/[A-Za-z0-9]+/\\d+|https?://(?:www\\.)?updup.net/[A-Za-z0-9]+" })
public class UpdupNet extends PluginForDecrypt {

    public UpdupNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        if (parameter.matches(".+/reDirect/.+")) {
            /* Single redirect url */
            this.br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.toString().length() < 20) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final DownloadLink dl = decryptSingle();
            if (dl == null) {
                return null;
            }
            decryptedLinks.add(dl);
        } else {
            /* Multiple reedirect urls */
            this.br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("class=notfound") || !this.br.containsHTML("class=mirrorlink")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String fpName = br.getRegex("id=filename><h1>([^<>\"]+)<").getMatch(0);
            FilePackage fp = null;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
            }

            final String[] links = br.getRegex("(/reDirect/[^/]+/\\d+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            this.br.setFollowRedirects(false);
            for (final String singleLink : links) {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                br.getPage(singleLink);
                final DownloadLink dl = decryptSingle();
                if (dl == null) {
                    return null;
                }
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                decryptedLinks.add(dl);
                distribute(dl);
            }

            if (fp != null) {
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

    private DownloadLink decryptSingle() {
        final String finallink = this.br.getRedirectLocation();
        if (finallink == null || finallink.contains(this.getHost())) {
            return null;
        }
        final DownloadLink dl = createDownloadlink(finallink);
        distribute(dl);
        return dl;
    }

}
