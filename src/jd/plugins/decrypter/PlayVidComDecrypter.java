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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.plugins.components.hds.HDSContainer;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "playvid.com" }, urls = { "https?://(www\\.)?playvid.com/(?:watch(?:\\?v=|/)|embed/|v/)[A-Za-z0-9\\-_]+|https?://(?:www\\.)?playvids\\.com/(?:[a-z]{2}/)?v/[A-Za-z0-9\\-_]+" })
public class PlayVidComDecrypter extends PluginForDecrypt {

    public PlayVidComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* 2017-01-25: Limited this to 1 - on too many requests we get HTTP/1.1 429 Too Many Requests. */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
    private String                        filename       = null;
    private String                        parameter      = null;
    private CryptedLink                   param          = null;
    private String                        videoID        = null;

    /** Settings stuff */
    private static final String           FASTLINKCHECK  = "FASTLINKCHECK";
    private static final String           ALLOW_BEST     = "ALLOW_BEST";
    private static final String           ALLOW_360P     = "ALLOW_360P";
    private static final String           ALLOW_480P     = "ALLOW_480P";
    private static final String           ALLOW_720P     = "ALLOW_720";

    @SuppressWarnings({ "static-access", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        videoID = new Regex(param.toString(), "([A-Za-z0-9\\-_]+)$").getMatch(0);
        if (param.toString().contains("playvid.com/")) {
            parameter = new Regex(param.toString(), "https?://").getMatch(-1) + "www.playvid.com/watch/" + videoID;
        } else {
            parameter = param.toString();
        }
        this.param = param;
        br.setFollowRedirects(true);
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("playvid.com");
        }
        ((jd.plugins.hoster.PlayVidCom) plugin).prepBrowser(br);
        // Log in if possible to get 720p quality
        getUserLogin(false);
        getPage(parameter);
        if (jd.plugins.hoster.PlayVidCom.isOffline(this.br)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Decrypt start */
        filename = br.getRegex("data\\-title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("data\\-callback=\"pv_hideshowTitle\">([^<>\"]*?)<").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("itemprop=\"name\" content=\"([^<>\"]+)\"").getMatch(0);
                if (filename == null) {
                    filename = new Regex(parameter, "/([^/]+)$").getMatch(0);
                    if (filename == null) {
                        logger.warning("Playvid.com decrypter failed..." + parameter);
                        return null;
                    }
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filename);
        /** Decrypt qualities START */

        foundQualities = ((jd.plugins.hoster.PlayVidCom) plugin).getQualities(this.br);
        if (foundQualities == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /** Decrypt qualities END */
        /** Decrypt qualities, selected by the user */
        final SubConfiguration cfg = SubConfiguration.getConfig("playvid.com");
        final boolean best = cfg.getBooleanProperty(ALLOW_BEST, false);
        final boolean q360p = cfg.getBooleanProperty(ALLOW_360P, true);
        final boolean q480p = cfg.getBooleanProperty(ALLOW_480P, true);
        final boolean q720p = cfg.getBooleanProperty(ALLOW_720P, true);
        final boolean all = (q360p == false && q480p == false && q720p == false);

        final HashMap<String, List<DownloadLink>> results = new HashMap<String, List<DownloadLink>>();

        List<DownloadLink> ret = getVideoDownloadlinks("720p");
        if (ret != null && (q720p || all)) {
            List<DownloadLink> list = results.get("720p");
            if (list == null) {
                list = new ArrayList<DownloadLink>();
                results.put("720p", list);
            }
            list.addAll(ret);
        }
        ret = getVideoDownloadlinks("hds_manifest");
        if (ret != null && (q720p || all)) {
            List<DownloadLink> list = results.get("720p");
            if (list == null || !best) {
                if (list == null) {
                    list = new ArrayList<DownloadLink>();
                    results.put("720p", list);
                }
                list.addAll(ret);
            }
        }

        ret = getVideoDownloadlinks("480p");
        if (ret != null && (q480p || all)) {
            List<DownloadLink> list = results.get("480p");
            if (list == null || !best) {
                if (list == null) {
                    list = new ArrayList<DownloadLink>();
                    results.put("480p", list);
                }
                list.addAll(ret);
            }
        }

        ret = getVideoDownloadlinks("360p");
        if (ret != null && (q360p || all)) {
            List<DownloadLink> list = results.get("360p");
            if (list == null || !best) {
                if (list == null) {
                    list = new ArrayList<DownloadLink>();
                    results.put("360p", list);
                }
                list.addAll(ret);
            }
        }

        for (List<DownloadLink> list : results.values()) {
            for (DownloadLink link : list) {
                fp.add(link);
                decryptedLinks.add(link);
            }
        }

        if (decryptedLinks.size() == 0) {
            logger.info("None of the selected qualities were found, decrypting done...");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    @SuppressWarnings("deprecation")
    private List<DownloadLink> getVideoDownloadlinks(final String qualityValue) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String directlink = foundQualities.get(qualityValue);
        if (directlink != null) {
            if (StringUtils.containsIgnoreCase(qualityValue, "hds_")) {
                final Browser brc = br.cloneBrowser();
                brc.getPage(directlink);
                brc.followRedirect();
                final List<HDSContainer> containers = HDSContainer.getHDSQualities(brc);
                if (containers != null) {
                    for (final HDSContainer container : containers) {
                        String fname = filename + "_" + qualityValue;
                        final DownloadLink link = createDownloadlink("http://playviddecrypted.com/" + UniqueAlltimeID.create());
                        link.setProperty("directlink", directlink);
                        link.setProperty("qualityvalue", qualityValue);
                        link.setProperty("mainlink", parameter);
                        if (container.getHeight() != -1) {
                            fname += "_" + container.getHeight() + "p";
                        }
                        fname += ".mp4";
                        link.setProperty("directname", fname);
                        link.setFinalFileName(fname);
                        link.setContentUrl(parameter);
                        container.write(link);
                        link.setAvailable(true);
                        if (container.getEstimatedFileSize() > 0) {
                            link.setDownloadSize(container.getEstimatedFileSize());
                        }
                        link.setLinkID(getHost() + "//" + videoID + "/" + qualityValue + "/" + container.getInternalID());
                        ret.add(link);
                    }
                }
            } else {
                final String fname = filename + "_" + qualityValue + ".mp4";
                final DownloadLink dl = createDownloadlink("http://playviddecrypted.com/" + UniqueAlltimeID.create());
                dl.setProperty("directlink", directlink);
                dl.setProperty("qualityvalue", qualityValue);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("directname", fname);
                dl.setLinkID(fname);
                dl.setFinalFileName(fname);
                dl.setContentUrl(parameter);
                dl.setLinkID(getHost() + "//" + videoID + "/" + qualityValue);
                if (SubConfiguration.getConfig("playvid.com").getBooleanProperty(FASTLINKCHECK, false)) {
                    dl.setAvailable(true);
                }
                ret.add(dl);
            }
        }
        if (ret.size() > 0) {
            return ret;
        } else {
            return null;
        }
    }

    /* Go through bot-protection. */
    private void getPage(final String url) throws Exception {
        br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 429) {
            boolean failed = true;
            for (int i = 0; i <= 2; i++) {
                final Form captchaform = br.getFormbyKey("secimgkey");
                final String secimgkey = captchaform.getInputField("secimgkey").getValue();
                final String captchaurl = "http://www." + br.getHost() + "/ccapimg?key=" + secimgkey;
                final String code = this.getCaptchaCode(captchaurl, this.param);
                captchaform.put("secimginp", code);
                br.submitForm(captchaform);
                failed = br.getHttpConnection().getResponseCode() == 429;
                if (failed) {
                    continue;
                }
                break;
            }
            if (failed) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        }
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private PluginForHost plugin = null;

    private boolean getUserLogin(final boolean force) throws Exception {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("playvid.com");
        }
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa == null) {
            logger.warning("There is no account available...");
            return false;
        }
        try {
            ((jd.plugins.hoster.PlayVidCom) plugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}