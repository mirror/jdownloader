package jd.utils;

import java.util.LinkedList;
import java.util.regex.Pattern;

import jd.plugins.Regexp;

public class LWindowInfos {
	public String[] LIST_IDS, LISTSTACKING_IDS;
	public String ACTIVE_WINDOW;

	public LWindowInfos() {
		String str = JDUtilities.runCommand("xprop", new String[] { "-root" },
				"/usr/bin", 1000);
		try {
			LIST_IDS = new Regexp(str, Pattern
					.compile("_NET_CLIENT_LIST\\(.*?\\): window id \\# (.*)"))
					.getFirstMatch().split(", ");
		} catch (Exception e) {
		}
		try {
			LISTSTACKING_IDS = new Regexp(
					str,
					Pattern
							.compile("_NET_CLIENT_LIST_STACKING\\(.*?\\): window id \\# (.*)"))
					.getFirstMatch().split(", ");
		} catch (Exception e) {
		}
		try {
			ACTIVE_WINDOW = new Regexp(str, Pattern
					.compile("_NET_ACTIVE_WINDOW\\(.*?\\): window id \\# (.*)"))
					.getFirstMatch().split(", ")[0];
		} catch (Exception e) {
		}
	}

	public WindowInformations[] getWindowInformations() {
		LinkedList<WindowInformations> infos = new LinkedList<WindowInformations>();
		try {
			for (int i = 0; i < LIST_IDS.length; i++) {
				infos.add(getWindowInformation(LIST_IDS[i]));
			}
		} catch (Exception e) {
		}
		return infos.toArray(new WindowInformations[infos.size()]);
	}

	public WindowInformations getWindowInformation(String id) {
		String str = JDUtilities.runCommand("xprop",
				new String[] { "-id", id }, "/usr/bin", 1000);
		WindowInformations info = new WindowInformations();
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

	public class WindowInformations {
		public String[] WM_CLASS;
		public String WM_ICON_NAME, WM_NAME;
		public int WM_PID = 0;

		public String toString() {
			String wc = "";
			if(WM_CLASS!=null)
			{
				boolean last = false;
			for (int i = 0; i < WM_CLASS.length; i++) {
				wc += ((last)? ", ":"") + WM_CLASS[i];
				last = true;
			}
			}
			return "WM_NAME=" + WM_NAME + System.getProperty("line.separator")
					+ "WM_ICON_NAME=" + WM_ICON_NAME
					+ System.getProperty("line.separator") + "WM_CLASS="
					+ wc + System.getProperty("line.separator")
					+ "WM_PID=" + WM_PID;
		}
	}
}
