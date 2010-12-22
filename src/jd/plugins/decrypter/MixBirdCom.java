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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixbird.com" }, urls = { "http://(www\\.)?mixbird\\.com/mixtapes/(listen|download)/[a-z0-9_]+" }, flags = { 0 })
public class MixBirdCom extends PluginForDecrypt {

    public MixBirdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("/download/", "/listen/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("GO Back\\! Ya dun fucked up\\!")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String tapeID = br.getRegex("\"playMixtape\\((\\d+)").getMatch(0);
        if (tapeID == null) return null;
        String artist = br.getRegex("\"> Artist: (.*?)</font>").getMatch(0);
        String fpName = br.getRegex("<title>Mixbird // (.*?)</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("rel=\"featured\"><img src=\"http://mixbird\\.com/mixtapes/images/\\d+_cover\\.jpg\" alt=\"(.*?)\"").getMatch(0);
        }
        br.getPage("http://mixbird.com/mixtapes/xml/playlist_" + tapeID + ".xml");
        String[] links = br.getRegex("url=\"(http://mixbird\\.com/mixtapes/tracks/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String finallink : links)
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        if (fpName != null) {
            if (artist != null) fpName = artist.trim() + " - " + fpName.trim();
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
