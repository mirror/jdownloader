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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "definebabe.com" }, urls = { "https?://(?:www\\.)?definebabes?\\.com/video/[a-z0-9]+/[a-z0-9\\-]+/" })
public class DefinebabeComDecrypter extends PornEmbedParser {
    public DefinebabeComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (jd.plugins.hoster.DefineBabeCom.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        decryptedLinks.addAll(this.findEmbedUrls(null));
        if (decryptedLinks.size() == 0) {
            /* Pass url to hosterplugin */
            final DownloadLink dl = createDownloadlink(parameter.replaceAll("https?://", "definebabedecrypted://"));
            dl.setName(getFilename(this.br, parameter));
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    public static String getURLTitle(final String url) {
        return new Regex(url, "([a-z0-9\\-]+)/?$").getMatch(0);
    }

    public static String getFilename(final Browser br, final String url) {
        final String url_filename = getURLTitle(url);
        String filename = br.getRegex("<div id=\"sp\">\\s*?<b>([^<>\"]+)</b>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename += ".mp4";
        return filename;
    }
}
