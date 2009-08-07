//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "movietown.info" }, urls = { "http://[\\w\\.]*?movietown\\.info/index/download\\.php\\?id=\\d+"}, flags = { 0 })


public class MovieTown extends PluginForDecrypt {

    public MovieTown(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.toString();
        this.setBrowserExclusive();
        br.getPage(url);
        String pw = br.getRegex("Passwort:<.*?<span class=.*?>(.*?)</span>").getMatch(0);

        String[] links = br.getRegex("Mirror.*?\\d+.*?<a href=\"(.*?)\"").getColumn(0);
        progress.setRange(links.length);
        for (String link : links) {
            DownloadLink dl = this.createDownloadlink(link);
            if (pw != null && !pw.contains("keins")) dl.addSourcePluginPassword(pw.trim());
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

}
