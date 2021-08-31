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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "byte.to" }, urls = { "https?://(?:www\\.)?byte\\.to/(?:category/[A-Za-z0-9\\-/]+-\\d+\\.html|\\?id=\\d+)|https?://byte\\.to/go\\.php\\?hash=.+" })
public class BteTo extends PluginForDecrypt {
    public BteTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* This is a modified CMS site */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(".+go\\.php.+")) {
            /* 2019-08-29: Single redirect URLs */
            br.setFollowRedirects(false);
            br.getPage(parameter);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                return null;
            }
            decryptedLinks.add(this.createDownloadlink(finallink));
        } else {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*Es existiert kein Eintrag mit der ID")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String fpName = br.getRegex("<title>Byte\\.to \\-([^<>\"]*?)</title>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<TITLE>\\s*(?:byte\\.to)?(.*?)</TITLE>").getMatch(0);
            }
            final String[] redirectIDs = br.getRegex("/widgets/button\\.php\\?([a-zA-Z0-9_/\\+\\=\\-%]+)").getColumn(0);
            for (String redirectID : redirectIDs) {
                if (redirectID.contains("%")) {
                    redirectID = Encoding.htmlDecode(redirectID);
                }
                decryptedLinks.add(this.createDownloadlink("https://" + this.getHost() + "/go.php?hash=" + redirectID));
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }
}
