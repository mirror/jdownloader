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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "biqle.ru", "daxab.com", "divxcim.com" }, urls = { "https?://(?:www\\.)?biqle\\.(com|ru)/watch/(?:\\-)?\\d+_\\d+", "https?://(?:www\\.)?daxab\\.com/embed/(?:\\-)?\\d+_\\d+", "https?://(?:www\\.)?divxcim\\.com/video_ext\\.php\\?oid=(?:\\-)?\\d+\\&id=\\d+" }, flags = { 0, 0, 0 })
public class BiqleRu extends PluginForDecrypt {

    public BiqleRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Converts embedded crap to vk.com video-urls. */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String videoid_part;
        if (parameter.matches("https?://(?:www\\.)?divxcim\\.com/video_ext\\.php\\?oid=(?:\\-)?\\d+\\&id=\\d+")) {
            final String oid = new Regex(parameter, "oid=((?:\\-)?\\d+)").getMatch(0);
            final String id = new Regex(parameter, "id=(\\d+)").getMatch(0);
            videoid_part = oid + "_:" + id;
        } else {
            videoid_part = new Regex(parameter, "((?:\\-)?\\d+_\\d+)").getMatch(0);
        }
        final String finallink = "https://vk.com/video" + videoid_part;
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
