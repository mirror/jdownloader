package jd.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jd.plugins.Plugin;

public class JarLoader extends URLClassLoader
{
    private boolean debug = false;
    private ZipFile f;
    private URL url;
    private Logger logger= Plugin.getLogger();

    public JarLoader(URL url){
        super(new URL[] { url });
    this.url = url;
       // JarLoader l = new JarLoader();
    try {
        JarURLConnection uc = (
                JarURLConnection)url.openConnection();
        logger.info(uc.getAttributes()+"Ä "+url);
        logger.info(uc.toString());
        logger.info(uc.getEntryName());
        logger.info(uc.getCertificates()+"");
        logger.info(uc.getJarFileURL()+"");
        logger.info(uc.getManifest().getEntries()+"");
      
    }
    catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
         
    }

    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
     String name_slash = name.replace('.', '/');
     Class c = findLoadedClass(name);
     if(c == null)
     {
         ZipEntry e = f.getEntry(name_slash+".class");
         
         if(e != null)
         {
          try {
              InputStream is = f.getInputStream(e);
              byte[] b = new byte[(int) e.getSize()];
              int n = 0;
              // Irritatingly, it doesn't always read 
              // all of the bytes the first time
              while(n < b.length)
               n += is.read(b, n, (b.length - n));
               c = defineClass(name, b, 0, b.length);
          }
          catch(ClassFormatError ex) {
              ex.printStackTrace();
          }
          catch(IOException ex2) { 
              ex2.printStackTrace();
          }
         }
     }

     if(c == null)
         c = findSystemClass(name);
     
     if(resolve)
         resolveClass(c);
     return c;
    }
  
    /** Test: JarLoader <jar file> <class name>... */
    //loadjardatei
    public void load(String dateiname,String classenname)
    { 
     try {
         f = new ZipFile(dateiname);
          try {
              Class c = this.loadClass(classenname, true);
          }
          catch(Exception e) {
              e.printStackTrace();
          }
     }
     catch(Exception e) {
         e.printStackTrace();
     }
    }
}