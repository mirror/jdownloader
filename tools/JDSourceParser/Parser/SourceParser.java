package Parser;

import java.io.File;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import myIrcBot.Utilities;

public class SourceParser {
	private static void parseAdditon(File file, String regexp) {
		String text = Utilities.getLocalFile(file);
		text = text.replaceAll("(?is)/\\*.*?\\*/", "");
		text = text.replaceAll("//.*", "");
		text = text.replaceAll(".*final .*", ""); // kann man nich verändern
		Matcher r = Pattern.compile(regexp).matcher(text);
		if (r.find())
			System.out.println(file);

		// System.out.println(text);
	}

	private static Vector<File> getFiles(File file) {
		Vector<File> ret = new Vector<File>();
		try {
			File[] list = file.listFiles();
			for (File file2 : list) {
				if (file2.isDirectory())
				{
					try {
						Vector<File> ints = getFiles(file2);
						for (File file3 : ints) {
							ret.add(file3);
						}
					} catch (Exception e) {
						// TODO: handle exception
					}

				}
				else if (file2.getName().matches(".*\\.java$"))
					ret.add(file2);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return ret;
	}
	public static void parse(String regexp) {
	     Vector<File> ret = getFiles(new File("src"));
	        for (File file : ret) {
	            parseAdditon(file, regexp);
	        }

    }
	public static void main(String[] args) {
		parse("system.update.error.message");

	}

}
