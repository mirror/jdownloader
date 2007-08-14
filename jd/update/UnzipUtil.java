package jd.update;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jd.plugins.Plugin;

/**
 * Simple utility class that unzips
 * 
 * @author Tom
 */
public final class UnzipUtil {

   private static Logger logger = Plugin.getLogger();

   /**
    * Unzips a file to a given location
    * 
    * @param file
    *           the file to unzip
    * @param unzipLocation
    *           a directory to unzip the file to
    */
   public final static void unzip(File file, String unzipLocation) {
      ZipFile zipFile;

      try {
         zipFile = new ZipFile(file);

         Enumeration entries = zipFile.entries();
         while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String newFileName = unzipLocation + File.separatorChar + entry.getName();

            if (entry.isDirectory()) {
               logger.info("Extracting directory: " + newFileName);
               (new File(newFileName)).mkdir();
               continue;
            }

            logger.info("Extracting file: " + newFileName);
            (new File(newFileName)).getParentFile().mkdirs();
            copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(
                  new FileOutputStream(newFileName)));
         }

         zipFile.close();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Copies the contents from one stream to another
    * 
    * @param in
    *           the InputStream to copy from
    * @param out
    *           the OutputStream to copy to
    * @throws IOException
    *            if something goes wrong
    */
   private static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
      byte[] buffer = new byte[1024];
      int len;

      while ((len = in.read(buffer)) >= 0)
         out.write(buffer, 0, len);

      in.close();
      out.close();
   }

}
