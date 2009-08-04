package jd.captcha.specials;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import jd.utils.JDUtilities;


import jd.nutils.Colors;

import jd.captcha.gui.ImageComponent;

import jd.captcha.gui.BasicWindow;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;

public class ChooseC extends BasicWindow {
	private static final long serialVersionUID = 1L;
	private JPanel panel, Cpanel;
	private int icd = 0;
	private int icdb = 0;
	private ImageComponent ic;
	private Vector<cPoint> ret = new Vector<cPoint>();
	private JSpinner tollerance;
	protected Captcha captcha;

	private void createIc() {
		Captcha captchaImage = new Captcha(captcha.getWidth(), captcha
				.getHeight());
		captchaImage.grid = new int[captcha.getWidth()][captcha.getHeight()];

		for (int x = 0; x < captchaImage.getWidth(); x++) {
			for (int y = 0; y < captchaImage.getHeight(); y++) {
				captchaImage.grid[x][y] = captcha.getPixelValue(x, y);

				for (cPoint cp : ret) {
					if (cp.distance == 0) {
						if (captcha.getPixelValue(x, y) == cp.color)
							captchaImage.grid[x][y] = 0xFF0000;
					} else if (Colors.getColorDifference(captcha.getPixelValue(
									x, y), cp.color) < cp.distance) {
						captchaImage.grid[x][y] = 0xFF0000;
					}
				}

			}

		}

		ic = new ImageComponent(captchaImage.getImage().getScaledInstance(
				captchaImage.getWidth() * 2, captchaImage.getHeight() * 2,
				Image.SCALE_DEFAULT));
		ic.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				final cPoint p = new cPoint(e.getX() / 2, e.getY() / 2,
						(Integer) tollerance.getValue(), captcha);
				if (!ret.contains(p)) {
					ret.add(p);
					BufferedImage image = new BufferedImage(8, 8,
							BufferedImage.TYPE_INT_RGB);
					Graphics2D graphics = image.createGraphics();
					for (int y = 0; y < image.getHeight(); y++) {
						for (int x = 0; x < image.getWidth(); x++) {
							graphics.setColor(new Color(p.color));
							graphics.fillRect(x, y, 1, 1);
						}
					}
					if (icdb == 10) {
						icd++;
						icdb = 0;
					}
					final ImageComponent px = new ImageComponent(image);
					px.setBackground(new Color(p.color));
					px.addMouseListener(new MouseListener() {

						public void mouseClicked(MouseEvent e) {
							Cpanel.remove(px);
							ret.remove(p);
							Cpanel.repaint();
							Cpanel.revalidate();
							if (icdb > 0)
								icdb--;
							else {
								icdb = 10;
								icd--;
							}
							panel.remove(ic);
							createIc();
							panel.repaint();
							panel.revalidate();
						}

						public void mouseEntered(MouseEvent e) {
							// TODO Auto-generated method stub

						}

						public void mouseExited(MouseEvent e) {
							// TODO Auto-generated method stub

						}

						public void mousePressed(MouseEvent e) {
							// TODO Auto-generated method stub

						}

						public void mouseReleased(MouseEvent e) {
							// TODO Auto-generated method stub

						}
					});
					setComponent(icdb++, icd, px);
					Cpanel.repaint();
					Cpanel.revalidate();
					panel.remove(ic);
					createIc();
					panel.repaint();
					panel.revalidate();
				}
			}

			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
		panel.add(ic, getGBC(0, 1, 1, 1));

	}
	private ChooseC() {
		super();
	}
	private void init(Captcha captcha) {

		this.captcha = captcha;
		this.setAlwaysOnTop(true);
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		setLayout(new BorderLayout());
		add(new JScrollPane(panel), BorderLayout.CENTER);
		setSize(captcha.getWidth() * 2 + 20, captcha.getHeight() * 2 + 300);
		setLocation(0, 0);
		Cpanel = new JPanel();
		Cpanel.setLayout(new GridBagLayout());

		panel.add(Cpanel, getGBC(0, 2, 1, 1));
		JButton bt = new JButton("OK");
		bt.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				destroy();

			}
		});
		setTitle("new ScrollPaneWindow");
		if (captcha != null) {
			createIc();
		}
		GridBagConstraints gb = Utilities.getGBC(0, 3, 1, 1);
		gb.anchor = GridBagConstraints.WEST;

		panel.add(bt, gb);
		gb.anchor = GridBagConstraints.EAST;

		tollerance = new JSpinner(new SpinnerNumberModel(25, 0, 100, 1));
		panel.add(tollerance, gb);
		// refreshUI();
		setVisible(true);
	}

	public void destroy() {
		super.destroy();
		synchronized (this) {
			this.notify();
		}
	}

	private void setComponent(final int x, final int y, final Component cmp) {
		if (cmp == null)
			return;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Cpanel.add(cmp, getGBC(x, y, 1, 1));
			}
		});
	}
	public static Vector<cPoint> getColors(Captcha captcha, Vector<cPoint> r) {
		ChooseC cc = new ChooseC();
		cc.ret=r;
		cc.init(captcha);
		synchronized (cc) {
			try {
				cc.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return cc.ret;
	}
	public static Vector<cPoint> getColors(Captcha captcha) {

		return getColors(captcha, new Vector<cPoint>());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
		System.out.println(path);
		File folder = new File(path+"/captchas/hotfile.com");
		File[] list = folder.listFiles();
		Captcha[] cs = new Captcha[7 < list.length ? 7 : list.length];

		JAntiCaptcha jac = new JAntiCaptcha(Utilities.getMethodDir(), "EasyCaptcha");
		Vector<cPoint> c = new Vector<cPoint>();
		for (int i = 0; i < cs.length; i++) {
			File captchafile = list[i];
			Image captchaImage = Utilities.loadImage(captchafile);

			Captcha captcha = jac.createCaptcha(captchaImage);
			captcha.setCaptchaFile(captchafile);
			cs[i] = captcha;
			c = getColors(captcha, c);
			for (cPoint j : c) {
				System.out.println("c:" + j.color);
			}
		}

	}

}

class cPoint extends Point {
	private static final long serialVersionUID = 1L;
	int color;
	int distance;

	public cPoint(int x, int y, int distance, Captcha captcha) {
		this(x, y, distance, captcha.getPixelValue(x, y));
	}

	public cPoint(int x, int y, int distance, int color) {
		super(x, y);
		this.color = color;
		this.distance = distance;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj) || ((cPoint) obj).color == color;
	}
}
