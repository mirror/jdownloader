//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.MofosCom.MofosConfigInterface;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mofos.com" }, urls = { "https?://members\\d+\\.mofos\\.com/(?:scene/view/\\d+(?:/[a-z0-9\\-_]+/?)?|hqpics/\\d+(?:/[a-z0-9\\-_]+/?)?)|https?://(?:www\\.)?mofos\\.com/tour/scene/[a-z0-9\\-]+/\\d+/?" })
public class MofosCom extends PluginForDecrypt {

    public MofosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_VIDEO          = "https?://members\\d+\\.mofos\\.com/scene/view/\\d+(?:/[a-z0-9\\-_]+/?)?";
    private static final String TYPE_PHOTO          = "https?://members\\d+\\.mofos\\.com/hqpics/\\d+(?:/[a-z0-9\\-_]+/?)?";

    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String>  all_known_qualities = Arrays.asList("1080_12000", "720_8000", "720_3800", "720_2600", "480_2000", "480_1500", "480_1000", "272_650");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "/(\\d+)/?").getMatch(0);
        /* Most likely this will contain a string + fid (usually only required for FREE['Tour']-URLs) */
        final String linkpart_free = new Regex(parameter, "tour/scene/([a-z0-9\\-]+/\\d+)").getMatch(0);
        // Login if possible
        final boolean loggedin = getUserLogin(false);
        if (!loggedin && requiresLogin(parameter)) {
            logger.info("No account present --> Cannot decrypt anything!");
            return decryptedLinks;
        }
        String title = null;
        if (isVideoUrl(parameter) && loggedin) {
            br.getPage(getVideoUrlPremium(fid));
            if (isOffline(this.br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            title = getTitle(this.br);

            List<String> selectedQualities = new ArrayList<String>();
            final MofosConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.MofosCom.MofosConfigInterface.class);
            final boolean fastLinkcheck = cfg.isFastLinkcheckEnabled();
            final boolean grab_best = cfg.isGrabBESTEnabled();
            final boolean grab_unknown_qualities = cfg.isAddUnknownQualitiesEnabled();
            final boolean grab1080_12000 = cfg.isGrab1080_12000Enabled();
            final boolean grab720_8000 = cfg.isGrab720_8000Enabled();
            final boolean grab720_3800 = cfg.isGrab720_3800Enabled();
            final boolean grab720_2600 = cfg.isGrab720_2600Enabled();
            final boolean grab720_2000 = cfg.isGrab720_2000Enabled();
            final boolean grab480_2000 = cfg.isGrab480_2000Enabled();
            final boolean grab480_1500 = cfg.isGrab480_1500Enabled();
            final boolean grab480_1000 = cfg.isGrab480_1000Enabled();
            final boolean grab272_650 = cfg.isGrab272_650Enabled();

            if (grab1080_12000) {
                selectedQualities.add("1080_12000");
            }
            if (grab720_8000) {
                selectedQualities.add("720_8000");
            }
            if (grab720_3800) {
                selectedQualities.add("720_3800");
            }
            if (grab720_2600) {
                selectedQualities.add("720_2600");
            }
            if (grab720_2000) {
                selectedQualities.add("720_2000");
            }
            if (grab480_2000) {
                selectedQualities.add("480_2000");
            }
            if (grab480_1500) {
                selectedQualities.add("480_1500");
            }
            if (grab480_1000) {
                selectedQualities.add("480_1000");
            }
            if (grab272_650) {
                selectedQualities.add("272_650");
            }

            if (title == null) {
                /* Fallback to id from inside url */
                title = fid;
            }
            final HashMap<String, DownloadLink> foundQualities = new HashMap<String, DownloadLink>();
            final String base_url = new Regex(this.br.getURL(), "(https?://[^/]+)/").getMatch(0);
            boolean streamDownload = false;
            String[] dlinfo = null;
            String htmldownload = this.br.getRegex("<div class=\"[^\"]*?download\\-frame[^\"]*?\">(.*?</a>)[\t\n\r ]*?</div>").getMatch(0);
            if (htmldownload != null) {
                dlinfo = htmldownload.split("</a>");
            } else {
                streamDownload = true;
                htmldownload = this.br.getRegex("(data\\-video.*?)>").getMatch(0);
                dlinfo = htmldownload.split("\n");
            }
            if (dlinfo != null) {
                for (final String video : dlinfo) {
                    String dlurl;
                    String quality;
                    String filesize = null;
                    if (streamDownload) {
                        /* No official downloadlinks available --> Download streams --> Quality selection is not designed for that */
                        final Regex streamInfo = new Regex(video, "data\\-video\\-mp4_(\\d+_\\d+)=\"(http[^<>\"]+)");
                        quality = streamInfo.getMatch(0);
                        dlurl = streamInfo.getMatch(1);
                    } else {
                        quality = new Regex(video, "mp4_(\\d+_\\d+)").getMatch(0);
                        filesize = new Regex(video, "<var>([^<>\"]+)</var>").getMatch(0);
                        if (filesize == null) {
                            filesize = new Regex(video, "title=\"([0-9\\.]+ (?:MiB|GiB))\"").getMatch(0);
                        }
                        dlurl = new Regex(video, "\"(/[^<>\"]*?download/[^<>\"]+/)\"").getMatch(0);
                    }
                    if (dlurl == null || quality == null) {
                        continue;
                    }
                    if (!dlurl.startsWith("http")) {
                        dlurl = base_url + dlurl;
                    }
                    if (streamDownload) {
                        /* 2017-03-08: Change server - according to a user, this speeds up the downloads */
                        dlurl = dlurl.replace("http.movies.mf.contentdef.com", "downloads.mf.contentdef.com");
                    }
                    final DownloadLink dl = this.createDownloadlink(dlurl);
                    if (filesize != null) {
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                        dl.setAvailable(true);
                    } else if (fastLinkcheck) {
                        /* Set available anyways if fast linkcheck is enabled! */
                        dl.setAvailable(true);
                    }
                    dl.setName(title + "_" + quality + ".mp4");
                    dl.setLinkID(dl.getName());
                    dl.setProperty("fid", fid);
                    dl.setProperty("quality", quality);
                    dl.setProperty("mainlink", this.br.getURL());
                    foundQualities.put(quality, dl);
                }
            }
            if (foundQualities.isEmpty()) {
                logger.info("Failed to find any quality");
            }
            boolean failedToGrabBest = true;
            if (grab_best) {
                for (final String knownQuality : this.all_known_qualities) {
                    if (foundQualities.containsKey(knownQuality)) {
                        decryptedLinks.add(foundQualities.get(knownQuality));
                        failedToGrabBest = false;
                        break;
                    }
                }
            }
            final boolean forceAllQualitiesBecauseGrabBestFailed = grab_best && failedToGrabBest;
            if (!grab_best || forceAllQualitiesBecauseGrabBestFailed) {
                boolean atLeastOneSelectedItemExists = false;
                for (final String selectedQuality : selectedQualities) {
                    if (foundQualities.containsKey(selectedQuality)) {
                        atLeastOneSelectedItemExists = true;
                        break;
                    }
                }
                final Iterator<Entry<String, DownloadLink>> iterator_all_found_downloadlinks = foundQualities.entrySet().iterator();
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    final String dl_quality_string = dl_entry.getKey();
                    final boolean is_unknown_quality = !this.all_known_qualities.contains(dl_quality_string);
                    /*
                     * Add quality if: user wants quality OR user wants best quality only but plugin failed to grab that OR user selected
                     * nothing (or only non-existant qualities) OR quality is unknown and user wants to have unknown qualities
                     */
                    if (selectedQualities.contains(dl_quality_string) || forceAllQualitiesBecauseGrabBestFailed || !atLeastOneSelectedItemExists || (is_unknown_quality && grab_unknown_qualities)) {
                        decryptedLinks.add(dl_entry.getValue());
                    }
                }
            }

        } else if (isFreeVideoUrl(parameter)) {
            /* Add single url --> Trailer download */
            final DownloadLink dl = this.createDownloadlink(parameter);
            dl.setProperty("mainlink", parameter);
            decryptedLinks.add(dl);
        } else {
            br.getPage(parameter);
            if (isOffline(this.br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            title = getTitle(this.br);
            if (title == null) {
                /* Fallback to id from inside url */
                title = fid;
            }
            final String pictures[] = getPictureArray(this.br);
            for (final String finallink : pictures) {
                final String number_formatted = new Regex(finallink, "(\\d+)\\.jpg").getMatch(0);
                final DownloadLink dl = this.createDownloadlink(finallink);
                dl.setFinalFileName(title + "_" + number_formatted + ".jpg");
                dl.setAvailable(true);
                dl.setProperty("fid", fid);
                dl.setProperty("linkpart", "");
                dl.setProperty("picnumber_formatted", number_formatted);
                dl.setProperty("mainlink", this.br.getURL());
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    @Override
    protected DownloadLink createDownloadlink(final String url) {
        return super.createDownloadlink(url.replaceAll("https?://", "mofosdecrypted://"));
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.MofosCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {

            aa.setValid(false);
            return false;
        }
        return true;
    }

    public static String[] getPictureArray(final Browser br) {
        return jd.plugins.decrypter.BabesComDecrypter.getPictureArray(br);
    }

    public static String getVideoUrlFree(final String linkpart) {
        return String.format("http://www.mofos.com/tour/scene/%s/", linkpart);
    }

    public static String getVideoUrlPremium(final String linkpart) {
        return String.format("http://members2.mofos.com/scene/view/%s/", linkpart);
    }

    public static String getPicUrl(final String linkpart) {
        return String.format("http://members2.mofos.com/hqpics/%s/", linkpart);
    }

    public static boolean requiresLogin(final String url) {
        return !isFreeVideoUrl(url);
    }

    public static boolean isFreeVideoUrl(final String url) {
        return url.contains("/tour/");
    }

    public static boolean isVideoUrl(final String url) {
        return isFreeVideoUrl(url) || url.matches(TYPE_VIDEO);
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    public static String getTitle(final Browser br) {
        return br.getRegex("<span>\\&nbsp;\\-\\&nbsp;([^<>]+)</span>").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }

}