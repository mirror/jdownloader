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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "newgrounds.com" }, urls = { "http://(?!www\\.)[^/]+\\.newgrounds\\.com/art/" }) 
public class NewgroundsComDecrypter extends PluginForDecrypt {

    public NewgroundsComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String ARTLINK = "http://(?:www\\.)?newgrounds\\.com/art/view/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String[] art_urls = this.br.getRegex("\"(https?://(?:www\\.)?newgrounds\\.com/art/view/[^<>\"]*?)\"").getColumn(0);
        if (art_urls == null || art_urls.length == 0) {
            return null;
        }
        for (final String art_url : art_urls) {
            final DownloadLink dl = createDownloadlink(art_url);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

}
