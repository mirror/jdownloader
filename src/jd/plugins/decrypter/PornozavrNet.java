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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornozavr.net" }, urls = { "https?://(?:www\\.)?pornozavr\\.net/([a-z0-9\\-]+)\\.html" })
public class PornozavrNet extends PornEmbedParser {
    public PornozavrNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter, "Offline Content"));
            return decryptedLinks;
        }
        String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = new Regex(parameter, this.getSupportedLinks()).getMatch(0).replace("-", " ");
        }
        decryptedLinks.addAll(findEmbedUrls(title));
        if (decryptedLinks.size() == 0) {
            final String dllink = br.getRegex("<source src=(?:\"|\\')(https?://[^<>\"\\']*?)(?:\"|\\')[^>]*?type=(?:\"|\\')(?:video/)?(?:mp4|flv)(?:\"|\\')").getMatch(0);
            if (dllink != null) {
                final DownloadLink dl = this.createDownloadlink(dllink);
                dl.setForcedFileName(title + ".mp4");
                decryptedLinks.add(dl);
            } else {
                decryptedLinks.add(createOfflinelink(parameter, "Failed to find any downloadable Content"));
                return decryptedLinks;
            }
        }
        return decryptedLinks;
    }
}