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


package jd.plugins.decrypt;  import java.awt.BorderLayout;
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
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class YouTubeCom extends PluginForDecrypt {

static private String host = "youtube.com";

private static final String VIDEO_ID = "video_id";
private static final String T = "\"t\"";
private static final String HOST = "BASE_YT_URL";
private static final String PLAYER = "get_video";
public static final int CONVERT_ID_AUDIO = 0;

public static final int CONVERT_ID_VIDEO = 1;

public static final int CONVERT_ID_AUDIO_AND_VIDEO = 2;
private String version = "1.0.0.0";

//http://youtube.com/watch?v=qgjWZXnTn9A

private Pattern patternSupported = Pattern.compile("http://.*?youtube\\.com/watch\\?v=[a-z-A-Z0-9]+", Pattern.CASE_INSENSITIVE);
static private final Pattern FILENAME = Pattern.compile("<div id=\"watch-vid-title\">[\\s\\S]*?<div >(.*?)</div>", Pattern.CASE_INSENSITIVE);

static private final Pattern patternswfArgs = Pattern.compile("(.*?swfArgs.*)", Pattern.CASE_INSENSITIVE);


public YouTubeCom() {
  super();
  steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
  currentStep = steps.firstElement();
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
  return host+"-"+version;
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

private boolean yConvertChecked = false;

private static final int saveyConvert = 1;




private void yConvertDialog() {
    if (yConvertChecked || useyConvert[1] == saveyConvert) return;
    new Dialog(((SimpleGUI) JDUtilities.getGUI()).getFrame()) {


        /**
		 * 
		 */
		private static final long serialVersionUID = -4282205277016215186L;

		void init() {
            setLayout(new BorderLayout());
            setModal(true);
            setTitle(JDLocale.L("plugins.YouTube.ConvertDialog.title", "Youtube.com ::Convert::"));
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
                    useyConvert = new int[] { ((meth) methods.getSelectedItem()).var, 0 };
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
            meth[] meths = new meth[3];
            meths[0] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.AudioOnly", "Nur Audio"), CONVERT_ID_AUDIO);
            meths[1] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.VideoOnly", "Nur Video"), CONVERT_ID_VIDEO);
            meths[2] = new meth(JDLocale.L("plugins.YouTube.ConvertDialog.AudioandVideo", "Audio und Video"), CONVERT_ID_AUDIO_AND_VIDEO);

            methods = new JComboBox(meths);
            checkyConvert = new JCheckBox(JDLocale.L("plugins.YouTube.ConvertDialog.KeepSettings", "Einstellungen für diese Sitzung beibehalten?"), true);
            Insets insets = new Insets(0, 0, 0, 0);
            JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("plugins.YouTube.ConvertDialog.action", "Wählen sie eine Aktion aus:")), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(panel, methods, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(panel, checkyConvert, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
            JButton btnOK = new JButton(JDLocale.L("gui.btn_continue", "OK"));
            btnOK.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    useyConvert = new int[] { ((meth) methods.getSelectedItem()).var, checkyConvert.isSelected() ? saveyConvert : 0 };
                    dispose();
                }

            });
            JDUtilities.addToGridBag(panel, btnOK, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
            add(panel, BorderLayout.CENTER);
            pack();
            setVisible(true);
        }

    }.init();
}

private int getYoutubeConvertTo() {

    yConvertDialog();
    return useyConvert[0];

}


private String clean(String s) {
    s = s.replaceAll("\"","");
    s = s.replaceAll("YouTube -","");
    s = s.replaceAll("YouTube","");
    s = s.trim();
    return s;
}

@Override
public PluginStep doStep(PluginStep step, String parameter) {
  if (step.getStep() == PluginStep.STEP_DECRYPT) {
      Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
      try {
          
          progress.setRange(1);
          URL url = new URL(parameter);
          RequestInfo reqinfo = getRequest(url);

          FilePackage fp = new FilePackage();
          String filename = getFirstMatch(reqinfo.getHtmlCode(), FILENAME, 1);
          String video_id="";
          String t="";

          fp.setName(filename);
          
          //logger.info(reqinfo.getHtmlCode());

          //logger.info(getFirstMatch(reqinfo.getHtmlCode(), patternswfArgs, 1));
          String[] lineSub = getFirstMatch(reqinfo.getHtmlCode(), patternswfArgs, 1).split(",|:");    
 
          for (int i = 0; i < lineSub.length; i++) {           
              String s = lineSub[i];
              
              if (s.indexOf(VIDEO_ID) > -1) {
                  video_id = clean(lineSub[i+1]);
              }
              
              if (s.indexOf(T) > -1) {
                  t = clean(lineSub[i+1]);
              }
          }

          
          
          String link = "http://"+host + "/"+PLAYER+"?" + VIDEO_ID +"="+ video_id + "&" + "t="+ t;     

          link = "< youtubedl url=\"" + parameter +  "\" decrypted=\"" + link + "\" convert=\"" + getYoutubeConvertTo() + "\" >";

          
          logger.info(link);
          
          fp.add(this.createDownloadlink(link));
          decryptedLinks.add(this.createDownloadlink(link));
          progress.increase(1);
          


          // Decrypt abschliessen

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