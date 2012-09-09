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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hydroshare.tv" }, urls = { "http://(www\\.)?hydroshare\\.tv/(?!Log\\-yourself\\-in|popularalbums|User\\-registration|artists|djs|brands|dj\\-learn|privacypolicy|termsofuse|contact\\-us|myaccount|dmca|hydroshare\\-(faq|distribution)|brand\\-learn|updatedvideos|albumsearch|newsletter|how\\-it\\-works|upload|recentalbums|Channels|Forgotten\\-password).*?\\.html" }, flags = { 0 })
public class HydroShareTvAlbum extends PluginForDecrypt {

    public HydroShareTvAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (!br.containsHTML("nmaplayer\\.swf")) {
            logger.info("Wrong/Unsupported link: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        final String uid = br.getRegex("uid: \"([^<>\"]*?)\"").getMatch(0);
        if (uid == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage("http://www.hydroshare.tv/modules/mod_nmap/audio/nmapl.php?s=" + uid);
        final String[] links = br.getRegex("<song file=\"(http://[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            decryptedLinks.add(createDownloadlink("directhttp://" + singleLink));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
