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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class EbibliotecaOrg extends PluginForDecrypt {
    public EbibliotecaOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ebiblioteca.org" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(lecturas/\\?/v/\\d+|descargar\\.php\\?x=\\d+\\&sec=\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    private final Pattern TYPE_COLLECTION = Pattern.compile("https?://[^/]+/lecturas/\\?/v/(\\d+)");
    private final Pattern TYPE_SINGLE     = Pattern.compile("https?://[^/]+/descargar\\.php\\?x=\\d+\\&sec=\\d+");

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getRedirectLocation() == null && looksLikeOfflineContent(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        if (br.getRedirectLocation() != null) {
            br.followRedirect();
            if (looksLikeOfflineContent(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!this.canHandle(br.getURL())) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (new Regex(param.getCryptedUrl(), TYPE_COLLECTION).patternFind()) {
            String fpName = br.getRegex("<h3[^>]*><strong>([^<]+)</strong></h3>").getMatch(0);
            final String[] htmls = br.getRegex("openUnload\\(([0-9, ]+)\\)").getColumn(0);
            if (htmls == null || htmls.length == 0) {
                if (br.containsHTML("(?i)>\\s*Este enlace externo fue eliminado a pedido del autor o de la editorial")) {
                    /* Looks like all external URLs have been deleted -> Nothing we can do about that. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            FilePackage fp = null;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
            }
            int progr = 1;
            for (final String html : htmls) {
                logger.info("Crawling item " + progr + "/" + htmls.length);
                final String[] vars = html.replace(" ", "").split(",");
                final String x = vars[0];
                final String cual = vars[1];
                br.getPage("/descargar.php?x=" + x + "&cual=" + cual + "&sec=" + System.currentTimeMillis());
                final String finallink = regexSingleFinalURL(br);
                final DownloadLink link = createDownloadlink(finallink);
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                ret.add(link);
                distribute(link);
                if (this.isAbort()) {
                    break;
                } else {
                    progr++;
                }
            }
        } else if (new Regex(param.getCryptedUrl(), TYPE_SINGLE).patternFind()) {
            if (br.containsHTML(">\\s*Error 399")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String finallink = regexSingleFinalURL(br);
            ret.add(this.createDownloadlink(finallink));
        } else {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private String regexSingleFinalURL(final Browser br) throws PluginException {
        final String url = br.getRegex("Descargar\\(\\)\\s*\\{\\s*window\\.location = '(http[^\\']+)'").getMatch(0);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return url;
    }

    private boolean looksLikeOfflineContent(final Browser br) {
        if (br.containsHTML("ERROR - No se pudo encontrar informaci")) {
            return true;
        } else if (br.getHttpConnection().getRequest().getHtmlCode().length() <= 200) {
            return true;
        } else {
            return false;
        }
    }
}
