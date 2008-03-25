package jd.unrar;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import jd.controlling.ProgressController;

public class Merge {

	public static File mergeIt(File file, String[] following, boolean autoDelete) {
		String name = file.getName();
		if (!name.matches(".*\\.[\\d]+$")) {
			return null;
		}
		final String matcher = name.replaceFirst("\\.[\\d]+$", "")
				+ "\\.[\\d]+";
		if (following != null)
			for (int i = 0; i < following.length; i++) {
				if (following[i].matches(matcher))
					return null;
			}
		File[] files = file.getParentFile().listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				if (pathname.isFile() && pathname.getName().matches(matcher))
					return true;
				return false;
			}
		});
	    ProgressController progress = new ProgressController("Default HJMerge", 100);
		try {
			File fout = new File(file
					.getParentFile(), name.replaceFirst("\\.[\\d]+$", ""));
			FileOutputStream out = new FileOutputStream(fout);
		    int    len  = 1048576;
		    byte[] buff = new byte[ (int)Math.min( len, files[0].length() ) ];
		    long length = 0;
		    for (int i = 0; i < files.length; i++) {
				length += files[i].length();
			}
		    int c=0;
		    long wrout = 0;

	        progress.setStatusText("HJMerge-process");
			for (int i = 0; i < files.length; i++) {

				FileInputStream in = new FileInputStream(files[i]);
			    while( 0 < (len = in.read( buff )) )
			    {
			    	out.write( buff, 0, len );
			    	c++;
			    	wrout+=buff.length;
			    	if(c==5)
			    	{
			    		c=0;
						progress.setStatus((int) (wrout*100/length));
						progress.setStatusText(wrout/1048576 + " MB merged");
			    	}
			    }

				in.close();
			}
			out.flush();
			
			out.close();
			if(autoDelete)
			for (int i = 0; i < files.length; i++) {
				files[i].delete();
			}
			progress.finalize();
			return fout;
		} catch (Exception e) {
			// TODO: handle exception
		}

		progress.finalize();
		return null;
	}
}
