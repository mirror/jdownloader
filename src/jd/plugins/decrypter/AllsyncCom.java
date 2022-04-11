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
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AllsyncCom extends PluginForDecrypt {
    public AllsyncCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "allsync.com" });
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/s/[A-Za-z0-9]+(\\?path=[^/]+)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String PROPERTY_AUTH_COOKIE_1 = "auth_cookie_1";
    private static final String PROPERTY_AUTH_COOKIE_2 = "auth_cookie_2";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String passCode = param.getDecrypterPassword();
        String authCookie1 = null;
        String authCookie2 = null;
        if (br.getURL().contains("/authenticate/")) {
            /* Password protected folder */
            for (int i = 0; i <= 3; i++) {
                final Form pwform = getPasswordForm(br);
                if (pwform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (passCode == null) {
                    passCode = getUserInput("Password?", param);
                }
                pwform.put("password", Encoding.urlEncode(passCode));
                br.submitForm(pwform);
                if (getPasswordForm(br) == null) {
                    logger.info("Correct password entered: " + passCode);
                    break;
                } else {
                    logger.info("Wrong password entered: " + passCode);
                    passCode = null;
                }
            }
            if (getPasswordForm(br) != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            authCookie1 = br.getCookie(br.getHost(), "oc_sessionPassphrase", Cookies.NOTDELETEDPATTERN);
            authCookie2 = br.getCookie(br.getHost(), "ocgesslygpy1", Cookies.NOTDELETEDPATTERN);
            if (authCookie1 == null || authCookie2 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* TODO: Add generic WebDAV crawler which can then handle the rest. */
        return decryptedLinks;
    }

    private Form getPasswordForm(final Browser br) {
        return br.getFormbyKey("password");
    }
}
