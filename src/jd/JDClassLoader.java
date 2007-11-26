package jd;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import jd.utils.JDUtilities;

/**
 * Diese Klasse steuert das Nachladen weiterer Klassen und Ressourcen. 
 * (Aus dem Homedir und aus dem Pluginverzeichnis)
 * 
 * @author astaldo
 *
 */
public class JDClassLoader extends java.lang.ClassLoader {
    private String      rootDir;
    private ClassLoader classLoaderParent;
    private URLClassLoader rootClassLoader;
    private JarFile jars[];
    private Logger logger = JDUtilities.getLogger();

    
    public JDClassLoader(String rootDir, ClassLoader classLoaderParent) {
        if (rootDir == null) 
            throw new IllegalArgumentException("Null root directory");
        this.rootDir = rootDir;
        this.classLoaderParent = classLoaderParent;
        logger.fine("rootDir:"+rootDir);
        try {
            rootClassLoader = new URLClassLoader(new URL[]{new File(rootDir).toURI().toURL()},null);
        }
        catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        //Hier werden die JAR Dateien ausgelesen
        File files[] = new File(new File(rootDir),"plugins").listFiles(new JDFileFilter(null, ".jar", false));
        if(files!=null){
            jars = new JarFile[files.length];
            for(int i=0;i<jars.length;i++){
                try {
                    logger.finer("Jar file loaded: "+files[i].getAbsolutePath());
                    jars[i] = new JarFile(files[i]);
                    
                  
                }
                catch (IOException e) {
                }
            }
        }
    }

    @Override
    protected URL findResource(String name) {
        URL url;
        url = rootClassLoader.findResource(name);
        if(url!= null)
            return url;
        return super.findResource(name);
    }
    @Override
    public URL getResource(String name) {
       System.out.println("getResource:"+name);
        if (jars != null) {
            //An dieser Stelle werden die JAR Dateien überprüft
            JarEntry entry;
            for (int i = 0; i < jars.length; i++) {
               
               
                if (jars[i] != null && ( entry = jars[i].getJarEntry(name))!=null) try {
                    System.out.println("getResource:"+entry.getName());
                    return new URL(entry.getName());
                }
                catch (MalformedURLException e) {
                }
            }
        }
        URL url = rootClassLoader.getResource(name);
        
        if(url != null){
            return url;
        }
        url = super.getResource(name);
        
        if(url != null)
            return url;
        try {
            //Falls immer noch nichts vorhanden, wird ein neu erzeugtes File Objekt zurückgegeben
            //Ist für das Abspeichern der Captcha notwendig
       
            return new File(new File(rootDir),name).toURI().toURL();
        }
        catch (MalformedURLException e) {
        }
        return null;
    }
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Vector<URL> urls = new Vector<URL>();
        if (jars != null) {
            JarEntry entry;
            for (int i = 0; i < jars.length; i++) {
                ;
                if (jars[i]!=null &&(entry = jars[i].getJarEntry(name)) != null) try {
                    //Das sollte nun hoffentlich eine Systemunabhängige Implementierung sein.
                    
                    String url=new File(jars[i].getName().replace("\\", "/")).toURI().toURL()+"!/"+entry.getName();
////                    url=url.replace("file:/", "file://");
//                   logger.finer(new URL("jar","",url)+"");
//                   logger.finer("jar:file:/"+jars[i].getName().replace("\\", "/")+"!/"+entry.getName());
                   urls.add(new URL("jar","",url)); 
               }
                catch (MalformedURLException e) {
                     e.printStackTrace();
                }
            }
        }
        return urls.elements();
    }
    /**
     * Lädt die Klasse.
     * Dazu wird zuerst überprüft, ob die Klasse durch einen System-Classloader geladen werden kann.
     * Erst zum Schluß wird versucht, diese Klasse selbst zu laden.
     */
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findSystemClass(name);
            }
            catch (Exception e) {
            }
            if (c == null) {
                try {
                    c = classLoaderParent.loadClass(name);
                }
                catch (Exception e) {
                }
            }
        }
        if (c == null) {
            JarEntry entry=null;
            for(int i=0;i<jars.length;i++){
                entry = jars[i].getJarEntry(name.replace('.', '/')+".class");
                if(entry != null){
                    try {
                        byte data[] = loadClassData(jars[i], entry);
                        
                        c = defineClass(name, data, 0, data.length,getClass().getProtectionDomain());
                        if (c == null) throw new ClassNotFoundException(name);
                    }
                    catch (ClassFormatError e) {
                         e.printStackTrace();
                    }
                    catch (IOException e) {
                         e.printStackTrace();
                    }
                }
            }
        }
        if (resolve) resolveClass(c);
        return c;
    }
    /**
     * Diese Methode lädt eine Klasse aus einer JAR nach
     * @param jarFile Die JARDatei
     * @param jarEntry Die Klasse innerhalb der JAR
     * @return Die eingelesenen Bytes
     * @throws IOException
     */
    private byte[] loadClassData(JarFile jarFile, JarEntry jarEntry) throws IOException {
        
        byte buff[] = new byte[(int)jarEntry.getSize()];
        DataInputStream dis = new DataInputStream(jarFile.getInputStream(jarEntry));
        dis.readFully(buff);
        dis.close();
        return buff;
    }
}
