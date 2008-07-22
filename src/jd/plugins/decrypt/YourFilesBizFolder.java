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
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class YourFilesBizFolder extends PluginForDecrypt {

    final static String host = "yourfiles.biz";
    final static String name = "yourfiles.biz Folder";
    private String version = "0.1.0";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?yourfiles\\.biz/.*/folders/[0-9]+/.+\\.html", Pattern.CASE_INSENSITIVE);

    public YourFilesBizFolder() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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
        return name;
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

                RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));

                if (reqinfo.getHtmlCode().contains("Ordner Passwort")) {

                    String url = parameter.substring(0, parameter.lastIndexOf("/") + 1) + new Regex(reqinfo.getHtmlCode(), "action\\=(folders\\.php\\?fid\\=.*)method\\=post>").getFirstMatch().trim();
                    String cookie = reqinfo.getCookie();
                    String password = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.decrypt.passwordProtected", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
                    String post = "act=login&password=" + password + "&login=Einloggen";
                    HashMap<String, String> reqinfoHeaders = new HashMap<String, String>();
                    reqinfoHeaders.put("Content-Type", "application/x-www-form-urlencoded");

                    reqinfo = HTTP.postRequest(new URL(url), cookie, parameter, reqinfoHeaders, post, false);

                    url = reqinfo.getConnection().getHeaderField("Location");
                    reqinfo = HTTP.getRequest(new URL(url), reqinfo.getCookie(), parameter, false);

                }

                ArrayList<ArrayList<String>> ids = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "href='http://yourfiles.biz/?d=°'");
                progress.setRange(ids.size());

                for (int i = 0; i < ids.size(); i++) {

                    decryptedLinks.add(this.createDownloadlink("http://yourfiles.biz/?d=" + ids.get(i).get(0)));
                    progress.increase(1);

                }

                step.setParameter(decryptedLinks);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return null;

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}