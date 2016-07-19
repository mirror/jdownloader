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

import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "primejailbait.com" }, urls = { "https?://(?:www\\.)?(?:primejailbait\\.com|primejb\\.net)/(id/\\d+|profile/[A-Za-z0-9\\-_]+(?:/fav/\\d+)?|setview/\\d+)/?$" }, flags = { 0 })
public class PrimeJailBaitCom extends antiDDoSForDecrypt {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "primejailbait.com", "primejb.net" };
    }

    public PrimeJailBaitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String TYPE_SINGLE       = "^https?://(?:www\\.)?(?:primejailbait\\.com|primejb\\.net)/id/\\d+/?$";
    private final String TYPE_SETVIEW      = "^https?://(?:www\\.)?(?:primejailbait\\.com|primejb\\.net)/setview/\\d+/?$";
    private final String TYPE_PROFILE_FAVS = "^https?://(?:www\\.)?(?:primejailbait\\.com|primejb\\.net)/profile/([A-Za-z0-9\\-_]+)/fav/(\\d+)/?$";
    private final String TYPE_PROFILE_ALL  = "^https?://(?:www\\.)?(?:primejailbait\\.com|primejb\\.net)/profile/([A-Za-z0-9\\-_]+)/?$";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        for (final String host : siteSupportedNames()) {
            Browser.setRequestIntervalLimitGlobal(host, 50, 15, 1000);
        }
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.containsHTML("images/404\\.png\"") || br.getURL().endsWith("/404/") || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_PROFILE_FAVS) || parameter.matches(TYPE_PROFILE_ALL) || parameter.matches(TYPE_SETVIEW)) {
            FilePackage fp = null;
            if (parameter.matches(TYPE_PROFILE_ALL)) {
                final String fpName = new Regex(parameter, TYPE_PROFILE_ALL).getMatch(0);
                fp = FilePackage.getInstance();
                fp.setName("Profile - " + fpName);
            }
            String getpage = null;

            // String next = null;
            // final int pics_per_page = 30;
            int currentPage = 1;
            String[] thumbinfo = null;
            do {
                logger.info("Decrypting page: " + currentPage);
                if (this.isAbort()) {
                    logger.info("User aborted decryption process");
                    break;
                }

                if (parameter.matches(TYPE_PROFILE_FAVS)) {
                    final Regex urlinfo = new Regex(parameter, TYPE_PROFILE_FAVS);
                    final String username = urlinfo.getMatch(0);
                    final String lid = urlinfo.getMatch(1);
                    getpage = "/profile_inf.php?user=" + username + "&list=" + lid + "&page=";
                } else if (parameter.matches(TYPE_PROFILE_ALL)) {
                    final String username = new Regex(parameter, TYPE_PROFILE_ALL).getMatch(0);
                    final String lastseen_id = br.getRegex("class='thumb' id='(\\d+)'").getMatch(0);
                    if (lastseen_id == null) {
                        /* Dont return null here - probably we simply decrypted everything. */
                        break;
                    }
                    /* If lastseen_id is not from the last page --> We will always get the same results! */
                    getpage = "/profile_inf.php?user=" + username + "&t=uploads&lastseen=" + lastseen_id + "&page=";
                } else {
                    /* To be defined - maybe needed for future linktypes */
                }
                if (currentPage > 1) {
                    getPage(getpage + currentPage);
                }
                thumbinfo = br.getRegex("<div class='thumb' (id='\\d+'>.*?)<span>By:").getColumn(0);
                if (thumbinfo == null || thumbinfo.length == 0) {
                    /* E.g. needed for TYPE_SETVIEW */
                    thumbinfo = br.getRegex("(<a[^>]+href=\"/id/\\d+/?\" id=\"thumb_\\d+\".*?)style=\"").getColumn(0);
                }
                if (thumbinfo == null || thumbinfo.length == 0) {
                    return null;
                }
                for (final String thumb : thumbinfo) {
                    final String id = new Regex(thumb, "/id/(\\d+)").getMatch(0);
                    String thumb_url = new Regex(thumb, "(https?://[a-z0-9\\-\\.]+/pics/(?:bigthumbs|original)/[^<>\"']+)").getMatch(0);
                    if (thumb_url == null) {
                        return null;
                    }
                    /* Build directlinks */
                    thumb_url = thumb_url.replace("/bigthumbs/", "/original/");
                    final DownloadLink dl = createDownloadlink("directhttp://" + thumb_url);
                    dl.setAvailable(true);
                    if (id != null) {
                        dl.setContentUrl("https://www." + Browser.getHost(parameter) + "/id/" + id);
                        String ext = Files.getExtension(thumb_url);
                        if (!StringUtils.contains(ext, "png") && !StringUtils.contains(ext, "gif") && !StringUtils.contains(ext, "jpg")) {
                            ext = "jpg";
                        }
                        dl.setFinalFileName(id + "." + ext);
                    }
                    decryptedLinks.add(dl);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    distribute(dl);
                }
                currentPage++;
            } while (getpage != null);

            if (decryptedLinks.size() == 0) {
                return null;
            }

        } else {
            final String ID = new Regex(parameter, "/id/(\\d+)").getMatch(0);
            String finallink = br.getRegex("<div id=\"bigwall\" class=\"right\">[\t\n\r ]+<img border=0 src='(https?://[^<>']+)'").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("'(https?://pics\\.(?:primejailbait\\.com|primejb\\.net)/pics/original/[^<>']+)'").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            DownloadLink link = createDownloadlink("directhttp://" + finallink);
            link.setAvailableStatus(AvailableStatus.TRUE);
            if (ID != null) {
                link.setContentUrl("https://www." + Browser.getHost(parameter) + "/id/" + ID);
                String ext = Files.getExtension(finallink);
                if (!StringUtils.contains(ext, "png") && !StringUtils.contains(ext, "gif") && !StringUtils.contains(ext, "jpg")) {
                    ext = "jpg";
                }
                link.setFinalFileName(ID + "." + ext);
            }
            decryptedLinks.add(link);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}