package jd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Files;
import org.appwork.utils.Files.Handler;
import org.appwork.utils.IO;

public class JD1Import {

    public static final class SandboxClassloader extends ClassLoader {
        private final HashMap<String, Class<?>> cache = new HashMap<String, Class<?>>();
        public ArrayList<File>                  jars;

        public SandboxClassloader(String org) {
            super(SandboxClassloader.class.getClassLoader());
            jars = new ArrayList<File>();
            final File jar = new File(org);
            final HashSet<Pattern> allowed = new HashSet<Pattern>();
            allowed.add(Pattern.compile("jdownloader\\.jar", Pattern.CASE_INSENSITIVE));
            allowed.add(Pattern.compile("libs[/\\\\].*\\.jar", Pattern.CASE_INSENSITIVE));
            allowed.add(Pattern.compile("plugins[/\\\\].*\\.jar", Pattern.CASE_INSENSITIVE));
            Files.walkThroughStructure(new Handler<RuntimeException>() {

                @Override
                public void intro(File f) throws RuntimeException {
                }

                @Override
                public void onFile(File f) throws RuntimeException {
                    if (f.getName().endsWith(".jar")) {
                        for (Pattern p : allowed) {
                            if (p.matcher(Files.getRelativePath(jar.getParentFile(), f)).matches()) {
                                System.out.println("Add JD09 Jar " + f);
                                jars.add(f);
                                return;
                            }
                        }

                    }
                }

                @Override
                public void outro(File f) throws RuntimeException {
                }

            }, jar.getParentFile());

        }

        @Override
        protected Class<?> findClass(String className) throws ClassNotFoundException {
            if (DownloadLink.class.getName().equals(className)) {
                return DownloadLink.class;

            }
            if (FilePackage.class.getName().equals(className)) {
                return FilePackage.class;

            }

            if (className.startsWith("org.appwork")) {
                Class<?> ret = findSystemClass(className);
                return ret;
            }

            if (className.startsWith("jd.JD1ImportSandbox")) {
                URL url = JD1Import.class.getResource("/" + className.replace(".", "/") + ".class");
                byte[] bytes;
                try {
                    bytes = IO.readURL(url);
                } catch (IOException e) {
                    throw new ClassNotFoundException();
                }
                Class<?> result = this.defineClass(className, bytes, 0, bytes.length, null);
                cache.put(className, result);
                return result;
            }
            for (File jar : jars) {

                JarFile jarFile = null;
                try {
                    byte classByte[];
                    jarFile = new JarFile(jar);
                    final String jarClassName = className.replace(".", "/") + ".class";
                    final JarEntry entry = jarFile.getJarEntry(jarClassName);
                    if (entry != null) {
                        final InputStream is = jarFile.getInputStream(entry);
                        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        final byte[] buffer = new byte[32767];
                        int read = 0;
                        while ((read = is.read(buffer)) != -1) {
                            if (read > 0) {
                                byteStream.write(buffer, 0, read);
                            }
                        }
                        classByte = byteStream.toByteArray();
                        Class<?> result = this.defineClass(className, classByte, 0, classByte.length, null);
                        cache.put(className, result);
                        return result;
                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        jarFile.close();
                    } catch (final Throwable e) {
                    }
                }
            }
            if (className.startsWith("java.")) {
                return findSystemClass(className);
            }
            if (className.startsWith("javax.")) {
                return findSystemClass(className);
            }
            if (className.startsWith("org.w3c.")) {
                return findSystemClass(className);
            }
            return findSystemClass(className);

        }

        @Override
        public URL findResource(final String name) {

            JarFile jarFile = null;
            for (File jar : jars) {

                try {
                    byte classByte[];
                    jarFile = new JarFile(jar);

                    final JarEntry entry = jarFile.getJarEntry(name);
                    if (entry != null) {

                        final String url = jar.toURL().toString();
                        return new URL("jar:" + url + "!/" + name);

                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        jarFile.close();
                    } catch (final Throwable e) {
                    }
                }
            }

            return null;

        }

        @Override
        protected String findLibrary(String libname) {
            return super.findLibrary(libname);
        }

        @Override
        public Class<?> loadClass(String paramString) throws ClassNotFoundException {
            return findClass(paramString);
        }

        @Override
        protected Class<?> loadClass(String paramString, boolean paramBoolean) throws ClassNotFoundException {
            return loadClass(paramString, paramBoolean);
        }
    }

    public static void run(String org) {
        try {

            final ClassLoader cl = new SandboxClassloader(org);

            Class<?> clazz = cl.loadClass("jd.JD1ImportSandbox");
            clazz.getMethod("run", new Class[] { String.class }).invoke(null, new Object[] { org });
            // boolean test = clazz.isAssignableFrom(jd.utils.JDUtilities.class);
            // jd.utils.JDUtilities.getDatabaseConnector();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
