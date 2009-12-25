//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.nutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class ClassFinder {

    /**
     * Don't let anyone instantiate this class.
     */
    private ClassFinder() {
    }

    public static ArrayList<Class<?>> getClasses(final String packageName) throws ClassNotFoundException, IOException {
        return getClasses(packageName, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     * 
     * @author DZone Snippts Section. http://snippets.dzone.com/posts/show/4831
     * @param packageName
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static ArrayList<Class<?>> getClasses(String packageName, ClassLoader classLoader) throws ClassNotFoundException, IOException {
        final Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

        final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        while (resources.hasMoreElements()) {
            try {
                classes.addAll(findPlugins(resources.nextElement(), packageName, classLoader));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * subdirs.
     * 
     * @author DZone Snippts Section. http://snippets.dzone.com/posts/show/4831
     * @param directory
     * @param packageName
     * @return
     * @throws ClassNotFoundException
     */
    private static List<Class<?>> findPlugins(final URL directory, final String packageName, final ClassLoader classLoader) throws ClassNotFoundException {
        final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        File[] files = null;

        try {
            files = new File(directory.toURI().getPath()).listFiles();
        } catch (Exception e) {
        }

        if (files == null) {
            try {
                // it's a jar
                final String path = directory.toString().substring(4);
                // split path | intern path
                final String[] splitted = path.split("!");

                splitted[1] = splitted[1].substring(1);

                final JarInputStream jarFile = new JarInputStream(new FileInputStream(new File(new URL(splitted[0]).toURI())));
                JarEntry e;

                String jarName;
                while ((e = jarFile.getNextJarEntry()) != null) {
                    jarName = e.getName();
                    if (jarName.startsWith(splitted[1])) {
                        Class<?> c = classLoader.loadClass(jarName.substring(0, jarName.length() - 6).replace("/", "."));
                        if (c != null) {
                            classes.add(c);
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            String fileName = null;
            for (File file : files) {
                try {
                    fileName = file.getName();
                    if (file.isDirectory()) {
                        try {
                            classes.addAll(findPlugins(file.toURI().toURL(), packageName + "." + fileName, classLoader));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    } else if (fileName.endsWith(".class")) {
                        classes.add(classLoader.loadClass(packageName + '.' + fileName.substring(0, fileName.length() - 6)));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return classes;
    }
}
