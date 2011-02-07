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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "reverbnation.com" }, urls = { "http://(www\\.)?reverbnation\\.com/.+" }, flags = { 0 })
public class ReverBnationCom extends PluginForDecrypt {

    public ReverBnationCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String artist = br.getRegex("=artist_(\\d+)\"").getMatch(0);
        if (artist == null) {
            artist = br.getRegex("CURRENT_PAGE_OBJECT = \\'artist_(\\d+)\\';").getMatch(0);
            if (artist == null) {
                artist = br.getRegex("_artist_(\\d+)").getMatch(0);
            }
        }
        String[] ids = br.getRegex("id=\"tuxedo_song_row_(\\d+)\"").getColumn(0);
        if (ids == null || ids.length == 0) {
            ids = br.getRegex("onclick=\"playSongNow\\(\\'(\\d+)\\'\\)").getColumn(0);
            if (ids == null || ids.length == 0) {
                ids = br.getRegex("onclick=\"addSongToQueue\\(\\'(\\d+)\\'\\)").getColumn(0);
            }
        }
        final String[] titleContent = br.getRegex("songpopup-(songname|artistname).*?\">(.*?)(</div>|\">)").getColumn(1);
        if (ids == null || ids.length == 0 || titleContent == null || titleContent.length == 0 || artist == null) { return null; }
        final String[] title = new String[ids.length];
        for (int i = 0, j = 0; j < ids.length; i += 2, j++) {
            if (!titleContent[i].contains(titleContent[i + 1])) {
                title[j] = titleContent[i + 1] + " - " + titleContent[i];
            } else {
                title[j] = titleContent[i];
            }
        }
        int nameCounter = 0;
        for (int i = 0; i < ids.length; i++) {
            final DownloadLink dlLink = createDownloadlink("reverbnationcomid" + ids[i] + "reverbnationcomartist" + artist);
            dlLink.setName(title[i].replaceAll("<span title=\"", ""));
            FilePackage fp = FilePackage.getInstance();
            fp.setName(titleContent[nameCounter + 1]);
            dlLink.setFilePackage(fp);
            decryptedLinks.add(dlLink);
            nameCounter += 2;
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }
}