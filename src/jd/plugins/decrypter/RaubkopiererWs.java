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

package jd.plugins.decrypter;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import jd.PluginWrapper;
import jd.captcha.specials.Raubkopierer;
import jd.controlling.ProgressController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.http.Browser;
import jd.nutils.Screen;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "raubkopierer.ws" }, urls = { "http://[\\w\\.]*?raubkopierer\\.(ws|cc)/\\w+/[\\w/]*?\\d+/.+"}, flags = { 0 })


public class RaubkopiererWs extends PluginForDecrypt {

    private ProgressController progress;
    private final String[] knownMirrors = new String[] { "Netload.in", "Filefactory.com" };
    private String fpName;
    private String fpPass;
    private Mirrors mirrors = new Mirrors();
    private boolean getNFO = false;
    private boolean getSample = false;
    private boolean extraPackage = false;
    private long partcount = 1;
    private FilePackage fp = FilePackage.getInstance();
    private FilePackage fpExtra = FilePackage.getInstance();

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

        /* error handling: no downloads on the page */
        br.getPage(parameter);
        if ((br.getRedirectLocation() != null && br.getRedirectLocation().contains("error")) || br.containsHTML("class=\"error_msg\"")) {
            logger.warning("No downloads found on " + parameter);
            logger.warning("Perhaps wrong URL or the download is not available anymore.");
            progress.setStatusText("No downloads found on " + parameter);
            progress.setColor(Color.RED);
            progress.finalize(10000l);
            return new ArrayList<DownloadLink>();
        }
        /* Get package name + password */
        fpName = br.getRegex("<h1>(.*?)(<img.*?)?</h1>").getMatch(0).trim();
        if (fpName != null && !fpName.isEmpty()) fp.setName(HTMLEntities.unhtmlentities(fpName));
        fpPass = br.getRegex("Passwort:</b></th>\\s+<td>(.*?)</td>").getMatch(0).trim();
        if (fpPass != null && !fpPass.isEmpty()) fp.setPassword(fpPass);

        Form form = br.getFormbyProperty("name", "go_captcha");
        if (form != null) {
            for (InputField field : form.getInputFieldsByType("submit")) {
                String name = field.getStringProperty("class", null);
                if (name == null) name = field.getKey();
                mirrors.add(name, "", true, false);
            }
            if (mirrors.isEmpty()) return null;
        } else
            return null;

        showMirrorDialog();

