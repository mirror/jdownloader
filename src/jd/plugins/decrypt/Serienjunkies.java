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

package jd.plugins.decrypt;

import jd.plugins.DownloadLink;

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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Serienjunkies extends PluginForDecrypt {
	private static final String host = "Serienjunkies.org";

	private String version = "5.0.0.0";

	private boolean next = false;

	private static final int saveScat = 1;

	private static final int sCatNoThing = 0;

	private static final int sCatNewestDownload = 1;

	private static final int sCatGrabb = 2;

	// private static final String PATTERN_FOR_CAPTCHA_BOT = "Du hast zu oft das
	// Captcha falsch eingegeben";

	private static int[] useScat = new int[] { 0, 0 };
	public static String lastHtmlCode = "";

	private boolean scatChecked = false;

	private JComboBox methods;

	private JCheckBox checkScat;

	public Serienjunkies() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
		this.setConfigElements();

		default_password.add("serienjunkies.dl.am");
		default_password.add("serienjunkies.org");

	}

	/*
	 * Diese wichtigen Infos sollte man sich unbedingt durchlesen
	 */
	@Override
	public String getCoder() {
		// von coa gefixed
		return "JD-Team";
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

	private void sCatDialog() {
		if (scatChecked || useScat[1] == saveScat)
			return;
		new Dialog(((SimpleGUI) JDUtilities.getGUI()).getFrame()) {

			/**
			 * 
			 */
			private static final long serialVersionUID = -5144850223169000644L;

			void init() {
				setLayout(new BorderLayout());
				setModal(true);
				setTitle(JDLocale.L("Plugins.SerienJunkies.CatDialog.title",
						"SerienJunkies ::CAT::"));
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
						useScat = new int[] {
								((meth) methods.getSelectedItem()).var, 0 };
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
				meths[0] = new meth("Kategorie nicht hinzufügen", sCatNoThing);
				meths[1] = new meth(
						"Alle Serien in dieser Kategorie hinzufügen", sCatGrabb);
				meths[2] = new meth(
						"Den neusten Download dieser Kategorie hinzufügen",
						sCatNewestDownload);
				methods = new JComboBox(meths);
				checkScat = new JCheckBox(
						"Einstellungen für diese Sitzung beibehalten?", true);
				Insets insets = new Insets(0, 0, 0, 0);
				JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L(
						"Plugins.SerienJunkies.CatDialog.action",
						"Wählen sie eine Aktion aus:")),
						GridBagConstraints.RELATIVE,
						GridBagConstraints.RELATIVE,
						GridBagConstraints.RELATIVE, 1, 0, 0, insets,
						GridBagConstraints.NONE, GridBagConstraints.WEST);
				JDUtilities.addToGridBag(panel, methods,
						GridBagConstraints.RELATIVE,
						GridBagConstraints.RELATIVE,
						GridBagConstraints.REMAINDER, 1, 0, 0, insets,
						GridBagConstraints.NONE, GridBagConstraints.WEST);
				JDUtilities.addToGridBag(panel, checkScat,
						GridBagConstraints.RELATIVE,
						GridBagConstraints.RELATIVE,
						GridBagConstraints.REMAINDER, 1, 0, 0, insets,
						GridBagConstraints.NONE, GridBagConstraints.WEST);
				JButton btnOK = new JButton(JDLocale
						.L("gui.btn_continue", "OK"));
				btnOK.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						useScat = new int[] {
								((meth) methods.getSelectedItem()).var,
								checkScat.isSelected() ? saveScat : 0 };
						dispose();
					}

				});
				JDUtilities.addToGridBag(panel, btnOK,
						GridBagConstraints.RELATIVE,
						GridBagConstraints.RELATIVE,
						GridBagConstraints.REMAINDER, 1, 0, 0, insets,
						GridBagConstraints.NONE, GridBagConstraints.WEST);
				add(panel, BorderLayout.CENTER);
				pack();
				setVisible(true);
			}

		}.init();
	}

	private int getSerienJunkiesCat() {

		sCatDialog();
		return useScat[0];

	}

	private String isNext() {
		if (next)
			return "|";
		else
			next = true;
		return "";

	}

	@Override
	public boolean collectCaptchas() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean useUserinputIfCaptchaUnknown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Pattern getSupportedLinks() {
		return null;
	}

	@Override
	public synchronized boolean canHandle(String data) {
		boolean cat = false;
		// http://serienjunkies.org/?cat=3217
		if (data.contains("serienjunkies.org") && data.contains("cat=")) {
			cat = (getSerienJunkiesCat() != sCatNoThing);
		}
		boolean rscom = (Boolean) this.getProperties().getProperty(
				"USE_RAPIDSHARE", true);
		boolean rsde = (Boolean) this.getProperties().getProperty(
				"USE_RAPIDSHAREDE", false);
		boolean net = (Boolean) this.getProperties().getProperty("USE_NETLOAD",
				false);
		boolean uploaded = (Boolean) this.getProperties().getProperty(
				"USE_UPLOADED", false);
		boolean simpleupload = (Boolean) this.getProperties().getProperty(
				"USE_SIMLEUPLOAD", false);
		boolean filefactory = (Boolean) this.getProperties().getProperty(
				"USE_FILEFACTORY", false);
		next = false;
		String hosterStr = "";
		if (rscom || rsde || net || uploaded) {
			hosterStr += "(";
			if (rscom)
				hosterStr += isNext() + "rc[\\_\\-]";
			if (rsde)
				hosterStr += isNext() + "rs[\\_\\-]";
			if (net)
				hosterStr += isNext() + "nl[\\_\\-]";
			if (uploaded)
				hosterStr += isNext() + "ut[\\_\\-]";
			if (simpleupload)
				hosterStr += isNext() + "su[\\_\\-]";
			if (filefactory)
				hosterStr += isNext() + "ff[\\_\\-]";
			if (cat)
				hosterStr += isNext() + "cat\\=[\\d]+";
			hosterStr += ")";
		} else {
			hosterStr += "not";
		}
		// http://links.serienjunkies.org/f-3bd58945ab43eae0/Episode%2006.html
		Matcher matcher = Pattern.compile(
				"http://.{0,10}serienjunkies\\.org.*" + hosterStr + ".*",
				Pattern.CASE_INSENSITIVE).matcher(data);
		if (matcher.find()) {
			return true;
		} else {
			String[] links = new Regexp(data,
					"http://.{3,10}\\.serienjunkies.org/.*",
					Pattern.CASE_INSENSITIVE).getMatches(0);
			for (int i = 0; i < links.length; i++) {
				if (!links[i]
						.matches("(?i).*http://.{3,10}\\.serienjunkies.org/.*(rc[\\_\\-]|rs[\\_\\-]|nl[\\_\\-]|ut[\\_\\-]|su[\\_\\-]|ff[\\_\\-]|cat\\=[\\d]+).*"))
					return true;
			}
		}
		return false;
	}

	@Override
	public Vector<String> getDecryptableLinks(String data) {
		String[] links = new Regexp(
				data,
				"http://.*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es)[^\"]*",
				Pattern.CASE_INSENSITIVE).getMatches(0);
		Vector<String> ret = new Vector<String>();
		scatChecked = true;
		for (int i = 0; i < links.length; i++) {
			if (canHandle(links[i]))
				ret.add(links[i]);
		}
		return ret;
	}

	@Override
	public String cutMatches(String data) {
		return data
				.replaceAll(
						"(?i)http://.*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es).*",
						"--CUT--");
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean doBotCheck(File file) {
		return false;
	}

	@Override
	public PluginStep doStep(PluginStep step, String parameter) {
		switch (step.getStep()) {
		case PluginStep.STEP_DECRYPT:

			Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
			if (parameter.matches(".*\\?cat\\=[\\d]+.*")) {
				int catst = getSerienJunkiesCat();
				scatChecked = false;
				int cat = Integer.parseInt(parameter.replaceFirst(
						".*cat\\=", "").replaceFirst("[^\\d].*", ""));
				if (sCatNewestDownload == catst) {
					request.withHtmlCode = false;
					request.redirect = false;
					request.getRequest("http://serienjunkies.org/");
					request.withHtmlCode = true;
					request.getRequest("http://serienjunkies.org/");


					Pattern pattern = Pattern
							.compile(
									"<a href=\"http://serienjunkies.org/\\?cat\\=([\\d]+)\">(.*?)</a><br",
									Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(request.getHtmlCode());
					String name = null;
					while (matcher.find()) {
						if (Integer.parseInt(matcher.group(1)) == cat) {
							name = matcher.group(2).toLowerCase();
							break;
						}
					}
					if (name == null)
						return null;
					request.getRequest(parameter);
					name += " ";
					String[] bet = null;
					while (bet == null) {
						name = name.substring(0, name.length() - 1);
						if (name.length() == 0)
							return null;
						try {
							bet = request.getRegexp(
									"<p><strong>(" + name
											+ ".*?)</strong>(.*?)</p>")
									.getMatches()[0];
						} catch (Exception e) {
							// TODO: handle exception
						}

					}
					lastHtmlCode=request.getHtmlCode();
					String[] links = getHttpLinks(bet[1], request.urlToString());
					for (int i = 0; i < links.length; i++) {
								decryptedLinks.add(this
										.createDownloadlink(links[i]));
					}

					step.setParameter(decryptedLinks);
					return null;
				} else if (catst == sCatGrabb) {
					request.getRequest("http://serienjunkies.org/?cat="+cat);
					String htmlcode = request.getHtmlCode();
					try {
						int pages = Integer.parseInt(request.getRegexp(
								"<p align=\"center\">  Pages \\(([\\d]+)\\):")
								.getFirstMatch());
						for (int i = 2; i < pages+1; i++) {
							htmlcode += "\n" + request.getRequest("http://serienjunkies.org/?cat="+cat+"&paged="+i);
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
					String[] sp = htmlcode
							.split("<strong>Größe:</strong>[\\s]*");
					for (int d = 0; d < sp.length; d++) {
						int size = Integer.parseInt(new Regexp(sp[d], "[\\d]+")
								.getFirstMatch());
						String[][] links = new Regexp(sp[d],
								"<p><strong>(.*?)</strong>(.*?)</p>")
								.getMatches();
						for (int i = 0; i < links.length; i++) {
							String[] links2 = getHttpLinks(links[i][1],
									parameter);
							for (int j = 0; j < links2.length; j++) {
								if (canHandle(links2[j])) {
									if (this.getProperties()
											.getBooleanProperty(
													"USE_DIREKTDECRYPT", false)) {
										step
												.setParameter((new jd.plugins.host.Serienjunkies())
														.getDLinks(links2[j]));
									} else {
										DownloadLink dlink = new DownloadLink(
												this,
												null,
												this.getHost(),
												JDUtilities
														.htmlDecode(links2[j]
																.replaceFirst(
																		"(?i)serienjunkies",
																		"sjdownload").replaceFirst("(/..[\\_\\-])", "$1/")),
												true);
										if (links[i][0] != null) {
											dlink.setSourcePluginComment(links[i][0]);
											if (parameter
													.matches("http://serienjunkies.org/sa[fv]e/.*"))
												size = 100;
											dlink
													.setDownloadMax(size * 1024 * 1024);
											dlink.setStatusText(links[i][0]);
										}
										dlink.setStatusText(getHostname(links2[j]));
										decryptedLinks.add(dlink);
										step.setParameter(decryptedLinks);
									}

								}

							}
						}
					}

					step.setParameter(decryptedLinks);
					return null;
				} else {
					return null;
				}
			}
			if (this.getProperties().getBooleanProperty("USE_DIREKTDECRYPT",
					false)) {
				step.setParameter((new jd.plugins.host.Serienjunkies())
						.getDLinks(parameter));
			} else {

				String name = null;
				// if (!parameter
				// .matches("http://serienjunkies.org/(sjsa[fv]e|sa[fv]e)/.*"))
				// {
				getLinkName(parameter);
				int size = 100;
				request.getRequest("http://serienjunkies.org/?s="
						+ parameter.replaceFirst(".*/", "").replaceFirst(
								"\\.html?$", "") + "&submit=Suchen");
				lastHtmlCode = request.getHtmlCode();
				String[] info = getLinkName(parameter);
				if (info != null) {
					name = info[1];
					size = Integer.parseInt(info[0]);
				}
				
				DownloadLink dlink = new DownloadLink(this, null, "rapidshare.com", JDUtilities.htmlDecode(parameter
						.replaceFirst("(?i)serienjunkies", "sjdownload").replaceFirst("(/..[\\_\\-])", "$1/")), true);
				if (name != null) {
					dlink.setSourcePluginComment(name);
					if (parameter
							.matches("http://serienjunkies.org/sa[fv]e/.*"))
						size = 100;
					dlink.setDownloadMax(size * 1024 * 1024);
				}
				dlink.setStatusText(getHostname(parameter));
				decryptedLinks.add(dlink);
				step.setParameter(decryptedLinks);
			}
		}
		return null;
	}
	
	private String getHostname(String link)
	{
		if(link.matches(".*rc[\\_\\-].*"))
			return "Rapidshare.com";
		else if(link.matches(".*rs[\\_\\-].*"))
			return "Rapidshare.de";
		else if(link.matches(".*nt[\\_\\-].*"))
			return "Netload.in";
		else if(link.matches(".*ut[\\_\\-].*"))
			return "Uploaded.to";
		else if(link.matches(".*su[\\_\\-].*"))
			return "SimpleUpload.net";
		else if(link.matches(".*ff[\\_\\-].*"))
			return "FileFactory.com";
		else
			return "Rapidshare.com";
	}
	private String[] getLinkName(String link) {
		String[] sp = lastHtmlCode.split("<strong>Größe:</strong>[\\s]*");
		for (int j = 0; j < sp.length; j++) {
			String size = new Regexp(sp[j], "[\\d]+").getFirstMatch();
			String[][] links = new Regexp(sp[j],
					"<p><strong>(.*?)</strong>(.*?)</p>").getMatches();

			for (int i = 0; i < links.length; i++) {
				try {

					if (links[i][1].contains(link))
						return new String[] { size, links[i][0] };
				} catch (Exception e) {
					// TODO: handle exception
				}

			}
		}

		return null;
	}

	private void setConfigElements() {
		ConfigEntry cfg;
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
				getProperties(), "USE_DIREKTDECRYPT", "Sofort entschlüsseln"));
		cfg.setDefaultValue(false);
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL,
				"Hoster Auswahl"));
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
				getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
		cfg.setDefaultValue(true);
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
				getProperties(), "USE_RAPIDSHAREDE", "Rapidshare.de"));
		cfg.setDefaultValue(false);
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
				getProperties(), "USE_NETLOAD", "Netload.in"));
		cfg.setDefaultValue(false);
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
				getProperties(), "USE_UPLOADED", "Uploaded.to"));
		cfg.setDefaultValue(false);
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
				getProperties(), "USE_SIMLEUPLOAD", "SimpleUpload.net"));
		cfg.setDefaultValue(false);
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
				getProperties(), "USE_FILEFACTORY", "FileFactory.com"));
		cfg.setDefaultValue(false);

	}

}
