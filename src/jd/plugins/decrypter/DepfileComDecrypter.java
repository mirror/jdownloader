//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

//This decrypter is there to seperate folder- and hosterlinks as hosterlinks look the same as folderlinks
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depfile.com" }, urls = { "https?://(www\\.)?(i\\-filez\\.com|d[ei]pfile\\.com|depfile\\.us)/(downloads/i/\\d+/f/[^\"\\']+|(?!downloads)[a-zA-Z0-9]+)" })
public class DepfileComDecrypter extends PluginForDecrypt {
    public DepfileComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "https?://(www\\.)?(?:d[ei]pfile\\.com|depfile\\.us)/(myspace|uploads|support|privacy|checkfiles|register|premium|terms|childpolicy|affiliate|report|data|dmca|favicon|ajax|API|robots)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        final String host = new Regex(parameter, "(https?://[^/]+)/").getMatch(0);
        final String folder_id = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        jd.plugins.hoster.DepfileCom.prepBrowser(this.br);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String nops = br.getRegex("current.>1/(\\d+)<").getMatch(0);
        int nps = 1;
        if (nops != null) {
            logger.info("Debug info: Number of pages: " + nops);
            nps = Integer.valueOf(nops);
        }
        for (int i = 2; i <= nps + 1; i++) {
            // mass adding links from folders can cause exceptions and high server loads. when possible best practice to set all info, a
            // full
            // linkcheck happens prior to download which can correct any false positives by doing the following..
            final String[] links = br.getRegex("<tr>\\s*<td[^>]+><input[^>]+>.*?</td>\\s*</tr>").getColumn(-1);
            if (links != null && links.length != 0) {
                // folder link
                for (String link : links) {
                    final String l = host + new Regex(link, "window.open\\('(.*?)'\\)").getMatch(0);
                    final String f = new Regex(link, "title=(\"|')(.*?)\\1").getMatch(1);
                    final String s = new Regex(link, ";(\\d+(\\.\\d+)?\\s*[a-zA-Z]{2})<").getMatch(0);
                    final boolean folder = new Regex(link, "<input type='text' class='view state_0 folder'").matches();
                    if (l != null && !l.contains(folder_id) && !new URL(l).getPath().equals("/downloads")) {
                        final DownloadLink d = createDownloadlink(l);
                        if (f != null) {
                            d.setName(f);
                        }
                        if (s != null) {
                            d.setDownloadSize(SizeFormatter.getSize(s));
                        }
                        if (!folder) {
                            // cant set true as we need to re-enter.
                            d.setAvailable(true);
                        }
                        decryptedLinks.add(d);
                        distribute(d);
                    }
                    final String fpName = br.getRegex("<th[^<>]*?>Folder name:</th>\\s*<td>(.*?)</td>").getMatch(0);
                    if (fpName != null) {
                        FilePackage fp = FilePackage.getInstance();
                        fp.setName(fpName.trim());
                        fp.addLinks(decryptedLinks);
                    }
                }
            }
            if (i < nps + 1) {
                br.getPage(parameter + "?page=" + i);
            }
        }
        // when 'folder link' above has failed...
        if (decryptedLinks.isEmpty() && br.containsHTML(">Description of the downloaded folder")) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        // single link
        if (decryptedLinks.isEmpty()) {
            final DownloadLink dl = createDownloadlink(param.toString());
            dl.setAvailableStatus(jd.plugins.hoster.DepfileCom.parseAvailableStatus(this.br, dl));
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}