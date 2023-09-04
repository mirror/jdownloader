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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DatoidCz;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { DatoidCz.class })
public class DatoidCzFolder extends PluginForDecrypt {
    public DatoidCzFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return DatoidCz.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/slozka/[A-Za-z0-9]+/[A-Za-z0-9]+");
        }
        return ret.toArray(new String[0]);
    }

    private final boolean USE_API = true;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<String> allPages = new ArrayList<String>();
        allPages.add("1");
        final String parameter = param.toString().replace("datoid.sk/", "datoid.cz/");
        if (USE_API) {
            br.getPage("http://api.datoid.cz/v1/getfilesoffolder?url=" + Encoding.urlEncode(parameter));
            final String error = PluginJSonUtils.getJsonValue(this.br, "error");
            if (error != null || this.br.toString().length() < 30) {
                if (error != null) {
                    logger.info("Cannot crawl anything because of error: " + error);
                } else {
                    logger.info("Cannot crawl anything because folder seems to be empty");
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] links = br.getRegex("\"(http:[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String alink : links) {
                ret.add(createDownloadlink(alink.replace("\\", "")));
            }
        } else {
            br.getPage(parameter);
            final String[] pages = br.getRegex("class=\"ajax\">(\\d+)</a>").getColumn(0);
            if (pages != null) {
                for (final String aPage : pages) {
                    if (!allPages.contains(aPage)) {
                        allPages.add(aPage);
                    }
                }
            }
            logger.info("Found " + allPages.size() + " pages, starting to decrypt...");
            for (final String currentPage : allPages) {
                logger.info("Decrypting page " + currentPage + " / " + allPages.size());
                if (!currentPage.equals("1")) {
                    br.getPage(parameter + "?current-page=" + currentPage);
                }
                final String[] links = br.getRegex("\"(/[^<>\"]*?)\">[\t\n\r ]+<div class=\"thumb").getColumn(0);
                if (links == null || links.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String singleLink : links) {
                    ret.add(createDownloadlink("http://datoid.cz" + singleLink));
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(parameter, "/slozka/[A-Za-z0-9]+/(.+)").getMatch(0));
        fp.addLinks(ret);
        return ret;
    }
}
