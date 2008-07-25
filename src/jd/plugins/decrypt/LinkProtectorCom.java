//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de

//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.

//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class LinkProtectorCom extends PluginForDecrypt {
    private static final String host = "link-protector.com";
    private static final String version = "1.0.0.0";

    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?link-protector\\.com/[\\d]{6}.*", Pattern.CASE_INSENSITIVE);

    public LinkProtectorCom() {
        super();
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

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            try {
                Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();

                URL url = new URL(parameter);
                RequestInfo requestInfo = HTTP.getRequest(url);
                boolean do_continue = false;
                String passCode = null;
                String referrer = null;
                for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                    if (requestInfo.containsHTML("Bad Referrer!")) {
                        referrer = new Regex(requestInfo.getHtmlCode(), Pattern.compile("Site below:<br><a href=(.*?)>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                        requestInfo = HTTP.getRequest(url, null, referrer, false);
                    } else if (requestInfo.containsHTML("<h1>PASSWORD PROTECTED LINK</h1>") || requestInfo.containsHTML("Incorrect Password")) {
                        if ((passCode = JDUtilities.getGUI().showUserInputDialog("Code?")) == null) {
                            break;
                        }
                        requestInfo = HTTP.postRequest(url, null, referrer, null, "u_name=user&u_password=" + JDUtilities.urlEncode(passCode), false);
                    } else {
                        do_continue = true;
                        break;
                    }
                }
                if (do_continue == true) {
                    String cryptedLink = new Regex(requestInfo.getHtmlCode(), Pattern.compile("write\\(stream\\('(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                    int charCode = Integer.parseInt(new Regex(requestInfo.getHtmlCode(), Pattern.compile("fromCharCode\\(yy\\[i\\]-(.*?)\\)\\;", Pattern.CASE_INSENSITIVE)).getFirstMatch());
                    String decryptedLink = decryptCode(cryptedLink, charCode);
                    String link = new Regex(decryptedLink, Pattern.compile("<iframe src=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getFirstMatch().trim();
                    decryptedLinks.add(this.createDownloadlink(link));
                }
                step.setParameter(decryptedLinks);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String decryptCode(String decryptedLink, int charCode) {
        String result = "";
        try {
            for (int i = 0; i * 4 < decryptedLink.length(); i++) {
                result = (char) (Integer.parseInt(decryptedLink.substring(i * 4, i * 4 + 4)) - charCode) + result;
            }
        } catch (Exception e) {
            result = "";
        }
        return result;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}
