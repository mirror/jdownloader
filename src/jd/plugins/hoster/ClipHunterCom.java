//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cliphunter.com" }, urls = { "http://cliphunterdecrypted\\.com/\\d+" })
public class ClipHunterCom extends PluginForHost {

    private String dllink = null;

    public ClipHunterCom(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String    ALLOW_BEST    = "ALLOW_BEST";
    private static final String    ALLOW_360P    = "ALLOW_360P";
    private static final String    ALLOW_360PFLV = "ALLOW_360PFLV";
    private static final String    ALLOW_480P    = "ALLOW_480P";
    private static final String    ALLOW_540P    = "ALLOW_540P";
    private static final String    ALLOW_720P    = "ALLOW_720P";
    private static final String    ALLOW_1080P   = "ALLOW_1080P";
    private static final String    FASTLINKCHECK = "FASTLINKCHECK";
    /**
     * sync with decrypter
     */
    public static final String[][] qualities     = jd.plugins.decrypter.ClipHunterComDecrypt.qualities;

    @Override
    public String getAGBLink() {
        return "http://www.cliphunter.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("cliphunter.com", "qchange", "h");
        if (downloadLink.getBooleanProperty("offline")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = downloadLink.getStringProperty("directlink");

        if (!linkOk(downloadLink)) {
            br.getPage(downloadLink.getStringProperty("originallink"));
            if (br.getURL().contains("error/missing") || br.containsHTML("(>Ooops, This Video is not available|>This video was removed and is no longer available at our site|<title></title>)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final LinkedHashMap<String, String> foundQualities = findAvailableVideoQualities(this.br);
            if (foundQualities == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String selectedQuality = downloadLink.getStringProperty("selectedquality");
            dllink = foundQualities.get(selectedQuality);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!linkOk(downloadLink)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private boolean linkOk(final DownloadLink dl) throws Exception {
        boolean linkOk = false;
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (con.getResponseCode() == 200 && !con.getContentType().contains("html")) {
                dl.setDownloadSize(con.getLongContentLength());
                linkOk = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return linkOk;
    }

    public static String decryptUrl(final String fun, final String value) {
        Object result = new Object();
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(fun);
            result = inv.invokeFunction("decrypt", value);
        } catch (final Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

    /**
     * Same function in hoster and decrypterplugin, sync it!!
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, String> findAvailableVideoQualities(final Browser br) throws Exception {
        // parse decryptalgo
        final String jsUrl = br.getRegex("<script.*?src=\"(https?://\\S+gexo\\S+player[_a-z\\d]+\\.js)\"").getMatch(0);
        final String[] encryptedUrls = br.getRegex("\"url\":\"([^<>\"]*?)\"").getColumn(0);
        final String json_full = br.getRegex("var gexoFiles = (\\{.+\\});").getMatch(0);
        if (jsUrl == null || ((encryptedUrls == null || encryptedUrls.length == 0) && json_full == null)) {
            if (!br.containsHTML("var flashVars")) {
                /* Offline / Player missing */
                return new LinkedHashMap<String, String>();
            }
            return null;
        }
        final Browser br2 = br.cloneBrowser();
        br2.getPage(jsUrl);
        String decryptAlgo = new Regex(br2, "decrypt\\:\\s?function(.*?\\})(,|;)").getMatch(0);
        if (decryptAlgo == null) {
            return null;
        }
        // Find available links
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        decryptAlgo = "function decrypt" + decryptAlgo + ";";
        String currentSr, tmpUrl, ext;
        if (json_full != null) {
            /* 2016-03-30: New json handling */
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json_full);
            final boolean has_only_one_entry = entries.size() == 1;
            LinkedHashMap<String, Object> videoinfo = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json_full);
            for (final Map.Entry<String, Object> videoQualityEntry : entries.entrySet()) {
                final String videoname = videoQualityEntry.getKey();
                videoinfo = (LinkedHashMap<String, Object>) videoQualityEntry.getValue();
                ext = (String) videoinfo.get("fmt");
                final String url = (String) videoinfo.get("url");
                tmpUrl = decryptUrl(decryptAlgo, url);
                currentSr = new Regex(videoname, "(_[A-Za-z0-9]+\\.(?:mp4|flv))$").getMatch(0);
                if (currentSr == null || tmpUrl == null) {
                    continue;
                }
                boolean foundKnownQuality = false;
                for (final String quality[] : qualities) {
                    if (quality.length >= 3) {
                        if (currentSr.contains(quality[1]) || currentSr.contains(quality[2])) {
                            foundQualities.put(quality[0], tmpUrl);
                            foundKnownQuality = true;
                            break;
                        }

                    } else {
                        if (currentSr.contains(quality[0])) {
                            foundQualities.put(quality[0], tmpUrl);
                            foundKnownQuality = true;
                            break;
                        }
                    }
                }
                if (!foundKnownQuality && has_only_one_entry) {
                    foundQualities.put(currentSr, tmpUrl);
                }
            }
        } else {
            /* Keep this as a fallback as we never know what they change next! */
            for (final String s : encryptedUrls) {
                tmpUrl = decryptUrl(decryptAlgo, s);
                currentSr = new Regex(tmpUrl, "sr=(\\d+)").getMatch(0);
                if (currentSr == null) {
                    continue;
                }
                boolean foundFittingQuality = false;
                for (final String quality[] : qualities) {
                    /* Now go through the different check-strings of that particular quality */
                    for (int counter = 1; counter <= quality.length - 1; counter++) {
                        final String checkString = quality[counter];
                        if (tmpUrl.contains(checkString)) {
                            foundQualities.put(quality[0], tmpUrl);
                            foundFittingQuality = true;
                            break;
                        }
                    }
                    if (foundFittingQuality) {
                        break;
                    }

                }
            }
        }
        return foundQualities;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "JDownloader's Cliphunter Plugin helps downloading videoclips from cliphunter.com. Cliphunter provides different video formats and qualities.";
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, JDL.L("plugins.hoster.cliphuntercom.fastLinkcheck", "Fast linkcheck for video links (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.cliphuntercom.checkbest", "Only grab the best available resolution")).setDefaultValue(false);
        getConfig().addEntry(hq);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_360P, JDL.L("plugins.hoster.cliphuntercom.check360mp4", "Grab low (i) (360p mp4)?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_360PFLV, JDL.L("plugins.hoster.cliphuntercom.check360flv", "Grab medium (l) (360p flv)?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480P, JDL.L("plugins.hoster.cliphuntercom.check480mp4", "Grab medium (p) (480p mp4)?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_540P, JDL.L("plugins.hoster.cliphuntercom.check540flv", "Grab high (h) (540p flv)?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720P, JDL.L("plugins.hoster.cliphuntercom.check720mp4", "Grab HD (hd) (720p mp4)?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080P, JDL.L("plugins.hoster.cliphuntercom.check1080mp4", "Grab Full HD (fhd) (1080p mp4)?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
