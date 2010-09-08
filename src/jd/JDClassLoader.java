//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import jd.controlling.JDLogger;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;

/**
 * Diese Klasse steuert das Nachladen weiterer Klassen und Ressourcen. (Aus dem
 * Homedir und aus dem Pluginverzeichnis)
 * 
 * @author astaldo
 */
public class JDClassLoader extends ClassLoader {

    private static final Logger logger = JDLogger.getLogger();

    public static byte[] getSig(final String jarfile) {
        try {
            final JarFile jar = new JarFile(jarfile);
            final String dsa = "META-INF/JDOWNLOA.DSA";

            final ZipEntry entry = jar.getEntry(dsa);

            final byte[] b = new byte[1];
            final InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            final byte[] res = new byte[692];
            int c = 0;
            int n = 0;
            while ((n = in.read(b)) > -1) {
                if (n > 0) {
                    c++;
                    if (c >= 24 && c < 24 + 692) {
                        res[c - 24] = b[0];
                        // System.out.println(c + ". " + b[0]);
                    }
                }
            }
            return res;
        } catch (final Exception e) {
            // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.
            // SEVERE,"Exception occurred",e);
        }
        return null;
    }

    private final Vector<File>    jarFile;

    private final JarFile[]       jars;
    private final ClassLoader     classLoaderParent;

    private final URLClassLoader  rootClassLoader;
    private final String          rootDir;

