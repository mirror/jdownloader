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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "newgrounds.com" }, urls = { "https?://(?!www\\.)[^/]+\\.newgrounds\\.com/(?:art|audio|movies|games)/" })
public class NewgroundsComDecrypter extends PluginForDecrypt {
    public NewgroundsComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_ART   = ".+/art/";
    private static final String TYPE_AUDIO = ".+/audio/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String fpName = null;
        if (parameter.matches(TYPE_AUDIO)) {
            fpName = new Regex(parameter, "http://([^/]+)\\.newgrounds\\.com/").getMatch(0);
            final String[][] urlinfo = this.br.getRegex("href=\"https?://(?:www\\.)?newgrounds\\.com/audio/listen/(\\d+)\">([^<>\"]+)</a>").getMatches();
            for (final String[] urlinfosingle : urlinfo) {
                final String fid = urlinfosingle[0];
                final String title = urlinfosingle[1];
                final DownloadLink dl = this.createDownloadlink("http://www.newgrounds.com/audio/listen/" + fid);
                dl.setAvailable(true);
                dl.setName(Encoding.htmlDecode(title) + "_" + fid + ".mp3");
                dl.setLinkID(fid);
                decryptedLinks.add(dl);
            }
        } else if (parameter.matches(TYPE_ART)) {
            final String[] art_urls = this.br.getRegex("\"(https?://(?:www\\.)?newgrounds\\.com/art/view/[^<>\"]*?)\"").getColumn(0);
            if (art_urls == null || art_urls.length == 0) {
                return null;
            }
            for (final String art_url : art_urls) {
                final DownloadLink dl = createDownloadlink(art_url);
                decryptedLinks.add(dl);
            }
        } else {
            /* movies & games */
            final String[] portal_view_urls = this.br.getRegex("\"(https?://(?:www\\.)?newgrounds\\.com/portal/view/[^<>\"]*?)\"").getColumn(0);
            if (portal_view_urls == null || portal_view_urls.length == 0) {
                return null;
            }
            for (final String art_url : portal_view_urls) {
                final DownloadLink dl = createDownloadlink(art_url);
                decryptedLinks.add(dl);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
