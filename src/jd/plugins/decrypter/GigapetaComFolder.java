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
import java.util.HashSet;
import java.util.List;

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
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GigapetaComFolder extends PluginForDecrypt {
    public GigapetaComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "gigapeta.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/([a-f0-9]{10,})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        // final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "lang", "en");
        br.getPage(param.getCryptedUrl());
        String folderTitle = br.getRegex("id=\"content\"><h1>([^<]+)</h1>").getMatch(0);
        if (folderTitle != null) {
            folderTitle = Encoding.htmlDecode(folderTitle).trim();
        }
        // br.getPage("/?lang=en");//sets lang cookie
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*You haven\\&#96;t uploaded any file")) {
            /* Empty folder */
            if (folderTitle != null) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, folderTitle);
            } else {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
            }
        } else if (br.containsHTML("(?i)>\\s*Folder not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String passCode = null;
        Form pwform = getFolderpwForm(this.br);
        if (pwform != null) {
            for (int i = 0; i <= 2; i++) {
                passCode = getUserInput("Password?", param);
                pwform.put("dirpass", Encoding.urlEncode(passCode));
                br.submitForm(pwform);
                pwform = getFolderpwForm(this.br);
                if (pwform == null) {
                    logger.info("User entered correct password: " + passCode);
                    break;
                } else {
                    logger.info("User entered wrong password: " + passCode);
                }
            }
            if (this.getFolderpwForm(br) != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        FilePackage fp = null;
        if (folderTitle != null) {
            fp = FilePackage.getInstance();
            fp.setName(folderTitle);
        }
        int maxpage = 1;
        final String[] pages = br.getRegex("\\?p=(\\d+)").getColumn(0);
        for (final String pageStr : pages) {
            final int pageTmp = Integer.parseInt(pageStr);
            if (pageTmp > maxpage) {
                maxpage = pageTmp;
            }
        }
        final HashSet<String> dupes = new HashSet<String>();
        final String baseurl = br.getURL();
        int page = 1;
        do {
            final String[] urls = br.getRegex("(?i)(/dl/[a-f0-9]{10,})").getColumn(0);
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int numberofNewItems = 0;
            for (final String url : urls) {
                if (!dupes.add(url)) {
                    /* Skip dupes */
                    continue;
                }
                final DownloadLink link = createDownloadlink(br.getURL(url).toString());
                if (passCode != null) {
                    link.setDownloadPassword(passCode);
                }
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                ret.add(link);
                distribute(link);
                numberofNewItems++;
            }
            logger.info("Crawled page " + page + "/" + maxpage + " | Found items so far: " + ret.size());
            if (numberofNewItems == 0) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else if (page >= maxpage) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else {
                /* Continue to next page */
                page++;
                br.getPage(baseurl + "?p=" + page);
            }
        } while (!this.isAbort());
        return ret;
    }

    private Form getFolderpwForm(final Browser br) {
        return br.getFormbyKey("dirpass");
    }
}
