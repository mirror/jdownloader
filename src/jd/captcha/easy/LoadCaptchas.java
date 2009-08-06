package jd.captcha.easy;

import jd.captcha.utils.Utilities;

import jd.utils.JDUtilities;

import jd.http.Browser;

import jd.captcha.gui.ImageComponent;

import jd.captcha.gui.BasicWindow;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class LoadCaptchas extends BasicWindow {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String[] getImages(Browser br) throws Exception {
		ArrayList<String> ret = new ArrayList<String>();
		Pattern[] basePattern = new Pattern[] {
				Pattern
						.compile(
								"(?s)<[ ]?input[^>]*?type=.?image[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s]",
								Pattern.CASE_INSENSITIVE),
				Pattern.compile(
						"(?s)<[ ]?IMG[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s]",
						Pattern.CASE_INSENSITIVE) };
		for (Pattern element : basePattern) {
			Matcher m = element.matcher(br.toString().toLowerCase());
			while (m.find()) {
				try {
					String src = m.group(1);
					if (!src.startsWith("http")) {
						if (src.charAt(0) == '/') {
							src = "http://" + br.getHost() + src;
						} else if (src.charAt(0) == '#') {
							src = "http://" + br.getURL() + src;
						} else {
							src = br.getBaseURL() + src;
						}
					}
					if (!ret.contains(src))
						ret.add(src);
				} catch (Exception e) {
					// TODO: handle exception
				}

			}
		}

		return ret.toArray(new String[] {});
	}
	private void openDir(String dir)
	{
		if(JOptionPane.showConfirmDialog(null, "Captcha Ordner:"+dir+" jetzt öffnen?")==JOptionPane.YES_OPTION)
            JDUtilities.openExplorer(new File(dir));

	}
	public LoadCaptchas() throws Exception {
		super();
		final String link = JOptionPane.showInputDialog("Bitte Link eingeben:");
		final int menge = Integer.parseInt(JOptionPane.showInputDialog(
				"Wieviele Captchas sollen heruntergeladen werden:", "500"));


		final Browser br = new Browser();
		br.getPage(link);
		String host = br.getHost().toLowerCase();
		if (host.matches(".*\\..*\\..*"))
			host = host.substring(host.indexOf('.') + 1);
		final String dir = JDUtilities.getJDHomeDirectoryFromEnvironment()
		.getAbsolutePath()
		+ "/captchas/" + host + "/";
		new File(dir).mkdir();
		String ct = br.getHttpConnection().getContentType().toLowerCase();
		if(ct !=null && ct.contains("image"))
		{
			for (int k = 0; k < menge; k++) {
				try {
					String ft = ".jpg";
					if (ct.equals("image/jpeg"))
						ft = ".jpg";
					else
					{
						ft=ct.replaceFirst("image/", ".");
					}
					File f2 = new File(dir
							+ System.currentTimeMillis()
							+ ft);
					br.getDownload(f2, link);

				} catch (Exception ev) {
					// TODO Auto-generated catch block
					ev.printStackTrace();
				}

			}
			openDir(dir);
			return;
		}
		this.setAlwaysOnTop(true);
		JPanel panel = new JPanel();

		setLayout(new BorderLayout());
		add(new JScrollPane(panel), BorderLayout.CENTER);
		setLocation(0, 0);
		setTitle("Klicken sie auf das Captcha");

		final String[] images = getImages(br);
		panel.setLayout(new GridLayout(images.length / 5 + 1, 5));
		final File[] files = new File[images.length];
		for (int j = 0; j < images.length; j++) {
			final int i = j;
			String ft = ".jpg";
			if (images[i].toLowerCase().contains("png"))
				ft = ".png";
			else if (images[i].toLowerCase().contains("gif"))
				ft = ".gif";
			else
			{
				br.getPage(images[i]);
				ct = br.getHttpConnection().getContentType().toLowerCase();
				if(ct !=null && ct.contains("image"))
				{
					if (ct.equals("image/jpeg"))
						ft = ".jpg";
					else
					{
						ft=ct.replaceFirst("image/", ".");
					}
				}
			}
			final String filetype = ft;
			final File f = new File(dir, System.currentTimeMillis() + filetype);
			files[i] = f;
			try {
				br.getDownload(f, images[i]);
			} catch (Exception e) {
				f.delete();
				continue;
			}
			Image captchaImage = Utilities.loadImage(f);
			int area = captchaImage.getHeight(null)
					* captchaImage.getHeight(null);
			if (area < 50 || area > 50000 || captchaImage.getHeight(null) > 400
					|| captchaImage.getWidth(null) > 400) {
				f.delete();

				continue;
			}
			ImageComponent ic0 = new ImageComponent(captchaImage);

			panel.add(ic0);
			ic0.addMouseListener(new MouseListener() {

				public void mouseClicked(MouseEvent e) {
					destroy();
					try {
						Browser brss = br.cloneBrowser();
						brss.getPage(link);
						String[] im = getImages(brss);
						File f2 = new File(dir + System.currentTimeMillis()
								+ filetype);
						br.getDownload(f2, im[i]);
						for (File file : files) {
							if (!file.equals(f))
								file.delete();
						}
						if (im[i].equals(images[i])) {
							for (int k = 0; k < menge - 2; k++) {
								final Browser brs = br.cloneBrowser();
								try {
									f2 = new File(dir
											+ System.currentTimeMillis()
											+ filetype);
									brs.getDownload(f2, images[i]);

								} catch (Exception ev) {
									// TODO Auto-generated catch block
									ev.printStackTrace();
								}

							}
						} else {
							for (int k = 0; k < menge - 2; k++) {

								final Browser brs = br.cloneBrowser();

								brs.getPage(link);

								try {
									f2 = new File(dir
											+ System.currentTimeMillis()
											+ filetype);

									brs.getDownload(f2, getImages(brs)[i]);

								} catch (Exception ev) {
									// TODO Auto-generated catch block
									ev.printStackTrace();
								}

							}
						}
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					openDir(dir);
				}

				public void mouseEntered(MouseEvent e) {
				}

				public void mouseExited(MouseEvent e) {
				}

				public void mousePressed(MouseEvent e) {
				}

				public void mouseReleased(MouseEvent e) {
				}
			});
		}

		this.pack();

		setVisible(true);
	}

	public static void main(String[] args) throws Exception {

		new LoadCaptchas();
	}
}
