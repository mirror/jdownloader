//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 *
 * @author raztoki
 * @author psp
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ouo.io" }, urls = { "http://ouo\\.io/(s/)?([A-Za-z0-9]+)(?:\\?s=((?:http|ftp).+))?" }, flags = { 0 })
public class OuoIo extends PluginForDecrypt {

    public OuoIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String parameter = null;
    private String fuid      = null;
    private String s_fuid    = null;
    private String s_value   = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        set(param.toString());
        if (s_value != null && s_fuid != null) {
            decryptedLinks.add(createDownloadlink(Encoding.urlDecode(s_value, false)));
            return decryptedLinks;
        } else if (s_fuid != null && s_value == null) {
            // fid is just a URL owner identifier, the sv is the end url! Without proper sv value you can't get the end URL!
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (!br.containsHTML("class=\"content\"")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String token = br.getRegex("name=\"_token\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        br.postPage("http://ouo.io/go/" + fuid, "_token=" + Encoding.urlEncode(token) + "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
        final String finallink = getFinalLink();
        if (finallink == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    private String getFinalLink() {
        final String finallink = br.getRegex("\"\\s*([^\r\n]+)\\s*\"\\s+id=\"btn-main\"").getMatch(0);
        return finallink;
    }

    private void set(final String downloadLink) {
        parameter = downloadLink;
        fuid = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        s_fuid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        s_value = new Regex(parameter, this.getSupportedLinks()).getMatch(2);
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        set(link.getDownloadURL());
        if (s_value != null && s_fuid != null) {
            return false;
        }
        return true;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

}
