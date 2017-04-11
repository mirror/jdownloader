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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "corenews.me" }, urls = { "http?://corenews\\.me/[a-zA-Z0-9\\\\-]+/" }) 
public class CoreNewsMe extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public CoreNewsMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, @SuppressWarnings("deprecation") ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML(">Es existiert kein Eintrag mit der ID") || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
            }
            return decryptedLinks;
        }
        final String source = br.getRegex("<div class=\"contenttext\"(.*?)<p><strong>Tracklist:</strong>").getMatch(0);
        final String fpName = br.getRegex("<title>[A-Za-z0-9\\-]+</title>").getMatch(0);
        final String[] links = new Regex(source, "<a href=\\\"(https?://[^\\\"]+)").getColumn(0);

        if (links != null) {
            for (final String link : links) {
                if ((!link.contains("http://bit.ly/uploadedpr")) && (!link.contains("corenews.me"))) {
                    final DownloadLink downloadLink = createDownloadlink(link);
                    decryptedLinks.add(downloadLink);
                }
            }
        }
        return decryptedLinks;
    }
}
