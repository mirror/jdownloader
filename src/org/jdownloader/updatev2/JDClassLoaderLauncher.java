package org.jdownloader.updatev2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.Regex;

public class JDClassLoaderLauncher {
    public static class JDCustomRootClassLoader extends URLClassLoader {
        private JDCustomRootClassLoader(URL[] urls) {
            super(urls);
        }

        private final boolean preferParentClassLoader(final String name) {
            return name.equals(getClass().getName());
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (JVMVersion.isMinimum(JVMVersion.JAVA_15) && "META-INF/services/javax.script.ScriptEngineFactory".equals(name)) {
                final Enumeration<URL> resources = super.getResources(name);
                final List<URL> ret = new ArrayList<URL>();
                while (resources != null && resources.hasMoreElements()) {
                    final URL url = resources.nextElement();
                    ret.add(url);
                }
                // Nashorn JS engine no longer available
                final URL scriptEngineHack = getResource("org/jdownloader/scripting/ScriptEngineFactoryService.html");
                if (scriptEngineHack != null) {
                    ret.add(scriptEngineHack);
                }
                return Collections.enumeration(ret);
            } else {
                return super.getResources(name);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    if (preferParentClassLoader(name) || findResource(name.replace(".", "/") + ".class") == null) {
                        return super.loadClass(name, resolve);
                    } else {
                        c = findClass(name);
                    }
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }

        public void addURL(URL url) {
            super.addURL(url);
        }
    }

    public static void updateClassPath() throws IOException {
        final ClassLoader cl = JDClassLoaderLauncher.class.getClassLoader();
        final URL classPath = Application.getRessourceURL("classpath");
        if (classPath != null) {
            final String sevenZipJBinding;
            if (Application.getResource("libs/sevenzipjbindingCustom.jar").isFile()) {
                // custom build
                sevenZipJBinding = "libs/sevenzipjbindingCustom";
            } else {
                // default build
                sevenZipJBinding = null;
            }
            final List<URL> classPathJARs = new ArrayList<URL>();
            System.out.println("SevenZipJBinding: " + sevenZipJBinding);
            for (final String classPathLine : Regex.getLines(IO.readURLToString(classPath))) {
                final File classPathJAR;
                if (sevenZipJBinding != null && classPathLine.startsWith("libs/sevenzipjbinding")) {
                    final String customclassPathLine = classPathLine.replace("libs/sevenzipjbinding", sevenZipJBinding);
                    classPathJAR = Application.getResource(customclassPathLine);
                    System.out.println("SevenZipJBinding: " + customclassPathLine + ":" + classPathJAR.isFile());
                } else {
                    classPathJAR = Application.getResource(classPathLine);
                }
                if (classPathJAR.isFile()) {
                    classPathJARs.add(classPathJAR.toURI().toURL());
                }
            }
            if (classPathJARs.size() > 0) {
                for (final URL classPathJAR : classPathJARs) {
                    if (cl instanceof JDCustomRootClassLoader) {
                        System.out.println("Add via JDLauncherClassLoader.addURL:" + classPathJAR);
                        ((JDCustomRootClassLoader) cl).addURL(classPathJAR);
                    } else {
                        System.out.println("Add via Application.addUrlToClassPath:" + classPathJAR);
                        Application.addUrlToClassPath(classPathJAR, cl);
                    }
                }
            }
        }
    }

    public void main(final String[] args, final String mainClass) {
        if (mainClass == null || mainClass.trim().length() == 0) {
            throw new RuntimeException("MainClass not set!");
        } else {
            if (JVMVersion.isMinimum(JVMVersion.JAVA_9)) {
                try {
                    final String rootClass = getClass().getName().replaceAll("\\.", "/") + ".class";
                    final URL caller = getClass().getClassLoader().getResource(rootClass);
                    String url = caller.toString();
                    url = url.replaceFirst("^jar:", "");
                    url = url.replace("!/" + rootClass, "");
                    final URL classLoaderURL = new URL(url);
                    if (classLoaderURL != null && url.toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
                        final JDCustomRootClassLoader classLoader = new JDCustomRootClassLoader(new URL[] { classLoaderURL });
                        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                        boolean restoreOldClassLoader = true;
                        try {
                            Thread.currentThread().setContextClassLoader(classLoader);
                            final Class<?> loader = classLoader.loadClass(mainClass);
                            final java.lang.reflect.Method mainMethod = loader.getMethod("main", new Class[] { String[].class });
                            mainMethod.invoke(null, new Object[] { args });
                            restoreOldClassLoader = false;
                            return;
                        } finally {
                            if (restoreOldClassLoader) {
                                Thread.currentThread().setContextClassLoader(oldClassLoader);
                            }
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            try {
                final Class<?> loader = getClass().getClassLoader().loadClass(mainClass);
                final java.lang.reflect.Method mainMethod = loader.getMethod("main", new Class[] { String[].class });
                mainMethod.invoke(null, new Object[] { args });
                return;
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