        progress.setRange(mirrors.size());
        for (int i = 0; i <= mirrors.size() - 1; i++) {
            if (!mirrors.get(i).getUse() && !mirrors.get(i).getUseForSample()) continue;
            for (int retry = 1; retry <= 5; retry++) {
                String captchaURL = "/captcha" + form.getRegex("<img\\ssrc=\"/captcha(.*?)\"").getMatch(0);
                File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaURL));
                Raubkopierer.prepareCaptcha(captchaFile);
                String code = getCaptchaCode("rbkprrws",captchaFile, param);
                br.postPage(parameter, "captcha=" + code + "&" + mirrors.get(i).getKey() + "=");
                if (!br.containsHTML("Fehler: Der Sicherheits-Code")) {
                    break;
                } else {
                    logger.warning(JDL.L("downloadlink.status.error.captcha_wrong", "Captcha wrong"));
                }
            }

            /* Get additional files */
            if (getNFO && nfoDLink == null && br.containsHTML("<a\\shref=\"/nfo_files")) {
                nfoDLink = createDownloadlink("http://" + this.getHost() + br.getRegex("<a\\shref=\"(/nfo_files/.*?\\.nfo)\"").getMatch(0));
            }
            if (getSample && mirrors.get(i).getUseForSample() && sampleDLink == null && br.containsHTML("<a\\shref=\"(/\\w+?/sl/\\w+?/\\d+?/goto-[a-z0-9]+?/?)\"")) {
                Browser br2 = br.cloneBrowser();
                String link = br.getRegex("<a\\shref=\"(/\\w+?/sl/\\w+?/\\d+?/goto-[a-z0-9]+?/?)\"").getMatch(0);
                br2.getPage(link.replace("goto", "frame"));
                sampleDLink = createDownloadlink(br2.getRedirectLocation());
            }
            if (mirrors.get(i).getUseForSample() && !mirrors.get(i).getUse()) continue;

            /*
             * Check container availability and get them. Break current
             * mirror-loop if got links from container
             */
            if (br.containsHTML("/container/(dlc|ccf|rsdf)_files/")) {
                if (getContainer(br.toString(), "dlc", decryptedLinks)) {
                    continue;
                } else if (getContainer(br.toString(), "ccf", decryptedLinks)) {
                    continue;
                } else if (getContainer(br.toString(), "rsdf", decryptedLinks)) {
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
                progress.setRange(parts.length * mirrors.useNum());
                progress.setStatus(Math.round(progress.getMax() * ((double) percent / 10000)));
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

    private boolean getContainer(String page, String containerFormat, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String container_link = new Regex(page, "href=\"(/container/" + containerFormat + "_files/.+?\\." + containerFormat + ")\"").getMatch(0);
        if (container_link != null) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            Browser browser = br.cloneBrowser();
            browser.getDownload(container, Encoding.htmlDecode(container_link));
            ArrayList<DownloadLink> dlinks = JDUtilities.getController().getContainerLinks(container);
            container.delete();
            if (dlinks.isEmpty()) return false;
            fp.addLinks(dlinks);
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
                /**
                 * TODO NO GUI IN PLUGINS!!
                 */
                new JDialog(SwingGui.getInstance().getMainFrame()) {
                    private static final long serialVersionUID = 1981746297816350752L;

                    private void init() {

                        setLayout(new MigLayout("wrap 1"));
                        setModal(true);
                        setTitle(JDL.L("plugins.decrypt.RaubkopiererWS.mirrorDialog.title", "Raubkopierer.ws::Mirrors"));
                        setAlwaysOnTop(true);

                        addWindowListener(new WindowAdapter() {

                            public void windowClosing(WindowEvent e) {
                                mirrors.clear();
                                dispose();
                            }

                        });

                        add(new JLabel(JDL.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.mirror", "Available Mirrors:")), "split 2");
                        add(new JSeparator(), "gaptop 3, growx, spanx");

                        final JCheckBox[] checkMirror = new JCheckBox[mirrors.useNum()];
                        for (int i = 0; i <= mirrors.size() - 1; i++) {
                            if (!mirrors.get(i).getUse()) continue;
                            mirrors.get(i).setName(mirrors.get(i).getKey());
                            for (String knownMirror : knownMirrors) {
                                if (knownMirror.toLowerCase().contains(mirrors.get(i).getKey().toLowerCase())) {
                                    mirrors.get(i).setName(knownMirror);
                                    break;
                                }
                            }
                            checkMirror[i] = new JCheckBox(mirrors.get(i).getName(), true);
                            checkMirror[i].setFocusPainted(false);
                            add(checkMirror[i]);
                        }

                        add(new JLabel(JDL.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.additional", "Additional files (if available):")), "split 2");
                        add(new JSeparator(), "gaptop 3, growx, spanx");

                        final JCheckBox checkNFO = new JCheckBox(JDL.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.nfo", "NFO File"), false);
                        checkNFO.setFocusPainted(false);
                        final JCheckBox checkSample = new JCheckBox(JDL.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.sample", "Sample Video from"), false);
                        checkSample.setFocusPainted(false);
                        final JCheckBox checkExtraFP = new JCheckBox(JDL.L("plugins.decrypt.RaubkopiererWs.mirrorDialog.extrapackage", "Additional package for NFO/Sample"), false);
                        checkExtraFP.setFocusPainted(false);
                        checkExtraFP.setSelected(true);
                        checkExtraFP.setEnabled(false);
                        final JComboBox comboSampleMirror = new JComboBox(mirrors.getNames());
                        comboSampleMirror.setFocusable(false);
                        comboSampleMirror.setEnabled(false);

                        ActionListener action = new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                if (checkNFO.isSelected() || checkSample.isSelected()) {
                                    checkExtraFP.setEnabled(true);
                                    if (e.getSource() == checkSample && checkSample.isSelected())
                                        comboSampleMirror.setEnabled(true);
                                    else if (e.getSource() == checkSample) comboSampleMirror.setEnabled(false);
                                } else {
                                    // checkExtraFP.setSelected(false);
                                    checkExtraFP.setEnabled(false);
                                    comboSampleMirror.setEnabled(false);
                                }
                            }
                        };

                        checkNFO.addActionListener(action);
                        checkSample.addActionListener(action);
                        add(checkNFO);
                        add(checkSample, "split 2");
                        add(comboSampleMirror);
                        add(checkExtraFP);

                        JButton btnOK = new JButton(JDL.L("gui.btn_ok", "OK"));
                        btnOK.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                /* Submit Mirror selection */
                                for (int i = 0; i <= checkMirror.length - 1; i++) {
                                    if (!checkMirror[i].isSelected()) mirrors.get(i).setUse(false);

                                }

                                /* Submit Additional Files Info */
                                mirrors.get(comboSampleMirror.getSelectedIndex()).setUseForSample(checkSample.isSelected());
                                getNFO = checkNFO.isSelected();
                                getSample = checkSample.isSelected();
                                extraPackage = checkExtraFP.isSelected() && checkExtraFP.isEnabled();
                                dispose();
                            }

                        });
                        add(btnOK, "align center, split 2, gaptop 8");

                        JButton btnAbort = new JButton(JDL.L("gui.btn_cancel", "Cancel"));
                        btnAbort.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                mirrors.clear();
                                dispose();
                            }

                        });
                        add(btnAbort);

                        getRootPane().setDefaultButton(btnOK);
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

    private class Mirrors {
        private class SingleMirror {
            public String key;
            public String name;
            public boolean use;
            public boolean useForSample;

            public SingleMirror(String key, String name, boolean use, boolean useForSample) {
                if (key == null) key = "unkown";
                this.key = key;
                if (name == null) name = "unknown";
                this.name = name;
                this.use = use;
                this.useForSample = useForSample;
            }

            public String getKey() {
                return key;
            }

            public String getName() {
                return name;
            }

            public boolean getUse() {
                return use;
            }

            public boolean getUseForSample() {
                return useForSample;
            }

            public void setName(String name) {
                this.name = name;
            }

            public void setUse(boolean use) {
                this.use = use;
            }

            public void setUseForSample(boolean useForSample) {
                this.useForSample = useForSample;
            }

        }

        public ArrayList<SingleMirror> mirrorArray;

        public Mirrors() {
            this.mirrorArray = new ArrayList<SingleMirror>();
        }

        public SingleMirror get(int index) {
            return this.mirrorArray.get(index);
        }

        public void add(String key, String name, boolean use, boolean useForSample) {
            this.mirrorArray.add(new SingleMirror(key, name, use, useForSample));
        }

        public String[] getNames() {
            String[] n = new String[this.useNum()];
            for (int i = 0; i <= this.mirrorArray.size() - 1; i++)
                if (this.mirrorArray.get(i).getUse()) n[i] = this.mirrorArray.get(i).getName();
            return n;
        }

        public int useNum() {
            int i = 0;
            for (SingleMirror m : this.mirrorArray)
                if (m.getUse()) i++;
            return i;
        }

        public boolean isEmpty() {
            return this.mirrorArray.isEmpty();
        }

        public void clear() {
            for (SingleMirror m : this.mirrorArray)
                m.setUse(false);
        }

        public int size() {
            return this.mirrorArray.size();
        }
    }

    // @Override
    

}
