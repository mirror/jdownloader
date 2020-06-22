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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ddownload.com" }, urls = { "https?://(?:www\\.)?ddownload\\.com/d/([A-Za-z0-9]+)" })
public class DdownloadComCrawler extends antiDDoSForDecrypt {
    public DdownloadComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid_short = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(fid_short)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Check for direct redirect */
        if (br.getURL().matches("https?://[^/]+/[a-z0-9]{12}")) {
            decryptedLinks.add(createDownloadlink(this.br.getURL()));
            return decryptedLinks;
        }
        String fid = null;
        final Form form = br.getFormbyProperty("name", "F1");
        if (form != null) {
            fid = form.getInputFieldByName("id").getValue();
        }
        if (fid == null || !fid.matches("[a-z0-9]{12}")) {
            return null;
        }
        decryptedLinks.add(createDownloadlink("https?://" + this.getHost() + "/" + fid));
        return decryptedLinks;
    }
}