    private final ArrayList<File> lafs;
    private static final byte[]   S        = new byte[] { (byte) 2, (byte) 1, (byte) 1, (byte) 49, (byte) 11, (byte) 48, (byte) 9, (byte) 6, (byte) 5, (byte) 43, (byte) 14, (byte) 3, (byte) 2, (byte) 26, (byte) 5, (byte) 0, (byte) 48, (byte) 11, (byte) 6, (byte) 9, (byte) 42, (byte) -122, (byte) 72, (byte) -122, (byte) -9, (byte) 13, (byte) 1, (byte) 7, (byte) 1, (byte) -96, (byte) -126, (byte) 2, (byte) -111, (byte) 48, (byte) -126, (byte) 2, (byte) -115, (byte) 48, (byte) -126, (byte) 2, (byte) 75, (byte) 2, (byte) 4, (byte) 70, (byte) -60, (byte) 21, (byte) -27, (byte) 48, (byte) 11, (byte) 6, (byte) 7, (byte) 42, (byte) -122, (byte) 72, (byte) -50, (byte) 56, (byte) 4, (byte) 3, (byte) 5, (byte) 0, (byte) 48, (byte) 44, (byte) 49, (byte) 24, (byte) 48, (byte) 22, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 10, (byte) 19, (byte) 15, (byte) 103, (byte) 117, (byte) 108, (byte) 108,
            (byte) 105, (byte) 32, (byte) 67, (byte) 111, (byte) 109, (byte) 109, (byte) 117, (byte) 110, (byte) 105, (byte) 116, (byte) 121, (byte) 49, (byte) 16, (byte) 48, (byte) 14, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 3, (byte) 19, (byte) 7, (byte) 97, (byte) 115, (byte) 116, (byte) 97, (byte) 108, (byte) 100, (byte) 111, (byte) 48, (byte) 30, (byte) 23, (byte) 13, (byte) 48, (byte) 55, (byte) 48, (byte) 56, (byte) 49, (byte) 54, (byte) 48, (byte) 57, (byte) 49, (byte) 54, (byte) 50, (byte) 49, (byte) 90, (byte) 23, (byte) 13, (byte) 48, (byte) 57, (byte) 48, (byte) 56, (byte) 48, (byte) 53, (byte) 48, (byte) 57, (byte) 49, (byte) 54, (byte) 50, (byte) 49, (byte) 90, (byte) 48, (byte) 44, (byte) 49, (byte) 24, (byte) 48, (byte) 22, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 10, (byte) 19, (byte) 15, (byte) 103, (byte) 117, (byte) 108, (byte) 108, (byte) 105,
            (byte) 32, (byte) 67, (byte) 111, (byte) 109, (byte) 109, (byte) 117, (byte) 110, (byte) 105, (byte) 116, (byte) 121, (byte) 49, (byte) 16, (byte) 48, (byte) 14, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 3, (byte) 19, (byte) 7, (byte) 97, (byte) 115, (byte) 116, (byte) 97, (byte) 108, (byte) 100, (byte) 111, (byte) 48, (byte) -126, (byte) 1, (byte) -72, (byte) 48, (byte) -126, (byte) 1, (byte) 44, (byte) 6, (byte) 7, (byte) 42, (byte) -122, (byte) 72, (byte) -50, (byte) 56, (byte) 4, (byte) 1, (byte) 48, (byte) -126, (byte) 1, (byte) 31, (byte) 2, (byte) -127, (byte) -127, (byte) 0, (byte) -3, (byte) 127, (byte) 83, (byte) -127, (byte) 29, (byte) 117, (byte) 18, (byte) 41, (byte) 82, (byte) -33, (byte) 74, (byte) -100, (byte) 46, (byte) -20, (byte) -28, (byte) -25, (byte) -10, (byte) 17, (byte) -73, (byte) 82, (byte) 60, (byte) -17, (byte) 68, (byte) 0, (byte) -61,
            (byte) 30, (byte) 63, (byte) -128, (byte) -74, (byte) 81, (byte) 38, (byte) 105, (byte) 69, (byte) 93, (byte) 64, (byte) 34, (byte) 81, (byte) -5, (byte) 89, (byte) 61, (byte) -115, (byte) 88, (byte) -6, (byte) -65, (byte) -59, (byte) -11, (byte) -70, (byte) 48, (byte) -10, (byte) -53, (byte) -101, (byte) 85, (byte) 108, (byte) -41, (byte) -127, (byte) 59, (byte) -128, (byte) 29, (byte) 52, (byte) 111, (byte) -14, (byte) 102, (byte) 96, (byte) -73, (byte) 107, (byte) -103, (byte) 80, (byte) -91, (byte) -92, (byte) -97, (byte) -97, (byte) -24, (byte) 4, (byte) 123, (byte) 16, (byte) 34, (byte) -62, (byte) 79, (byte) -69, (byte) -87, (byte) -41, (byte) -2, (byte) -73, (byte) -58, (byte) 27, (byte) -8, (byte) 59, (byte) 87, (byte) -25, (byte) -58, (byte) -88, (byte) -90, (byte) 21, (byte) 15, (byte) 4, (byte) -5, (byte) -125, (byte) -10, (byte) -45, (byte) -59, (byte) 30,
            (byte) -61, (byte) 2, (byte) 53, (byte) 84, (byte) 19, (byte) 90, (byte) 22, (byte) -111, (byte) 50, (byte) -10, (byte) 117, (byte) -13, (byte) -82, (byte) 43, (byte) 97, (byte) -41, (byte) 42, (byte) -17, (byte) -14, (byte) 34, (byte) 3, (byte) 25, (byte) -99, (byte) -47, (byte) 72, (byte) 1, (byte) -57, (byte) 2, (byte) 21, (byte) 0, (byte) -105, (byte) 96, (byte) 80, (byte) -113, (byte) 21, (byte) 35, (byte) 11, (byte) -52, (byte) -78, (byte) -110, (byte) -71, (byte) -126, (byte) -94, (byte) -21, (byte) -124, (byte) 11, (byte) -16, (byte) 88, (byte) 28, (byte) -11, (byte) 2, (byte) -127, (byte) -127, (byte) 0, (byte) -9, (byte) -31, (byte) -96, (byte) -123, (byte) -42, (byte) -101, (byte) 61, (byte) -34, (byte) -53, (byte) -68, (byte) -85, (byte) 92, (byte) 54, (byte) -72, (byte) 87, (byte) -71, (byte) 121, (byte) -108, (byte) -81, (byte) -69, (byte) -6, (byte) 58,
            (byte) -22, (byte) -126, (byte) -7, (byte) 87, (byte) 76, (byte) 11, (byte) 61, (byte) 7, (byte) -126, (byte) 103, (byte) 81, (byte) 89, (byte) 87, (byte) -114, (byte) -70, (byte) -44, (byte) 89, (byte) 79, (byte) -26, (byte) 113, (byte) 7, (byte) 16, (byte) -127, (byte) -128, (byte) -76, (byte) 73, (byte) 22, (byte) 113, (byte) 35, (byte) -24, (byte) 76, (byte) 40, (byte) 22, (byte) 19, (byte) -73, (byte) -49, (byte) 9, (byte) 50, (byte) -116, (byte) -56, (byte) -90, (byte) -31, (byte) 60, (byte) 22, (byte) 122, (byte) -117, (byte) 84, (byte) 124, (byte) -115, (byte) 40, (byte) -32, (byte) -93, (byte) -82, (byte) 30, (byte) 43, (byte) -77, (byte) -90, (byte) 117, (byte) -111, (byte) 110, (byte) -93, (byte) 127, (byte) 11, (byte) -6, (byte) 33, (byte) 53, (byte) 98, (byte) -15, (byte) -5, (byte) 98, (byte) 122, (byte) 1, (byte) 36, (byte) 59, (byte) -52, (byte) -92,
            (byte) -15, (byte) -66, (byte) -88, (byte) 81, (byte) -112, (byte) -119, (byte) -88, (byte) -125, (byte) -33, (byte) -31, (byte) 90, (byte) -27, (byte) -97, (byte) 6, (byte) -110, (byte) -117, (byte) 102, (byte) 94, (byte) -128, (byte) 123, (byte) 85, (byte) 37, (byte) 100, (byte) 1, (byte) 76, (byte) 59, (byte) -2, (byte) -49, (byte) 73, (byte) 42, (byte) 3, (byte) -127, (byte) -123, (byte) 0, (byte) 2, (byte) -127, (byte) -127, (byte) 0, (byte) -116, (byte) 41, (byte) 78, (byte) 97, (byte) 29, (byte) -39, (byte) -6, (byte) -125, (byte) -99, (byte) -18, (byte) -77, (byte) 120, (byte) 40, (byte) 126, (byte) -104, (byte) 69, (byte) -60, (byte) -33, (byte) 40, (byte) 9, (byte) 11, (byte) -48, (byte) 85, (byte) -62, (byte) -93, (byte) 100, (byte) 38, (byte) 106, (byte) 74, (byte) -116, (byte) 0, (byte) 123, (byte) -11, (byte) -122, (byte) 125, (byte) 127, (byte) 2, (byte) 21,
            (byte) -77, (byte) 18, (byte) -53, (byte) -85, (byte) 48, (byte) 118, (byte) 56, (byte) 80, (byte) -38, (byte) 67, (byte) -56, (byte) -96, (byte) 122, (byte) 66, (byte) 9, (byte) -7, (byte) -124, (byte) -90, (byte) -3, (byte) 115, (byte) 122, (byte) -128, (byte) -32, (byte) -31, (byte) -22, (byte) -12, (byte) 1, (byte) 76, (byte) 102, (byte) -54, (byte) 124, (byte) -112, (byte) 0, (byte) -55, (byte) -11, (byte) -101, (byte) -69, (byte) -19, (byte) 77, (byte) -65, (byte) -86, (byte) 61, (byte) 111, (byte) 119, (byte) -107, (byte) -56, (byte) 110, (byte) -116, (byte) -39, (byte) -9, (byte) 46, (byte) 18, (byte) -106, (byte) -28, (byte) 23, (byte) 113, (byte) -39, (byte) -12, (byte) 41, (byte) 47, (byte) -79, (byte) -72, (byte) 125, (byte) -84, (byte) -120, (byte) -115, (byte) 125, (byte) -95, (byte) 111, (byte) -44, (byte) 125, (byte) -77, (byte) 45, (byte) 10, (byte) -5,
            (byte) -104, (byte) 47, (byte) -41, (byte) -119, (byte) 106, (byte) 97, (byte) -95, (byte) -9, (byte) 80, (byte) -29, (byte) 26, (byte) -111, (byte) -102, (byte) -109, (byte) -116, (byte) 48, (byte) 11, (byte) 6, (byte) 7, (byte) 42, (byte) -122, (byte) 72, (byte) -50, (byte) 56, (byte) 4, (byte) 3, (byte) 5, (byte) 0, (byte) 3, (byte) 47, (byte) 0, (byte) 48, (byte) 44, (byte) 2, (byte) 20, (byte) 31, (byte) 93, (byte) -31, (byte) 109, (byte) -22, (byte) -112, (byte) -62, (byte) 99, (byte) -20, (byte) -5, (byte) 17, (byte) -9, (byte) 123, (byte) 116, (byte) 1, (byte) -78, (byte) 3, (byte) 60, (byte) 122, (byte) 60, (byte) 2, (byte) 20, (byte) 48, (byte) -86, (byte) 15, (byte) 10, (byte) 63, (byte) -70, (byte) -20, (byte) 37, (byte) -16, (byte) -79, (byte) 19, (byte) 58, (byte) 104, (byte) -122, (byte) 26, (byte) -85, (byte) -49, (byte) 63, (byte) 104, (byte) 49, (byte) 49,
            (byte) -127                   };

