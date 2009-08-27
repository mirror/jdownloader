import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.Regexp;

import jd.captcha.JACMethod;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class captchaMethodeChecker {

	private static String getLocalFile(File file) {
		if (!file.exists())
			return "";
		BufferedReader f;
		try {
			f = new BufferedReader(new InputStreamReader(new FileInputStream(
					file), "UTF-8"));

			String line;
			StringBuffer ret = new StringBuffer();
			String sep = "\r\n";
			while ((line = f.readLine()) != null) {
				ret.append(line + sep);
			}
			f.close();
			return ret.toString();
		} catch (IOException e) {

		}
		return "";
	}
	private static String getHost(String source)
	{
		return new Regex(source, "names\\s*=\\s*\\{\\s*\"([^\"]*)\"\\s*\\}").getMatch(0);
	}
	private static String getMethode(String host)
	{
        for (JACMethod method : JACMethod.getMethods()) {
            if (host.equalsIgnoreCase(method.getServiceName())) {
                return method.getFileName();
            }
        }
        File dir = JDUtilities.getResourceFile(JDUtilities.getJACMethodsDirectory()+host);
        if(dir.exists())return host;
        return null;
	}
	private static void parseAdditon(File file, String regexp) {
		String text = getLocalFile(file);
		// text = text.replaceAll("(?is)/\\*.*?\\*/", "");
		// text = text.replaceAll("//.*", "");
		// text = text.replaceAll(".*final .*", ""); // kann man nich verändern
		String[] res = new Regex(text, regexp).getRow(0);
		if (res != null && res.length > 0) {
			String host = getHost(text);
				for (String string : res) {
					if(string.split(",").length==2)
					{
						String meth = getMethode(host);
						if(meth==null)
						{
							System.out.println(host);
							System.out.println(file.getName());
							System.out.println("");
							System.out.println(string);
							System.out.println("__________________________");

						}
					}
					else
					{
						String host2 = string.replaceFirst(".*?\"", "").replaceFirst("\".*", "");
						String meth = getMethode(host2);
						if(meth==null)
						{
							System.out.println(host2);
							System.out.println(file.getName());
							System.out.println("");
							System.out.println(string);
							System.out.println("__________________________");

						}
					}
				}

		}
	}

	private static Vector<File> getFiles(File directory) {
		Vector<File> ret = new Vector<File>();

		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				ret.addAll(getFiles(file));
			} else if (file.getName().matches(".*\\.java$")) {
				ret.add(file);
			}
		}

		return ret;
	}

	public static void parse(String regexp) {
		for (File file : getFiles(new File("src/jd/plugins/hoster"))) {
			parseAdditon(file, regexp);
		}
	}

	public static void main(String[] args) {
		parse("(getCaptchaCode\\(.*?\\);)");
	}

}
