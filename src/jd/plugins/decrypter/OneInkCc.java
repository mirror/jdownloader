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
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "1ink.cc" }, urls = { "https?://(?:www\\.)?1ink\\.(?:cc|live)/[A-Za-z0-9]+" })
public class OneInkCc extends PluginForDecrypt {
    public OneInkCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getRedirectLocation() != null) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String[] keys = new String[] { "token", "uri", "key", "pub", "r", "pubkey", "codec", "api" };
        final Form passForm = new Form();
        passForm.setMethod(MethodType.POST);
        passForm.setAction("/api/pass.php");
        int foundValueNum = 0;
        for (final String key : keys) {
            final String valueOfKey = br.getRegex(key + "=([a-z0-9]+)").getMatch(0);
            if (valueOfKey != null) {
                passForm.put(key, valueOfKey);
                foundValueNum++;
            }
        }
        if (foundValueNum < 3) {
            return null;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.submitForm(passForm);
        final String finallink = br.toString();
        if (finallink == null || !finallink.startsWith("http")) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
