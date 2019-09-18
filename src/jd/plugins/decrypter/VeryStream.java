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
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "verystream.com" }, urls = { "https?://(?:\\w+\\.)?verystream\\.com/(?:e|embed)/.+" })
public class VeryStream extends antiDDoSForDecrypt {
    public VeryStream(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<meta\\s*name\\s*=\\s*\"og:title\"\\s*content\\s*=\\s*\"([^\"]+)").getMatch(0);
        String[] links = br.getRegex("<track[^>]+src\\s*=\\s*\"([^\"]+)\"[^>]+kind\\s*=\\s*\"caption").getColumn(0);
        // NOTE: The video stream is already handled by the hoster plugin, this simply covers the optional subtitle files.
        if (links != null && links.length > 0) {
            for (String link : links) {
                DownloadLink dl = createDownloadlink(Encoding.htmlDecode(link));
                if (fpName != null && fpName.length() > 0) {
                    String filename = fpName;
                    final String extOld = getFileNameExtensionFromString(filename);
                    final String extNew = getFileNameExtensionFromString(link);
                    if (extOld != null && extNew != null) {
                        filename = filename.replaceAll(Pattern.quote(extOld) + "$", extNew);
                        dl.setFinalFileName(filename);
                    }
                }
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }
}