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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

//This decrypter is there to seperate folder- and hosterlinks as hosterlinks look the same as folderlinks
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depfile.com" }, urls = { "https?://(www\\.)?(i\\-filez\\.com|d[ei]pfile\\.com|depfile\\.us)/(downloads/i/\\d+/f/[^\"\\']+|(?!downloads)[a-zA-Z0-9]+)" })
public class DepfileComDecrypter extends PluginForDecrypt {

    public DepfileComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "https?://(www\\.)?(?:d[ei]pfile\\.com|depfile\\.us)/(myspace|uploads|support|privacy|checkfiles|register|premium|terms|childpolicy|affiliate|report|data|dmca|favicon|ajax|API|robots)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        plugin = JDUtilities.getPluginForHost("depfile.com");
        final String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        final String folder_id = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        ((jd.plugins.hoster.DepfileCom) plugin).setBrowser(br);
        ((jd.plugins.hoster.DepfileCom) plugin).prepBrowser();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        handleErrors();
        // mass adding links from folders can cause exceptions and high server loads. when possible best practice to set all info, a full
        // linkcheck happens prior to download which can correct any false positives by doing the following..
        final String[] links = br.getRegex("<tr><td[^>]+><input[^>]+>.*?</td></tr>").getColumn(-1);
        if (links != null && links.length != 0) {
            // folder link
            for (String link : links) {
                final String l = new Regex(link, "https?://(?:d[ei]pfile\\.com|depfile\\.us)/[A-Za-z0-9]+|https?://(www\\.)?(?:d[ei]pfile\\.com|depfile\\.us)/[a-zA-Z0-9]{8}\\?cid=[a-z0-9]{32}").getMatch(-1);
                final String f = new Regex(link, "title=(\"|')(.*?)\\1").getMatch(1);
                final String s = new Regex(link, ">(\\d+(\\.\\d+)? [a-z]{2})<").getMatch(0);
                if (l != null && !l.contains(folder_id) && !new URL(l).getPath().equals("/downloads")) {
                    final DownloadLink d = createDownloadlink(l);
                    if (f != null) {
                        d.setName(f);
                    }
                    if (s != null) {
                        d.setDownloadSize(SizeFormatter.getSize(s));
                    }
                    d.setAvailable(true);
                    decryptedLinks.add(d);
                }
            }
            final String fpName = br.getRegex("<th>Folder name:</th>\\s*<td>(.*?)</td>").getMatch(0);
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
        // when 'folder link' above has failed...
        if (br.containsHTML(">Description of the downloaded folder")) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        // single link
        if (decryptedLinks.isEmpty()) {
            decryptedLinks.add(createDownloadlink(param.toString()));
        }
        return decryptedLinks;
    }

    private PluginForHost plugin = null;

    private void handleErrors() throws Exception {
        try {
            ((jd.plugins.hoster.DepfileCom) plugin).setBrowser(br);
            ((jd.plugins.hoster.DepfileCom) plugin).handleErrors();
        } catch (final Exception e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}