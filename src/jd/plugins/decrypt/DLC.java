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
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.GetRequest;
import jd.http.Request;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.utils.JDUtilities;

public class DLC extends PluginForDecrypt {

    final static String host = "multihost";

    private String version = "1.0.0.0";
    // http://anon0098.anonhost.biz/store/file/dlc/forcedl.php?file=Clubland.Das.Ganze.Leben.Ist.Eine.Show.German.2007.DL.PAL.REPACK.DVDR-RSG.dlc
    private Pattern patternSupported = Pattern.compile("(http|dlc|ccf|rsdf)://.*(dlc|ccf|rsdf).*", Pattern.CASE_INSENSITIVE);

    public DLC() {
        super();
        this.setConfigEelements();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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
        return "DLC";
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

        logger.info("");

        Browser br = new Browser();

        // br.setCurrentURL("http://rs-layer.com/directory-95259-c8cw68wh.dlc");
        GetRequest req = new GetRequest(parameter);
        req.setFollowRedirects(true);
        req.getHeaders().put("Referer", parameter);
        Browser.forwardCookies(req);
        try {
            req.connect();
        } catch (IOException e) {
            return null;
        }
        Browser.updateCookies(req);

        String name = this.getFileNameFormHeader(req.getHttpConnection());

        File res;
        if (name.toLowerCase().endsWith("dlc")) {
            res = JDUtilities.getResourceFile("container/DecryptDLC_" + System.currentTimeMillis() + ".dlc");
        } else if (name.toLowerCase().endsWith("rsdf")) {
            res = JDUtilities.getResourceFile("container/DecryptDLC_" + System.currentTimeMillis() + ".rsdf");
        } else if (name.toLowerCase().endsWith("ccf")) {
            res = JDUtilities.getResourceFile("container/DecryptDLC_" + System.currentTimeMillis() + ".ccf");
        } else {
            return null;
        }

        JDUtilities.download(res, req.getHttpConnection());
        JDUtilities.getController().loadContainerFile(res);
        return null;
    }

    private void setConfigEelements() {
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}