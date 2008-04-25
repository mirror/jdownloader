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
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;

public class XLSpreadCom extends PluginForDecrypt {
    final static String host             = "xlspread.com";

    private String      version          = "1.0.0.0";

    // http://www.xlspread.com/download.html?id=b0b18a2f966cc247660845508e1111b4
    // http://www.xlspread.com/download.html?id=61a7912765cb3d04fb98ce2e7dcbb4a4
    private Pattern     patternSupported = getSupportPattern("http://[*]xlspread.com/download.html\\?id=[a-zA-Z0-9]{32}");

    public XLSpreadCom() {
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
        return "Xlspread.com-3.0.0.";
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
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {

                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);
                int count = 0;
                ArrayList<ArrayList<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "</td></tr><tr><td><b>°</b>°downlink.php?id=°&amp;hoster");
                System.out.println(links.size());
                // Anzahl der Links zählen
                for (int i = 0; i < links.size(); i++) {
                    if ((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE", true) && links.get(i).get(0).equals("Rapidshare")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_UPLOADED", true) && links.get(i).get(0).equals("Uploaded")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_NETLOAD", true) && links.get(i).get(0).equals("Netload")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_MEINUPLOAD", true) && links.get(i).get(0).equals("MeinUpload")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_SHAREONLINE", true) && links.get(i).get(0).equals("Share-Online")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD", true) && links.get(i).get(0).equals("Simpleupload")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_BLUEHOST", true) && links.get(i).get(0).equals("Bluehost")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_FASTLOAD", true) && links.get(i).get(0).equals("Fastload")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_DATENKLO", true) && links.get(i).get(0).equals("Datenklo")) {
                        count++;
                    }
                    if ((Boolean) this.getProperties().getProperty("USE_SHAREBASE", true) && links.get(i).get(0).equals("ShareBase")) {
                        count++;
                    }
                }
                progress.setRange(count);

                if ((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("Rapidshare")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=rapidshare")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_UPLOADED", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("Uploaded")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=uploaded")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_NETLOAD", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("Netload")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=netload")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_MEINUPLOAD", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("MeinUpload")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=meinupload")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_SHAREONLINE", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("Share-Online")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=share-online")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_BLUEHOST", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("Bluehost")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=bluehost")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("Simpleupload")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=simpleupload")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_FASTLOAD", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("Fastload")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=fastload")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_DATENKLO", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("Datenklo")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=datenklo")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }
                if ((Boolean) this.getProperties().getProperty("USE_SHAREBASE", true)) {
                    for (int i = 0; i < links.size(); i++) {
                        if (links.get(i).get(0).equalsIgnoreCase("ShareBase")) {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id=" + links.get(i).get(2) + "&hoster=sharebase")).getHtmlCode(), "<iframe src=\"", "\"")));
                            progress.increase(1);
                        }
                    }
                }

                // Decrypt abschliessen
                step.setParameter(decryptedLinks);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void setConfigEelements() {

        String[] USEARRAY = new String[] { "Rapidshare.com", "Uploaded.to", "Netload.in", "MeinUpload.com", "Share-Online.biz", "SimpleUpload.net", "BlueHost.to", "Fast-load.net", "Datenklo.net", "ShareBase.de" };
        String[] USEARRAYPROPERTY = new String[] { "USE_RAPIDSHARE", "USE_UPLOADED", "USE_NETLOAD", "USE_MEINUPLOAD", "USE_SHAREONLINE", "USE_SIMPLEUPLOAD", "USE_BLUEHOST", "USE_FASTLOAD", "USE_DATENKLO", "USE_SHAREBASE" };

        ConfigEntry cfg;
        ConfigContainer hoster = null;

        int c = 0;
        int max = 6;
        for (int i = 0; i < USEARRAY.length; i++) {

            if (c == 0) {
                hoster = new ConfigContainer(this, JDLocale.L("plugins.decrypt.general.hosterSelection", "Hoster Auswahl") + " " + (i + 1) + "-" + Math.min(USEARRAY.length, (i + max)));
                config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CONTAINER, hoster));
            }

            hoster.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USEARRAYPROPERTY[i], USEARRAY[i]));
            cfg.setDefaultValue(true);
            c++;
            if (c == max) c = 0;
        }
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}