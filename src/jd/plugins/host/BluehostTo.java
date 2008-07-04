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

package jd.plugins.host;

import java.io.File;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class BluehostTo extends PluginForHost {
    // http://bluehost.to/dl=uScPWKtIN
    // http://bluehost.to/file/uScPWKtIN/rnt-cckw.r07
    // http://bluehost.to/file/uScPWKtIN/
    // http://bluehost.to/file/uScPWKtIN
    // http://bluehost.to/dl=0DEH7n9A8
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?bluehost\\.to/(dl=|file/).*", Pattern.CASE_INSENSITIVE);

    static private final String HOST = "bluehost.to";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0.1";
    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String CODER = "JD-Team";

    public BluehostTo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
        // setConfigElements();

    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {

        if (step == null) {
            logger.info("Plugin Ende erreicht.");
            return null;
        }

        logger.info("get Next Step " + step);
        String user = this.getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = this.getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);

        if (user != null && pass != null && this.getProperties().getBooleanProperty(PROPERTY_PREMIUM_USER, false)) {
            try {
                return this.doPremiumStep(step, downloadLink);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                return this.doFreeStep(step, downloadLink);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    public PluginStep doPremiumStep(PluginStep step, DownloadLink downloadLink) {
        return step;

    }

    public PluginStep doFreeStep(PluginStep step, DownloadLink downloadLink) {
        try {
            String page = null;

            Browser br = new Browser();

            br.getPage("http://bluehost.to/image/head.gif");
            br.getPage(downloadLink);
            page = br.getPage("http://bluehost.to/fileinfo/url=" + downloadLink.getDownloadURL());
            String[] dat = page.split("\\, ");
            if (dat.length != 5) {

                step.setStatus(PluginStep.STATUS_ERROR);

                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                return step;
            }
            int wait = Integer.parseInt(dat[4].trim());

            if (wait > 0) {
                while (wait > 0) {

                    wait -= 1;
                    downloadLink.setStatusText(String.format(JDLocale.L("plugin.host.bluehostto.waitforhh", "JD HappyHour in %s min"), JDUtilities.formatSeconds(wait)));
                    downloadLink.requestGuiUpdate();
                    Thread.sleep(1000);

                }

            }
            HTTPConnection con = br.openFormConnection(br.getForms()[2]);
            int length = con.getContentLength();
            downloadLink.setDownloadMax(length);
            logger.info("Filename: " + getFileNameFormHeader(con));
            if (getFileNameFormHeader(con) == null || getFileNameFormHeader(con).indexOf("?") >= 0) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                step.setParameter(20000l);
                return step;
            }
            downloadLink.setName(getFileNameFormHeader(con));

            dl = new RAFDownload(this, downloadLink, con);
            dl.setResume(false);
            dl.setChunkNum(1);

            dl.startDownload();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {

    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            String page;
            // dateiname, dateihash, dateisize, dateidownloads, zeit bis
            // happyhour
            Browser br = new Browser();
            page = br.getPage("http://bluehost.to/fileinfo/url=" + downloadLink.getDownloadURL());
            String[] dat = page.split("\\, ");
            if (dat.length != 5) return false;
            downloadLink.setName(dat[0]);
            downloadLink.setDownloadMax(Integer.parseInt(dat[2]));
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        // if
        // (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM,
        // true) &&
        // this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false))
        // {
        // return 20;
        // } else {
        return 1;
        // }
    }

    // private void setConfigElements() {
    // ConfigEntry cfg;
    // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,
    // getProperties(), PROPERTY_PREMIUM_USER,
    // JDLocale.L("plugins.hoster.rapidshare.de.premiumUser", "Premium User")));
    // cfg.setDefaultValue("Kundennummer");
    // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD,
    // getProperties(), PROPERTY_PREMIUM_PASS,
    // JDLocale.L("plugins.hoster.rapidshare.de.premiumPass", "Premium Pass")));
    // cfg.setDefaultValue("Passwort");
    // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
    // getProperties(), PROPERTY_USE_PREMIUM,
    // JDLocale.L("plugins.hoster.rapidshare.de.usePremium", "Premium Account
    // verwenden")));
    // cfg.setDefaultValue(false);
    //
    // }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://bluehost.to/agb.php";
    }
}
