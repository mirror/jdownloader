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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "reverbnation.com" }, urls = { "http://(www\\.)?reverbnation\\.com/.+" }, flags = { 0 })
public class ReverBnationCom extends PluginForDecrypt {

    public ReverBnationCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
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
        if (ids == null || ids.length == 0 || artist == null) return null;
        for (String id : ids) {
            decryptedLinks.add(createDownloadlink("reverbnationcomid" + id + "reverbnationcomartist" + artist));
        }

        return decryptedLinks;
    }

}
