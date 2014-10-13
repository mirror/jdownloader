//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser.BrowserException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mdr.de" }, urls = { "http://mdrdecrypted\\.de/\\d+" }, flags = { 2 })
public class MdrDe extends PluginForHost {

    /** Settings stuff */
    private static final String ALLOW_SUBTITLES      = "ALLOW_SUBTITLES";
    private static final String ALLOW_BEST           = "ALLOW_BEST";
    private static final String ALLOW_720x576        = "ALLOW_720x576";
    private static final String ALLOW_960x544        = "ALLOW_960x544";
    private static final String ALLOW_640x360        = "ALLOW_640x360";
    private static final String ALLOW_512x288        = "ALLOW_512x288";
    private static final String ALLOW_480x272_higher = "ALLOW_480x272";
    private static final String ALLOW_480x272_lower  = "ALLOW_480x272";
    private static final String ALLOW_256x144        = "ALLOW_256x144";

    public MdrDe(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.mdr.de/impressum/index.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        final String mainlink = link.getStringProperty("mainlink", null);
        /* Filter old links */
        if (mainlink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        try {
            br.getPage(mainlink);
        } catch (final BrowserException eb) {
            final long response = br.getRequest().getHttpConnection().getResponseCode();
            if (response == 404 || response == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw eb;
        }
        final String filename = link.getStringProperty("plain_filename", null);
        final String plain_filesize = link.getStringProperty("plain_filesize", null);
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(plain_filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        final String dllink = link.getStringProperty("directlink", null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public String getDescription() {
        return "JDownloader's mdr Plugin helps downloading videoclips from mdr.de. You can choose between different video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_SUBTITLES, JDL.L("plugins.hoster.MdrDe.grabsubtitles", "Grab subtitles whenever possible")).setDefaultValue(false).setEnabled(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.MdrDe.best", "Load best version ONLY")).setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720x576, JDL.L("plugins.hoster.MdrDe.load256x144", "Load 720x576")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_960x544, JDL.L("plugins.hoster.MdrDe.load960x544", "Load 960x544")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_640x360, JDL.L("plugins.hoster.MdrDe.load640x360", "Load 640x360")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_512x288, JDL.L("plugins.hoster.MdrDe.load512x288", "Load 512x288")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480x272_higher, JDL.L("plugins.hoster.MdrDe.load480x272_higher", "Load 480x272 higher")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480x272_lower, JDL.L("plugins.hoster.MdrDe.load480x272_lower", "Load 480x272 lower")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_256x144, JDL.L("plugins.hoster.MdrDe.load256x144", "Load 256x144")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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