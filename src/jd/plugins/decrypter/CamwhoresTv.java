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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "camwhores.tv" }, urls = { "https?://(?:www\\.)?camwhores(tv)?\\.(?:tv|video|biz|sc|io|adult|cc|co|org)/videos/\\d+/[a-z0-9\\-]+/" })
public class CamwhoresTv extends PornEmbedParser {
    public CamwhoresTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.br.setCookiesExclusive(true);
        final String parameter = param.toString().replaceFirst("camwhores.tv/", "camwhores.cc/");
        br.getPage(parameter);
        if (jd.plugins.hoster.CamwhoresTv.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (StringUtils.containsIgnoreCase(br.getRedirectLocation(), "cwcams.com/landing")) {
            return decryptedLinks;
        } else if (StringUtils.containsIgnoreCase(br.getRedirectLocation(), "de.stripchat.com")) {
            return decryptedLinks;
        }
        br.followRedirect();
        final String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (decryptedLinks.size() == 0) {
            /* Probably a selfhosted video. */
            final DownloadLink dl = createDownloadlink(createDownloadUrlForHostPlugin(parameter));
            final String id = new Regex(parameter, "/videos/(\\d+)").getMatch(0);
            dl.setLinkID(getHost() + "://" + id);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    public static String createDownloadUrlForHostPlugin(final String dl) {
        return dl.replaceFirst("camwhores.+?/", "camwhoresdecrypted.tv/");
    }
}
