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
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.DownloadLink;

public class ShareOnAll extends PluginForDecrypt {
    final static String host             = "shareonall.com";
    private String      version          = "1.0.0.0";
    private Pattern     patternSupported = getSupportPattern("http://[*]shareonall\\.com/[+]");
    private String[][]  conf             = new String[][] { { "USE_RAPIDSHARE", "Rapidshare.com" }, { "USE_FILEFACTORY", "Filefactory.com" }, { "USE_MEGAUPLOAD", "Megaupload.com" }, { "USE_DEPOSITFILES", "DepositFiles.com" }, { "USE_DIVSHARE", "DivShare.com" }, { "USE_ZSHARE", "ZShare.net" } };

    public ShareOnAll() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
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
        return "ShareOnAll.com-1.0.0.";
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

    private boolean checkLink(String link) {

        for (int i = 0; i < conf.length; i++) {
            if ((Boolean) this.getProperties().getProperty(conf[i][0], true)) {
                Pattern pattern = Pattern.compile(".*" + conf[i][1] + ".*", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(link);
                if (matcher.find()) return true;
            }

        }
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                parameter = parameter.replace("\\?.*", "");
                URL url = new URL("http://www.shareonall.com/showlinks.php?f=" + getFirstMatch(parameter, Pattern.compile("http://.*?shareonall.com/(.*)", Pattern.CASE_INSENSITIVE), 1));
                RequestInfo reqinfo = getRequest(url);

                // Links herausfiltern
                ArrayList<ArrayList<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "<a href=\'°\' target='_blank'>");
                progress.setRange(links.size());
                for (int i = 0; i < links.size(); i++) {
                    if (checkLink(links.get(i).get(0))) decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
                    progress.increase(1);
                }

                // Decrypt abschliessen
                // veraltet: firePluginEvent(new PluginEvent(this,
                // PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void setConfigEelements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        for (int i = 1; i < conf.length; i++) {
            config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), conf[i][0], conf[i][1]));
            cfg.setDefaultValue(false);
        }
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}