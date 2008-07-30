package jd.wizardry7.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class DefaultHeader extends JPanel {

	static Font descriptionFont = new Font("Arial", Font.PLAIN, 10);
	static Font titleFont = new Font("Arial", Font.BOLD, 12);
	private static void paintHorizontalGradient(Graphics2D g2, int x, int y, int w, int h) {
        Color gradientColor = Color.lightGray;
        g2.setPaint(new GradientPaint(100F, h, Color.WHITE, w, h, gradientColor));
        g2.fillRect(x, y, w, h);
    }
	
	private Icon icon;
	JLabel stepdescription;

	JLabel steptext;
	
	public DefaultHeader(String titleKey, String descriptionKey, Icon icon) {
		this(titleKey, descriptionKey, icon, null);
	}

	
	public DefaultHeader(String titleKey, String descriptionKey, Icon icon, int... messageArguments) {
		super(new BorderLayout(0,0));
		if (messageArguments==null) {
			steptext = new JLabel(titleKey);
		}
		else {
			steptext = new JLabel(titleKey + "  Step " + messageArguments[0] + " of " + messageArguments[1]);
		}
		
		stepdescription= new JLabel(descriptionKey);
		this.icon = icon;
		
		this.createHeader();
	}
	
	
	
	
	private JPanel createHeader() {
		FormLayout layout = new FormLayout("l:d, 3dlu, r:p:g", "pref, 2px, f:pref:grow");
		this.setBorder(Borders.DIALOG_BORDER);
		this.setLayout(layout);
		
		CellConstraints cc = new CellConstraints();
		
		steptext.setFont(titleFont);
		this.add(steptext, cc.xy(1,1));

		if (icon!=null) this.add(new JLabel(icon), cc.xywh(3, 1, 1, 3));
		
		stepdescription.setFont(descriptionFont);
		stepdescription.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));
		this.add(stepdescription, cc.xy(1,3));
		
		return this;
	}
	
    @Override
    protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		paintHorizontalGradient(g2, 0, 0, this.getWidth(), this.getHeight());
	}
    
}
    