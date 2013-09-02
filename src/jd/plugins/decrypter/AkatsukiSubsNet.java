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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "akatsuki-subs.net" }, urls = { "http://(www\\.)?akatsuki\\-subs\\.net/(projekte/(laufend|abgeschlossen)/[a-z0-9\\-]+/|\\d+/releases/[a-z0-9]+/[a-z0-9\\-]+\\-\\d+/)" }, flags = { 0 })
public class AkatsukiSubsNet extends PluginForDecrypt {

    public AkatsukiSubsNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String   PROJECTLINK = "http://(www\\.)?akatsuki\\-subs\\.net/projekte/(laufend|abgeschlossen)/[a-z0-9\\-]+/";
    private static final String   RELEASELINK = "http://(www\\.)?akatsuki\\-subs\\.net/\\d+/releases/op/[a-z0-9\\-]+\\-\\d+/";
    private static final String[] QUALITIES   = { "hd", "ru boxvideo", "sd", "rs", "bt" };

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Seite nicht gefunden")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches(PROJECTLINK)) {
            String fpName = br.getRegex("<title>([^<>\"]*?)\\| Akatsuki\\-Subs</title>").getMatch(0);
            if (fpName == null) fpName = new Regex(parameter, "/projekte/(laufend|abgeschlossen)/([a-z0-9\\-]+)/").getMatch(1);
            fpName = Encoding.htmlDecode(fpName.trim());
            // Get all tables
            final String[] tables = br.getRegex("<table (class|id)=\"dl\"(.*?)</table>").getColumn(1);
            for (final String table : tables) {
                // Get entries of each table
                final String[] tableEntries = new Regex(table, "<tr(.*?)</tr>").getColumn(0);
                for (final String tableEntry : tableEntries) {

                    for (final String quality : QUALITIES) {
                        final String[] currentLinks = new Regex(tableEntry, "class=\"" + quality + "\" href=\"(http[^<>\"]*?)\"").getColumn(0);
                        if (currentLinks != null && currentLinks.length != 0) {
                            int counter = 1;
                            for (final String currentLink : currentLinks) {
                                final DownloadLink dl = createDownloadlink(currentLink);
                                FilePackage fp = FilePackage.getInstance();
                                if (counter == 1) {
                                    fp.setName(fpName + " (mp4)");
                                } else if (counter == 2) {
                                    fp.setName(fpName + " (mkv)");
                                } else {
                                    fp.setName(fpName + " (other)");
                                }
                                dl._setFilePackage(fp);
                                decryptedLinks.add(dl);
                                counter++;
                            }
                        }
                    }
                }
            }
        } else {
            // Get all tables
            final String fpName = br.getRegex("<title>([^<>\"]*?)\\| Akatsuki\\-Subs</title>").getMatch(0);
            final String[] tables = br.getRegex("<table id=\"dl_[a-z0-9]+\"(.*?)</table>").getColumn(0);
            for (final String table : tables) {
                // Get entries of each table
                final String[] tableEntries = new Regex(table, "<tr(.*?)</tr>").getColumn(0);
                for (final String tableEntry : tableEntries) {
                    for (final String quality : QUALITIES) {
                        final String[] currentLinks = new Regex(tableEntry, "class=\"" + quality + "\" href=\"(http[^<>\"]*?)\"").getColumn(0);
                        if (currentLinks != null && currentLinks.length != 0) {
                            for (final String currentLink : currentLinks) {
                                decryptedLinks.add(createDownloadlink(currentLink));
                            }
                        }
                    }
                }
            }
            final String[] externalMirrors = br.getRegex("href=\"(http[^<>\"]*?)\" target=\"_blank\" rel=\"nofollow\">[A-Za-z0-9\\-\\.]+ Mirror</a><br").getColumn(0);
            if (externalMirrors != null && externalMirrors.length != 0) {
                for (final String mirror : externalMirrors) {
                    decryptedLinks.add(createDownloadlink(mirror));
                }
            }

            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        return decryptedLinks;
    }

}
