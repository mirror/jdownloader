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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40025 $", interfaceVersion = 2, names = { "shaanig.se" }, urls = { "https?://(www\\.)?shaanig\\.se/((series|episode)/)?.*" })
public class Shaanig extends PluginForDecrypt {
    public Shaanig(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        String page = br.getPage(parameter);
        String fpName = br.getRegex("<meta (?:name|property)=\"og:title\" content=[\"'](?:Watch ?)?([^<>\"]*?)(?: Free Online \\| Shaanig)?[\"'] ?/>").getMatch(0);
        String[][] links = br.getRegex("(?:href|src)=\"(https?://(?:ouo\\.io|dl[0-9]+\\.serverdl\\.in)/[^\"]+)").getMatches();
        if (links == null || links.length == 0) {
            int episodeListStart = page.indexOf("<div id=\"seasons\">");
            int episodeListEnd = page.indexOf("<div class=\"mvi-content", episodeListStart + 1);
            if (episodeListEnd > episodeListStart) {
                String episodeListSnippet = page.substring(episodeListStart, episodeListEnd);
                links = new Regex(episodeListSnippet, "<a href=\"([^\"]+)\">").getMatches();
            }
        }
        for (String[] link : links) {
            decryptedLinks.add(createDownloadlink(br.getURL(Encoding.htmlDecode(link[0])).toString()));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}