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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "allofbach.com" }, urls = { "https?://(?:www\\.)?allofbach\\.com/[A-Za-z]{2}/.+" }, flags = { 0 })
public class AllofbachCom extends PluginForDecrypt {

    public AllofbachCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String[] vimeo_ids = br.getRegex("data\\-(?:vimeo|video)\\-id=\"(\\d+)\"").getColumn(0);
        if (vimeo_ids != null) {
            for (final String vimeo_id : vimeo_ids) {
                final String vimeo_url = jd.plugins.decrypter.VimeoComDecrypter.createPrivateVideoUrlWithReferer(vimeo_id, this.br.getURL());
                final DownloadLink dl = createDownloadlink(vimeo_url);
                decryptedLinks.add(dl);
            }
        }

        return decryptedLinks;
    }

}
