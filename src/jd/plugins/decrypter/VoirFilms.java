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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "voirfilms.ec" }, urls = { "https?://(?:\\w+\\.)?voirfilms\\.ec/[^/]+\\.html?" })
public class VoirFilms extends PluginForDecrypt {
    public VoirFilms(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // Load page
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>(?:\\s*film\\s+)?([^<]+)(?:\\s+streaming vf\\s*)").getMatch(0);
        final String[] links = br.getRegex("(?:href|data-src)\\s*=\\s*\"((?!javascript)[^\"]+)\"[^>]+target\\s*=\\s*\"filmPlayer").getColumn(0);
        if (links != null && links.length > 0) {
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                if (link.contains("/video.php?")) {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(false);
                    brc.getPage(link);
                    final String redirect = brc.getRedirectLocation();
                    if (redirect != null) {
                        decryptedLinks.add(createDownloadlink(redirect));
                    } else {
                        final String refresh = brc.getRegex("<META\\s*HTTP-EQUIV\\s*=\\s*\"Refresh\"\\s*CONTENT\\s*=\"\\d+; URL=(https?://[^<>\"']+)\"").getMatch(0);
                        if (refresh != null) {
                            decryptedLinks.add(createDownloadlink(refresh));
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                } else {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        }
        if (fpName != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(Encoding.htmlDecode(fpName));
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}