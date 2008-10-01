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

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class GigaSizeCom extends PluginForHost {

    private static final String AGB_LINK = "http://www.gigasize.com/page.php?p=terms";

    public GigaSizeCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        br.setDebug(true);
        if (br.containsHTML("versuchen gerade mehr")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l); }
        Form[] forms = br.getForms();
        if (getPluginConfig().getBooleanProperty("USE_FREE_ACCOUNT", false)) {
            Form login = forms[0];
            login.put("uname", getPluginConfig().getStringProperty("FREE_USER"));
            login.put("passwd", getPluginConfig().getStringProperty("FREE_PASS"));
            login.put("remember", "false");
            br.submitForm(login);
            if (br.containsHTML("badLogin")) {
                logger.severe("User account " + getPluginConfig().getStringProperty("FREE_USER") + "not valid.");
                getPluginConfig().setProperty("USE_FREE_ACCOUNT", false);
                getPluginConfig().save();
            }
        }
        if (forms.length < 2) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        Form captchaForm = forms[1];
        String captchaCode = getCaptchaCode("http://www.gigasize.com/randomImage.php", downloadLink);
        captchaForm.put("txtNumber", captchaCode);
        br.submitForm(captchaForm);
        Form download = br.getFormbyID("formDownload");

        dl = br.openDownload(downloadLink, download);
        if (!dl.getConnection().isContentDisposition()) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.getPage(downloadLink.getDownloadURL());

        String[] dat = br.getRegex("strong>Name</strong>: <b>(.*?)</b></p>.*?<p>Gr.*? <span>(.*?)</span>").getRow(0);

        downloadLink.setName(dat[0]);

        downloadLink.setDownloadSize(Regex.getSize(dat[1]));
        return true;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2851 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        ConfigEntry conditionEntry;
        config.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_FREE_ACCOUNT", JDLocale.L("plugins.host.gigasize.freeaccount.use", "Use Free account")).setDefaultValue(false));
        ConfigEntry ce;
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), "FREE_USER", JDLocale.L("plugins.host.gigasize.freeaccount.user", "Email")));
        ce.setEnabledCondidtion(conditionEntry, "==", true);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getPluginConfig(), "FREE_PASS", JDLocale.L("plugins.host.gigasize.freeaccount.pass", "Password")));
        ce.setEnabledCondidtion(conditionEntry, "==", true);
    }
}