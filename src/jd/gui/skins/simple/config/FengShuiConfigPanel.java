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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.gui.skins.simple.Progressor;


import jd.router.GetRouterInfo;

import jd.config.Configuration;

import jd.gui.skins.simple.components.BrowseFile;

import jd.config.SubConfiguration;

import jd.gui.skins.simple.Link.JLinkButton;

import jd.utils.JDTheme;

import jd.JDInit;

import jd.config.MenuItem;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import jd.utils.JDLocale;

import jd.gui.skins.simple.SimpleGUI;

import jd.utils.JDUtilities;

import jd.gui.UIInterface;

import net.miginfocom.swing.MigLayout;

public class FengShuiConfigPanel extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1715405893428812995L;
	private static final String GAPLEFT = "gapleft 15!, ";
	private static final String GAPBOTTOM = ", gapbottom :60:push";
	private static final String SPAN = ", spanx" + GAPBOTTOM;

	private JButton more, apply, cancel, premium, btnAutoConfig,
			btnSelectRouter, btnTestReconnect;
	private JComboBox languages;
	private SubConfiguration guiConfig = JDUtilities
			.getSubConfig(SimpleGUI.GUICONFIGNAME);
	private Configuration config = JDUtilities.getConfiguration();
	private BrowseFile downloadDirectory;
	private String ddir = null;
	private JTextField username, password, ip, routername;
	private String routerIp =null;
	private Progressor prog;
	private JPanel panel;
	private JProgressBar progress = null;
	private Object getLanguage() {
		return guiConfig.getProperty(SimpleGUI.PARAM_LOCALE, Locale
				.getDefault());
	}

	public String getDownloadDirectory() {
		if (ddir == null) {
			ddir = config
					.getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY);
			if (ddir == null)
				ddir = JDUtilities.getResourceFile("downloads")
						.getAbsolutePath();
		}
		return ddir;
	}

	public void getRouterIp() {

		new Thread(new Runnable() {

			public void run() {
				if (routerIp == null || routerIp.matches("[\\s]*")) {
						// System.out.println(routerIp);
						ip.setText(new GetRouterInfo(prog).getAdress());

				}
				if(routername.getText()==null || routername.getText().matches("[\\s]*"))
				{
					if(GetRouterInfo.isFritzbox(ip.getText()))
					{
						if(GetRouterInfo.isUpnp(ip.getText(), "49000"))
						{
							JDUtilities.getGUI().showHTMLDialog("Fritz!Box erkannt", "Sie haben eine Fritz!Box, der Reconnect läuft über Upnp.<br> Sie brauchen keinen Reconnecteinstellungen zu tätigen.");
						}
						else
						{
						JDUtilities.getGUI().showHTMLDialog("Fritz!Box erkannt", "Bitte aktivieren sie Upnp bei ihrer Fritz!Box <br>" +
									"<a href=\"http://"+ip.getText()+"\">zur Fritz!Box</a><br><a href=\"http://wiki.jdownloader.org/index.php?title=Fritz!Box_Upnp\">Wikiartikel: Fritz!Box Upnp</a>");
			
					
							
						}
					}
				}
			}
		}).start();

	}

	private JPanel getPanel() {
		panel = new JPanel(new MigLayout("ins 32 22 15 22",
				"[right, pref!]0[right,grow,fill]0[]"));
		routerIp=config.getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
		addSeparator(
				panel,
				JDLocale.L("gui.config.general.name", "Allgemein"),
				JDUtilities.getscaledImageIcon(JDTheme
						.V("gui.images.configuration"), 32, 32),
				JDLocale
						.L(
								"gui.fengshuiconfig.general.tooltip",
								"<html>Some hosters use captchas which are impossible<br> to enter for people with disabilities. With this functionality<br> the JD team addresses the requirement for<br> people with disabilities not to be discriminated against.<br> Read more about it at:<br> <b>http://en.wikipedia.org/wiki/Web_accessibility"));
		languages = new JComboBox(JDLocale.getLocaleIDs().toArray(
				new String[] {}));
		languages.setSelectedItem(getLanguage());
		addComponents(panel, JDLocale.L("gui.config.gui.language", "Sprache"),
				languages);
		JPanel subPanel = new JPanel(new MigLayout("ins 0",
				"0[fill, grow]15[]0"));
		downloadDirectory = new BrowseFile();
		downloadDirectory.setEditable(true);

		downloadDirectory.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		downloadDirectory.setText(getDownloadDirectory());

		subPanel.add(downloadDirectory);
		addComponents(panel, JDLocale.L("gui.config.general.downloadDirectory",
				"Downloadverzeichnis"), subPanel);

		addSeparator(
				panel,
				JDLocale.L("gui.config.plugin.host.name", "Host Plugins"),
				JDUtilities.getscaledImageIcon(JDTheme
						.V("gui.images.config.host"), 32, 32),
				JDLocale
						.L(
								"gui.fengshuiconfig.plugin.host.tooltip",
								"<html>If you have a Premium Account for a hoster you can enter you login<br> password here and JD will use them automatically henceforth<br> if you download files with that hoster"));
		panel
				.add(premium = new JButton(JDLocale.L("gui.menu.plugins.phost",
						"Premium Hoster")), GAPLEFT
						+ "align leading, wmax pref" + SPAN);
		premium.addActionListener(this);

		JLinkButton label;
		if(routerIp==null)
		progress = new JProgressBar();
		try {
			label = new JLinkButton("<html><u><b  color=\"#006400\">"
					+ JDLocale.L("gui.config.reconnect.name", "Reconnect"),
					JDUtilities.getscaledImageIcon(JDTheme
							.V("gui.images.reconnect"), 32, 32), new URL(
							"http://google.de"));

			label.setIconTextGap(8);
			panel.add(label, "align left, split 2");
			panel.add(new JSeparator(), "gapleft 10, spanx, pushx, growx");
			if(routerIp==null)
			panel.add(progress, "span 3, pushx, growx");
			else
				panel.add(new JSeparator(), "span 3, pushx, growx",15);
			JLabel tip = new JLabel(JDUtilities.getscaledImageIcon(JDTheme
					.V("gui.images.config.tip"), 16, 16));
			tip.setToolTipText(JDLocale.L(
					"gui.fengshuiconfig.reconnect.tooltip",
					"<html>Sometimes your Router needs a kick in the balls!"));
			panel.add(tip, GAPLEFT + "w pref!, wrap");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Container reconnectPanel = new JPanel(new MigLayout());
		reconnectPanel.add(btnAutoConfig = new JButton(JDLocale.L(
				"gui.config.liveHeader.autoConfig",
				"Router automatisch setzten")), "pushx");
		btnAutoConfig.addActionListener(this);
		reconnectPanel.add(btnSelectRouter = new JButton(JDLocale.L(
				"gui.config.liveHeader.selectRouter", "Router auswählen")),
				"w pref!");
		btnSelectRouter.addActionListener(this);
		reconnectPanel.add(btnTestReconnect = new JButton(JDLocale.L(
				"modules.reconnect.testreconnect", "Test reconnect")),
				"w pref!, wrap");
		btnTestReconnect.addActionListener(this);

		panel.add(reconnectPanel, "spanx, pushx, growx");
		reconnectPanel = new JPanel(new MigLayout());

		reconnectPanel.add(new JLabel(JDLocale.L(
				"gui.config.fengshui.routerip", "RouterIP:")));
		reconnectPanel.add(ip = new JTextField(12));
		ip.setText(routerIp);
		reconnectPanel.add(new JLabel(JDLocale.L(
				"gui.config.fengshui.routername", "Routername:")));
		reconnectPanel.add(routername = new JTextField(12), "wrap");
		routername.setEnabled(false);
		routername.setEditable(false);

		reconnectPanel.add(new JLabel(JDLocale.L("gui.config.fengshui.user",
				"Username:")));
		reconnectPanel.add(username = new JTextField(12));
		username.setText(config.getStringProperty(
				"Configuration.PARAM_HTTPSEND_USER", ""));
		reconnectPanel.add(new JLabel(JDLocale.L(
				"gui.config.fengshui.password", "Password:")));
		reconnectPanel.add(password = new JTextField(12));
		password.setText(config.getStringProperty(
				"Configuration.PARAM_HTTPSEND_PASS", ""));
		panel.add(reconnectPanel, "spanx, pushx, growx");

		prog = new Progressor() {
			private static final long serialVersionUID = 1L;

			public int getMaximum() {
				return progress.getMaximum();
			}

			public String getMessage() {
				return null;
			}

			public int getMinimum() {
				return progress.getMinimum();
			}

			public String getString() {
				return progress.getString();
			}

			public int getValue() {
				return progress.getValue();
			}

			public void setMaximum(int value) {
				progress.setMaximum(value);

			}

			public void setMessage(String txt) {

			}

			public void setMinimum(int value) {
				progress.setMinimum(value);
			}

			public void setString(String txt) {

				progress.setString(txt);

			}

			public void setStringPainted(boolean v) {
				progress.setStringPainted(v);
			}

			public void setThread(Thread th) {

			}

			public void setValue(int value) {
				progress.setValue(value);
				if (value == 100) {
					progress.removeAll();
					
					
					panel.remove(15);
					progress=null;
					panel.invalidate();
					panel.add(new JSeparator(), "span 3, pushx, growx",15);
					panel.validate();
					panel.repaint();
				}
			}
		};

		getRouterIp();

		JPanel bpanel = new JPanel(new MigLayout());
		bpanel.add(new JSeparator(), "spanx, pushx, growx, gapbottom 5");
		bpanel.add(more = new JButton("More"), "tag help2");
		more.addActionListener(this);
		bpanel.add(apply = new JButton("Apply"), "w pref!, tag apply");
		apply.addActionListener(this);
		bpanel.add(cancel = new JButton("Cancel"), "w pref!, tag cancel, wrap");
		cancel.addActionListener(this);
		panel.add(bpanel, "spanx, pushx, growx");

		return panel;
	}

	private void addComponents(JPanel panel, String label,
			JComponent... components) {

		panel.add(new JLabel("<html><b color=\"#4169E1\">" + label),
				"gapleft 22, gaptop 5" + GAPBOTTOM);
		for (int i = 0; i < components.length; i++) {
			if (i == components.length - 1)
				panel.add(components[i], GAPLEFT + SPAN + ", gapright 5");
			else
				panel.add(components[i], GAPLEFT);
		}
	}

	private void addSeparator(JPanel panel, String title, Icon icon, String help) {
		JLinkButton label;
		try {
			label = new JLinkButton("<html><u><b  color=\"#006400\">" + title,
					icon, new URL("http://google.de"));

			label.setIconTextGap(8);
			panel.add(label, "align left, split 2");
			panel.add(new JSeparator(), "gapleft 10, spanx, pushx, growx");
			panel.add(new JSeparator(), "span 3, pushx, growx");

			JLabel tip = new JLabel(JDUtilities.getscaledImageIcon(JDTheme
					.V("gui.images.config.tip"), 16, 16));
			tip.setToolTipText(help);
			panel.add(tip, GAPLEFT + "w pref!, wrap");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public FengShuiConfigPanel() {
		super();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setTitle("Feng Shui Config");

		JPanel panel = getPanel();
		Dimension minSize = panel.getMinimumSize();
		this.setContentPane(new JScrollPane(panel));
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.pack();
		Dimension ps = this.getPreferredSize();
		this.setPreferredSize(new Dimension(Math.min(800, ps.width), Math.min(
				600, ps.height)));
		this.pack();
		panel.setPreferredSize(minSize);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	public static void main(String[] args) {
		new FengShuiConfigPanel();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel)
			dispose();
		else if (e.getSource() == more) {
//			save();
			UIInterface ui = JDUtilities.getGUI();
			ConfigurationDialog.showConfig(((SimpleGUI) ui).getFrame(), ui);
			// ConfigurationDialog.showConfig(new JFrame(), ui);
			dispose();
		} else if (e.getSource() == apply) {
			save();
			dispose();
		} else if (e.getSource() == premium) {
			final JDInit init = new JDInit(null);
			init.initPlugins();
			JPopupMenu popup = new JPopupMenu(JDLocale.L(
					"gui.menu.plugins.phost", "Premium Hoster"));

			for (Iterator<PluginForHost> it = JDUtilities.getPluginsForHost()
					.iterator(); it.hasNext();) {
				final Plugin helpplugin = it.next();
				if (helpplugin.createMenuitems() != null) {
					JMenu item;
					popup.add(item = new JMenu(helpplugin.getPluginName()));
					item.setHorizontalTextPosition(JMenuItem.RIGHT);

					// m.setItems(helpplugin.createMenuitems());

					if (item != null) {
						popup.add(item);

						item.addMenuListener(new MenuListener() {
							public void menuCanceled(MenuEvent e) {
							}

							public void menuDeselected(MenuEvent e) {
							}

							public void menuSelected(MenuEvent e) {
								JMenu m = (JMenu) e.getSource();
								JMenuItem c;
								m.removeAll();
								for (MenuItem menuItem : helpplugin
										.createMenuitems()) {
									c = SimpleGUI.getJMenuItem(menuItem);
									if (c == null) {
										m.addSeparator();
									} else {
										m.add(c);
									}

								}

							}

						});
					} else {
						popup.addSeparator();
					}
				}
			}

			popup.show(((JButton) e.getSource()), 100, 25);
		}

	}

	public void save() {
		boolean saveit = false;
		if (!getLanguage().equals(languages.getSelectedItem())) {
			guiConfig.setProperty(SimpleGUI.PARAM_LOCALE, languages
					.getSelectedItem());
			guiConfig.save();
		}
		if (!downloadDirectory.getText().equals(getDownloadDirectory())) {
			config.setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY,
					downloadDirectory.getText());
			saveit = true;
		}
		if(!ip.getText().matches("[\\s]*") && !ip.getText().equals(routerIp))
		{
			config.setProperty(Configuration.PARAM_HTTPSEND_IP,
					ip.getText());
			saveit = true;
		}
		if (saveit)
			JDUtilities.saveConfig();

	}
}
