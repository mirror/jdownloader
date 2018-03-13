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
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ref.so" }, urls = { "http://(www\\.)?ref\\.so/[a-z0-9]+" })
public class RefSo extends PluginForDecrypt {
    public RefSo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static Object ctrlLock = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        synchronized (ctrlLock) {
            br.getPage(parameter);
            if (br.containsHTML(">Wrong<|Url not Found<") || br.getHttpConnection().getResponseCode() == 404) {
                logger.info("Link offline: " + parameter);
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            final String fid = parameter.substring(parameter.lastIndexOf("/") + 1);
            String captchaurl = br.getRegex("\"(/verifyimg/get[^<>\"/]+)\"").getMatch(0);
            if (captchaurl != null) {
                for (int i = 0; i <= 3; i++) {
                    final File cf = downloadCaptcha(this.getLocalCaptchaFile(), captchaurl);
                    final String caphash = JDHash.getMD5(cf);
                    if (caphash.equals("517b6c33551ee6f755bbe486225ab9a6")) {
                        logger.info("Blank captcha - try again later!");
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    final String code = getCaptchaCode(cf, param);
                    if (code == null || code.equals("")) {
                        /* Try to avoid blank captchas */
                        this.sleep(3000, param);
                        continue;
                    }
                    br.postPage(br.getURL(), "dr=&module=short&action=showShort&fileId=" + fid + "&vcode=" + Encoding.urlEncode(code));
                    captchaurl = br.getRegex("\"(/verifyimg/get[^<>\"/]+)\"").getMatch(0);
                    if (captchaurl != null) {
                        /* Try to avoid blank captchas */
                        this.sleep(3000, param);
                        continue;
                    }
                    break;
                }
                if (captchaurl != null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
        }
        String link = br.getRegex("class=\"img_btn hide fleft\">[\t\n\r ]+<a href=\"(http[^<>\"]*?)\"").getMatch(0);
        if (link == null && !br.getURL().contains("ref.so/")) {
            link = br.getURL();
        }
        if (link == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(link));
        return decryptedLinks;
    }

    private File downloadCaptcha(final File captchaFile, final String captchaAddress) throws IOException, PluginException {
        this.br.setFollowRedirects(true);
        try {
            Browser.download(captchaFile, this.br.openGetConnection(captchaAddress));
        } catch (IOException e) {
            captchaFile.delete();
            throw e;
        }
        return captchaFile;
    }

    /* More can lead to blank captchas */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