    private static final int      S_LENGTH = JDClassLoader.S.length; // at now
                                                                     // equals
                                                                     // 692

    /**
     * 
     * @param rootDir
     * @param classLoaderParent
     */
    public JDClassLoader(final String rootDir, final ClassLoader classLoaderParent) {
        if (rootDir == null) { throw new IllegalArgumentException("Null root directory"); }

        this.rootDir = rootDir;
        this.classLoaderParent = classLoaderParent;
        JDClassLoader.logger.finest("rootDir:" + rootDir);

        URLClassLoader urlClassLoader = null;
        try {
            urlClassLoader = new URLClassLoader(new URL[] { new File(rootDir).toURI().toURL() }, null);
        } catch (final MalformedURLException e) {
            JDLogger.exception(e);
        }
        this.rootClassLoader = urlClassLoader;

        JarFile[] jars = null;
        Vector<File> jarFile = null;
        ArrayList<File> lafs = null;

        boolean isWebupdater = false;
        System.out.println(rootDir);
        System.out.println(" " + this.rootClassLoader.getResource("jd"));
        isWebupdater = (classLoaderParent.getResource("jd") + "").contains("jdupdate.jar") || (classLoaderParent.getResource("jd") + "").contains("webupdater.jar");
        if (!isWebupdater) {
            // Hier werden die JAR Dateien ausgelesen
            final Vector<JarFile> jarFiles = new Vector<JarFile>();
            jarFile = new Vector<File>();
            final ArrayList<String> names = new ArrayList<String>();
            File[] files = new File(new File(rootDir), "plugins").listFiles(new JDFileFilter(null, ".jar", false));
            // do not load optional jars in eclipse.
            if (files != null && JDUtilities.getRunType() != JDUtilities.RUNTYPE_LOCAL) {
                File file;
                String name;
                String absolutePath;
                final int length = files.length;
                for (int i = 0; i < length; i++) {
                    try {
                        file = files[i];
                        absolutePath = file.getAbsolutePath();
                        name = file.getName();
                        if (!absolutePath.endsWith("webupdater.jar") || !absolutePath.endsWith("jdupdate.jar")) {
                            if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED && !this.comp(JDClassLoader.getSig(absolutePath))) {
                                JDClassLoader.logger.severe("Not loaded due to sig violation: " + file);
                                continue;
                            }

                            if (names.contains(name)) {
                                JDClassLoader.logger.severe("Duplicate Jars found: " + absolutePath);
                            } else {
                                names.add(name);
                                JDClassLoader.logger.finer("Jar file loaded: " + absolutePath);
                                jarFile.add(file);
                                jarFiles.add(new JarFile(file));
                            }
                        }
                    } catch (final Exception e) {
                        JDLogger.exception(e);
                    }
                }
            }
            // Hier werden lokale JAR Dateien ausgelesen
            files = new File(rootDir).listFiles(new JDFileFilter(null, ".jar", false));
            if (files != null) {
                File file;
                String name;
                String absolutePath;
                final int length = files.length;
                for (int i = 0; i < length; i++) {
                    try {
                        file = files[i];
                        name = file.getName();
                        absolutePath = file.getAbsolutePath();
                        if (!absolutePath.endsWith("webupdater.jar") && !absolutePath.endsWith("jdupdate.jar")) {
                            if (!this.comp(JDClassLoader.getSig(absolutePath))) {
                                JDClassLoader.logger.severe("Not loaded due to sig violation: " + file);
                                continue;
                            }
                            if (names.contains(name)) {
                                JDClassLoader.logger.severe("Duplicate Jars found: " + absolutePath);
                            } else {
                                names.add(name);
                                JDClassLoader.logger.finer("Jar file loaded: " + absolutePath);
                                jarFile.add(file);
                                jarFiles.add(new JarFile(file));
                            }
                        }

                    } catch (final IOException e) {
                        JDLogger.exception(e);
                    }
                }
            }

            // und hier folgen die LAFS

            // Hier werden lokale JAR Dateien ausgelesen
            files = new File(rootDir, "libs/laf").listFiles(new JDFileFilter(null, ".jar", false));
            lafs = new ArrayList<File>();
            if (files != null) {
                File file;
                String name;
                String absolutePath;
                final int length = files.length;
                for (int i = 0; i < length; i++) {
                    try {
                        file = files[i];
                        name = file.getName();
                        absolutePath = file.getAbsolutePath();
                        if (!absolutePath.endsWith("webupdater.jar") && !absolutePath.endsWith("jdupdate.jar")) {
                            // if (!comp(getSig(files[i].getAbsolutePath()))) {
                            // logger.severe("Not loaded due to sig violation: "
                            // + files[i]);
                            // continue;
                            // }
                            if (names.contains(name)) {
                                JDClassLoader.logger.severe("Duplicate Jars found: " + absolutePath);
                            } else {
                                names.add(name);
                                JDClassLoader.logger.finer("Look and Feel JAR loaded: " + absolutePath);
                                jarFile.add(file);
                                lafs.add(file);
                                jarFiles.add(new JarFile(file));
                            }
                        }

                    } catch (final IOException e) {
                        JDLogger.exception(e);
                    }
                }
            }
            jars = jarFiles.toArray(new JarFile[jarFiles.size()]);
        }
        this.jars = jars;
        this.jarFile = jarFile;
        this.lafs = lafs;
    }

    private boolean comp(final byte[] sig) {
        // if (sig == null) return false;
        // for (int i = 0; i < S.length; i++) {
        // byte a = S[i];
        // byte b = sig[i];
        // if (a != b) return false;
        // }
        // return true;
        if (sig == null) { return false; }
        final int length = JDClassLoader.S_LENGTH;
        for (int i = 0; i < length; i++) {
            if (JDClassLoader.S[i] != sig[i]) { return false; }
        }
        return true;
        // May be Arrays.equals(S, sig) ?
    }

    public String findJar(final String name) throws ClassNotFoundException {
        for (final JarFile element : this.jars) {
            if (element.getJarEntry(name.replace('.', '/') + ".class") != null) { return element.getName(); }
        }
        return null;
    }

    @Override
    protected URL findResource(final String name) {
        final URL url = this.rootClassLoader.findResource(name);
        return url != null ? url : super.findResource(name);
    }

    public Vector<File> getJars() {
        final Vector<File> ret = new Vector<File>();
        ret.addAll(this.jarFile);
        return ret;
    }

    public ArrayList<File> getLafs() {
        return this.lafs;
    }

    public URL getResource(final byte[] key) {
        return this.getResource(new String(key));
    }

    @Override
    public URL getResource(final String name) {
        if (this.jars != null) {
            // An dieser Stelle werden die JAR Dateien überprüft
            JarEntry entry;
            for (final JarFile element : this.jars) {
                if (element != null && (entry = element.getJarEntry(name)) != null) {
                    try {
                        return new URL(entry.getName());
                    } catch (final MalformedURLException e) {
                    }
                }
            }
        }
        URL url = this.rootClassLoader.getResource(name);
        if (url != null) { return url; }

        url = super.getResource(name);
        if (url != null) { return url; }

        url = this.classLoaderParent.getResource(name);
        if (url != null) { return url; }

        try {
            // Falls immer noch nichts vorhanden, wird ein neu erzeugtes File
            // Objekt zurückgegeben
            // Ist für das Abspeichern der Captcha notwendig

            return new File(new File(this.rootDir), name).toURI().toURL();
        } catch (final MalformedURLException e) {
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        final Vector<URL> urls = new Vector<URL>();

        final Enumeration<URL> en = this.classLoaderParent.getResources(name);
        while (en.hasMoreElements()) {
            urls.add(en.nextElement());
        }
        if (this.jars != null) {
            JarEntry entry;
            for (final JarFile element : this.jars) {
                if (element != null && (entry = element.getJarEntry(name)) != null) {
                    try {
                        // Das sollte nun hoffentlich eine Systemunabhängige
                        // Implementierung sein.
                        urls.add(new URL("jar", "", new File(element.getName().replace("\\", "/")).toURI().toURL() + "!/" + entry.getName()));
                    } catch (final MalformedURLException e) {
                        JDLogger.exception(e);
                    }
                }
            }
        }
        JDClassLoader.logger.info(urls + "");
        return urls.elements();
    }

    /**
     * Lädt die Klasse. Dazu wird zuerst überprüft, ob die Klasse durch einen
     * System-Classloader geladen werden kann. Erst zum Schluß wird versucht,
     * diese Klasse selbst zu laden.
     */
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        Class<?> c = this.findLoadedClass(name);
        if (c == null) {
            try {
                c = this.findSystemClass(name);
            } catch (final Exception e) {
            }
            if (c == null) {
                try {
                    c = this.classLoaderParent.loadClass(name);
                } catch (final Exception e) {
                }
            }
        }
        if (c == null) {
            JarEntry entry = null;
            for (final JarFile element : this.jars) {
                if ((entry = element.getJarEntry(name.replace('.', '/') + ".class")) != null) {
                    try {
                        final byte data[] = this.loadClassData(element, entry);
                        System.out.println("Loaded class " + name + " from " + element.getName());
                        c = this.defineClass(name, data, 0, data.length, this.getClass().getProtectionDomain());
                        if (c == null) { throw new ClassNotFoundException(name); }
                    } catch (final java.lang.VerifyError e) {
                        JDLogger.exception(e);
                    } catch (final ClassFormatError e) {
                        JDLogger.exception(e);
                    } catch (final IOException e) {
                        JDLogger.exception(e);
                    }
                }
            }
        }
        if (resolve) {
            this.resolveClass(c);
        }
        return c;
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
    private byte[] loadClassData(final JarFile jarFile, final JarEntry jarEntry) throws IOException {
        final byte buff[] = new byte[(int) jarEntry.getSize()];
        final DataInputStream dis = new DataInputStream(jarFile.getInputStream(jarEntry));
        dis.readFully(buff);
        dis.close();
        return buff;
    }
}
