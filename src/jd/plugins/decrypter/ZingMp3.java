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
import java.util.List;
import java.util.regex.Matcher;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 1 $", interfaceVersion = 1, names = { "mp3.zing.vn" }, urls = { "http://mp3\\.zing\\.vn/album/(\\S+)" }, flags = { 0 })
public class ZingMp3 extends PluginForDecrypt {

    public ZingMp3(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.getCryptedUrl();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(url);
        // package name
        String title = br.getRegex("<title[^>]*>(.*?)</title>").getMatch(0);
        title = title.split("\\|")[0];
        FilePackage filePackage = FilePackage.getInstance();
        filePackage.setName(title);
        // get all songs
        List<String> allMatches = new ArrayList<String>();
        Matcher m = br.getRegex("\"http://mp3.zing.vn/bai-hat/(.*?)\"").getMatcher();
        while (m.find()) {
            if (!allMatches.contains(m.group())) {
                allMatches.add(m.group());
            }
        }
        String link = null;
        for (final String s : allMatches) {
            link = s.replace("\"", "");
            DownloadLink dllink = createDownloadlink(link);
            dllink._setFilePackage(filePackage);
            decryptedLinks.add(dllink);
        }
        return decryptedLinks;
    }

}
