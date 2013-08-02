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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abmp3.com" }, urls = { "http://(www\\.)?abmp3\\.(com|info)/download/\\d+\\-.*?\\.html" }, flags = { 0 })
public class AbEmPeThreeCom extends PluginForDecrypt {

    public AbEmPeThreeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static Object        LOCK          = new Object();
    private static final String CAPTCHAREGEX  = "id=\"image_c\" src=\"(/.*?)\"";
    private static final String CAPTCHAREGEX2 = "\"(/code\\.php\\?par=\\d+)\"";
    private static final String DONE          = "Done#\\|#";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("abmp3.com/", "abmp3.info/");
        br.getPage(parameter);
        // Invalid link
        if (br.containsHTML("(<TITLE>404 Not Found</TITLE>|<H1>Not Found</H1>)")) {
            logger.info("Link offline (error 404) : " + parameter);
            return decryptedLinks;
        }
        // Link offline
        if (br.containsHTML(">This file has been deleted") || br.containsHTML("This file has been removed =\\(")) {
            logger.info("Link deleted: " + parameter);
            return decryptedLinks;
        }
        String captchaUrl = null;
        boolean failed = true;
        String fileID = new Regex(parameter, "abmp3\\.info/download/(\\d+)-").getMatch(0);
        for (int i = 0; i <= 5; i++) {
            captchaUrl = br.getRegex(CAPTCHAREGEX).getMatch(0);
            if (captchaUrl == null) captchaUrl = br.getRegex(CAPTCHAREGEX2).getMatch(0);
            if (captchaUrl == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            captchaUrl = "http://abmp3.info" + captchaUrl;
            final File dlledCaptcha = downloadCaptcha(getLocalCaptchaFile(), captchaUrl);
            String code = getCaptchaCode(dlledCaptcha, param);

            br.getPage("http://www.abmp3.info/chk_cd.php?id=" + fileID + "&code=" + code);
            if (!br.containsHTML(DONE)) {
                br.clearCookies("http://abmp3.info");
                br.getPage(parameter);
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        final String finallink = br.getRegex("Done#\\|#(http://.+)").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink("directhttp://" + finallink.trim()));

        return decryptedLinks;
    }

    public File downloadCaptcha(final File captchaFile, final String captchaAdress) throws IOException, PluginException {
        final Browser rcBr = br.cloneBrowser();
        rcBr.setFollowRedirects(true);
        try {
            Browser.download(captchaFile, rcBr.openGetConnection(captchaAdress));
        } catch (IOException e) {
            captchaFile.delete();
            throw e;
        }
        return captchaFile;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}