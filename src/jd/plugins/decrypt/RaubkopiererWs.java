//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTMLEntities;
import jd.http.URLConnectionAdapter;
import jd.nutils.Screen;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class RaubkopiererWs extends PluginForDecrypt {

    private ProgressController progress;
    private static final String[] knownMirrors = new String[] { "Netload.in", "Filefactory.com" };
    private static String fpName;
    private static String fpPass;
    private static ArrayList<InputField> mirrors;
    private static boolean getNFO = false;
    private static boolean getSample = false;
    private static boolean extraPackage = false;
    private static long partcount = 1;
    private static FilePackage fp = FilePackage.getInstance();
    private static FilePackage fpExtra = FilePackage.getInstance();

    public RaubkopiererWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        this.progress = progress;
        DownloadLink nfoDLink = null;
        DownloadLink sampleDLink = null;

        /* single part-link handling */
        if (parameter.contains("goto")) {
            br.getPage(parameter.replace("goto", "frame"));
            DownloadLink dlink = createDownloadlink(br.getRedirectLocation());
            decryptedLinks.add(dlink);
            return decryptedLinks;
        }

        br.getPage(parameter);
        if ((br.getRedirectLocation() != null && br.getRedirectLocation().contains("error")) || br.containsHTML("class=\"error_msg\"")) {
            logger.warning("No downloads on this page!");
            return null;
        }
        if (br.containsHTML("<h1>(.*?)(<img.*?)?</h1>")) fpName = br.getRegex("<h1>(.*?)(<img.*?)?</h1>").getMatch(0).trim();
        if (fpName != null && !fpName.isEmpty()) fp.setName(HTMLEntities.unhtmlentities(fpName));
        if (br.containsHTML("Passwort:</b></th>\\s+<td>(.*?)</td>")) fpPass = br.getRegex("Passwort:</b></th>\\s+<td>(.*?)</td>").getMatch(0).trim();
        if (fpPass != null && !fpPass.isEmpty()) fp.setPassword(fpPass);

        Form form = br.getFormbyProperty("name", "go_captcha");
        if (form != null) {
            mirrors = form.getInputFieldsByType("submit");
            if (mirrors.isEmpty()) return null;
        } else
            return null;

        showMirrorDialog();

        progress.setRange(mirrors.size());
        for (int i = 0; i <= mirrors.size() - 1; i++) {
            for (int retry = 1; retry <= 5; retry++) {
                String captchaURL = "/captcha" + form.getRegex("<img\\ssrc=\"/captcha(.*?)\"").getMatch(0);
                if (captchaURL == null) return null;
                URLConnectionAdapter con = br.openGetConnection(captchaURL);
                File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, con);
                String code = getCaptchaCode(captchaFile, param);
                br.postPage(parameter, "captcha=" + code + "&" + mirrors.get(i).getKey() + "=");
                if (!br.containsHTML("Fehler: Der Sicherheits-Code")) {
                    break;
                } else {
                    logger.warning(JDLocale.L("downloadlink.status.error.captcha_wrong", "Captcha wrong"));
                }
            }

            /* Get additional files */
            if (getNFO && nfoDLink == null && br.containsHTML("<a\\shref=\"/nfo_files")) {
                nfoDLink = createDownloadlink("http://" + this.getHost() + br.getRegex("<a\\shref=\"(/nfo_files/.*?\\.nfo)\"").getMatch(0));
                // fp.add(nfoDLink);
                // decryptedLinks.add(nfoDLink);
            }
            if (getSample && sampleDLink == null && br.containsHTML("<a\\shref=\"(/\\w+?/sl/\\w+?/\\d+?/goto-[a-z0-9]+?/?)\"")) {
                Browser br2 = br.cloneBrowser();
                String link = br.getRegex("<a\\shref=\"(/\\w+?/sl/\\w+?/\\d+?/goto-[a-z0-9]+?/?)\"").getMatch(0);
                br2.getPage(link.replace("goto", "frame"));
                sampleDLink = createDownloadlink(br2.getRedirectLocation());
                // fp.add(sampleDLink);
                // decryptedLinks.add(sampleDLink);
            }

            /*
             * Check container availability and get them. Break current
             * mirror-loop if got links from container
             */
            if (br.containsHTML("/container/(dlc|ccf|rsdf)_files/")) {
                if (!getContainer(br.toString(), parameter, "dlc", decryptedLinks)) {
                    if (!getContainer(br.toString(), parameter, "ccf", decryptedLinks)) {
                        if (!getContainer(br.toString(), parameter, "rsdf", decryptedLinks)) {
                            ;
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            /*
             * Continue here if there are no containers or got no links from
             * container
             */
            if (!br.containsHTML("<a\\shref=\"(/\\w+?/dl/.+?/goto-[a-z0-9]+?/?)\"")) {
                logger.severe("No downloads on this page!");
                return null;
            }
            String[] parts = br.getRegex("<a\\shref=\"(/\\w+?/dl/.+?/goto-[a-z0-9]+?/?)\"").getColumn(0);
            if (parts.length != 0) {
                partcount = parts.length;
                int percent = progress.getPercent();
                progress.setRange(parts.length * mirrors.size());
                progress.setStatus((long) Math.round(progress.getMax() * ((double) percent / 10000)));
                for (String part : parts) {
                    br.getPage(part.replace("goto", "frame"));
                    DownloadLink dlink = createDownloadlink(br.getRedirectLocation());
                    if (fpPass != null && !fpPass.isEmpty()) dlink.addSourcePluginPassword(fpPass);
                    dlink.setFilePackage(fp);
                    // fp.add(dlink);
                    decryptedLinks.add(dlink);
                    progress.increase(1);
                }
            } else
                progress.increase(partcount);
        }

        /* Add additional files to FilePackage and decryptedLinks */
        if (!extraPackage) {
            if (nfoDLink != null) {
                nfoDLink.setFilePackage(fp);
                // fp.add(nfoDLink);
                decryptedLinks.add(nfoDLink);
            }
            if (sampleDLink != null) {
                sampleDLink.setFilePackage(fp);
                // fp.add(sampleDLink);
                decryptedLinks.add(sampleDLink);
            }
        } else {
            if (fpName.isEmpty() || fpName == null) fpName = "raubkopierer.ws";
            fpExtra.setName(fpName + " - Extras");
            if (nfoDLink != null) {
                nfoDLink.setFilePackage(fpExtra);
                // fpExtra.add(nfoDLink);
                decryptedLinks.add(nfoDLink);
            }
            if (sampleDLink != null) {
                sampleDLink.setFilePackage(fpExtra);
                // fpExtra.add(sampleDLink);
                decryptedLinks.add(sampleDLink);
            }
        }

        return decryptedLinks;
    }

    private boolean getContainer(String page, String cryptedLink, String containerFormat, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String container_link = new Regex(page, "href=\"(/container/" + containerFormat + "_files/.+?\\." + containerFormat + ")\"").getMatch(0);
        if (container_link != null) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            Browser browser = br.cloneBrowser();
            browser.getDownload(container, Encoding.htmlDecode(container_link));
            Vector<DownloadLink> dlinks = JDUtilities.getController().getContainerLinks(container);
            container.delete();
            if (dlinks.isEmpty()) return false;
            fp.addAll(dlinks);
            decryptedLinks.addAll(dlinks);
            progress.increase(partcount);
            return true;
        }
        return false;
    }

    private void showMirrorDialog() {
        mirrorDialog();
    }

    /**
     * Mirrorauswahl Popup-Dialog mit dynamischen Mirror-Checkboxen
     * 
     * @info Hab mich beim Serienjunkies Plugin bedient ;) greetz ManiacMansion
     */
    private void mirrorDialog() {

        new GuiRunnable<Object>() {
            private static final long serialVersionUID = 7264910846281984510L;

            // @Override
            public Object runSave() {

                new JDialog(SimpleGUI.CURRENTGUI) {
                    private static final long serialVersionUID = 1981746297816350752L;

                    private void init() {

                        setLayout(new MigLayout("wrap 1"));
                        setModal(true);
                        setTitle(JDLocale.L("plugins.decrypt.RaubkopiererWS.mirrorDialog.title", "Raubkopierer.ws::Mirrors"));
                        setAlwaysOnTop(true);

                        addWindowListener(new WindowAdapter() {

                            public void windowClosing(WindowEvent e) {
                                mirrors.clear();
                                dispose();
                            }

                        });

                        add(new JLabel(JDLocale.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.mirror", "Wähle die gewünschten Mirrors aus:")));

                        final JCheckBox[] checkMirror = new JCheckBox[mirrors.size()];
                        for (int i = 0; i <= mirrors.size() - 1; i++) {
                            String mirrorDisplayName = mirrors.get(i).getKey();
                            for (String knownMirror : knownMirrors) {
                                if (knownMirror.toLowerCase().contains(mirrors.get(i).getKey().toLowerCase())) {
                                    mirrorDisplayName = knownMirror;
                                    break;
                                }
                            }
                            checkMirror[i] = new JCheckBox(mirrorDisplayName, true);
                            checkMirror[i].setFocusPainted(false);
                            add(checkMirror[i]);
                        }

                        add(new JSeparator(), "growx, spanx");

                        add(new JLabel(JDLocale.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.additional", "Folgendes downloaden falls vorhanden:")));

                        final JCheckBox checkNFO = new JCheckBox(JDLocale.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.nfo", "NFO Datei"), false);
                        checkNFO.setFocusPainted(false);
                        add(checkNFO);
                        final JCheckBox checkSample = new JCheckBox(JDLocale.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.sample", "Sample Video"), false);
                        checkSample.setFocusPainted(false);
                        add(checkSample);
                        final JCheckBox checkExtraFP = new JCheckBox(JDLocale.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.extrapackage", "Zusätzliches Paket für NFO/Sample"), false);
                        checkExtraFP.setFocusPainted(false);
                        add(checkExtraFP);

                        JButton btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
                        btnOK.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                /* Mirror selection */
                                ArrayList<InputField> c = new ArrayList<InputField>();
                                for (int i = 0; i <= checkMirror.length - 1; i++) {
                                    if (!checkMirror[i].isSelected()) c.add(mirrors.get(i));
                                }
                                mirrors.removeAll(c);

                                /* Additional Files */
                                getNFO = checkNFO.isSelected();
                                getSample = checkSample.isSelected();
                                extraPackage = checkExtraFP.isSelected();
                                dispose();
                            }

                        });
                        add(btnOK);
                        pack();
                        setLocation(Screen.getCenterOfComponent(null, this));
                        setResizable(false);
                        setVisible(true);
                    }
                }.init();
                return null;
            }
        }.waitForEDT();
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}