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

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

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
        final String fpName = new Regex(parameter, "https?://([^/]+)\\.newgrounds\\.com/").getMatch(0);
        if (parameter.matches(TYPE_AUDIO)) {
            final String[][] urlinfo = br.getRegex("\"(?:https?:)?//(?:www\\.)?newgrounds\\.com/audio/listen/(\\d+)\">([^<>\"]+)</a>").getMatches();
            for (final String[] urlinfosingle : urlinfo) {
                final String fid = urlinfosingle[0];
                final String title = urlinfosingle[1];
                final DownloadLink dl = createDownloadlink("https://www.newgrounds.com/audio/listen/" + fid);
                dl.setAvailable(true);
                dl.setName(Encoding.htmlDecode(title) + "_" + fid + ".mp3");
                dl.setLinkID(fid);
                decryptedLinks.add(dl);
            }
        } else if (parameter.matches(TYPE_ART)) {
            final String[] view_urls = br.getRegex("\"((?:https?:)?//(?:www\\.)?newgrounds\\.com/art/view/[^<>\"]*?)\"").getColumn(0);
            if (view_urls == null || view_urls.length == 0) {
                return null;
            }
            for (String view_url : view_urls) {
                if (view_url.startsWith("//")) {
                    view_url = "https:" + view_url;
                }
                final DownloadLink dl = createDownloadlink(view_url);
                dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
                dl.setAvailable(true); // <---
                decryptedLinks.add(dl);
            }
        } else {
            /* movies & games */
            final String[] view_urls = br.getRegex("\"((?:https?:)?//(?:www\\.)?newgrounds\\.com/portal/view/[^<>\"]*?)\"").getColumn(0);
            if (view_urls == null || view_urls.length == 0) {
                return null;
            }
            for (String view_url : view_urls) {
                if (view_url.startsWith("//")) {
                    view_url = "https:" + view_url;
                }
                final DownloadLink dl = createDownloadlink(view_url);
                dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
