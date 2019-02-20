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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filecheck.link", "container.cool", "redirect.codes" }, urls = { "https?://(?:www\\.)?filecheck\\.link/d/[A-Za-z0-9]+", "https?://(?:www\\.)?container\\.cool/d/[A-Za-z0-9]+", "https?://(?:www\\.)?redirect\\.codes/d/[A-Za-z0-9]+" })
public class FilecheckLink extends antiDDoSForDecrypt {
    public FilecheckLink(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<h1>File Not Found</h1><br>")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Website of [XFS] filehost */
        final String website = br.getRegex("name=\"F1\" action=\"(https?://[^/]+/)").getMatch(0);
        final String fid = br.getRegex("\"id\" value=\"([^\"]+)\"").getMatch(0);
        if (website == null || fid == null) {
            return null;
        }
        final String finallink = website + fid;
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
