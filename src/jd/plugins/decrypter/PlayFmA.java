//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Set;
import java.util.TreeSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "play.fm" }, urls = { "http://(www\\.)?play\\.fm/artist/\\w+[^#]" }, flags = { 0 })
public class PlayFmA extends PluginForDecrypt {

    public PlayFmA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        setBrowserExclusive();
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>(.*?) -.*</title>").getMatch(0);
        final ArrayList<String> song = new ArrayList<String>();
        final Set<String> finalPages = new TreeSet<String>();
        final String[] pages = br.getRegex("<a class=\"paginator\".*?href=\"(.*?/chrono/page/\\d+)\"><").getColumn(0);
        // doppelte Eintraege loeschen
        for (final String p : pages) {
            finalPages.add(p);
        }
        progress.setRange(finalPages.size());
        for (final String page : finalPages) {
            progress.increase(1);
            final String[] matches = br.getRegex("#play_\\d+").getColumn(-1);
            for (final String match : matches) {
                song.add(match);
            }
            br.getPage("http://www.play.fm" + page);
        }
        if (song == null || song.size() == 0) { return null; }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName.trim());
        for (final String url : song) {
            final DownloadLink dlLink = createDownloadlink("http://www.play.fm/" + url);
            dlLink.setFilePackage(fp);
            decryptedLinks.add(dlLink);
        }

        return decryptedLinks;

    }
}
