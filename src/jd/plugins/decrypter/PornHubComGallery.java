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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhub.com" }, urls = { "https?://(www\\.|[a-z]{2}\\.)?pornhub\\.com/album/\\d+" })
public class PornHubComGallery extends PluginForDecrypt {
    public PornHubComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceAll("https://", "http://");
        parameter = parameter.replaceAll("^http://(www\\.)?([a-z]{2}\\.)?", "http://www.");
        br.setFollowRedirects(true);
        jd.plugins.hoster.PornHubCom.getPage(br, parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        String fpName = br.getRegex("class=\"photoAlbumTitleV2\">([^<>\"]*?)<").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("title>([^<>\"]*?)</title>").getMatch(0);
        }
        if (fpName == null) {
            fpName = "pornhub.com album " + new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        final String[] links = br.getRegex("\"/photo/(\\d+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleID : links) {
            final DownloadLink dl = createDownloadlink("http://www.pornhub.com/photo/" + singleID);
            dl.setName(singleID + ".jpg");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
