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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lesen.to" }, urls = { "http://(www\\.)?lesen\\.to/(protection/folder_\\d+\\.html|wp/tipp/Download/\\d+/)" }, flags = { 0 })
public class LsnTo extends PluginForDecrypt {

    private static final String RECAPTCHA = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";

    public LsnTo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().toLowerCase();
        br.getPage(parameter);

        if (parameter.matches("http://(www\\.)?lesen\\.to/wp/tipp/download/\\d+/")) {
            final String redirect = br.getRedirectLocation();
            if (redirect == null) {
                logger.info("Cannot decrypt link: " + parameter);
                logger.info("Unsupported link: " + redirect);
                return decryptedLinks;
            }
            if (!redirect.contains("lesen.to/")) {
                decryptedLinks.add(createDownloadlink(redirect));
                return decryptedLinks;
            }
            String newLink = new Regex(redirect, "(http://(www\\.)?lesen\\.to/protection/folder_\\d+\\.html)").getMatch(0);
            if (newLink == null) {
                if (redirect.matches("http://(www\\.)?lesen\\.to/(download|firstload)")) {
                    logger.info("Cannot decrypt link: " + parameter);
                    logger.info("Link offline: " + redirect);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(newLink);
        }

        boolean failed = true;
        if (br.containsHTML(RECAPTCHA)) {
            for (int i = 0; i <= 5; i++) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.containsHTML(RECAPTCHA)) {
                    br.getPage(parameter);
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        }
        if (br.containsHTML("Anfrage abgefangen")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
        final Form form = br.getForm(1);
        if (form != null) {
            br.submitForm(form);
        }
        String[] links = br.getRegex("class=\"container\"><a href=\"(http://.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("\"(http://(www\\.)?lesen\\.to/protection/.*?)\"").getColumn(0);
        }
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (singleLink.matches("http://(www\\.)?lesen\\.to/protection/.*?")) {
                br.getPage(singleLink);
                final String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            } else {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}