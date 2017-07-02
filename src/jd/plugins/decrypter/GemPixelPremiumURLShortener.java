//    jDownloader - Downloadmanager
//    Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.HashSet;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

// DEV NOTES: no error handling should work for any language default. no results = no link! clean and efficient

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uploadid.net", "shortenurl.pw", "888.xirkle.com", "gempixel.com", "susutin.com" }, urls = { "https?://(?:www\\.)?uploadid\\.net/[a-zA-Z0-9]+", "https?://(?:www\\.)?shortenurl\\.pw/[a-zA-Z0-9]+", "https?://(?:www\\.)?888\\.xirkle\\.com/[a-zA-Z0-9]+", "https?://(?:www\\.)?gempixel\\.com/short/[a-zA-Z0-9]+", "https?://(?:www\\.)?susutin\\.com/[a-zA-Z0-9]+" })
public class GemPixelPremiumURLShortener extends antiDDoSForDecrypt {

    public GemPixelPremiumURLShortener(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        getPage(parameter);
        // can be password protected
        Form password = getPassword();
        if (password != null) {
            final int repeat = 3;
            for (int i = 0; i <= repeat; i++) {
                if (password == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Could not find password form");
                }
                final String pass = getUserInput("Password protected link", param);
                if (inValidate(pass)) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                password.put("password", Encoding.urlEncode(pass));
                submitForm(password);
                password = getPassword();
                if (password != null) {
                    if (i + 1 >= repeat) {
                        logger.warning("Incorrect solve of password");
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                    continue;
                } else {
                    break;
                }
            }
        }
        // link can also be found within javascript window.location
        final String link = br.getRegex("window\\.location\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
        if (!inValidate(link)) {
            decryptedLinks.add(createDownloadlink(link));
            return decryptedLinks;
        }
        // link can be one or they can be a may response!
        String[] hrefs = br.getRegex("<a [^>]*>.*?<\\s*/a\\s*>").getColumn(-1);
        if (hrefs != null) {
            for (final String href : hrefs) {
                if (new Regex(href, "btn btn-primary btn-block redirect|\"btn-primary\"").matches()) {
                    final String b64 = new Regex(href, "/\\?r=([a-zA-Z0-9_/\\+\\=\\-%]+)").getMatch(0);
                    final HashSet<String> results = jd.plugins.decrypter.GenericBase64Decrypter.handleBase64Decode(b64);
                    if (results != null) {
                        for (final String result : results) {
                            decryptedLinks.add(createDownloadlink(result));
                        }
                        return decryptedLinks;
                    } else {
                        final String lnk = new Regex(href, "href\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                        if (lnk != null) {
                            decryptedLinks.add(createDownloadlink(lnk));
                        }
                        return decryptedLinks;
                    }
                }
            }
        }
        return decryptedLinks;
    }

    private final Form getPassword() {
        for (final Form password : br.getForms()) {
            // verify that password form pre decrypter task and not login.
            if (password.hasInputFieldByName("password") && password.hasInputFieldByName("token")) {
                return password;
            }
        }
        return null;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.GemPixel_PremiumURLShortener;
    }

    @Override
    public String siteSupportedPath() {
        if ("gempixel.com".equalsIgnoreCase(getHost())) {
            return "/host";
        }
        return super.siteSupportedPath();
    }

}