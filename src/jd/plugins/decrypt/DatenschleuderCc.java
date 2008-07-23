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

public class DatenschleuderCc extends PluginForDecrypt {

    final static String host = "datenschleuder.cc";
    private String version = "0.2.0";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?datenschleuder\\.cc/dl/(id|dir)/[0-9]+/[a-zA-Z0-9]+/.+", Pattern.CASE_INSENSITIVE);

    private static final String[] USEARRAY = new String[] { "Rapidshare.com", "Netload.in", "Uploaded.to", "Datenklo.net", "Share.Gulli.com", "Archiv.to", "Bluehost.to", "Share-Online.biz", "Speedshare.org" };

    public DatenschleuderCc() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }

    @Override
    public String getCoder() {
        return "jD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
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

    private boolean getUseConfig(String link) {

        if (link == null) return false;
        link = link.toLowerCase();

        for (int i = 0; i < USEARRAY.length; i++) {

            if (link.contains(USEARRAY[i].toLowerCase())) { return getProperties().getBooleanProperty(USEARRAY[i], true); }

        }

        return false;

    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {

        if (step.getStep() == PluginStep.STEP_DECRYPT) {

            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();

            try {

                RequestInfo reqinfo = HTTP.getRequest(new URL(parameter), null, null, true);
                ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "<a href=\"http://www.datenschleuder.cc/redir.php?id=°\"");

                progress.setRange(links.size());

                for (int i = 0; i < links.size(); i++) {

                    reqinfo = HTTP.getRequest(new URL("http://www.datenschleuder.cc/redir.php?id=" + links.get(i).get(0)));
                    String link = SimpleMatches.getBetween(reqinfo.getHtmlCode(), "<frame src=\"", "\" name=\"dl\">");
                    link = link.replace("http://anonym.to?", "");
                    progress.increase(1);

                    if (getUseConfig(link)) decryptedLinks.add(createDownloadlink(link));

                }

                logger.info(decryptedLinks.size() + " " + JDLocale.L("plugins.decrypt.general.downloadsDecrypted", "Downloads entschlüsselt"));
                step.setParameter(decryptedLinks);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return null;

    }

    private void setConfigEelements() {

        ConfigEntry cfg;

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.decrypt.general.hosterSelection", "Hoster Auswahl")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        for (int i = 0; i < USEARRAY.length; i++) {

            config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USEARRAY[i], USEARRAY[i]));
            cfg.setDefaultValue(true);

        }

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}