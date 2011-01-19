//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {}, flags = {})
public class CMS extends PluginForDecrypt {
    public static final String[] ANNOTATION_NAMES = new String[] { "romhood.com", "turk-crew.com", "uwarez.ws", "oxygen-warez.com", "filefox.in", "pirate-loads.com", "fettrap.com", "omega-music.com", "hardcoremetal.biz", "flashload.org", "oneload.org", "1dl.in", "oneload.org", "saugking.net", "dark-load.net", "crimeland.de", "musik.am", "spreaded.net", "relfreaks.com", "xxx-4-free.net", "porn-traffic.net", "chili-warez.net", "game-freaks.net", "sceneload.to", "epicspeedload.in", "serienfreaks.to", "serienfreaks.in", "warez-load.com", "ddl-scene.com", "ddl-base.ws", "sauggirls.com", "pornfox.in", "xflat24.com", "gamegalaxy.ws", "ddl.byte.to", "jokermovie.org", "top-hitz.com", "sound-load.com", "toxic.to", "sound-load.net", "sfulc.exofire.net/cms", "dream-team.bz/cms", "titanload.to", "gate-warez.com", "hot-porn-ddl.com", "defrap.org", "defrap.biz", "dream.loadz.biz", "ebook-hell.me" };

    /**
     * Returns the annotations names array
     */
    public static String[] getAnnotationNames() {
        return ANNOTATION_NAMES;
    }

    /**
     * Returns the annotation pattern array
     */
    public static String[] getAnnotationUrls() {
        String[] names = getAnnotationNames();

        String[] ret = new String[names.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = "http://[\\w\\.]*?" + names[i].replaceAll("\\.", "\\\\.") + "/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html)";
        }
        return ret;
    }

    /**
     * Returns the annotations flags array
     */
    public static int[] getAnnotationFlags() {
        String[] names = getAnnotationNames();

        int[] ret = new int[names.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = 0;
        }
        return ret;
    }

    private Pattern PAT_CAPTCHA    = Pattern.compile("<IMG SRC=\".*?/gfx/secure/", Pattern.CASE_INSENSITIVE);

