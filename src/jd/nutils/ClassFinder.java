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

public class ClassFinder {

    public static ArrayList<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
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
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
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
    private static List<Class<?>> findPlugins(URL directory, String packageName, ClassLoader classLoader) throws ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        File[] files = null;

        try {
            files = new File(directory.toURI().getPath()).listFiles();
        } catch (Exception e) {
        }

        if (files == null) {
            try {
                // it's a jar
                String path = directory.toString().substring(4);
                // split path | intern path
                String[] splitted = path.split("!");

                splitted[1] = splitted[1].substring(1);
                File file = new File(new URL(splitted[0]).toURI());

                JarInputStream jarFile = new JarInputStream(new FileInputStream(file));
                JarEntry e;

                while ((e = jarFile.getNextJarEntry()) != null) {
                    if (e.getName().startsWith(splitted[1])) {
                        Class<?> c = classLoader.loadClass(e.getName().substring(0, e.getName().length() - 6).replace("/", "."));
                        if (c != null) {
                            classes.add(c);
                        }
                    }
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }

        } else {
            for (File file : files) {
                if (file.isDirectory()) {
                    try {
                        classes.addAll(findPlugins(file.toURI().toURL(), packageName + "." + file.getName(), classLoader));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                } else if (file.getName().endsWith(".class")) {
                    Class<?> c = classLoader.loadClass(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                    classes.add(c);
                }
            }
        }
        return classes;
    }
}
