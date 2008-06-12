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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class UCMS extends PluginForDecrypt {
    static private final String host = "Underground CMS";
    private String version = "1.0.0.0";

    private Pattern patternSupported = getSupportPattern("(http://[*]filefox.in/\\?id=[+])" + "|(http://[*]alphawarez.us/\\?id=[+])" + "|(http://[*]pirate-loads.com/\\?id=[+])" + "|(http://[*]fettrap.com/\\?id=[+])" + "|(http://[*]omega-music.com(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]hardcoremetal.biz/\\?id=[+])" + "|(http://[*]flashload.org/\\?id=[+])" + "|(http://[*]twin-warez.com/\\?id=[+])" + "|(http://[*]oneload.org/\\?id=[+])" + "|(http://[*]steelwarez.com/\\?id=[+])" + "|(http://[*]fullstreams.info/\\?id=[+])" + "|(http://[*]lionwarez.com/\\?id=[+])" + "|(http://[*]1dl.in/\\?id=[+])" + "|(http://[*]chrome-database.com/\\?id=[+])" + "|(http://[*]oneload.org/\\?id=[+])" + "|(http://[*]youwarez.biz/\\?id=[+])" + "|(http://[*]saugking.net/\\?id=[+])" + "|(http://[*]leetpornz.com/\\?id=[+])" + "|(http://[*]freefiles4u.com/\\?id=[+])"
            + "|(http://[*]dark-load.net/\\?id=[+])" + "|(http://[*]wrzunlimited.1gb.in/\\?id=[+])" + "|(http://[*]crimeland.de/\\?id=[+])" + "|(http://[*]get-warez.in/\\?id=[+])" + "|(http://[*]meinsound.com/\\?id=[+])" + "|(http://[*]projekt-tempel-news.de.vu/\\?id=[+])" + "|(http://[*]datensau.org/\\?id=[+])" + "|(http://[*]musik.am(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]spreaded.net(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]relfreaks.com(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]babevidz.com(/\\?id=[+]|/category/[+]/[+].html))" + "|(http://[*]serien24.com(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]porn-freaks.net(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]xxx-4-free.net(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]xxx-reactor.net(/\\?id=[+]|/download/[+]/[+].html))"
            + "|(http://[*]porn-traffic.net(/\\?id=[+]|/category/[+]/[+].html))" + "|(http://[*]chili-warez.net(/\\?id=[+]|/[+]/[+].html))" + "|(http://[*]game-freaks.net(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]isos.at(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]your-load.com(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]mov-world.net(/\\?id=[+]|/category/[+]/[+].html))" + "|(http://[*]xtreme-warez.net(/\\?id=[+]|/category/[+]/[+].html))" + "|(http://[*]sceneload.to(/\\?id=[+]|/download/[+]/[+].html))" + "|(http://[*]oxygen-warez.com(/\\?id=[+]|/category/[+]/[+].html))" + "|(http://[*]serienfreaks.to(/\\?id=[+]|/category/[+]/[+].html))" + "|(http://[*]serienfreaks.in(/\\?id=[+]|/category/[+]/[+].html))" + "|(http://[*]warez-load.com(/\\?id=[+]|/category/[+]/[+].html))" + "|(http://[*]ddl-scene.com(/\\?id=[+]|/category/[+]/[+].html))"
            + "|(http://[*]mp3king.cinipac-hosting.biz/\\?id=[+])");

    private Pattern PAT_CAPTCHA = Pattern.compile("<IMG SRC=\"/gfx/secure/");
    private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\"Zum Download\" onClick=\"if)|(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\"Download\" onClick=\"if)");

    public UCMS() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
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
        return "Underground CMS-1.0.0.";
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
                File captchaFile = null;
                String capTxt = "";
                String host = url.getHost();

                if (!host.startsWith("http")) host = "http://" + host;

                ArrayList<String> pass = getAllSimpleMatches(reqinfo.getHtmlCode(), Pattern.compile("CopyToClipboard\\(this\\)\\; return\\(false\\)\\;\">(.*?)<\\/a>", Pattern.CASE_INSENSITIVE), 1);
                if (pass.size() > 0) {
                    if (!pass.get(0).equals("n/a") && !pass.get(0).equals("-")) this.default_password.add(pass.get(0));
                }

                ArrayList<ArrayList<String>> forms = getAllSimpleMatches(reqinfo.getHtmlCode(), Pattern.compile("<FORM ACTION=\"([^\"]*)\" ENCTYPE=\"multipart/form-data\" METHOD=\"POST\" NAME=\"([^\"]*)\"(.*?)<\\/FORM>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));

                for (int i = 0; i < forms.size(); i++) {
                    if (forms.get(i).get(1).contains("download") || forms.get(i).get(1).contains("mirror")) {
                        for (int retry = 0; retry < 5; retry++) {
                            Matcher matcher = PAT_CAPTCHA.matcher(forms.get(i).get(2));

                            if (matcher.find()) {
                                if (captchaFile != null && capTxt != null) {
                                    JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                                }

                                logger.finest("Captcha Protected");
                                String captchaAdress = host + getBetween(forms.get(i).get(2), "<IMG SRC=\"", "\"");
                                captchaFile = getLocalCaptchaFile(this);
                                JDUtilities.download(captchaFile, captchaAdress);

                                capTxt = JDUtilities.getCaptcha(this, "hardcoremetal.biz", captchaFile, false);

                                String posthelp = getFormInputHidden(forms.get(i).get(2));
                                if (forms.get(i).get(0).startsWith("http")) {
                                    reqinfo = postRequest(new URL(forms.get(i).get(0)), posthelp + "&code=" + capTxt);
                                } else {
                                    reqinfo = postRequest(new URL(host + forms.get(i).get(0)), posthelp + "&code=" + capTxt);
                                }
                            } else {
                                if (captchaFile != null && capTxt != null) {
                                    JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                                }

                                Matcher matcher_no = PAT_NO_CAPTCHA.matcher(reqinfo.getHtmlCode());

                                if (matcher_no.find()) {
                                    logger.finest("Not Captcha protected");
                                    String posthelp = getFormInputHidden(forms.get(i).get(2));

                                    if (forms.get(i).get(0).startsWith("http")) {
                                        reqinfo = postRequest(new URL(forms.get(i).get(0)), posthelp);
                                    } else {
                                        reqinfo = postRequest(new URL(host + forms.get(i).get(0)), posthelp);
                                    }

                                    break;
                                }

                            }
                            if (reqinfo.containsHTML("Der Sichheitscode wurde falsch eingeben")) {
                                logger.warning("Captcha Detection failed");
                                reqinfo = getRequest(url);
                            } else {
                                break;
                            }
                            if (reqinfo.getConnection().getURL().toString().equals(host + forms.get(i).get(0))) break;
                        }
                        ArrayList<ArrayList<String>> links = null;

                        if (reqinfo.containsHTML("unescape")) {
                            links = getAllSimpleMatches(JDUtilities.htmlDecode(JDUtilities.htmlDecode(JDUtilities.htmlDecode(getBetween(reqinfo.getHtmlCode(), "unescape\\(unescape\\(\"", "\"")))), "ACTION=\"°\"");
                        } else {
                            links = getAllSimpleMatches(reqinfo.getHtmlCode(), "ACTION=\"°\"");
                        }
                        for (int j = 0; j < links.size(); j++) {
                            // System.out.println(JDUtilities.htmlDecode(links.get(j).get(0)));
                            decryptedLinks.add(this.createDownloadlink(getHttpLinkList(JDUtilities.htmlDecode(links.get(j).get(0)))));
                        }
                    }
                }

                // Decrypten abschliessen
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