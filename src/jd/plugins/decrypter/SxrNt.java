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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SxrNt extends PluginForDecrypt {
    public SxrNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sexuria.net", "sexuria.org" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(\\d+)-([a-z0-9\\-]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    /** This may be the future replacement of {@link #Sxrcm} jd.plugins.decrypter.Sxrcm */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_name = new Regex(contenturl, this.getSupportedLinks()).getMatch(1);
        final String fpName = url_name.replace("-", " ").trim();
        String[] links = br.getRegex("target=\"_blank\" href=\"(https?://[^\"]+)\" class=\"btn vertab\"").getColumn(0);
        if (links.length == 0) {
            /* 2020-12-15 */
            links = br.getRegex("target=\"_blank\" href=\"(http[^\"]+)\"[^>]*>\\d+").getColumn(0);
        }
        if (links.length == 0) {
            /* Fallback */
            links = HTMLParser.getHttpLinks(br.toString(), br.getURL());
        }
        String extractionPassword = br.getRegex("<strong>\\s*Password file\\s*</strong></td>\\s*<td>([^<>\"]+)</td>").getMatch(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        ArrayList<String> extractionPassword_s = null;
        if (!StringUtils.isEmpty(extractionPassword)) {
            extractionPassword = extractionPassword.trim();
            param.setDecrypterPassword(extractionPassword);
            // extractionPassword_s = new ArrayList<String>(PasswordUtils.getPasswords(extractionPassword));
            extractionPassword_s = new ArrayList<String>();
            extractionPassword_s.add(extractionPassword);
        }
        for (final String singleLink : links) {
            /* Ship items which would be handled by this crawler again! */
            if (this.canHandle(singleLink)) {
                continue;
            }
            final DownloadLink dl = createDownloadlink(singleLink);
            if (extractionPassword_s != null) {
                dl.setSourcePluginPasswordList(extractionPassword_s);
            }
            ret.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (fpName != null) {
            fp.setName(Encoding.htmlDecode(fpName).trim());
        } else {
            /* Fallback */
            fp.setName(br._getURL().getPath());
        }
        fp.addLinks(ret);
        return ret;
    }
}
