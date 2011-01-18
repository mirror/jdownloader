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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshack.com" }, urls = { "http://[\\w\\.]*?fileshack\\.com/(file|file_download)\\.x/[0-9]+" }, flags = { 2 })
public class FilesHackCom extends PluginForHost {

    private static final String fileshackservers = "fileshackservers";

    /** The list of servers displayed in the plugin configuration pane */
    private static final String[] FILESHACK_SERVERS = new String[] { "Public Central USA", "Public Eastern USA", "Public Europe", "Public Western USA" };

    /**
     * The number of the default server [used if no server is configured or if
     * the configured server was not found in the list of obtained servers]
     */
    private static final int DEFAULT_SERVER_NUMBER = 2;

    /**
     * The name of the default server [used if no server is configured or if the
     * configured server was not found in the list of obtained servers]
     */
    private static final String DEFAULT_SERVER_NAME = FILESHACK_SERVERS[DEFAULT_SERVER_NUMBER];

    /** The {@link Pattern} used to get the server strings from the HTML page */
    private static final Pattern SERVERS_STRINGS_PATTERN = Pattern.compile("'(/popup\\..*?(central|east|europe|west)\\.public.*?pay=0)'");

    public FilesHackCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileshack.com/extras/tos.x";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(file\\.|file_download\\.)", "file\\."));
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), fileshackservers, FILESHACK_SERVERS, JDL.L("plugins.host.FilesHackCom.servers", "Use this server:")).setDefaultValue(0));
    }

    // thx to jiaz & bogdan.solga for the help ;)
    private int getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(fileshackservers, -1)) {
        case 0:
            logger.fine("The server " + FILESHACK_SERVERS[0] + " is configured");
            return 0;
        case 1:
            logger.fine("The server " + FILESHACK_SERVERS[1] + " is configured");
            return 1;
        case 2:
            logger.fine("The server " + FILESHACK_SERVERS[2] + " is configured");
            return 2;
        case 3:
            logger.fine("The server " + FILESHACK_SERVERS[3] + " is configured");
            return 3;
        default:
            logger.fine("No server is configured, returning default server [" + DEFAULT_SERVER_NAME + "]");
            return DEFAULT_SERVER_NUMBER;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File was deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<dt>Filename</dt>.*?<dd title=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("<dt>Size</dt>.*?<dd>.*?\\((.*?)\\)</dd>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replace(",", "");
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setDebug(true);
        String dlink = link.getDownloadURL();
        String fid = new Regex(dlink, "fileshack\\.com/file\\.x/(\\d+)").getMatch(0);
        dlink = "http://www.fileshack.com/login.x?fid=" + fid;
        br.getPage(dlink);
        Form form = br.getFormbyProperty("name", "nologinform");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(form);
        if (br.containsHTML("FileShack Age Verification")) {
            Form ageform = br.getForm(2);
            if (ageform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            ageform.put("birth_month", "11");
            ageform.put("birth_day", "15");
            ageform.put("birth_year", "1970");
            br.submitForm(ageform);
        }

        String[] strings = br.getRegex(SERVERS_STRINGS_PATTERN).getColumn(0);
        if (strings.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        List<String> serversList = Arrays.asList(strings);
        if (serversList.size() > 1) Collections.sort(serversList);

        int configuredServerNumber = getConfiguredServer();
        int numberOfObtainedServerStrings = serversList.size();
        if (configuredServerNumber >= numberOfObtainedServerStrings) {
            String how = configuredServerNumber == numberOfObtainedServerStrings ? "equal to" : "bigger than";
            logger.warning("The configured server number [" + configuredServerNumber + "] is " + how + " the number of obtained server strings [" + numberOfObtainedServerStrings + "]");

            if (numberOfObtainedServerStrings > 1) {
                String identifier = DEFAULT_SERVER_NAME.replace("Public", "").replace("USA", "");
                identifier = identifier.toLowerCase().trim();

                // verify if the configured default server was found in the page
                int which = -1;
                for (String server : serversList) {
                    if (server.indexOf(identifier) > 0) {
                        which = serversList.indexOf(server);
                        logger.info("Using default server [" + DEFAULT_SERVER_NAME + "]");
                        configuredServerNumber = which;
                        break;
                    }
                }

                if (which == -1) {
                    logger.info("The default server [" + DEFAULT_SERVER_NAME + "] was not found in the list of obtained servers, using the first server from the list of obtained servers");
                    configuredServerNumber = 0;
                }
            } else {
                logger.warning("There is only one server string obtained, using it...");
                configuredServerNumber = 0;
            }
        }

        String usedString = serversList.get(configuredServerNumber);
        if (usedString == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        logger.fine("Using server string '" + usedString + "'");
        br.getPage("http://www.fileshack.com" + usedString);
        String frameserver = new Regex(br.getURL(), "(http://[a-z]+\\.[a-z0-9]+\\.fileshack\\.com)").getMatch(0);
        if (frameserver == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dlframe = br.getRegex("frameborder=\"[0-9]\" src=\"(.*?)\"").getMatch(0);
        if (dlframe == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dlframe = frameserver + dlframe;
        br.getPage(dlframe);
        String nextframe = br.getRegex("\"><a href=\"(.*?)\"").getMatch(0);
        if (nextframe == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        nextframe = frameserver + nextframe;
        sleep(6000l, link);
        br.getPage(nextframe);
        String dllink = br.getRegex("downloadbutton\"><a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0).startDownload();
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
