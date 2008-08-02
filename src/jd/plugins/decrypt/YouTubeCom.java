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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class YouTubeCom extends PluginForDecrypt {
	

    static private String host = "youtube.com";
    static private final Pattern patternswfArgs = Pattern.compile("(.*?swfArgs.*)", Pattern.CASE_INSENSITIVE);
    private static final String PLAYER = "get_video";
    private static final String T = "\"t\"";
    private static final String VIDEO_ID = "video_id";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?youtube\\.com/watch\\?v=[a-z-_A-Z0-9]+", Pattern.CASE_INSENSITIVE);
    
    static public final Pattern YT_FILENAME = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);


    public YouTubeCom() {
        super();
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
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);

            String video_id = "";
            String t = "";

            String match = new Regex(reqinfo.getHtmlCode(), patternswfArgs).getFirstMatch();
            if (match == null) {
                return null;
            }
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
            
            Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
            
            if (HTTP.getRequestWithoutHtmlCode(new URL(link + "&fmt=18"), null, null, true).getResponseCode() == 200) {
            	possibleconverts.add(ConversionMode.VIDEOMP4);
            }
            if (HTTP.getRequestWithoutHtmlCode(new URL(link + "&fmt=13"), null, null, true).getResponseCode() == 200) {
            	possibleconverts.add(ConversionMode.VIDEO3GP);
            }
            
            possibleconverts.add(ConversionMode.AUDIOMP3);
            possibleconverts.add(ConversionMode.VIDEOFLV);
            possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);
            
            ConversionMode ConvertTo = ConvertDialog.DisplayDialog(possibleconverts.toArray());
            if (ConvertTo != null) {

                if (ConvertTo == ConvertDialog.ConversionMode.VIDEOMP4) {
                    link += "&fmt=18";
                } else if (ConvertTo == ConvertDialog.ConversionMode.VIDEO3GP) {
                    link += "&fmt=13";
                }
                

                
                String name = new Regex(reqinfo.getHtmlCode(), YT_FILENAME).getFirstMatch();
                link = "< youtubedl url=\"" + parameter + "\" decrypted=\"" + link + "\" convert=\"" + ConvertTo.name() + "\" name=\"" + name + "\" >";
                logger.info(link);
                
                DownloadLink thislink = createDownloadlink(link);
                thislink.setBrowserUrl(parameter);
                logger.info("BrowserURL: " + thislink.getBrowserUrl());
                thislink.setName(name);
                thislink.setSourcePluginComment("Convert to "+ConvertTo.GetText());
                FilePackage fp = new FilePackage();  
                fp.setName(name);
                fp.add(thislink);
                decryptedLinks.add(thislink);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    /*
    private int getYoutubeConvertTo(boolean hasMp4, boolean has3gp) {
        yConvertDialog(hasMp4, has3gp);
        return useyConvert[0];

    }

    private void yConvertDialog(final boolean hasMp4, final boolean has3gp) {
        if (yConvertChecked || useyConvert[1] == saveyConvert) {
            return;
        }
        new Dialog(((SimpleGUI) JDUtilities.getGUI()).getFrame()) {

            private static final long serialVersionUID = -4282205277016215186L;

            void init() {
                setLayout(new BorderLayout());
                setModal(true);
                setTitle(JDLocale.L("plugins.YouTube.ConvertDialog.title", "Youtube.com Dateiformat"));
                setAlwaysOnTop(true);
                setLocation(20, 20);
                JPanel panel = new JPanel(new GridBagLayout());
                final class meth {
                    public String name;

                    public int var;

                    public meth(String name, int var) {
                        this.name = name;
                        this.var = var;
                    }

                    @Override
                    public String toString() {

                        return name;
                    }
                }
                ;

                addWindowListener(new WindowListener() {

                    public void windowActivated(WindowEvent e) {

                    }

                    public void windowClosed(WindowEvent e) {

                    }

                    public void windowClosing(WindowEvent e) {
                        // useyConvert = new int[] { ((meth)
                        // methods.getSelectedItem()).var, 0 };
                        useyConvert = new int[] { -1, -1 };
                        dispose();
                    }

                    public void windowDeactivated(WindowEvent e) {

                    }

                    public void windowDeiconified(WindowEvent e) {

                    }

                    public void windowIconified(WindowEvent e) {

                    }

                    public void windowOpened(WindowEvent e) {

                    }
                });

                int n = 3;
                if (hasMp4) {
                    n++;
                }
                if (has3gp) {
                    n++;
                }

                meth[] meths = new meth[n];
                n = 0;
                meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.Mp3", "Audio (MP3)"), CONVERT_ID_AUDIO);
                meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.Flv", "Video (FLV)"), CONVERT_ID_VIDEO);
                meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.FlvAndMp3", "Audio und Video (MP3 & FLV)"), CONVERT_ID_AUDIO_AND_VIDEO);
                if (hasMp4) {
                    meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.Mp4", "Video (MP4)"), CONVERT_ID_MP4);
                }
                if (has3gp) {
                    meths[n++] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.3gp", "Video (3GP)"), CONVERT_ID_3GP);
                }

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
    */
}