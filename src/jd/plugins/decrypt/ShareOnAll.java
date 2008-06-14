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
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ShareOnAll extends PluginForDecrypt {
    final static String host             = "shareonall.com";
    private static final String IGNORE_LIST = "IGNORE_LIST";
    private String      version          = "1.0.0.0";
    private Pattern     patternSupported = getSupportPattern("http://[*]shareonall\\.com/[+]");
    private String[] ignoreList;
 
    public ShareOnAll() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
        this.ignoreList=JDUtilities.splitByNewline(getProperties().getStringProperty(IGNORE_LIST, ""));
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

        for(String hoster:ignoreList){
            if(hoster.trim().length()>2&&link.toLowerCase().contains(hoster.toLowerCase().trim()))return false;
        }
        return true;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                parameter = parameter.replace("\\?.*", "");
                URL url = new URL("http://www.shareonall.com/showlinks.php?f=" + SimpleMatches.getFirstMatch(parameter, Pattern.compile("http://.*?shareonall.com/(.*)", Pattern.CASE_INSENSITIVE), 1));
                RequestInfo reqinfo = HTTP.getRequest(url);

                // Links herausfiltern
                ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "<a href=\'°\' target='_blank'>");
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
       // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.decrypt.shareonall.ignorelist","Liste der ignorierten Domains(domain1.com;domain2.com;...)")));

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, getProperties(), IGNORE_LIST,JDLocale.L("plugins.decrypt.shareonall.ignorelist","Liste der ignorierten Domains(Eine Domain/Zeile)")) );
    
   
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}