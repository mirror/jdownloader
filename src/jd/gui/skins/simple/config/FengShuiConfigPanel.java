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

package jd.gui.skins.simple.config;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.miginfocom.swing.MigLayout;


public class FengShuiConfigPanel {

    private static final String GAPLEFT = "gapleft 15!, ";
    private static final String GAPBOTTOM = ", gapbottom :100:push";
	private static final String SPAN = ", spanx" + GAPBOTTOM;
    


    private static JPanel getPanel() {
		JPanel panel = new JPanel(new MigLayout("ins 32 22 15 22", "[right, pref!]0[right,grow,fill]0[]"));
		
	    addSeparator(panel, "Download Location", getImageIcon("res/package.png"), "<html>Determines where files <br>are both stored and extracted to.");
        JPanel subPanel = new JPanel(new MigLayout("ins 0", "0[fill, grow]15[]0"));
        JTextField folder = new JTextField(System.getProperty("user.dir"));
        subPanel.add(folder);
        subPanel.add(new JButton("Browse"));
        addComponents(panel, "Choose Folder", subPanel);

        addSeparator(panel, "Router Reconnect", getImageIcon("res/reconnect.png"), "<html>Sometimes your Router needs a kick in the balls!");
        JPanel subPanel2 = new JPanel(new MigLayout("ins 0", "0[]15[]0"));
        subPanel2.add(new JButton("Test Reconnect"));
        subPanel2.add(new JButton("Change Settings"));
        addComponents(panel, "Is reconnect working?", subPanel2);
	      
        addSeparator(panel, "Premium Accounts", getImageIcon("res/star.png"), "<html>If you have a Premium Account for a hoster you can enter you login<br> password here and JD will use them automatically henceforth<br> if you download files with that hoster");
        addComponents(panel, "Enter your credentials");
        panel.add(new JButton("Account Settings"), GAPLEFT + "align leading, wmax pref" + SPAN);
        
        addSeparator(panel, "Barrierefreies Internet", getImageIcon("res/accessibility.png"), "<html>Some hosters use captchas which are impossible<br> to enter for people with disabilities. With this functionality<br> the JD team addresses the requirement for<br> people with disabilities not to be discriminated against.<br> Read more about it at:<br> <b>http://en.wikipedia.org/wiki/Web_accessibility");
        JCheckBox accessibility = new JCheckBox(" I have disabilities. Process captchas for me!");
        addComponents(panel, "Automatic download?", accessibility);

        addSeparator(panel, "Update Mode", getImageIcon("res/update_manager.png"), "<html>You can either update to the stable or newest beta version of JD");
        JCheckBox beta = new JCheckBox(" Always update to latest beta?");
        beta.setSelected(true);
        addComponents(panel, "Stable or Beta?", beta);
        
        Container bpanel = new JPanel(new MigLayout());
        bpanel .add(new JSeparator(), "spanx, pushx, growx, gapbottom 5");
        bpanel.add(new JButton("More"), "tag help2");
        bpanel.add(new JButton("Apply"), "w pref!, tag apply");
        bpanel.add(new JButton("Cancel"), "w pref!, tag cancel, wrap");
        panel.add(bpanel, "spanx, pushx, growx");
		return panel;
	}

    private static void addComponents(JPanel panel, String label, JComponent... components) {
        panel.add(new JLabel("<html><b color=\"#4169E1\">" + label), "gapleft 22, gaptop 5" + GAPBOTTOM);
        for (int i = 0; i < components.length; i++) {
            if (i == components.length-1) panel.add(components[i], GAPLEFT + SPAN + ", gapright 5");
            else panel.add(components[i], GAPLEFT);
        }
    }

    private static void addSeparator(JPanel panel, String title, ImageIcon icon, String help) {
        JLabel label = new JLabel("<html><u><b  color=\"#006400\">"+title, icon, SwingConstants.CENTER);
        label.setIconTextGap(8);
        panel.add(label, "align left, split 2");
	    panel.add(new JSeparator(), "gapleft 10, spanx, pushx, growx");
	    panel.add(new JSeparator(), "span 3, pushx, growx");
	    JLabel tip = new JLabel(getImageIcon("res/bulb.gif"));
	    tip.setToolTipText(help);
        panel.add(tip, GAPLEFT + "w pref!, wrap");
    }
	
    private static ImageIcon getImageIcon(String icon) {
        return new ImageIcon(icon);
    }
    
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame("Feng Shui Config");
        JPanel panel = getPanel();
        Dimension minSize = panel.getMinimumSize();
        frame.setContentPane(new JScrollPane(panel));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        Dimension ps = frame.getPreferredSize();
        frame.setPreferredSize(new Dimension(Math.min(800, ps.width), Math.min(600, ps.height)));
        frame.pack();
        panel.setPreferredSize(minSize);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
    }
}
