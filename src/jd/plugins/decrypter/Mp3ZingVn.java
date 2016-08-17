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

/**
 * @author noone2407
 */
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

/**
 *
 * @author noone2407
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mp3.zing.vn" }, urls = { "http://mp3\\.zing\\.vn/album/\\S+" }) 
public class Mp3ZingVn extends PluginForDecrypt {

    public Mp3ZingVn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String url = parameter.toString();
        br.setFollowRedirects(true);
        br.getPage(url);
        // package name
        String title = br.getRegex("<title[^>]*>(.*?)</title>").getMatch(0);
        if (title != null) {
            title = title.split("\\|")[0];
        }
        // get all songs
        final String[] allMatches = br.getRegex("\"(http://mp3.zing.vn/bai-hat/.*?)\"").getColumn(0);
        if (allMatches != null) {
            for (final String s : allMatches) {
                decryptedLinks.add(createDownloadlink(s));
            }
        }
        if (title != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(title);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
