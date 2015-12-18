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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ok.ru" }, urls = { "http://(www\\.|m\\.)?ok\\.ru/(?:video|videoembed)/\\d+" }, flags = { 0 })
public class OkRuDecrypter extends PluginForDecrypt {

    public OkRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceAll("http://(m|www)\\.", "http://www.").replace("/videoembed/", "/video/");
        final String vid = new Regex(parameter, "(\\d+)$").getMatch(0);
        /* Load plugin */
        JDUtilities.getPluginForHost("ok.ru");
        jd.plugins.hoster.OkRu.prepBR(this.br);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        String externID = this.br.getRegex("data\\-ytid=\"([^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("https://www.youtube.com/watch?v=" + externID));
            return decryptedLinks;
        }
        final DownloadLink main = this.createDownloadlink("http://okdecrypted" + System.currentTimeMillis() + new Random().nextInt(1000000000));
        main.setLinkID(vid);
        main.setContentUrl(parameter);
        main.setName(vid);
        if (jd.plugins.hoster.OkRu.isOffline(this.br)) {
            main.setAvailable(false);
        }
        main.setProperty("mainlink", parameter);
        decryptedLinks.add(main);

        return decryptedLinks;
    }
}
