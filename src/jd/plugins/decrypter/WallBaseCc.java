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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wallbase.cc" }, urls = { "http://(www\\.)?wallbase.cc/wallpaper/\\d+" }, flags = { 0 })
public class WallBaseCc extends PluginForDecrypt {

    public WallBaseCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>(.*?) \\- Wallpaper \\(").getMatch(0);
        String finallink = br.getRegex("<div id=\"bigwall\" class=\"right\">[\t\n\r ]+<img src=\"(http://.*?)\"").getMatch(0);
        if (finallink == null) finallink = br.getRegex("\"(http://ns\\d+\\.ovh\\.net/.*?)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        finallink = Encoding.htmlDecode(finallink);
        DownloadLink dl = createDownloadlink("directhttp://" + finallink);
        if (filename != null) {
            String ext = finallink.substring(finallink.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".jpg";
            /** We have to add the ID because many filenames are the same */
            dl.setFinalFileName(new Regex(parameter, "(\\d+)$").getMatch(0) + "_" + Encoding.htmlDecode(filename) + ext);
        }
        decryptedLinks.add(dl);

        return decryptedLinks;
    }

}