    private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\".*?Download.*?\".*?Click)", Pattern.CASE_INSENSITIVE);

    public CMS(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        try {
            br.getPage(parameter);
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            String capTxt = "";
            String host = br.getHost();

            if (!host.startsWith("http")) {
                host = "http://" + host;
            }
            String pass = br.getRegex(Pattern.compile("CopyToClipboard\\(this\\)\\; return\\(false\\)\\;\">(.*?)<\\/a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (pass == null) pass = br.getRegex("<B>Passwort:</B> <input value=\"(.*?)\".*?<").getMatch(0);

            if (pass == null) pass = br.getRegex("<p><b>Passwort:</b>\\s*(.*?)\\s*</p>").getMatch(0);
            if (pass == null) pass = br.getRegex("<dt class=\"\">Passwort:</dt>.*?<dd class=\"\">(.*?)</dd>").getMatch(0);
            if (pass != null) {
                if (pass.equals("keins ben&ouml;tigt") || pass.equals("kein pw") || pass.equals("N/A") || pass.equals("n/a") || pass.equals("-") || pass.equals("-kein Passwort-") || pass.equals("-No Pass-") || pass.equals("ohne PW")) {
                    pass = null;
                }
            }

            String forms[][] = br.getRegex(Pattern.compile("<FORM ACTION=\"([^\"]*)\" ENCTYPE=\"multipart/form-data\" METHOD=\"POST\" NAME=\"(mirror|download)[^\"]*\"(.*?)</FORM>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
            if (forms.length != 0) {
                for (String[] element : forms) {
                    for (int retry = 0; retry < 5; retry++) {
                        Matcher matcher = PAT_CAPTCHA.matcher(element[2]);
                        if (matcher.find()) {
                            logger.finest("Captcha Protected");
                            String captchaAdress = host + new Regex(element[2], Pattern.compile("<IMG SRC=\"(/.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            capTxt = getCaptchaCode("ucms", captchaAdress, param);

                            String posthelp = HTMLParser.getFormInputHidden(element[2]);
                            if (element[0].startsWith("http")) {
                                br.postPage(element[0], posthelp + "&code=" + capTxt);
                            } else {
                                br.postPage(host + element[0], posthelp + "&code=" + capTxt);
                            }
                        } else {
                            Matcher matcher_no = PAT_NO_CAPTCHA.matcher(element[2]);
                            if (matcher_no.find()) {
                                logger.finest("Not Captcha protected");
                                String posthelp = HTMLParser.getFormInputHidden(element[2]);
                                if (element[0].startsWith("http")) {
                                    br.postPage(element[0], posthelp);
                                } else {
                                    br.postPage(host + element[0], posthelp);
                                }
                                break;
                            }
                        }
                        if (br.containsHTML("Der Sichheitscode wurde falsch eingeben")) {
                            logger.warning("Captcha Detection failed");
                            br.getPage(parameter);
                        } else {
                            break;
                        }
                        if (br.getHttpConnection().getURL().toString().equals(host + element[0])) {
                            break;
                        }
                    }
                    /*
                     * Bei hardcoremetal.biz wird mittlerweile der Download als
                     * DLC-Container angeboten! Workaround für diese Seite
                     */
                    if (br.containsHTML("ACTION=\"/download\\.php\"")) {
                        Form forms2[] = br.getForms();
                        for (Form form : forms2) {
                            if (form.containsHTML("dlc")) {
                                File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                                Browser.download(container, br.openFormConnection(form));
                                decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
                                break;
                            }
                        }
                    } else {
                        String links[] = null;
                        if (br.containsHTML("unescape\\(unescape\\(unescape")) {
                            String temp = br.getRegex(Pattern.compile("unescape\\(unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            String temp2 = Encoding.htmlDecode(Encoding.htmlDecode(Encoding.htmlDecode(temp)));
                            links = new Regex(temp2, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        } else if (br.containsHTML("unescape\\(unescape")) {
                            String temp = br.getRegex(Pattern.compile("unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            String temp2 = Encoding.htmlDecode(Encoding.htmlDecode(temp));
                            links = new Regex(temp2, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        } else {
                            links = br.getRegex(Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        }
                        for (String element2 : links) {
                            DownloadLink link = createDownloadlink(Encoding.htmlDecode(element2));
                            link.addSourcePluginPassword(pass);
                            decryptedLinks.add(link);
                        }
                    }
                }
            } else {
                /* workaround for java ucms */
                String[] forms2 = br.getRegex("document.writeln\\('(<form.*?'</form>)").getColumn(0);
                ArrayList<Form> forms3 = new ArrayList<Form>();
                for (String form : forms2) {
                    String temp = form.replaceAll("(document\\.writeln\\('|'\\);)", "");
                    Form tform = new Form(temp);
                    tform.setAction(param.getCryptedUrl());
                    tform.remove(null);
                    tform.remove(null);
                    forms3.add(tform);
                }
                boolean cont = false;
                Browser brc = null;
                for (Form tform : forms3) {
                    for (int retry = 0; retry < 5; retry++) {
                        brc = br.cloneBrowser();
                        cont = false;
                        if (tform.containsHTML("<img src=")) {
                            logger.finest("Captcha Protected");
                            String captchaAdress = host + tform.getRegex(Pattern.compile("<img src=\"(/captcha/.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            capTxt = getCaptchaCode("ucms", captchaAdress, param);

                            tform.put("code", capTxt);
                            brc.submitForm(tform);
                        } else {
                            logger.finest("Not Captcha protected");
                            brc.submitForm(tform);
                        }
                        if (brc.containsHTML("CMS.RELEASE")) {
                            cont = true;
                            break;
                        }
                    }
                    if (cont) {
                        String[] links2 = brc.getRegex("href=\\\\\"(.*?)\\\\\"").getColumn(0);
                        for (String dl : links2) {
                            dl = dl.replaceAll("\\\\/", "/");
                            if (!dl.startsWith("http")) {
                                Browser br2 = br.cloneBrowser();
                                br2.getPage(dl);
                                String flink = br2.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
                                if (flink == null && br2.getRedirectLocation() != null) {
                                    dl = br2.getRedirectLocation();
                                } else {
                                    dl = flink;
                                }
                            }
                            DownloadLink link = createDownloadlink(dl);
                            link.addSourcePluginPassword(pass);
                            decryptedLinks.add(link);
                        }
                    }
                }

            }
            if (decryptedLinks.size() == 0) {
                String[] links2 = br.getRegex("onclick=\"window.open\\(\\'([^']*)\\'\\)\\;\" value=\"Download\"").getColumn(0);
                for (String dl : links2) {
                    DownloadLink link = createDownloadlink(dl);
                    link.addSourcePluginPassword(pass);
                    decryptedLinks.add(link);
                }
            }
        } catch (PluginException e2) {
            throw e2;
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            return null;
        }

        return decryptedLinks;
    }

}
