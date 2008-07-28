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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RelinkUs extends PluginForDecrypt {

    final static String host = "relink.us";

    private String version = "1.0.0.0";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?relink\\.us\\/go\\.php\\?id=\\d+", Pattern.CASE_INSENSITIVE);
    
    private static final String USE_RSDF = "USE_RSDF";
    private static final String USE_CCF = "USE_CCF";
    private static final String USE_DLC = "USE_DLC";

    public RelinkUs() {
        super();
        this.setConfigEelements();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        //currentStep = steps.firstElement();
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
    public String getPluginID() {
        return "Relink.us-1.0.0.";
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
        return version;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        //if (step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = HTTP.getRequest(url);

                if (getProperties().getBooleanProperty(USE_RSDF, true)) {
                    add_relinkus_container(reqinfo, cryptedLink, "rsdf");
                }
                if (getProperties().getBooleanProperty(USE_CCF, true)) {
                    add_relinkus_container(reqinfo, cryptedLink, "ccf");
                }
                if (getProperties().getBooleanProperty(USE_DLC, true)) {
                    add_relinkus_container(reqinfo, cryptedLink, "dlc");
                }

                add_relinkus_links(reqinfo, decryptedLinks);
                ArrayList<String> more_links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), Pattern.compile("<a href=\"go\\.php\\?id\\=(\\d*)\\&seite\\=(\\d*)\">", Pattern.CASE_INSENSITIVE), 0);
                for (int i = 0; i < more_links.size(); i++) {
                    url = new URL("http://relink.us/" + JDUtilities.htmlDecode(SimpleMatches.getBetween(more_links.get(i), "<a href=\"", "\">")));
                    reqinfo = HTTP.getRequest(url);
                    add_relinkus_links(reqinfo, decryptedLinks);
                }

                //step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void add_relinkus_links(RequestInfo reqinfo, Vector<DownloadLink> decryptedLinks) throws IOException {
        ArrayList<String> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), Pattern.compile("action=\\'([^\\']*?)\\' method=\\'post\\' target=\\'\\_blank\\'", Pattern.CASE_INSENSITIVE), 1);
        progress.addToMax(links.size());
        for (int i = 0; i < links.size(); i++) {
            reqinfo = HTTP.postRequest(new URL("http://relink.us/" + JDUtilities.htmlDecode(links.get(i))), "submit=Open");
            String link = SimpleMatches.getBetween(reqinfo.getHtmlCode(), "iframe name=\"pagetext\" height=\"100%\" frameborder=\"no\" width=\"100%\" src=\"", "\"");

            if (link.contains("yourlayer")) {
                /* CHECKME: wann passiert das hier? */
                reqinfo = HTTP.getRequest(new URL(JDUtilities.htmlDecode(link)));
                link = SimpleMatches.getSimpleMatch(reqinfo.getHtmlCode(), "frameborder=\"0\" src=\"°\">", 0);
            }

            decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(link)));
            progress.increase(1);
        }
    }

    private void add_relinkus_container(RequestInfo reqinfo, String cryptedLink, String ContainerFormat) throws IOException {
        ArrayList<String> container_link = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), Pattern.compile("<a target=\"blank\" href=\\'([^\\']*?)\\'><img src=\\'images\\/" + ContainerFormat + "\\.gif\\'", Pattern.CASE_INSENSITIVE), 1);
        if (container_link.size() == 1) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + ContainerFormat);
            URL container_url = new URL("http://relink.us/" + JDUtilities.htmlDecode(container_link.get(0)));
            HTTPConnection container_con = new HTTPConnection(container_url.openConnection());
            container_con.setRequestProperty("Referer", cryptedLink);
            JDUtilities.download(container, container_con);
            JDUtilities.getController().loadContainerFile(container);
        } else
            logger.severe("Please Update RelinkUs Plugin(Container Pattern)");
    }

    private void setConfigEelements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_RSDF, JDLocale.L("plugins.decrypt.relinkus.usersdf", "Use RSDF Container")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_CCF, JDLocale.L("plugins.decrypt.relinkus.useccf", "Use CCF Container")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_DLC, JDLocale.L("plugins.decrypt.relinkus.usedlc", "Use DLC Container")).setDefaultValue(true));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}