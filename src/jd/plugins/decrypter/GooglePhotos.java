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
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "photos.google.com" }, urls = { "https?://photos\\.google\\.com/share/[A-Za-z0-9\\-_]+\\?key=[A-Za-z0-9\\-_]+" })
public class GooglePhotos extends PluginForDecrypt {

    public GooglePhotos(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final boolean fastlinkcheck = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.GooglePhotos.FAST_LINKCHECK, false);
        br = new Browser();
        // use english not german!
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Album is empty<")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Regex urlinfo = new Regex(parameter, "/share/([A-Za-z0-9\\-_]+)\\?key=([A-Za-z0-9\\-_]+)");
        final String idMAIN = urlinfo.getMatch(0);
        final String key = urlinfo.getMatch(1);
        final String[] ids = br.getRegex("\\[\"([A-Za-z0-9\\-_]{22,})\",\\[\"https").getColumn(0);
        String fpName = br.getRegex("<title>(?:Shared album\\s*-\\s*)?[A-Za-z0-9 ]+ – ([^<>\"]+)\\s*-\\s*Google (?:Ph|F)otos</title>").getMatch(0);
        if (fpName == null) {
            fpName = idMAIN;
        }
        if (ids == null || ids.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String idSINGLE : ids) {
            final String finallink = "https://photos.google.com/share/" + idMAIN + "/photo/" + idSINGLE + "?key=" + key;
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setName(idSINGLE);
            if (fastlinkcheck) {
                dl.setAvailable(true);
            }
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
