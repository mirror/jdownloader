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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.utils.recaptcha.api2.Recaptcha2Helper;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ouo.io" }, urls = { "http://ouo\\.io/[A-Za-z0-9]+" }, flags = { 0 })
public class OuoIo extends PluginForDecrypt {

    public OuoIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = parameter.substring(parameter.lastIndexOf("/") + 1);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        final String token = br.getRegex("name=\"_token\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String sitekey = br.getRegex("sitekey: \"([^<>\"]*?)\"").getMatch(0);
        if (token == null || sitekey == null) {
            return null;
        }
        boolean success = false;
        String responseToken = null;
        for (int i = 0; i <= 5; i++) {
            Recaptcha2Helper rchelp = new Recaptcha2Helper();
            rchelp.init(this.br, sitekey, this.getHost());
            final File outputFile = rchelp.loadImageFile();
            final String code = getCaptchaCode("recaptcha", outputFile, param);
            success = rchelp.sendResponse(code);
            if (!success) {
                continue;
            }
            responseToken = rchelp.getResponseToken();
        }
        br.postPage("http://ouo.io/go/" + fid, "_token=" + Encoding.urlEncode(token) + "&g-recaptcha-response=" + Encoding.urlEncode(responseToken));
        final String finallink = br.getRegex("\"(http://[^<>\"]*?)\" id=\"btn-main\"").getMatch(0);
        if (finallink == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
