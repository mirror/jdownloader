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
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.EroProfileCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eroprofile.com" }, urls = { "https?://(www\\.)?eroprofile\\.com/([A-Za-z0-9\\-_]+$|m/(videos|photos)/albums?/[A-Za-z0-9\\-_]+)" })
public class EroProfileComGallery extends PluginForDecrypt {
    public EroProfileComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
    }

    /* must be static so all plugins share same lock */
    private static Object LOCK = new Object();

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setReadTimeout(3 * 60 * 1000);
        br.setCookiesExclusive(false);
        br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
        br.setCookie(this.getHost(), "lang", "en");
        boolean loggedin = false;
        synchronized (LOCK) {
            /** Login process */
            // force login to see if that solves problems
            loggedin = getUserLogin(true);
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // Check if account needed but none account entered
        if (br.containsHTML(jd.plugins.hoster.EroProfileCom.NOACCESS) && !loggedin) {
            logger.info("Account needed to decrypt link: " + parameter);
            return ret;
        } else if (br.containsHTML(jd.plugins.hoster.EroProfileCom.NOACCESS)) {
            logger.info("No cookies, login maybe failed: " + parameter);
            return ret;
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Album not found<|>\\s*No photos found|^No htmlCode read$")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean isProfile = param.getCryptedUrl().matches("(?i).*eroprofile\\.com/[A-Za-z0-9\\-_]+$");
        final boolean isAlbums = param.getCryptedUrl().matches("(?i)^.+/m/(videos|photos)/albums/.+");
        String fpName = br.getRegex("Browse photos from album \\&quot;([^<>\"]*?)\\&quot;<").getMatch(0);
        if (fpName == null) {
            fpName = EroProfileCom.getFilename(br);
        }
        if (fpName != null) {
            final String by = br.getRegex(">\\s*by\\s*<[^>]*>\\s*<a\\s*href[^>]*>\\s*(.*?)\\s*<").getMatch(0);
            if (by != null) {
                fpName = by + "-" + fpName;
            }
            fpName = fpName.replaceAll("(\\s*:\\s*)", "-");
        }
        final List<String> pagesDones = new ArrayList<String>();
        final List<String> pagesLeft = new ArrayList<String>();
        pagesLeft.add("1");
        while (!isAbort()) {
            final String page;
            if (pagesLeft.size() > 0) {
                page = pagesLeft.remove(0);
                if (pagesDones.contains(page)) {
                    continue;
                }
            } else {
                break;
            }
            if (!page.equals("1")) {
                br.getPage(parameter + "?pnum=" + page);
            }
            pagesDones.add(page);
            final String[] nextPages = br.getRegex("\\?pnum=(\\d+)\"").getColumn(0);
            if (nextPages != null && nextPages.length != 0) {
                for (final String nextPage : nextPages) {
                    if (!pagesLeft.contains(nextPage)) {
                        pagesLeft.add(nextPage);
                    }
                }
            }
            if (!isProfile && !isAlbums) {
                final String[][] videosOrPhotos = br.getRegex("<a href\\s*=\\s*\"(/m/(?:videos|photos)/view/([A-Za-z0-9\\-_]+))\"").getMatches();
                if (videosOrPhotos != null) {
                    for (final String singleLink[] : videosOrPhotos) {
                        final DownloadLink dl = createDownloadlink("https://www.eroprofile.com" + singleLink[0]);
                        // final filename is set later in hosterplugin
                        dl.setName(singleLink[1] + (StringUtils.containsIgnoreCase(singleLink[0], "/m/videos") ? ".mp4" : ".jpg"));
                        dl.setAvailable(true);
                        ret.add(dl);
                    }
                }
            }
            final String[][] albums = br.getRegex("<a href\\s*=\\s*\"(/m/(?:videos|photos)/albums?/([A-Za-z0-9\\-_]+))\"").getMatches();
            if (albums != null && albums.length > 0) {
                for (final String album[] : albums) {
                    if (!StringUtils.containsIgnoreCase(param.getCryptedUrl(), album[0])) {
                        final DownloadLink dl = createDownloadlink("https://www.eroprofile.com" + album[0]);
                        ret.add(dl);
                    }
                }
            }
            if (ret.size() == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (isProfile) {
                break;
            }
        }
        if (fpName != null && !isAlbums) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("eroprofile.com");
        Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            return false;
        }
        try {
            ((jd.plugins.hoster.EroProfileCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(hostPlugin, aa);
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}