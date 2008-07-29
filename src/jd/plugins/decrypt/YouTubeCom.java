//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class YouTubeCom extends PluginForDecrypt {

    static private String host = "youtube.com";

    private static final String VIDEO_ID = "video_id";
    private static final String T = "\"t\"";
    // private static final String HOST = "BASE_YT_URL";
    private static final String PLAYER = "get_video";

    public static final int CONVERT_ID_AUDIO = 0;
    public static final int CONVERT_ID_VIDEO = 1;
    public static final int CONVERT_ID_AUDIO_AND_VIDEO = 2;
    public static final int CONVERT_ID_MP4 = 3;
    public static final int CONVERT_ID_3GP = 4;

    private String version = "1.0.0.0";

    private Pattern patternSupported = Pattern.compile("http://.*?youtube\\.com/watch\\?v=[a-z-_A-Z0-9]+", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternswfArgs = Pattern.compile("(.*?swfArgs.*)", Pattern.CASE_INSENSITIVE);

    public YouTubeCom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        // currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "b0ffed";
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

    private static int[] useyConvert = new int[] { 0, 0 };

    private JComboBox methods;

    private JCheckBox checkyConvert;
    private JButton btnOK;

    private boolean yConvertChecked = false;

    private static final int saveyConvert = 1;

    private void yConvertDialog(final boolean hasMp4, final boolean has3gp) {
        if (yConvertChecked || useyConvert[1] == saveyConvert) return;
        new Dialog(((SimpleGUI) JDUtilities.getGUI()).getFrame()) {

            /**
             * 
             */
            private static final long serialVersionUID = -4282205277016215186L;

            void init() {
                setLayout(new BorderLayout());
                setModal(true);
                setTitle(JDLocale.L("plugins.YouTube.ConvertDialog.title", "Youtube.com Dateiformat"));
                setAlwaysOnTop(true);
                setLocation(20, 20);
                JPanel panel = new JPanel(new GridBagLayout());
                final class meth {
                    public int var;

                    public String name;

                    public meth(String name, int var) {
                        this.name = name;
                        this.var = var;
                    }

                    @Override
                    public String toString() {
                        // TODO Auto-generated method stub
                        return name;
                    }
                }
                ;

                addWindowListener(new WindowListener() {

                    public void windowActivated(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowClosed(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowClosing(WindowEvent e) {
                        // useyConvert = new int[] { ((meth)
                        // methods.getSelectedItem()).var, 0 };
                        useyConvert = new int[] { -1, -1 };
                        dispose();
                    }

                    public void windowDeactivated(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowDeiconified(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowIconified(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowOpened(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }
                });

                int n = 3;
                if (hasMp4) n++;
                if (has3gp) n++;

                meth[] meths = new meth[n];
                n = 0;
                meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.Mp3", "Audio (MP3)"), CONVERT_ID_AUDIO);
                meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.Flv", "Video (FLV)"), CONVERT_ID_VIDEO);
                meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.FlvAndMp3", "Audio und Video (MP3 & FLV)"), CONVERT_ID_AUDIO_AND_VIDEO);
                if (hasMp4) meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.Mp4", "Video (MP4)"), CONVERT_ID_MP4);
                if (has3gp) meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.3gp", "Video (3GP)"), CONVERT_ID_3GP);

                methods = new JComboBox(meths);
                checkyConvert = new JCheckBox(JDLocale.L("plugins.YouTube.ConvertDialog.KeepSettings", "Format für diese Sitzung beibehalten"), false);
                Insets insets = new Insets(0, 0, 0, 0);
                JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("plugins.YouTube.ConvertDialog.action", "Dateiformat: ")), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JDUtilities.addToGridBag(panel, methods, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JDUtilities.addToGridBag(panel, checkyConvert, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                btnOK = new JButton(JDLocale.L("gui.btn_continue", "OK"));
                btnOK.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        if (e.getSource() == btnOK) {
                            useyConvert = new int[] { ((meth) methods.getSelectedItem()).var, checkyConvert.isSelected() ? saveyConvert : 0 };
                        } else {
                            useyConvert = new int[] { -1, -1 };
                        }

                        dispose();
                    }

                });
                JDUtilities.addToGridBag(panel, btnOK, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
                add(panel, BorderLayout.CENTER);
                pack();
                setVisible(true);
            }

        }.init();
    }

    private int getYoutubeConvertTo(boolean hasMp4, boolean has3gp) {

        yConvertDialog(hasMp4, has3gp);
        return useyConvert[0];

    }

    private String clean(String s) {
        s = s.replaceAll("\"", "");
        s = s.replaceAll("YouTube -", "");
        s = s.replaceAll("YouTube", "");
        s = s.trim();
        return s;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        // //if (step.getStep() == PluginStep.STEP_DECRYPT) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);

            String video_id = "";
            String t = "";

            String match = new Regex(reqinfo.getHtmlCode(), patternswfArgs).getFirstMatch();
            if (match == null) return null;
            String[] lineSub = match.split(",|:");

            for (int i = 0; i < lineSub.length; i++) {
                String s = lineSub[i];

                if (s.indexOf(VIDEO_ID) > -1) {
                    video_id = clean(lineSub[i + 1]);
                }

                if (s.indexOf(T) > -1) {
                    t = clean(lineSub[i + 1]);
                }
            }

            String link = "http://" + host + "/" + PLAYER + "?" + VIDEO_ID + "=" + video_id + "&" + "t=" + t;

            boolean hasMp4 = false;
            boolean has3gp = false;

            if (HTTP.getRequestWithoutHtmlCode(new URL(link + "&fmt=18"), null, null, true).getResponseCode() == 200) hasMp4 = true;
            if (HTTP.getRequestWithoutHtmlCode(new URL(link + "&fmt=13"), null, null, true).getResponseCode() == 200) has3gp = true;

            int convertId = getYoutubeConvertTo(hasMp4, has3gp);
            if (convertId != -1) {

                if (convertId == CONVERT_ID_MP4) {
                    link += "&fmt=18";
                } else if (convertId == CONVERT_ID_3GP) {
                    link += "&fmt=13";
                }

                link = "< youtubedl url=\"" + parameter + "\" decrypted=\"" + link + "\" convert=\"" + convertId + "\" >";

                decryptedLinks.add(this.createDownloadlink(link));
            }

            // step.setParameter(decryptedLinks);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}