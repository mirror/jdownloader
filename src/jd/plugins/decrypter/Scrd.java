//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "secured.in" }, urls = { "http://[\\w\\.]*?secured\\.in/download-[\\d]+-[\\w]+\\.html" }, flags = { 0 })
public class Scrd extends PluginForDecrypt {

    static private final String MAINPAGE = "http://secured.in";

    private static boolean isExternInterfaceActive() {
        final OptionalPluginWrapper plg = JDUtilities.getOptionalPlugin("externinterface");
        return plg != null && plg.isLoaded() && plg.isEnabled();
    }

    public Scrd(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        /*
         * !!! Old content in Revision 11602 !!!
         */
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // Captcha
        br.getPage(parameter);
        if (br.getRegex("Sicherheitscode").matches()) {
            logger.fine("The current page is captcha protected, getting captcha ID...");
            for (int i = 0; i <= 3; i++) {
                final String recaptchaID = br.getRegex("k=([a-zA-Z0-9]+)\"").getMatch(0);
                final Form captchaForm = br.getForm(0);
                if (recaptchaID == null || captchaForm == null) { return null; }
                logger.fine("The current recaptcha ID is '" + recaptchaID + "'");
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId(recaptchaID);
                rc.setForm(captchaForm);
                rc.load();
                final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                final String c = this.getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.containsHTML("Sicherheitscode")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("Sicherheitscode")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        }
        // CNL2
        if (br.containsHTML("cnl\\.jpg") && isExternInterfaceActive()) {
            br.submitForm(br.getForm(0));
            if (br.toString().trim().equals("success")) { return decryptedLinks; }
        }
        // Containerhandling
        if (br.containsHTML("dlc\\.jpg")) {
            File container = null;
            final String dlcLink = br.getRegex("container-download-dlc.*?href=\"(.*?)\" class=\"container-format-dlc\"").getMatch(0);
            if (dlcLink != null) {
                container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc", true);
                if (!container.exists()) {
                    container.createNewFile();
                }
                br.cloneBrowser().getDownload(container, MAINPAGE + dlcLink);
            }
            if (container != null) {
                logger.info("Container found: " + container);
                decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
                container.delete();
                if (decryptedLinks.size() > 0) { return decryptedLinks; }
            }
        }
        // Filehandling
        final String[] cLinks = br.getRegex("href=\"(/links/.*?)\"").getColumn(0);
        if (cLinks == null || cLinks.length == 0) { return null; }
        for (final String bla : cLinks) {
            br.getPage(MAINPAGE + bla);
            if (!br.containsHTML("Base64")) {
                continue;
            }
            final String dllink = Encoding.Base64Decode(br.getRegex("Base64\\.decode\\(\"(.*?)\\\\n").getMatch(0));
            if (dllink == null) {
                continue;
            }
            decryptedLinks.add(createDownloadlink(dllink));
        }

        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }
}