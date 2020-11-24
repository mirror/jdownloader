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

import java.net.URL;
import java.util.ArrayList;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "catcut.net" }, urls = { "https?://(?:www\\.)?catcut\\.net/(?:s/)?[A-Za-z0-9]+" })
public class CatcutNet extends PluginForDecrypt {
    public CatcutNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String redirect = br.getRedirectLocation();
        if (redirect != null && !new Regex(redirect, this.getSupportedLinks()).matches()) {
            /* 2020-11-24: Direct redirect (mostly to advertising websites) */
            decryptedLinks.add(this.createDownloadlink(redirect));
            return decryptedLinks;
        } else if (redirect != null) {
            br.setFollowRedirects(true);
            br.followRedirect();
        }
        String finallink = br.getRegex("<span\\s*id\\s*=\\s*\"noCaptchaBlock\"[^<>]+>\\s*?<a href\\s*=\\s*\"(http[^<>\"]+)\"").getMatch(0);
        if (finallink == null) {
            // now within base64 element
            String go_url = br.getRegex("var\\s*go_url\\s*=\\s*decodeURIComponent\\('(.*?)'\\)").getMatch(0);
            if (go_url != null) {
                // under the a value
                go_url = Encoding.urlDecode(go_url, true);
                final String a = new Regex(go_url, "a=([a-zA-Z0-9_/\\+\\=\\-%]+)&?").getMatch(0);
                if (a != null && Encoding.isHtmlEntityCoded(go_url)) {
                    go_url = Encoding.Base64Decode(a);
                }
                if (go_url.matches("https?://[^/]+/.*away3\\.php.*")) {
                    /* 2020-09-07: New way */
                    go_url += "&q=&r=&p=0&t=1&s=1&u14=&v14=&w7=";
                    final long timeBefore = System.currentTimeMillis();
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    go_url += "&x=" + Encoding.urlEncode(recaptchaV2Response);
                    final long timePassed = System.currentTimeMillis() - timeBefore;
                    final long waittime = 15000 - timePassed;
                    if (waittime > 0) {
                        logger.info("Waiting: " + waittime);
                        this.sleep(waittime, param);
                    } else {
                        logger.info("Skipping waittime as captcha solving took so much time");
                    }
                    br.getPage(go_url);
                    finallink = new URL(Encoding.htmlDecode(br.toString())).toString();
                } else {
                    /* Old way (without captcha & waittime) */
                    finallink = go_url;
                }
            }
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
