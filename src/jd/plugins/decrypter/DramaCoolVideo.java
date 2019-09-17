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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dramacool.video" }, urls = { "https?://(?:www\\d*\\.)?dramacool\\.video/(?:.+\\.html?|drama-detail/.+)" })
public class DramaCoolVideo extends PluginForDecrypt {
    public DramaCoolVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>(?:Watch\\s+)([^<]+)\\s+\\|[\\s\\w]+").getMatch(0);
        String[] links = br.getRegex("<li[^>]+data-video=\"([^\"]+)\">").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("<li>\\s*<a href=\"([^\"]+)\" class=\"img\">\\s*<span class=\"type[^\"]*\">").getColumn(0);
        }
        if (links != null && links.length > 0) {
            for (String link : links) {
                if (link.startsWith("/")) {
                    link = br.getURL(link).toString();
                }
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty("ALLOW_MERGE", true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}