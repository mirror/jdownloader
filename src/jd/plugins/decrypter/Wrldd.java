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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wii-reloaded.info" }, urls = { "http://[\\w\\.]*?(protect\\.wii|wii)-reloaded\\.(info|org)(/protect)?/get\\.php\\?i=[a-zA-Z-0-9]+" }, flags = { 0 })
public class Wrldd extends PluginForDecrypt {

    public Wrldd(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        int submitvalue = getPluginConfig().getIntegerProperty("WIIReloaded_SubmitValue", 5);
        br.setDebug(true);
        String parameter = param.toString().replaceFirst("reloaded.info", "reloaded.org");
        ArrayList<String> link_passwds = new ArrayList<String>();
        link_passwds.add("wii-reloaded.info");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        this.setBrowserExclusive();
        progress.setRange(3);
        //Fix for redirect-links of the mainpage
        if (parameter.contains("protect.wii")){
            String linkid = new Regex(parameter, "get\\.php\\?i=([a-zA-Z-0-9]+)").getMatch(0);
            parameter = "http://www.wii-reloaded.org/protect/get.php?i=" + linkid;
        }
        br.getPage(parameter);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        br.getPage(parameter);
        progress.increase(1);
        int max = 10;
        while (br.containsHTML("captcha/captcha\\.php") || br.containsHTML("Sicherheitscode war falsch")) {
            if (max-- <= 0) {
                logger.severe("Captcha Code has been wrong many times. abort.");
                return null;
            }
            String adr = "http://www.wii-reloaded.org/protect/captcha/captcha.php";

            progress.addToMax(1);

            String capTxt = getCaptchaCode("wii", adr, param);
            Form post = br.getForm(0);
            post.put("sicherheitscode", capTxt);
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
            br.submitForm(post);
        }
        String[] ids = br.getRegex("onClick=\"popup_dl\\((.*?)\\)\"").getColumn(0);
        logger.finer("ids found" + ids.length);
        progress.addToMax(ids.length);
        Browser brc = br.cloneBrowser();
        for (String element : ids) {
            for (int retry = 1; retry < 5; retry++) {
                brc.getPage("http://www.wii-reloaded.org/protect/hastesosiehtsaus.php?i=" + element);
                if (brc.containsHTML("captcha/numeric.php")) {
                    String adr = "http://www.wii-reloaded.org/protect/captcha/numeric.php";

                    String capTxt = getCaptchaCode("wii-numeric", adr, param);
                    Form post = brc.getForm(0);
                    post.put("insertvalue", capTxt);
                    brc.submitForm(post);

                } else {
                    Form form = brc.getForm(0);
                    if (form != null) {
                        form.put("insertvalue", submitvalue + "");
                        brc.submitForm(form);
                        if (brc.getRedirectLocation() == null) {
                            /* neuer submit value suchen */
                            logger.info("Searching new SubmitValue");
                            boolean found = false;
                            for (int i = 0; i <= 100; i++) {
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                                form.put("insertvalue", i + "");
                                brc.submitForm(form);
                                if (brc.getRedirectLocation() != null) {
                                    found = true;
                                    getPluginConfig().setProperty("WIIReloaded_SubmitValue", i);
                                    submitvalue = i;
                                    logger.info("SubmitValue found!");
                                    break;
                                }
                            }
                            if (found == false) {
                                logger.info("SubmitValue NOT found!");
                                getPluginConfig().setProperty("WIIReloaded_SubmitValue", -1);
                                return null;
                            }
                        }
                    }
                }
                if (brc.getRedirectLocation() != null) {
                    DownloadLink link = createDownloadlink(brc.getRedirectLocation());
                    link.setSourcePluginPasswordList(link_passwds);
                    decryptedLinks.add(link);
                    break;
                }

            }
            progress.increase(1);
        }
        progress.increase(1);
        return decryptedLinks;
    }

    // @Override

}
