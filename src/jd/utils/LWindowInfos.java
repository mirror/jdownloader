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


package jd.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import jd.plugins.Regexp;

public class LWindowInfos {
	private static LinkedList<WindowInformations> windowInformations = new LinkedList<WindowInformations>();
	public static WindowInformations[] getWindowInfos()
	{
		String str = JDUtilities.runCommand("xprop", new String[] { "-root" },
				"/usr/bin", 1000);
		try {
			String[] LIST_IDS = new Regexp(str, Pattern
					.compile("_NET_CLIENT_LIST\\(.*?\\): window id \\# (.*)"))
					.getFirstMatch().split(", ");
			LinkedList<WindowInformations> windowInformations2 = new LinkedList<WindowInformations>();
			for (int i = 0; i < LIST_IDS.length; i++) {
				windowInformations2.add(getWindowInformation(LIST_IDS[i]));
			}
			windowInformations=windowInformations2;
		} catch (Exception e) {
		}
		return windowInformations.toArray(new WindowInformations[windowInformations.size()]);
	}
	public static WindowInformations getActiveWindow()
	{
		String str = JDUtilities.runCommand("xprop", new String[] { "-root" },
				"/usr/bin", 1000);
		try {
			String ACTIVE_WINDOW_ID = new Regexp(str, Pattern
					.compile("_NET_ACTIVE_WINDOW\\(.*?\\): window id \\# (.*)"))
					.getFirstMatch().split(", ")[0];
			return getWindowInformation(ACTIVE_WINDOW_ID);
		} catch (Exception e) {
		}
		return null;
	}
	private static WindowInformations getWindowInformation(String id) {
		Iterator<WindowInformations> iter = windowInformations.iterator();
		while (iter.hasNext()) {
			WindowInformations info = (WindowInformations) iter
					.next();
			if(info.ID.equals(id))
				return info;
		}
		String str = JDUtilities.runCommand("xprop",
				new String[] { "-id", id }, "/usr/bin", 1000);
		WindowInformations info = new WindowInformations();
		info.ID=id;
		try {
			info.WM_CLASS = new Regexp(str, Pattern
					.compile("WM_CLASS\\(.*?\\) = \"(.*)")).getFirstMatch()
					.replaceFirst("\"$", "").split("\", \"");
		} catch (Exception e) {
		}
		try {
			info.WM_NAME = new Regexp(str, Pattern
					.compile("WM_NAME\\(.*?\\) = \"(.*)")).getFirstMatch()
					.replaceFirst("\"$", "");
		} catch (Exception e) {
		}
		try {
			info.WM_ICON_NAME = new Regexp(str, Pattern
					.compile("WM_ICON_NAME\\(.*?\\) = \"(.*)")).getFirstMatch()
					.replaceFirst("\"$", "");
		} catch (Exception e) {
		}
		try {
			// _NET_WM_PID(CARDINAL) = 5324
			info.WM_PID = Integer.parseInt(new Regexp(str, Pattern
					.compile("_NET_WM_PID\\(CARDINAL\\) = ([\\d]*)"))
					.getFirstMatch());
		} catch (Exception e) {
			// TODO: handle exception
		}
		return info;

	}

	public static class WindowInformations {
		public String[] WM_CLASS;
		public String WM_ICON_NAME, WM_NAME, ID;
		public int WM_PID = 0;

		public String toString() {
			String wc = "";
			if (WM_CLASS != null) {
				boolean last = false;
				for (int i = 0; i < WM_CLASS.length; i++) {
					wc += ((last) ? ", " : "") + WM_CLASS[i];
					last = true;
				}
			}
			return "WM_NAME=" + WM_NAME + System.getProperty("line.separator")
					+ "ID=" + ID + System.getProperty("line.separator")
					+ "WM_ICON_NAME=" + WM_ICON_NAME
					+ System.getProperty("line.separator") + "WM_CLASS=" + wc
					+ System.getProperty("line.separator") + "WM_PID=" + WM_PID;
		}
	}
}
