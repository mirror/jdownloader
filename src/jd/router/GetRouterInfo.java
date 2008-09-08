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

package jd.router;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import jd.http.HTTPConnection;

import jd.gui.skins.simple.Progressor;

import jd.gui.skins.simple.config.GUIConfigEntry;

import jd.gui.skins.simple.ConfirmCheckBoxDialog;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.utils.JDLocale;

import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.gui.skins.simple.ProgressDialog;
import jd.http.Browser;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

public class GetRouterInfo {
	private class InternalAuthenticator extends Authenticator {
		private String username, password;

		public InternalAuthenticator(String user, String pass) {
			username = user;
			password = pass;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}
	public static boolean isFritzbox(String iPaddress)
	{
		Browser br = new Browser();
		try {
			String html = br.getPage("http://" + iPaddress);
			if(html.matches("(?is).*fritz.?box.*"))return true;
		} catch (Exception e) {
		}
		return false;
	}
	public static boolean isUpnp(String iPaddress,String port)
	{
//		curl "http://fritz.box:49000/upnp/control/WANIPConn1" -H "Content-Type: text/xml; charset="utf-8"" -H "SoapAction: urn:schemas-upnp-org:service:WANIPConnection:1#GetStatusInfo" -d "" -s
		
			Browser br = new Browser();
			try {
				
				HashMap<String, String> h = new HashMap<String, String>();	
				h.put("SoapAction", "urn:schemas-upnp-org:service:WANIPConnection:1#GetStatusInfo");
				h.put("CONTENT-TYPE", "text/xml; charset=\"utf-8\"");
				br.setHeaders(h);
				HTTPConnection con = br.openPostConnection("http://"+iPaddress+":"+port+"/upnp/control/WANIPConn1", "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:GetStatusInfo xmlns:u='urn:schemas-upnp-org:service:WANIPConnection:1' /> </s:Body> </s:Envelope>");
			
				if(con.getHeaderField(null).contains("200")) return true;
			} catch (Exception e) {
			}
		return false;
	}
	public static boolean validateIP(String iPaddress) {
		final Pattern IP_PATTERN = Pattern
				.compile("\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
		if( IP_PATTERN.matcher(iPaddress).matches())
			return true;
		else
		{
			try {
				if (InetAddress.getByName(iPaddress).isReachable(
						1500)) {
					return true;
				}
			} catch (UnknownHostException e) {

				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
		return false;
	}

	public String adress = null;

	public boolean cancel = false;
	public boolean testAll = false;

	private Logger logger = JDUtilities.getLogger();

	private String loginPass;

	private String loginUser;

	public String password = null;

	private Progressor progressBar;

	private Vector<String[]> routerDatas = null;

	public String username = null;

	public GetRouterInfo(Progressor progress) {
		progressBar = progress;
		if (progressBar != null) {
			progressBar.setMaximum(100);
		}
	}

	private boolean checkport80(String host) {
		Socket sock;
		try {
			sock = new Socket(host, 80);
			sock.setSoTimeout(200);
			return true;
		} catch (UnknownHostException e) {
		} catch (IOException e) {
		}
		return false;

	}

	public String getAdress() {
		if (adress != null) {
			return adress;
		}
		setProgressText("try to find the router ip");
		// String[] hosts = new String[]{"192.168.2.1", "192.168.1.1",
		// "192.168.0.1", "fritz.box"};

		if (new File("/sbin/route").exists()) {
			try {
				String routingt = JDUtilities.runCommand("/sbin/route", null,
						"/", 2).replaceFirst(".*\n.*", "");
				Pattern pattern = Pattern.compile(".{16}(.{16}).*",
						Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(routingt);
				while (matcher.find()) {
					String hostname = matcher.group(1).trim();
					if (!hostname.matches("[\\s]*\\*[\\s]*")) {
						setProgressText("testing " + hostname);
						try {
							if (InetAddress.getByName(hostname).isReachable(
									1500)) {
								if (checkport80(hostname)) {
									adress = hostname;
									return adress;
								}
							}
						} catch (UnknownHostException e) {

							e.printStackTrace();
						} catch (IOException e) {

							e.printStackTrace();
						}
					}

				}
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
		Vector<String> hosts = new Vector<String>();
		if (!hosts.contains("192.168.2.1")) {
			hosts.add("192.168.2.1");
		}
		if (!hosts.contains("192.168.1.1")) {
			hosts.add("192.168.1.1");
		}
		if (!hosts.contains("192.168.0.1")) {
			hosts.add("192.168.0.1");
		}
		if (!hosts.contains("fritz.box")) {
			hosts.add("fritz.box");
		}

		String ip = null;
		String localHost;
		try {
			localHost = InetAddress.getLocalHost().getHostName();
			for (InetAddress ia : InetAddress.getAllByName(localHost)) {

				if (GetRouterInfo.validateIP(ia.getHostAddress() + "")) {
					ip = ia.getHostAddress();

					if (ip != null) {
						String host = ip.substring(0, ip.lastIndexOf("."))
								+ ".";
						for (int i = 0; i < 255; i++) {
							String lhost = host + i;
							if (!lhost.equals(ip) && !hosts.contains(lhost)) {
								hosts.add(lhost);
							}

						}
					}
				}
			}

		} catch (UnknownHostException exc) {
		}
		int size = hosts.size();

		for (int i = 0; i < size && !cancel; i++) {
			setProgress(i * 100 / size);
			final String hostname = hosts.get(i);
			setProgressText("testing " + hostname);
			try {
				if (InetAddress.getByName(hostname).isReachable(1500)) {
					if (checkport80(hostname)) {
						adress = hostname;
						setProgress(100);
						return adress;
					}
				}

			} catch (IOException e) {
			}
		}
		setProgress(100);
		return null;

	}

	public String[] getRouterData(String ip) {
		setProgressText("Get Routerdata");
		adress = ip;
		if (testAll)
			routerDatas = new HTTPLiveHeader().getLHScripts();
		if (getRouterDatas() == null) {
			return null;
		}

		int retries = JDUtilities.getConfiguration().getIntegerProperty(
				Configuration.PARAM_HTTPSEND_RETRIES, 5);
		int wipchange = JDUtilities.getConfiguration().getIntegerProperty(
				Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 20);
		JDUtilities.getConfiguration().setProperty(
				Configuration.PARAM_HTTPSEND_RETRIES, 0);
		JDUtilities.getConfiguration().setProperty(
				Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 10);
		JDUtilities.getConfiguration().setProperty(
				Configuration.PARAM_HTTPSEND_USER, username);
		JDUtilities.getConfiguration().setProperty(
				Configuration.PARAM_HTTPSEND_PASS, password);
		final int size = routerDatas.size();
		for (int i = 0; i < size && !cancel; i++) {

			String[] data = routerDatas.get(i);
			if (data.length < 6) {
				String[] newDat = new String[6];
				for (int j = 0; j < data.length; j++) {
					newDat[j] = data[j];
				}
				for (int j = data.length; j < newDat.length; j++) {
					newDat[j] = "";
				}
				data = newDat;
			}
			setProgressText("Testing router: " + data[1]);
			setProgress(i * 100 / size);

			if (isEmpty(username)) {
				JDUtilities.getConfiguration().setProperty(
						Configuration.PARAM_HTTPSEND_USER, data[4]);
			} else {
				data[4] = username;
			}
			if (isEmpty(password)) {
				JDUtilities.getConfiguration().setProperty(
						Configuration.PARAM_HTTPSEND_PASS, data[5]);
			} else {
				data[5] = password;
			}
			JDUtilities.getConfiguration().setProperty(
					Configuration.PARAM_HTTPSEND_REQUESTS, data[2]);
			JDUtilities.saveConfig();
			if (Reconnecter.waitForNewIP(1)) {
				JDUtilities.getConfiguration().setProperty(
						Configuration.PARAM_HTTPSEND_RETRIES, retries);
				JDUtilities.getConfiguration()
						.setProperty(
								Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE,
								wipchange);
				JDUtilities.saveConfig();
				setProgress(100);
				return data;
			}
		}
		setProgress(100);
		return null;
	}

	public Vector<String[]> getRouterDatas() {
		if (routerDatas != null)
			return routerDatas;
		if (getAdress() == null)
			return null;

		try {
			// progress.setStatusText("Load possible RouterDatas");
			Authenticator.setDefault(new InternalAuthenticator(loginUser,
					loginPass));

			Browser br = new Browser();
			String html = br.getPage("http://" + adress).toLowerCase();
			Vector<String[]> routerData = new HTTPLiveHeader().getLHScripts();
			Vector<String[]> retRouterData = new Vector<String[]>();
			for (int i = 0; i < routerData.size(); i++) {
				String[] dat = routerData.get(i);
				if (dat.length > 3)
					if (html.contains(dat[0].toLowerCase())
							|| html.contains(dat[1].toLowerCase())
							|| html.matches(dat[3])) {
						retRouterData.add(dat);
					} else {
						if (html.contains(dat[0].toLowerCase())
								|| html.contains(dat[1].toLowerCase())) {
							retRouterData.add(dat);
						}
					}
			}
			routerDatas = retRouterData;
			return retRouterData;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private boolean isEmpty(String arg) {
		return (arg == null || arg.matches("[\\s]*"));
	}

	public void setLoginPass(String text) {
		loginPass = text;
	}

	public void setLoginUser(String text) {
		loginUser = text;
	}

	private void setProgress(int val) {
		if (progressBar != null) {
			progressBar.setValue(val);
		} else {
			logger.info(val + "%");
		}
	}

	private void setProgressText(String text) {
		if (progressBar != null) {
			progressBar.setString(text);
			progressBar.setStringPainted(true);
		} else {
			logger.info(text);
		}
	}

	public static void autoConfig(final Object pass, final Object user,
			final Object ip, final Object routerScript, final Object routername) {

		final ConfirmCheckBoxDialog ccbd = new ConfirmCheckBoxDialog(
				JDLocale.L("gui.config.liveHeader.warning.wizard.title",
						"Routererkennung jetzt starten?"),
				JDLocale
						.L(
								"gui.config.liveHeader.warning.wizard",
								"Die automatische Suche nach den Einstellungen kann einige Minuten in Anspruch nehmen.\r\n Bitte geben Sie vorher Ihre Router Logindaten ein. Jetzt ausführen?"),
				JDLocale.L("gui.config.liveHeader.warning.wizard.checkall",
						"Alle Router prüfen"));
		if (ccbd.isOk) {
			Thread th;
			final ProgressDialog progress = new ProgressDialog(
					ConfigurationDialog.PARENTFRAME,
					JDLocale.L("gui.config.liveHeader.progress.message",
							"jDownloader sucht nach Ihren Routereinstellungen"),
					null, false, false);

			th = new Thread() {
				@Override
				public void run() {
					String pw = "";
					String username = "";
					String ipadresse = "";
					if (pass instanceof GUIConfigEntry) {
						pw = (String) ((GUIConfigEntry) pass).getText();
						username = (String) ((GUIConfigEntry) user).getText();
						ipadresse = (String) ((GUIConfigEntry) ip).getText();
					} else if (pass instanceof JTextField) {
						pw = (String) ((JTextField) pass).getText();
						username = (String) ((JTextField) user).getText();
						ipadresse = (String) ((JTextField) ip).getText();
					}
					GetRouterInfo routerInfo = new GetRouterInfo(progress);
					routerInfo.testAll = ccbd.isChecked;
					routerInfo.setLoginPass(pw);
					routerInfo.setLoginUser(username);
					if (username != null && !username.matches("[\\s]*")) {
						routerInfo.username = username;
					}
					if (pw != null && !pw.matches("[\\s]*")) {
						routerInfo.password = pw;
					}
					String[] data;
					if (GetRouterInfo.validateIP(ipadresse + "")) {
						data = routerInfo.getRouterData(ipadresse + "");
					} else {
						data = routerInfo.getRouterData(null);
					}
					if (data == null) {
						progress.setVisible(false);
						progress.dispose();
						JDUtilities
								.getGUI()
								.showMessageDialog(
										JDLocale
												.L(
														"gui.config.liveHeader.warning.notFound",
														"jDownloader konnte ihre Routereinstellung nicht automatisch ermitteln."));
						return;
					}
					if (routerScript != null
							&& routerScript instanceof GUIConfigEntry)
						((GUIConfigEntry) routerScript).setData(data[2]);
					if (routername != null && routername instanceof JTextArea) {
						((JTextField) routername).setText(data[1]);
					}
					JDUtilities.getConfiguration().setProperty(
							Configuration.PARAM_HTTPSEND_ROUTERNAME, data[1]);
					if (username == null || username.matches("[\\s]*")) {
						if (user instanceof GUIConfigEntry)
							((GUIConfigEntry) user).setData(data[4]);
						else if (user instanceof JTextField)
							((JTextField) user).setText(data[4]);
					}
					if (pw == null || pw.matches("[\\s]*")) {
						if (pass instanceof GUIConfigEntry)
							((GUIConfigEntry) pass).setData(data[5]);
						else if (pass instanceof JTextField)
							((JTextField) pass).setText(data[5]);
					}
					progress.setVisible(false);
					progress.dispose();
					JDUtilities.getGUI().showMessageDialog(
							JDLocale.L(
									"gui.config.liveHeader.warning.yourRouter",
									"Sie haben eine")
									+ " " + data[1]);

				}
			};
			th.start();
			progress.setThread(th);
			progress.setVisible(true);

		}
	}
	public static void main(String[] args) {
		isUpnp("10.11.12.253", "49000");
	}
}
