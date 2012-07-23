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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "http://(www\\.)?soundcloud\\.com/[^\"\\'\\&]+(\\?page=\\d+)?" }, flags = { 0 })
public class SoundCloudComDecrypter extends PluginForDecrypt {

    public SoundCloudComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("(/download|\\\\)", "").replace("www.", "");
        br.setFollowRedirects(true);
        boolean decryptList = parameter.matches(".*?soundcloud\\.com/[a-z\\-_0-9]+/(tracks|favorites)(\\?page=\\d+)?");
        if (!decryptList) {
            decryptList = !parameter.matches(".*?soundcloud\\.com/[a-z\\-_0-9]+/[a-z\\-_0-9]+(/)?");
            if (!decryptList) decryptList = parameter.contains("/groups/");
        }
        if (decryptList) {
            br.getPage(parameter);
            if (br.containsHTML(">Oops, looks like we can\\'t find that page")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML(">Favorites<abbr> \\(0\\)") && br.containsHTML(">Sets|<abbr> \\(0\\)") && br.containsHTML(">Tracks<abbr> \\(0\\)")) {
                logger.info("Nothing to decrypt from link: " + parameter);
                return decryptedLinks;
            }
            String fpName = br.getRegex("<title>(.*?) on SoundCloud \\- Create, record and share your sounds for free</title>").getMatch(0);
            String[] links = br.getRegex("\"(http://soundcloud\\.com/[^\"\\'<>]+)\" property=\"og:song\"").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("class=\"info\"><span>\\d+\\.</span>[\t\n\r ]+<a href=\"(/.*?)\"").getColumn(0);
                if (links == null || links.length == 0) {
                    links = br.getRegex("class=\"action\\-overlay\\-inner\"><a href=\"(/.*?)\"").getColumn(0);
                    if (links == null || links.length == 0) {
                        links = br.getRegex("<h3><a href=\"(/.*?)\"").getColumn(0);
                    }
                }
            }
            String[] playListLinks = br.getRegex("<li class=\"playlist\\-player \"><a href=\"(/[^\"\\'<>]+)(\\?size=large)?\"").getColumn(0);
            if ((links == null || links.length == 0) && (playListLinks == null || playListLinks.length == 0)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (links != null && links.length != 0) {
                for (String sclink : links)
                    decryptedLinks.add(createDownloadlink("http://soundclouddecrypted.com" + sclink));
            }
            if (playListLinks != null && playListLinks.length != 0) {
                for (String sclink : playListLinks)
                    decryptedLinks.add(createDownloadlink("http://soundclouddecrypted.com" + sclink));
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        } else {
            decryptedLinks.add(createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted")));
        }
        return decryptedLinks;
    }

}
