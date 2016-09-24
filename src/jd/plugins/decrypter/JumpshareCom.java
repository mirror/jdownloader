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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jumpshare.com" }, urls = { "https?://(?:www\\.)?(?:jmp\\.sh/(?!v/)[A-Za-z0-9]+|jumpshare\\.com/b/[A-Za-z0-9]+)" })
public class JumpshareCom extends PluginForDecrypt {

    public JumpshareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_FOLDER = "https?://(?:www\\.)?jumpshare\\.com/b/[A-Za-z0-9]+";
    private static final String TYPE_FILE   = "https?://(?:www\\.)?jumpshare\\.com/v/[A-Za-z0-9]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(TYPE_FOLDER)) {
            this.br.setFollowRedirects(true);
            br.getPage(parameter);

            if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Folder Not Found|The folder you are looking for does not exist")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }

            final String folderid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
            String fpName = br.getRegex("property=\"og:title\" content=\"([^<>]+)\"").getMatch(0);
            if (fpName == null) {
                fpName = folderid;
            }
            final String[] links = br.getRegex("/v/([A-Za-z0-9]+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String linkid : links) {
                final String url = "https://" + this.getHost() + "/v/" + linkid;
                /* Contenturl for the user to copy - let's use the same urls they use in browser. */
                final String url_content = url + "?b=";
                final DownloadLink dl = createDownloadlink(url);
                dl.setLinkID(linkid);
                dl.setContentUrl(url_content);
                decryptedLinks.add(dl);
            }

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        } else {
            this.br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String finallink = this.br.getRedirectLocation();
            if (finallink == null) {
                return null;
            }
            /* Validate result */
            if (!finallink.matches(TYPE_FOLDER) && !finallink.matches(TYPE_FILE) && finallink.contains(this.getHost())) {
                logger.info("WTF, invalid finallink");
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }

}
