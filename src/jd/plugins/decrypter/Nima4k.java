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
import java.util.Arrays;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nima4k.org" }, urls = { "https?://(www\\.)?nima4k\\.org/(release|go)/[0-9]+/.+" })
public class Nima4k extends PluginForDecrypt {
    public Nima4k(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        String page = br.getPage(parameter);
        String fpName = br.getRegex("<title>NIMA4K - ([^<]+)</title>").getMatch(0);
        String password = br.getRegex("<strong>Passwort:</strong>(?: )?([^<]+)<").getMatch(0);
        String[][] links = br.getRegex("<a[^>]+href=\"([^\"]+)\"[^>]+class=\"[^\"]*btn btn-orange dl-button[^\"]*\"").getMatches();
        for (String[] link : links) {
            String singleURL = Encoding.htmlDecode(link[0]);
            if (StringUtils.containsIgnoreCase(singleURL, "/go/")) {
                final Browser brLink = br.cloneBrowser();
                brLink.setFollowRedirects(true);
                brLink.getPage(singleURL);
                singleURL = brLink.getURL();
            }
            DownloadLink dl = createDownloadlink(singleURL);
            if (password != null && password.length() > 0) {
                dl.setSourcePluginPasswordList(new ArrayList<String>(Arrays.asList(password.trim())));
            }
            decryptedLinks.add(dl);
        }
        // Fallback, in case they introduce another redirect view layer.
        if (!parameter.equals(br.getURL()) && StringUtils.containsIgnoreCase(parameter, "/go/")) {
            decryptedLinks.add(createDownloadlink(br.getURL()));
        }
        final FilePackage fp = FilePackage.getInstance();
        if (fpName != null) {
            fp.setName(Encoding.htmlDecode(fpName.trim()));
        }
        if (password != null && password.length() > 0) {
            fp.setProperty("PWLIST", new ArrayList<String>(Arrays.asList(password)));
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}