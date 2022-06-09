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
import java.util.List;

import org.appwork.utils.DebugMode;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FotografDe extends PluginForDecrypt {
    public FotografDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fotograf.de" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://[\\w\\-]+\\." + buildHostsPatternPart(domains) + "/login");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            logger.warning("This plugin isn't finished yet.");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* TODO: Add support for pre authed URLs like: https://thormedia.fotograf.de/kunden/subscribe/blabla */
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        /* All galleries are password protected */
        String passCode = null;
        for (int i = 0; i <= 2; i++) {
            final Form passform = getPassForm(br);
            if (passform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            passCode = getUserInput("Password?", param);
            passform.put(Encoding.urlEncode("data[GuestAccess][code][]"), Encoding.urlEncode(passCode));
            br.submitForm(passform);
            if (getPassForm(br) == null) {
                logger.info("Correct password is: " + passCode);
                break;
            } else {
                logger.info("Wrong password: " + passCode);
                continue;
            }
        }
        if (getPassForm(br) != null) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        if (br.containsHTML("(?i)Wir verleihen Ihren Fotos gerade den letzten Schliff")) {
            throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, "GALLERY_IS_NOT_YET_READY_" + passCode, "This gallery is not yet ready. Try again later. Password: " + passCode);
        }
        // String fpName = br.getRegex("").getMatch(0);
        // final String[] links = br.getRegex("").getColumn(0);
        // if (links == null || links.length == 0) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // for (final String singleLink : links) {
        // decryptedLinks.add(createDownloadlink(singleLink));
        // }
        // if (fpName != null) {
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(Encoding.htmlDecode(fpName).trim());
        // fp.addLinks(decryptedLinks);
        // }
        return decryptedLinks;
    }

    private Form getPassForm(final Browser br) {
        return br.getFormbyProperty("id", "GuestAccessGuestLoginForm");
    }
}
