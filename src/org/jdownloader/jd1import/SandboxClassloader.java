package org.jdownloader.jd1import;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.URLStream;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public final class SandboxClassloader extends ClassLoader {
    protected final File jars[];

    public SandboxClassloader(final File root) {
        super(SandboxClassloader.class.getClassLoader());
        final ArrayList<File> jars = new ArrayList<File>();
        final HashSet<Pattern> allowed = new HashSet<Pattern>();
        allowed.add(Pattern.compile("jdownloader\\.jar", Pattern.CASE_INSENSITIVE));
        allowed.add(Pattern.compile("jdownloader\\.jar.backup_\\d+", Pattern.CASE_INSENSITIVE));
        allowed.add(Pattern.compile("libs[/\\\\].*\\.jar", Pattern.CASE_INSENSITIVE));
        allowed.add(Pattern.compile("plugins[/\\\\].*\\.jar", Pattern.CASE_INSENSITIVE));
        Files.walkThroughStructure(new org.appwork.utils.Files.AbstractHandler<RuntimeException>() {
            @Override
            public void onFile(File f) throws RuntimeException {
                for (Pattern p : allowed) {
                    if (p.matcher(Files.getRelativePath(root, f)).matches()) {
                        System.out.println("Add JD09 Jar " + f);
                        jars.add(f);
                        return;
                    }
                }
            }
        }, root);
        this.jars = jars.toArray(new File[] {});
    }

    @Override
    protected synchronized Class<?> findClass(String className) throws ClassNotFoundException {
        if (DownloadLink.class.getName().equals(className)) {
            return DownloadLink.class;
        }
        if (FilePackage.class.getName().equals(className)) {
            return FilePackage.class;
        }
        if (className.startsWith("org.appwork")) {
            final Class<?> ret = findSystemClass(className);
            return ret;
        }
        final Class<?> loaded = findLoadedClass(className);
        if (loaded != null) {
            return loaded;
        }
        if (className.startsWith(JD1ImportSandbox.class.getName())) {
            final URL url = SandboxClassloader.class.getResource("/" + className.replace(".", "/") + ".class");
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream() {
                @Override
                public synchronized byte[] toByteArray() {
                    return buf;
                }
            };
            InputStream is = null;
            try {
                is = URLStream.openStream(url);
                IO.readStream(-1, is, byteStream, true);
            } catch (IOException e) {
                throw new ClassNotFoundException("Missing:" + className, e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (Throwable e) {
                }
            }
            final Class<?> result = this.defineClass(className, byteStream.toByteArray(), 0, byteStream.size(), null);
            return result;
        }
        final String jarClassName = className.replace(".", "/") + ".class";
        for (File jar : jars) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(jar);
                final JarEntry entry = jarFile.getJarEntry(jarClassName);
                if (entry != null) {
                    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream() {
                        @Override
                        public synchronized byte[] toByteArray() {
                            return buf;
                        }
                    };
                    final InputStream is = jarFile.getInputStream(entry);
                    try {
                        IO.readStream(-1, is, byteStream, true);
                    } catch (IOException e) {
                        throw new ClassNotFoundException("Missing:" + jarClassName, e);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                    final Class<?> result = this.defineClass(className, byteStream.toByteArray(), 0, byteStream.size(), null);
                    return result;
                }
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (jarFile != null) {
                        jarFile.close();
                    }
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
                jarFile = new JarFile(jar);
                final JarEntry entry = jarFile.getJarEntry(name);
                if (entry != null) {
                    final String url = jar.toURI().toURL().toString();
                    return new URL("jar:" + url + "!/" + name);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (jarFile != null) {
                        jarFile.close();
                    }
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