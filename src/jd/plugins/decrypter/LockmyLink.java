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

import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "lockmy.link" }, urls = { "https?://(?:www\\.)?lockmy\\.link/l/([A-Za-z0-9]+)/?" })
public class LockmyLink extends PluginForDecrypt {
    public LockmyLink(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String shortID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.getHeaders().put("Origin", "https://" + this.getHost());
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        br.getHeaders().put("sec-ch-ua", "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"");
        br.getHeaders().put("sec-ch-ua-mobile", "?0");
        br.getHeaders().put("sec-ch-ua-platform", "\"Windows\"");
        /* Important! Without this header, this is all we'll get: "<p>ERROR! Please reload the page</p>" */
        br.getHeaders().put("sec-fetch-site", "same-origin");
        br.getHeaders().put("sec-fetch-mode", "cors");
        br.getHeaders().put("sec-fetch-dest", "empty");
        // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)
        // Chrome/117.0.0.0 Safari/537.36");
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* E.g. redirect to "/404" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String contenturl = br.getURL();
        /* Wait-time: Check every second (same as in browser) */
        final Browser brc = this.br.cloneBrowser();
        int rounds = -1;
        // final String paramurlEncoded = Encoding.urlEncode("[\"" + param.getCryptedUrl() + "\"]");
        final String paramraw = "[\"" + param.getCryptedUrl() + "\"]";
        while (true) {
            rounds++;
            // final String response = brc.postPage("/api/ajax.php", "url=" + paramurlEncoded);
            /* Important! */
            brc.getHeaders().put("Referer", contenturl);
            final String response = brc.postPageRaw("/api/ajax.php", "url=" + paramraw);
            if (!response.matches("^\\d+$")) {
                /* 2023-03-30: wait time required & checked */
                break;
            }
            if (isAbort()) {
                logger.info("Stopping because: Aborted by user");
                return ret;
            } else if (rounds >= 600) {
                logger.info("Stopping because: Waited too long");
                break;
            } else {
                logger.info("Waiting 1000ms | Remaining seconds to wait: " + brc.getRequest().getHtmlCode());
                sleep(1001, param);
                continue;
            }
        }
        /* "Workaround" for json response */
        brc.getRequest().setHtmlCode(PluginJSonUtils.unescape(brc.getRequest().getHtmlCode()));
        String[] results = regexDownloadurls(brc);
        if (results == null || results.length == 0) {
            logger.info("Captcha required");
            final String captchaImageBase64 = brc.getRegex("data:image/png;base64,([a-zA-Z0-9_/\\+\\=]+)").getMatch(0);
            if (captchaImageBase64 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            byte[] image_bytes = org.appwork.utils.encoding.Base64.decode(captchaImageBase64);
            if (image_bytes == null || image_bytes.length == 0) {
                image_bytes = org.appwork.utils.encoding.Base64.decodeFast(captchaImageBase64);
            }
            final File captchaImage = getLocalCaptchaFile(".png");
            IO.writeToFile(captchaImage, image_bytes);
            String captchaDescr = brc.getRegex("<p class=\"title\">([^<]*)</p>").getMatch(0);
            if (captchaDescr == null) {
                /* 2022-02-16 */
                captchaDescr = br.getRegex("<p>([^<>\"]+)</p></div></div>").getMatch(0);
            }
            if (captchaDescr == null) {
                /* Fallback */
                captchaDescr = "Click on the lock symbol";
            }
            final ClickedPoint cp = getCaptchaClickedPoint(captchaImage, param, captchaDescr);
            br.postPage("/api/ajax.php", "shortId=" + shortID + "&coords=" + cp.getX() + ".5-" + cp.getY());
            /* "Workaround" for json response */
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.getRequest().getHtmlCode()));
            if (br.containsHTML("(?i)class=\"title\">\\s*ERROR")) {
                /*
                 * 2021-10-20: html may also contain: "<p>Link not found</p>" --> This is wrong! This response will only happen if the
                 * captcha-answer is wrong!
                 */
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            results = regexDownloadurls(br);
        }
        if (results == null || results.length == 0) {
            if (br.containsHTML("not_found")) {
                /*
                 * This may also happen if user fails to solve the captcha but we can't just retry it because in order to do that we would
                 * need to wait another ~120 seconds.
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        for (final String result : results) {
            final DownloadLink link = createDownloadlink(result);
            /* Put all results into one package. */
            link._setFilePackage(fp);
            ret.add(link);
        }
        return ret;
    }

    private String[] regexDownloadurls(final Browser br) {
        return br.getRegex("target=\"_blank\" href=\"(https?[^\"]+)").getColumn(0);
    }
}
