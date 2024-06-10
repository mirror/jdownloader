//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.IOException;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

//Links come from a decrypter plugin
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moddb.com" }, urls = { "https?://(www\\.)?moddbdecrypted\\.com/(games|mods|engines|groups)/.*?/(addons|downloads)/[0-9a-z-]+" })
public class ModDbCom extends PluginForHost {
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("moddbdecrypted.com/", "moddb.com/"));
    }

    private final String   moddbservers  = "moddbservers";
    /* The list of server values displayed to the user */
    private final String[] servers       = new String[] { "fdcservers.net(WORLDWIDE)", "moddb.com #4(TEXAS, US)", "moddb.com #5(COLORADO, US)", "moddb.com #6(COLORADO, US)", "Mod DB #8 (CALIFORNIA, US)", "Mod DB #10 (NETHERLANDS, EU)", "Mod DB #11 (NETHERLANDS, EU)", "Mod DB #13 (NETHERLANDS, EU)" };
    private String         FDCCDNREGEX1  = "Mirror provided by FDCCDN.*?<a href=\"(.*?)\"";
    private String         FDCCDNREGEX2  = "http://www\\.fdcservers\\.net.*?<a href=\"(.*?)\"";
    private String         SERVER4REGEX  = "Mirror provided by Mod DB #4.*?<a href=\"(.*?)\"";
    private String         SERVER5REGEX  = "Mirror provided by Mod DB #5.*?<a href=\"(.*?)\"";
    private String         SERVER6REGEX  = "Mirror provided by Mod DB #6.*?<a href=\"(.*?)\"";
    private String         SERVER8REGEX  = "Mirror provided by Mod DB #8.*?<a href=\"(.*?)\"";
    private String         SERVER10REGEX = "Mirror provided by Mod DB #10.*?<a href=\"(.*?)\"";
    private String         SERVER11REGEX = "Mirror provided by Mod DB #11.*?<a href=\"(.*?)\"";
    private String         SERVER13REGEX = "Mirror provided by Mod DB #13.*?<a href=\"(.*?)\"";

    public ModDbCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("An error has occured") || br.containsHTML("The download requested could not be found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h5>Filename</h5>.*?<span class=\"summary\">(.*?)</span>").getMatch(0);
        String filesize = br.getRegex("<h5>Size</h5>.*?<span class=\"summary\">.*?\\((.*?bytes)\\)</span>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<h5>Size</h5>.*?<span class=\"summary\">(.*?)\\(").getMatch(0);
        }
        if (filename != null) {
            filename = Encoding.htmlOnlyDecode(filename).trim();
            link.setName(filename);
        } else {
            logger.warning("Failed to find filename");
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        } else {
            logger.warning("Failed to find filesize");
        }
        return AvailableStatus.TRUE;
    }

    private String findLink(int configuredServer, String singlemirrorpage) throws IOException {
        String dllink = null;
        // Try to find the link for the selected servers
        if (configuredServer == 1) {
            dllink = br.getRegex(FDCCDNREGEX1).getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex(FDCCDNREGEX2).getMatch(0);
            }
        } else if (configuredServer == 2) {
            dllink = br.getRegex(SERVER4REGEX).getMatch(0);
        } else if (configuredServer == 3) {
            dllink = br.getRegex(SERVER5REGEX).getMatch(0);
        } else if (configuredServer == 4) {
            dllink = br.getRegex(SERVER6REGEX).getMatch(0);
        } else if (configuredServer == 5) {
            dllink = br.getRegex(SERVER8REGEX).getMatch(0);
        } else if (configuredServer == 6) {
            dllink = br.getRegex(SERVER10REGEX).getMatch(0);
        } else if (configuredServer == 7) {
            dllink = br.getRegex(SERVER11REGEX).getMatch(0);
        } else if (configuredServer == 8) {
            dllink = br.getRegex(SERVER13REGEX).getMatch(0);
        }
        // Some servers aren't always available, therefore we got this check,
        // but it's basically just good for the logger
        if (dllink != null && dllink.contains("members/register")) {
            logger.info("The server you tried to use is only available for premium users, using other server...");
            dllink = null;
        }
        // If the link for the selected server couldn't be found the plugin
        // tries quite
        // many methods to get *any* other valid link
        if (dllink == null) {
            logger.warning("Something is broken here, your selected server hasn't been found so jd will try to find an alternative link");
            dllink = br.getRegex(FDCCDNREGEX1).getMatch(0);
            if (dllink != null && dllink.contains("members/register")) {
                dllink = null;
            }
            if (dllink == null) {
                dllink = br.getRegex(FDCCDNREGEX2).getMatch(0);
                if (dllink != null && dllink.contains("members/register")) {
                    dllink = null;
                }
                if (dllink == null) {
                    dllink = br.getRegex(SERVER4REGEX).getMatch(0);
                    if (dllink != null && dllink.contains("members/register")) {
                        dllink = null;
                    }
                    if (dllink == null) {
                        dllink = br.getRegex(SERVER5REGEX).getMatch(0);
                        if (dllink != null && dllink.contains("members/register")) {
                            dllink = null;
                        }
                        if (dllink == null) {
                            dllink = br.getRegex(SERVER6REGEX).getMatch(0);
                            if (dllink != null && dllink.contains("members/register")) {
                                dllink = null;
                            }
                            if (dllink == null) {
                                dllink = br.getRegex(SERVER8REGEX).getMatch(0);
                                if (dllink != null && dllink.contains("members/register")) {
                                    dllink = null;
                                }
                                if (dllink == null) {
                                    dllink = br.getRegex(SERVER10REGEX).getMatch(0);
                                    if (dllink != null && dllink.contains("members/register")) {
                                        dllink = null;
                                    }
                                    if (dllink == null) {
                                        dllink = br.getRegex(SERVER11REGEX).getMatch(0);
                                        if (dllink != null && dllink.contains("members/register")) {
                                            dllink = null;
                                        }
                                        if (dllink == null) {
                                            dllink = br.getRegex(SERVER13REGEX).getMatch(0);
                                            if (dllink != null && dllink.contains("members/register")) {
                                                dllink = null;
                                            }
                                            if (dllink == null) {
                                                dllink = br.getRegex("Mirror provided by Mod DB.*?<a href=\"(.*?)\"").getMatch(0);
                                                if (dllink != null && dllink.contains("members/register")) {
                                                    dllink = null;
                                                }
                                                if (dllink == null) {
                                                    dllink = br.getRegex("Click to <a href=\"(.*?)</a>").getMatch(0);
                                                    if (dllink != null && dllink.contains("members/register")) {
                                                        dllink = null;
                                                    }
                                                    if (dllink == null) {
                                                        if (singlemirrorpage != null) {
                                                            br.getPage("http://www.moddb.com" + singlemirrorpage);
                                                            dllink = br.getRegex("Click to <a href=\"(.*?)</a>").getMatch(0);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(/downloads/mirror/[^<>\"]*?)\"").getMatch(0);
        }
        return dllink;
    }

    @Override
    public String getAGBLink() {
        return "http://www.moddb.com/terms-of-use";
    }

    private int getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(moddbservers, -1)) {
        case 0:
            logger.fine("The server fdcserver.net is configured");
            return 1;
        case 1:
            logger.fine("The servermoddb.com #4  is configured");
            return 2;
        case 2:
            logger.fine("The server  ismoddb.com #5 configured");
            return 3;
        case 3:
            logger.fine("The server  ismoddb.com #6 configured");
            return 4;
        case 4:
            logger.fine("The server  ismoddb.com #8 configured");
            return 5;
        case 5:
            logger.fine("The server  ismoddb.com #10 configured");
            return 6;
        case 6:
            logger.fine("The server  ismoddb.com #11 configured");
            return 7;
        case 7:
            logger.fine("The server  ismoddb.com #13 configured");
            return 8;
        default:
            logger.fine("No server is configured, returning default server (fdcserver.net)");
            return 1;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        int configuredServer = getConfiguredServer();
        // Get pages with the mirrors
        String singlemirrorpage = getSinglemirrorpage(br);
        final String dllink = findLink(configuredServer, singlemirrorpage);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.info("invalid final downloadlink (dllink) ?!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // Also used by the decrypter
    public static String getSinglemirrorpage(final Browser br) throws IOException {
        // Get pages with the mirror
        String singlemirrorpage = br.getRegex("window\\.open\\(\\'(.*?)\\'").getMatch(0);
        String mirrorid = br.getRegex("id=\"downloads(\\d+)report\"").getMatch(0);
        if (mirrorid == null) {
            mirrorid = br.getRegex("siteareaid=(\\d+)\"").getMatch(0);
            if (mirrorid == null) {
                mirrorid = br.getRegex("start/(\\d+)").getMatch(0);
            }
        }
        if (mirrorid != null) {
            br.getPage("https://www.moddb.com/downloads/start/" + mirrorid + "/all");
        } else {
            String standardmirrorpage = br.getRegex("attr\\(\"href\", \"(.*?)\\&amp;").getMatch(0);
            if (standardmirrorpage != null) {
                br.getPage("http://www.moddb.com" + standardmirrorpage);
            }
        }
        return singlemirrorpage;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), moddbservers, servers, JDL.L("plugins.hoster.L4dMapsCom.servers", "Use this server:")).setDefaultValue(0));
    }
}