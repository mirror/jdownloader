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

import java.awt.Dialog.ModalityType;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "megatv.com" }, urls = { "https?://(?:www\\.)?megatv\\.com/[^<>\"]+\\.asp\\?catid=\\d+\\&subid=\\d+\\&pubid=\\d+|https?://(?:www\\.)?megatv\\.com\\.cy/cgibin/hweb\\?\\-A=\\d+\\&\\-V=[A-Za-z0-9]+" })
public class MegatvComDecrypter extends PluginForDecrypt {
    public MegatvComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String getAjaxURLByGroup(final Browser br, final String targetType) throws MalformedURLException {
        if (targetType == null) {
            return null;
        }
        final UrlQuery query = new UrlQuery().parse(br.getURL());
        final String catid = query.get("catid");
        final String[] prototypes = br.getRegex("addPrototypeElement\\((.*?)\\)").getColumn(0);
        String ajax_url = null;
        for (String varsStr : prototypes) {
            varsStr = varsStr.replace(" ", "").replace("'", "");
            final String[] vars = varsStr.split(",");
            final String id = vars[0];
            final String type = vars[1];
            final String url = vars[2];
            final String urlparams = vars[3];
            final String booleanValue = vars[4];
            if (catid != null && !urlparams.contains(catid)) {
                continue;
            } else if (!targetType.equalsIgnoreCase(type)) {
                continue;
            }
            ajax_url = url + "?" + urlparams;
            ajax_url += "&ajaxid=" + id + "&ajaxgroup=" + targetType;
            break;
        }
        final String urlpart = new Regex(br.getURL(), "https?://[^/]+/([^/]+)").getMatch(0);
        if (ajax_url != null) {
            if (!ajax_url.contains(urlpart)) {
                ajax_url = "https://www.megatv.com/" + urlpart + "/" + ajax_url;
            } else {
                ajax_url = "https://www.megatv.com/" + ajax_url;
            }
            if (targetType.equalsIgnoreCase("REST")) {
                ajax_url += "&page1=";
            }
        }
        return ajax_url;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        if (parameter.matches(".+\\.asp\\?catid=\\d+\\&subid=\\d+\\&pubid=\\d+")) {
            /* Old */
            final ArrayList<String> dupes = new ArrayList<String>();
            final UrlQuery query = new UrlQuery().parse(parameter);
            final String catid = query.get("catid");
            final String subid = query.get("subid");
            final String pubid = query.get("pubid");
            try {
                br.getPage(parameter);
                final String ajax_url = getAjaxURLByGroup(br, "REST");
                if (ajax_url == null) {
                    throw new DecrypterException("Decrypter broken");
                }
                int page_num = 1;
                final int entries_per_page = 10;
                int added_links_num = 0;
                boolean foundNewItem;
                do {
                    foundNewItem = false;
                    br.getPage(ajax_url + page_num);
                    final String[] pubIDs = br.getRegex("catid=" + catid + "\\&subid=" + subid + "\\&pubid=(\\d+)").getColumn(0);
                    for (final String videoID : pubIDs) {
                        // final String url_real = "http://www.megatv.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" +
                        // videoID;
                        if (dupes.contains(videoID)) {
                            continue;
                        }
                        dupes.add(videoID);
                        foundNewItem = true;
                        final DownloadLink dl = createDownloadlink("https://www.megatvdecrypted.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" + videoID);
                        dl.setLinkID(videoID);
                        dl.setContentUrl("https://www.megatv.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" + videoID);
                        dl.setAvailable(true);
                        dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                        decryptedLinks.add(dl);
                    }
                    added_links_num = pubIDs.length;
                    page_num++;
                } while (!this.isAbort() && added_links_num >= entries_per_page && foundNewItem);
            } catch (final Throwable e) {
            }
            if (decryptedLinks.size() == 0) {
                /* Decrypter failed or there is only 1 video --> Add to host plugin */
                final String url_real = "http://www.megatv.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" + pubid;
                final DownloadLink dl = createDownloadlink("http://www.megatvdecrypted.com/classics.asp?catid=" + catid + "&subid=" + subid + "&pubid=" + pubid);
                dl.setLinkID(pubid);
                dl.setContentUrl(parameter); // url_real is not correct anymore
                decryptedLinks.add(dl);
            }
        } else {
            /* New 2017-01-30 */
            this.br.setFollowRedirects(true);
            this.br.getPage(parameter);
            if (isOffline()) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, parameter, "For this URL JDownloader can crawl the single video only or all related videos. What would you like to do?", null, "All videos AND the single video?", "Single Video?") {
                @Override
                public ModalityType getModalityType() {
                    return ModalityType.MODELESS;
                }

                @Override
                public boolean isRemoteAPIEnabled() {
                    return true;
                }
            };
            boolean decryptRelatedVideos = false;
            try {
                UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
                decryptRelatedVideos = true;
            } catch (DialogCanceledException e) {
                decryptRelatedVideos = false;
            } catch (DialogClosedException e) {
                decryptRelatedVideos = false;
            }
            /* Decrypt current url */
            final DownloadLink dlsingle = crawlSingle();
            if (dlsingle == null) {
                return null;
            }
            decryptedLinks.add(dlsingle);
            distribute(dlsingle);
            if (decryptRelatedVideos) {
                final String[] allRelatedVideoUrls = this.br.getRegex("<option value=\"(/cgibin/hweb\\?[^<>\"]+)\"").getColumn(0);
                for (String relatedVideoUrl : allRelatedVideoUrls) {
                    if (this.isAbort()) {
                        return decryptedLinks;
                    }
                    relatedVideoUrl = "http://www." + this.br.getHost() + relatedVideoUrl;
                    this.br.getPage(relatedVideoUrl);
                    if (isOffline()) {
                        decryptedLinks.add(this.createOfflinelink(relatedVideoUrl));
                        continue;
                    }
                    final DownloadLink dl = crawlSingle();
                    if (dl == null) {
                        return null;
                    }
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
            }
        }
        return decryptedLinks;
    }

    private boolean isOffline() {
        return this.br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("cgibin");
    }

    private DownloadLink crawlSingle() {
        String finallink = this.br.getRegex(Pattern.compile("\"(https?://media\\.livenews\\.com\\.cy/DesktopModules/IST[^<>\"]*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (finallink == null) {
            return null;
        }
        /* Remove linebreaks ... */
        finallink = finallink.replace("\n", "").replace("\r", "");
        return this.createDownloadlink(finallink);
    }
}
