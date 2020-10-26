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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pruteklyncs.xyz" }, urls = { "https?://(?:www\\.)?pruteklyncs\\.xyz/[A-Za-z0-9]+" })
public class PruteklyncsXyz extends PluginForDecrypt {
    public PruteklyncsXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String iframeURL = br.getRegex("<iframe src=\"(https?://[^\"]+)\">").getMatch(0);
        if (iframeURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(iframeURL);
        if (br.containsHTML("passster-captcha-js")) {
            /* 2020-10-26: Cheap clientside captcha */
            final String nonce = PluginJSonUtils.getJson(br, "nonce");
            final String post_id = PluginJSonUtils.getJson(br, "post_id");
            if (StringUtils.isEmpty(nonce) || StringUtils.isEmpty(post_id)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.postPage("/wp-admin/admin-ajax.php", "action=validate_input&nonce=" + nonce + "&captcha=success&post_id=" + post_id + "&type=captcha&protection=full&elementor_content=");
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
        }
        final String[] links = br.getRegex("href=\"(https?://[^\"]+)\" target=\"_blank\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        return decryptedLinks;
    }
}
