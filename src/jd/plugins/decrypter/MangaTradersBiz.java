//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mangatraders.biz" }, urls = { "https?://(?:www\\.)?mangatraders\\.(biz|org)/series/\\w+|https?://(?:www\\.)?mangatraders\\.biz/read\\-online/[A-Za-z0-9\\-_]+chapter\\-\\d+\\-page\\-\\d+\\.html" })
public class MangaTradersBiz extends PluginForDecrypt {

    private PluginForHost plugin = null;

    private Browser prepBrowser(final Browser prepBr) {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("mangatraders.biz");
            if (plugin == null) {
                throw new IllegalStateException("mangatraders hoster plugin not found!");
            }
        }
        // set cross browser support
        ((jd.plugins.hoster.MangaTradersBiz) plugin).setBrowser(br);
        return prepBr;
    }

    private void getPage(final String page) throws Exception {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("mangatraders.biz");
            if (plugin == null) {
                throw new IllegalStateException("mangatraders hoster plugin not found!");
            }
        }
        // set cross browser support
        ((jd.plugins.hoster.MangaTradersBiz) plugin).getPage(page);
    }

    public MangaTradersBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceAll("http(s?)://[^/]+(/.+)", "http$1://" + this.getHost() + "$2");
        prepBrowser(br);
        final FilePackage fp = FilePackage.getInstance();
        if (parameter.matches(".+/read\\-online/.+")) {
            getPage(parameter);
            if (isOffline(this.br)) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String extension_fallback = ".jpg";
            final Regex urlinfo = new Regex(parameter, "read\\-online/(.+)\\-chapter\\-(\\d+)\\-page\\-\\d+\\.html$");
            final String url_chapter = urlinfo.getMatch(1);
            final String url_name = urlinfo.getMatch(0);
            String ext = null;

            short page_max = 0;
            final String[] pages = this.br.getRegex(">Page (\\d+)</option>").getColumn(0);
            for (final String page_temp_str : pages) {
                final short page_temp = Short.parseShort(page_temp_str);
                if (page_temp > page_max) {
                    page_max = page_temp;
                }
            }

            fp.setName(url_chapter + "_" + url_name);

            final int padLength = getPadLength(page_max);

            for (short page = 1; page <= page_max; page++) {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                final String page_formatted = String.format(Locale.US, "%0" + padLength + "d", page);
                final String page_url = String.format("http://%s/read-online/%s-chapter-%s-page-%s.html", this.getHost(), url_name, url_chapter, Short.toString(page));
                getPage(page_url);

                final String finallink = this.br.getRegex("class=\"CurImage\" src=\"(http[^<>\"]+)\"").getMatch(0);
                if (finallink == null) {
                    return null;
                }
                ext = getFileNameExtensionFromURL(finallink, extension_fallback);
                if (ext == null) {
                    ext = extension_fallback;
                }
                final String filename = url_chapter + "_" + url_name + "_" + page_formatted + ext;

                final DownloadLink dl = this.createDownloadlink(finallink);
                dl._setFilePackage(fp);
                dl.setFinalFileName(filename);
                // dl.setContentUrl(page_url);
                dl.setLinkID(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
            }
        } else {
            // pages now are just links to download files. We need to be logged in to get this information.
            if (!login()) {
                decryptedLinks.add(createOfflinelink(parameter, "In order to use this website you need an Account!"));
                return decryptedLinks;
            }
            br.setFollowRedirects(true);

            getPage(parameter);
            if (isOffline(this.br)) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }

            final String seriesNameUrl = new Regex(parameter, "series/(.+)").getMatch(0);
            String fpName = br.getRegex("class=\"SeriesName\">([^<>]+)<").getMatch(0);
            if (fpName == null) {
                /* Fallback */
                fpName = seriesNameUrl;
            }
            fp.setName(fpName);

            // logger.info(br.toString());
            final String[][] linkinfos = br.getRegex("linkValue=\"([A-Za-z0-9]+)\">\\s*<[^<>]+>([^<>]+)</span>").getMatches();

            if (linkinfos == null || linkinfos.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (linkinfos != null && linkinfos.length != 0) {
                for (final String linkinfo[] : linkinfos) {
                    final DownloadLink dlink = createDownloadlink("http://" + this.getHost() + "/downloadlink/" + linkinfo[0]);
                    dlink.setProperty("mainlink", parameter);
                    dlink.setName(linkinfo[1]);
                    dlink.setAvailable(true);
                    decryptedLinks.add(dlink);
                }
            }
        }

        if (decryptedLinks.size() > 0) {
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        } else {
            return null;
        }
    }

    private final boolean login() throws Exception {
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa != null) {
            try {
                synchronized (jd.plugins.hoster.MangaTradersBiz.ACCLOCK) {
                    ((jd.plugins.hoster.MangaTradersBiz) plugin).login(aa, false);
                }
            } catch (final PluginException e) {
                aa.setValid(false);
                logger.info("Account seems to be invalid!");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private final int getPadLength(final int size) {
        if (size < 10) {
            return 1;
        } else if (size < 100) {
            return 2;
        } else if (size < 1000) {
            return 3;
        } else if (size < 10000) {
            return 4;
        } else if (size < 100000) {
            return 5;
        } else if (size < 1000000) {
            return 6;
        } else if (size < 10000000) {
            return 7;
        } else {
            return 8;
        }
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}