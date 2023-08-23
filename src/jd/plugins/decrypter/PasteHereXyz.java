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
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PasteHereXyz extends AbstractPastebinCrawler {
    public PasteHereXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    String getFID(final String url) {
        return new Regex(url, this.getSupportedLinks()).getMatch(0);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pastehere.xyz" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-zA-Z0-9]+)/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public PastebinMetadata crawlMetadata(final CryptedLink param, final Browser br) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("<strong>Alert!</strong>\\s*Paste not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (getPwProtectedForm(br) != null) {
            final String initialURL = br.getURL();
            int counter = 0;
            boolean success = false;
            do {
                final Form pwprotected = getPwProtectedForm(br);
                String passCode = null;
                if (param.getDownloadLink() != null) {
                    passCode = param.getDownloadLink().getDownloadPassword();
                }
                if (passCode == null || counter > 0) {
                    passCode = this.getUserInput("Enter password", param);
                }
                pwprotected.put("mypass", Encoding.urlEncode(passCode));
                this.br.submitForm(pwprotected);
                if (br.containsHTML(">\\s*Password is Wrong")) {
                    logger.info("User entered invalid password: " + passCode);
                    /* Reload page */
                    br.getPage(initialURL);
                    counter++;
                    continue;
                } else {
                    logger.info("User entered valid password: " + passCode);
                    param.setDecrypterPassword(passCode);
                    success = true;
                    break;
                }
            } while (!this.isAbort() && counter <= 2);
            if (!success) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        final String plaintxt = br.getRegex("<div[^>]+id=\"p_data\"[^>]*>(.*?)\\s*</div>\\s*</div>").getMatch(0);
        if (plaintxt == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PastebinMetadata metadata = new PastebinMetadata(param, this.getFID(param.getCryptedUrl()));
        metadata.setPastebinText(plaintxt);
        final String title = br.getRegex("class=\"pastitle\"[^>]*>([^<]+)<").getMatch(0);
        if (title != null) {
            metadata.setTitle(Encoding.htmlDecode(title).trim());
        } else {
            logger.warning("Unable to find paste title");
        }
        return metadata;
    }

    private Form getPwProtectedForm(final Browser br) {
        return br.getFormbyKey("mypass");
    }

    @Override
    protected ArrayList<DownloadLink> crawlAdditionalURLs(final Browser br, final CryptedLink param, final PastebinMetadata metadata) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String pasteDirecturl = br.getRegex("href=\"(/paste\\.php\\?download[^\"]+)\"").getMatch(0);
        if (pasteDirecturl != null) {
            final DownloadLink direct = this.createDownloadlink("directhttp://" + br.getURL(pasteDirecturl).toString());
            if (metadata.getPassword() != null) {
                direct.setPasswordProtected(true);
                direct.setDownloadPassword(metadata.getPassword());
            }
            ret.add(direct);
        }
        return ret;
    }
}