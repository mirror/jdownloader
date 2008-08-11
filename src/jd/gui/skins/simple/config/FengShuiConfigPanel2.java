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
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.miginfocom.swing.MigLayout;


public class FengShuiConfigPanel2 {

	private static final String WRAP = ", wrap, gapbottom :100:push";
    private static final String GAPLEFT = "gapleft 15!, ";


    private static JPanel getPanel() {
		JPanel panel = new JPanel(new MigLayout("ins 32 22 15 22", "[right, pref!]0[grow,fill]0[]"));
		
	    addSeparator(panel, "Download Location", getImageIcon("res/package.png"), "<html>Determines where files <br>are both stored and extracted to.");
        JTextField folder = new JTextField(System.getProperty("user.dir"));
//        folder.setEditable(false);
        addComponents(panel, "Choose Folder", folder, new JButton("Browse"));

        addSeparator(panel, "Router Reconnect", getImageIcon("res/reconnect.png"), "<html>Sometimes your Router needs a kick in the balls!");
        addComponents(panel, "Is reconnect working?");
        panel.add(new JButton("Test Reconnect"), GAPLEFT + "wmax pref");
        panel.add(new JButton("Change Settings"), "wmax pref, spanx");
	      
        addSeparator(panel, "Premium Accounts", getImageIcon("res/star.png"), "<html>If you have a Premium Account for a hoster you can enter you login<br> password here and JD will use them automatically henceforth<br> if you download files with that hoster");
        addComponents(panel, "Enter your credentials");
        panel.add(new JButton("Account Settings"), GAPLEFT + "align leading, wmax pref" + WRAP);

        
        addSeparator(panel, "Barrierefreies Internet", getImageIcon("res/accessibility.png"), "<html>Some hosters use captchas which are impossible<br> to enter for people with disabilities. With this functionality<br> the JD team addresses the requirement for<br> people with disabilities not to be discriminated against.<br> Read more about it at:<br> <b>http://en.wikipedia.org/wiki/Web_accessibility");
        JCheckBox accessibility = new JCheckBox(" I have disabilities.");
        addComponents(panel, "Automatic download?", accessibility);
//        panel.add(accessibility, GAPLEFT + "align leading, wmax pref" + WRAP);

        addSeparator(panel, "Update Mode", getImageIcon("res/update_manager.png"), "<html>You can either update to the stable or newest beta version of JD");
        JCheckBox beta = new JCheckBox(" Update to latest beta?");
        beta.setSelected(true);
        addComponents(panel, "Stable or Beta?", beta);
//        panel.add(beta, GAPLEFT + "align leading, wmax pref" + WRAP);
        
        Container bpanel = new JPanel(new MigLayout());
        bpanel .add(new JSeparator(), "spanx, pushx, growx, gapbottom 5");
        bpanel.add(new JButton("More"), "tag help2");
        bpanel.add(new JButton("Apply"), "w pref!, tag apply");
        bpanel.add(new JButton("Cancel"), "w pref!, tag cancel, wrap");
        panel.add(bpanel, "spanx, pushx, growx");
		return panel;
	}

    private static void addComponents(JPanel panel, String label, JComponent... components) {
        panel.add(new JLabel("<html><b color=\"#4169E1\">" + label), "gapleft 22, gaptop 14");
        for (int i = 0; i < components.length; i++) {
            if (i == components.length-1) panel.add(components[i], GAPLEFT + WRAP + ", gapright 5");
            else panel.add(components[i], GAPLEFT);
        }
    }

    private static void addSeparator(JPanel panel, String title, ImageIcon icon, String help) {
        JLabel label = new JLabel("<html><u><b  color=\"#006400\">"+title, icon, SwingConstants.CENTER);
        label.setIconTextGap(8);
        panel.add(label, "align left, split 2");
	    panel.add(new JSeparator(), "gapleft 10, spanx, pushx, growx");
	    panel.add(new JSeparator(), "span 2, pushx, growx");
	    JLabel tip = new JLabel(getImageIcon("res/bulb.gif"));
	    tip.setToolTipText(help);
//	    panel.add(new JSeparator(), "split 2, pushx, growx");
        panel.add(tip, GAPLEFT + "w pref!, wrap");
    }
	
    private static ImageIcon getImageIcon(String icon) {
        return new ImageIcon(icon);
    }
    
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame("Feng Shui Config");
        frame.setContentPane(getPanel());
//        frame.setContentPane(new JScrollPane(getPanel()));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        Dimension ps = frame.getPreferredSize();
        frame.setPreferredSize(new Dimension(Math.min(800, ps.width), Math.min(600, ps.height)));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
    }
}
