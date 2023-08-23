//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class JwpasteCom extends AbstractPastebinCrawler {
    public JwpasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    String getFID(final String url) {
        return new Regex(url, this.getSupportedLinks()).getMatch(0);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "jwpaste.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/v/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public PastebinMetadata crawlMetadata(final CryptedLink param, final Browser br) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("class=\"paste\\-error\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Form pwform = this.getPasswordProtectedForm(br);
        if (pwform != null) {
            final String initialURL = br.getURL();
            logger.info("Paste is password protected");
            int tries = 0;
            boolean success = false;
            do {
                final String passCode = getUserInput("Password?", param);
                pwform.put("password", Encoding.urlEncode(passCode));
                pwform.put("botonPastePass", "Senden");
                br.submitForm(pwform);
                if (br.containsHTML("(?i)incorrecta, retrocede y reintenta nuevamente|Incorrect password rewind and retry again")) {
                    logger.info("User entered incorrect password: " + passCode);
                    /* We need to access main URL again so we can get that password form again. */
                    br.getPage(initialURL);
                    pwform = this.getPasswordProtectedForm(br);
                    if (pwform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    tries++;
                    continue;
                } else {
                    logger.info("User entered correct password: " + passCode);
                    param.setDecrypterPassword(passCode);
                    success = true;
                    break;
                }
            } while (tries <= 2 && !success);
            if (!success) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        final String plaintxt = br.getRegex("<td[^>]*nowrap align=\"left\"[^>]*><pre(.*?)</table>").getMatch(0);
        if (plaintxt == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PastebinMetadata metadata = new PastebinMetadata(param, this.getFID(param.getCryptedUrl()));
        metadata.setPastebinText(plaintxt);
        return metadata;
    }

    private Form getPasswordProtectedForm(final Browser br) {
        return br.getFormbyKey("password");
    }
}