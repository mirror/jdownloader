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
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import jd.utils.JDUtilities;

/**
 * Diese Klasse steuert das Nachladen weiterer Klassen und Ressourcen. (Aus dem
 * Homedir und aus dem Pluginverzeichnis)
 * 
 * @author astaldo
 * 
 */
public class JDClassLoader extends java.lang.ClassLoader {
    private ClassLoader classLoaderParent;
    private Vector<File> jarFile;
    private JarFile jars[];
    private Logger logger = JDUtilities.getLogger();
    private URLClassLoader rootClassLoader;
    private String rootDir;

    public JDClassLoader(String rootDir, ClassLoader classLoaderParent) {
        if (rootDir == null) { throw new IllegalArgumentException("Null root directory"); }
        this.rootDir = rootDir;
        this.classLoaderParent = classLoaderParent;
        logger.fine("rootDir:" + rootDir);
        try {
            rootClassLoader = new URLClassLoader(new URL[] { new File(rootDir).toURI().toURL() }, null);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        // Hier werden die JAR Dateien ausgelesen
        Vector<JarFile> jarFiles = new Vector<JarFile>();
        jarFile = new Vector<File>();
        ArrayList<String> names = new ArrayList<String>();
        File[] files = new File(new File(rootDir), "plugins").listFiles(new JDFileFilter(null, ".jar", false));
        if (files != null) {
            // jars = new JarFile[files.length];
            for (int i = 0; i < files.length; i++) {
                try {
                    if (!files[i].getAbsolutePath().endsWith("webupdater.jar")) {
                        if (names.contains(files[i].getName())) {
                            logger.severe("Duplicate Jars found: " + files[i].getAbsolutePath());
                        } else {
                            names.add(files[i].getName());
                            logger.finer("Jar file loaded: " + files[i].getAbsolutePath());
                            // jars[i] = new JarFile(files[i]);
                            jarFile.add(files[i]);

                            jarFiles.add(new JarFile(files[i]));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // Hier werden lokale JAR Dateien ausgelesen
        files = new File(rootDir).listFiles(new JDFileFilter(null, ".jar", false));
        if (files != null) {
            // jars = new JarFile[files.length];
            for (int i = 0; i < files.length; i++) {
                try {
                    if (!files[i].getAbsolutePath().endsWith("webupdater.jar")) {
                        if (names.contains(files[i].getName())) {
                            logger.severe("Duplicate Jars found: " + files[i].getAbsolutePath());
                        } else {
                            names.add(files[i].getName());
                            logger.finer("Jar file loaded: " + files[i].getAbsolutePath());
                            jarFile.add(files[i]);
                            jarFiles.add(new JarFile(files[i]));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        jars = jarFiles.toArray(new JarFile[] {});
    }

    @Override
    protected URL findResource(String name) {

        URL url;
        url = rootClassLoader.findResource(name);
        if (url != null) { return url; }

        return super.findResource(name);
    }

    public Vector<File> getJars() {
        Vector<File> ret = new Vector<File>();
        ret.addAll(jarFile);
        return ret;
    }

    public URL getResource(byte[] key) {

        if (jars != null) {
            // An dieser Stelle werden die JAR Dateien überprüft
            JarEntry entry;
            for (JarFile element : jars) {

                if (element != null && (entry = element.getJarEntry(new String(key))) != null) {
                    try {
                        System.out.println("getResource:" + entry.getName());
                        return new URL(entry.getName());
                    } catch (MalformedURLException e) {
                    }
                }
            }
        }
        URL url = rootClassLoader.getResource(new String(key));

        if (url != null) { return url; }
        url = super.getResource(new String(key));

        if (url != null) { return url; }
        url = classLoaderParent.getResource(new String(key));
        if (url != null) { return url; }
        try {
            // Falls immer noch nichts vorhanden, wird ein neu erzeugtes File
            // Objekt zurückgegeben
            // Ist für das Abspeichern der Captcha notwendig

            return new File(new File(rootDir), new String(key)).toURI().toURL();
        } catch (MalformedURLException e) {
        }
        return null;
    }

    @Override
    public URL getResource(String name) {

        if (jars != null) {
            // An dieser Stelle werden die JAR Dateien überprüft
            JarEntry entry;
            for (JarFile element : jars) {

                if (element != null && (entry = element.getJarEntry(name)) != null) {
                    try {
                        System.out.println("getResource:" + entry.getName());
                        return new URL(entry.getName());
                    } catch (MalformedURLException e) {
                    }
                }
            }
        }
        URL url = rootClassLoader.getResource(name);

        if (url != null) { return url; }
        url = super.getResource(name);

        if (url != null) { return url; }
        url = classLoaderParent.getResource(name);
        if (url != null) { return url; }
        try {
            // Falls immer noch nichts vorhanden, wird ein neu erzeugtes File
            // Objekt zurückgegeben
            // Ist für das Abspeichern der Captcha notwendig

            return new File(new File(rootDir), name).toURI().toURL();
        } catch (MalformedURLException e) {
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Vector<URL> urls = new Vector<URL>();

        Enumeration<URL> en = classLoaderParent.getResources(name);

        while (en.hasMoreElements()) {
            URL tmp = en.nextElement();

            urls.add(tmp);
        }
        if (urls.size() > 0) { return urls.elements(); }
        if (jars != null) {
            JarEntry entry;
            for (JarFile element : jars) {
                // logger.info(jars[i]+" -tttt "+jars[i].entries());
                if (element != null && (entry = element.getJarEntry(name)) != null) {
                    try {
                        // Das sollte nun hoffentlich eine Systemunabhängige
                        // Implementierung sein.

                        String url = new File(element.getName().replace("\\", "/")).toURI().toURL() + "!/" + entry.getName();
                        // // url=url.replace("file:/", "file://");
                        // logger.finer(new URL("jar","",url)+"");
                        // logger.finer("jar:file:/"+jars[i].getName().replace("\\",
                        // "/")+"!/"+entry.getName());
                        urls.add(new URL("jar", "", url));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        logger.info(urls + "");

        return urls.elements();
    }

    /**
     * Lädt die Klasse. Dazu wird zuerst überprüft, ob die Klasse durch einen
     * System-Classloader geladen werden kann. Erst zum Schluß wird versucht,
     * diese Klasse selbst zu laden.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findSystemClass(name);
            } catch (Exception e) {
            }
            if (c == null) {
                try {
                    c = classLoaderParent.loadClass(name);
                } catch (Exception e) {
                }
            }
        }
        if (c == null) {
            JarEntry entry = null;
            for (JarFile element : jars) {
                entry = element.getJarEntry(name.replace('.', '/') + ".class");
                if (entry != null) {
                    try {
                        byte data[] = loadClassData(element, entry);
System.out.println("Loaded class "+name+" from "+element.getName());
                        c = defineClass(name, data, 0, data.length, getClass().getProtectionDomain());
                        if (c == null) { throw new ClassNotFoundException(name); }
                    } catch (ClassFormatError e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
    public String findJar(String name) throws ClassNotFoundException { 
            JarEntry entry = null;
            for (JarFile element : jars) {
                entry = element.getJarEntry(name.replace('.', '/') + ".class");
                if (entry != null) {
                    return element.getName();
                }            
        }
       return null;
    }
    /**
     * Diese Methode lädt eine Klasse aus einer JAR nach
     * 
     * @param jarFile
     *            Die JARDatei
     * @param jarEntry
     *            Die Klasse innerhalb der JAR
     * @return Die eingelesenen Bytes
     * @throws IOException
     */
    private byte[] loadClassData(JarFile jarFile, JarEntry jarEntry) throws IOException {

        byte buff[] = new byte[(int) jarEntry.getSize()];
        DataInputStream dis = new DataInputStream(jarFile.getInputStream(jarEntry));
        dis.readFully(buff);
        dis.close();
        return buff;
    }
}
