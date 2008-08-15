//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RelinkUs extends PluginForDecrypt {

    final static String host = "relink.us";

    private static final String USE_CCF = "USE_CCF";

    private static final String USE_DLC = "USE_DLC";

    private static final String USE_RSDF = "USE_RSDF";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?relink\\.us\\/go\\.php\\?id=\\d+", Pattern.CASE_INSENSITIVE);

    public RelinkUs() {
        super();
        setConfigElements();
    }

    private void add_relinkus_container(RequestInfo reqinfo, String cryptedLink, String ContainerFormat) throws IOException {
        String container_link = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<a target=\"blank\" href=\\'([^\\']*?)\\'><img src=\\'images\\/" + ContainerFormat + "\\.gif\\'", Pattern.CASE_INSENSITIVE)).getFirstMatch(1);
        if (container_link != null) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + ContainerFormat);
            URL container_url = new URL("http://relink.us/" + Encoding.htmlDecode(container_link));
            HTTPConnection container_con = new HTTPConnection(container_url.openConnection());
            container_con.setRequestProperty("Referer", cryptedLink);
            Browser.download(container, container_con);
            JDUtilities.getController().loadContainerFile(container);
        }
    }

    private void add_relinkus_links(RequestInfo reqinfo, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String links[] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("action=\\'([^\\']*?)\\' method=\\'post\\' target=\\'\\_blank\\'", Pattern.CASE_INSENSITIVE)).getMatches(1);
        progress.addToMax(links.length);
        for (String link : links) {
            reqinfo = HTTP.postRequest(new URL("http://relink.us/" + Encoding.htmlDecode(link)), "submit=Open");
            String dl_link = new Regex(reqinfo.getHtmlCode(), "iframe name=\"pagetext\" height=\"100%\" frameborder=\"no\" width=\"100%\" src=\"\n?(.*?)\"", Pattern.CASE_INSENSITIVE).getFirstMatch();
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(dl_link)));
            progress.increase(1);
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);

            if (getPluginConfig().getBooleanProperty(USE_RSDF, true)) {
                add_relinkus_container(reqinfo, cryptedLink, "rsdf");
            }
            if (getPluginConfig().getBooleanProperty(USE_CCF, true)) {
                add_relinkus_container(reqinfo, cryptedLink, "ccf");
            }
            if (getPluginConfig().getBooleanProperty(USE_DLC, true)) {
                add_relinkus_container(reqinfo, cryptedLink, "dlc");
            }

            add_relinkus_links(reqinfo, decryptedLinks);
            String more_links[] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<a href=\"(go\\.php\\?id=\\d+\\&seite=\\d+)\">", Pattern.CASE_INSENSITIVE)).getMatches(1);
            for (String link : more_links) {
                url = new URL("http://relink.us/" + link);
                reqinfo = HTTP.getRequest(url);
                add_relinkus_links(reqinfo, decryptedLinks);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_RSDF, JDLocale.L("plugins.decrypt.relinkus.usersdf", "Use RSDF Container")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_CCF, JDLocale.L("plugins.decrypt.relinkus.useccf", "Use CCF Container")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_DLC, JDLocale.L("plugins.decrypt.relinkus.usedlc", "Use DLC Container")).setDefaultValue(true));
    }
}