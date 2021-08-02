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
package jd.plugins.hoster;

import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "definebabe.com", "definefetish.com" }, urls = { "definebabedecrypted://(?:www\\.)?definebabes?\\.com/video/[a-z0-9]+/[a-z0-9\\-]+/", "http://(www\\.)?definefetish\\.com/video/[a-z0-9]+/[a-z0-9\\-]+/" })
public class DefineBabeCom extends PluginForHost {
    public DefineBabeCom(PluginWrapper wrapper) {
        super(wrapper);
        /* Don't overload the server. */
        this.setStartIntervall(3 * 1000l);
    }

    /* Tags: TubeContext@Player */
    /* Sites using the same player: definebabes.com, definebabe.com, definefetish.com */
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://www.definebabe.com/about/privacy/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        /* Definebabes.com simply redirecty to definebabe.com */
        link.setUrlDownload(link.getDownloadURL().replace("definebabes.com/", "definebabe.com/").replace("definebabedecrypted://", "http://"));
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Please, call later\\.")) {
            link.getLinkStatus().setStatusText("Server is busy");
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = br.getRegex("<strong style=\"font:bold 18px Verdana;\">([^<>]*?)</strong>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
        }
        String videoID = br.getRegex("video_id=(\\d+)").getMatch(0);
        if (videoID == null) {
            videoID = br.getRegex("id=\\'comment_object_id\\' value=\"(\\d+)\"").getMatch(0);
        }
        if (videoID == null) {
            /* 2021-07-26 */
            videoID = br.getRegex("'video_id'\\s*:\\s*(\\d+)").getMatch(0);
        }
        if (videoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Set fallback-filename */
        if (!link.isNameSet()) {
            link.setName(videoID + ".mp4");
        }
        link.setLinkID(this.getHost() + "://" + videoID);
        final boolean useNewAPI = true;
        if (useNewAPI) {
            br.getPage("/player/config.php?id=" + videoID);
            try {
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                this.dllink = (String) entries.get("video_url");
            } catch (final Throwable e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Broken video?", e);
            }
        } else {
            br.getPage("http://www." + link.getHost() + "/playlist/playlist.php?type=regular&video_id=" + videoID);
            final String decrypted = decryptRC4HexString("TubeContext@Player", br.toString().trim());
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(decrypted);
            final Map<String, Object> videos = (Map<String, Object>) entries.get("videos");
            /* Usually only 360 is available */
            final String[] qualities = { "1080p", "720p", "480p", "360p", "320p", "240p", "180p" };
            for (final String currentqual : qualities) {
                final Map<String, Object> quality_info = (Map<String, Object>) videos.get("_" + currentqual);
                if (quality_info != null) {
                    dllink = (String) quality_info.get("fileUrl");
                    break;
                }
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim() + ".mp4";
        link.setFinalFileName(filename);
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openGetConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    server_issues = true;
                }
                link.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) {
        return (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Error with fetching video"));
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("Please, call later\\.")) {
            link.getLinkStatus().setStatusText("Server is busy");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is busy", 5 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    /**
     * @author makaveli
     * @throws Exception
     */
    private static String decryptRC4HexString(final String plainTextKey, final String hexStringCiphertext) throws Exception {
        String ret = "";
        try {
            Cipher rc4 = Cipher.getInstance("RC4");
            rc4.init(Cipher.DECRYPT_MODE, new SecretKeySpec(plainTextKey.getBytes(), "RC4"));
            ret = new String(rc4.doFinal(HexFormatter.hexToByteArray(hexStringCiphertext)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            if (e.getMessage().contains("Illegal key size")) {
                getPolicyFiles();
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unlimited Strength JCE Policy Files needed!");
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @SuppressWarnings("deprecation")
    private static void getPolicyFiles() throws Exception {
        int ret = -100;
        UserIO.setCountdownTime(120);
        ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_LARGE, "Java Cryptography Extension (JCE) Error: 32 Byte keylength is not supported!", "At the moment your Java version only supports a maximum keylength of 16 Bytes but the keezmovies plugin needs support for 32 byte keys.\r\nFor such a case Java offers so called \"Policy Files\" which increase the keylength to 32 bytes. You have to copy them to your Java-Home-Directory to do this!\r\nExample path: \"jre6\\lib\\security\\\". The path is different for older Java versions so you might have to adapt it.\r\n\r\nMake sure to download the files that match your current Java version!\r\n\r\nBy clicking on CONFIRM a browser instance will open which leads to the downloadpage of the file.\r\n\r\nThanks for your understanding.", null, "CONFIRM", "Cancel");
        if (ret != -100) {
            if (UserIO.isOK(ret)) {
                LocalBrowser.openDefaultURL(new URL("http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html"));
                LocalBrowser.openDefaultURL(new URL("http://h10.abload.de/img/jcedp50.png"));
            } else {
                return;
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
