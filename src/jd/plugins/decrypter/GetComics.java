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
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision: 39909 $", interfaceVersion = 3, names = { "getcomics.info" }, urls = { "https?://getcomics\\.info/(?!share/|page/)[A-Za-z0-9_\\-]+/[A-Za-z0-9_\\-]+" })
public class GetComics extends PluginForDecrypt {
    public GetComics(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // Load page
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String title = br.getRegex("<title>(.+?) &ndash; GetComics").getMatch(0);
        final String[] links = br.getRegex("<div class=\"aio-[^\"]+\"><a[^>]+href=\"([^\"]+)\"[^>]+class=\"aio-[^\"]+\" title=\"[^\"]+Link\"").getColumn(0);
        if (links != null && links.length > 0) {
            for (String link : links) {
                if (StringUtils.containsIgnoreCase(link, "run.php-urls")) {
                    // checks for correct referer!
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(false);
                    brc.getPage(link);
                    String redirect = brc.getRedirectLocation();
                    if (redirect == null) {
                        sleep(1000, param);
                        brc.getPage(parameter);
                        brc.getPage(link);
                        redirect = brc.getRedirectLocation();
                    }
                    if (redirect != null) {
                        decryptedLinks.add(createDownloadlink(redirect));
                    }
                } else {
                    decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
                }
            }
        }
        if (title != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(Encoding.htmlDecode(title));
            filePackage.addLinks(decryptedLinks);
        }
        //
        return decryptedLinks;
    }
}