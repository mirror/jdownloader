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
import java.util.Locale;
import java.util.Random;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.SuicidegirlsCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "suicidegirls.com" }, urls = { "https?://(?:www\\.)?suicidegirls\\.com/(?:girls|members)/[A-Za-z0-9\\-_]+/(?:album/\\d+/[A-Za-z0-9\\-_]+/)?" })
public class SuicidegirlsComCrawler extends PluginForDecrypt {
    public SuicidegirlsComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    private static final String TYPE_ALBUM = "(?i)https?://(?:www\\.)?suicidegirls\\.com/(?:girls|members)/[A-Za-z0-9\\-_]+/album/\\d+/[A-Za-z0-9\\-_]+/";
    private static final String TYPE_USER  = "(?i)https?://(?:www\\.)?suicidegirls\\.com/(?:girls|members)/[A-Za-z0-9\\-_]+/";

    private final int padLength(final int size) {
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
            /*
             * 2023-03-11: This whole function can be replaced by StringUtils.getPadLength but I'm leaving it here just because of the
             * djmakinera comment lol
             */
            return 8;// hello djmakinera
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final PluginForHost plugin = this.getNewPluginForHostInstance(this.getHost());
        plugin.setBrowser(this.br);
        final boolean loggedin = ((jd.plugins.hoster.SuicidegirlsCom) plugin).login(br) != null;
        ((jd.plugins.hoster.SuicidegirlsCom) plugin).prepBR(br);
        br.setFollowRedirects(true);
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<String> dupecheck = new ArrayList<String>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (br.containsHTML("class=\"album-join-message\"")) {
            throw new AccountRequiredException();
        }
        final Regex urlinfo = new Regex(param.getCryptedUrl(), "(girls|members)/([A-Za-z0-9\\-_]+)/");
        final String member_type = urlinfo.getMatch(0);
        final String username = urlinfo.getMatch(1);
        String fpName = null;
        if (param.getCryptedUrl().matches(TYPE_ALBUM)) {
            fpName = br.getRegex("<h2 class=\"title\">([^<>\"]*?)</h2>").getMatch(0);
            if (fpName == null) {
                /* Fallback to url-packagename */
                fpName = br._getURL().toExternalForm();
            }
            fpName = username + " - " + fpName;
            final String[] links = br.getRegex("<li class=\"photo-container\" id=\"thumb-\\d+\" data-index=\"\\d+\"[^>]*>\\s*<a href=\"(http[^<>\"]*?)\"").getColumn(0);
            if ((links == null || links.length == 0) && !loggedin) {
                /* Account is needed most times */
                throw new AccountRequiredException();
            }
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int imageIndex = 1;
            for (String directlink : links) {
                directlink = Encoding.htmlOnlyDecode(directlink);
                final DownloadLink dl = this.createDownloadlink("http://suicidegirlsdecrypted/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String extension = getFileNameExtensionFromURL(directlink, ".jpg");
                final String imageName = String.format(Locale.US, "%0" + padLength(links.length) + "d", imageIndex++) + "_" + fpName + extension;
                dl.setProperty(SuicidegirlsCom.PROPERTY_DIRECTURL, directlink);
                dl.setProperty(SuicidegirlsCom.PROPERTY_IMAGE_NAME, imageName);
                dl.setFinalFileName(imageName);
                dl.setLinkID(directlink);
                dl.setAvailable(true);
                dl.setContentUrl(directlink);
                dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
                ret.add(dl);
            }
        } else {
            /* TYPE_USER */
            br.getPage("/" + member_type + "/" + username + "/photos/view/photosets/");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final short max_entries_per_page = 9;
            int addedlinks_total = 0;
            int addedlinks = 0;
            do {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return ret;
                }
                addedlinks = 0;
                if (addedlinks_total > 0) {
                    br.getPage("/" + member_type + "/" + username + "/photos/?partial=true&offset=" + addedlinks_total);
                }
                final String[] links = br.getRegex("\"(/(?:girls|members)/[^/]+/album/\\d+/[A-Za-z0-9\\-_]+/)[^<>\"]*?\"").getColumn(0);
                if ((links == null || links.length == 0) && addedlinks_total == 0 && !loggedin) {
                    /* Account is needed most times */
                    throw new AccountRequiredException();
                }
                if (links == null || links.length == 0) {
                    break;
                }
                for (final String singleLink : links) {
                    final String final_album_url = "https://www." + this.getHost() + singleLink;
                    if (!dupecheck.contains(final_album_url)) {
                        dupecheck.add(final_album_url);
                        ret.add(createDownloadlink(final_album_url));
                        addedlinks++;
                        addedlinks_total++;
                    }
                }
            } while (addedlinks >= max_entries_per_page);
            if (ret.size() == 0) {
                return null;
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(ret);
        }
        return ret;
    }
}
