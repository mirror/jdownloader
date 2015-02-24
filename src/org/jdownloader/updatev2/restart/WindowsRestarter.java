package org.jdownloader.updatev2.restart;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.updatev2.RestartController;

public class WindowsRestarter extends Restarter {

    @Override
    protected List<String> getApplicationStartCommands(File root) {
        final String[] binaryPaths = new String[] { "JDownloader2.exe", "JDownloader 2.exe", "JDownloader.exe" };
        for (final String binaryPath : binaryPaths) {
            final File restartBinary = new File(root, binaryPath);
            if (restartBinary.exists() && restartBinary.isFile()) {
                getLogger().info("Found binary: " + restartBinary + " for restart");
                try {
                    final String binaryHash = Hash.getMD5(restartBinary);
                    if ("a08b3424355c839138f326c06a964b9e".equals(binaryHash)) {
                        getLogger().info("Workaround: found locking binary " + binaryHash);
                        continue;
                    }
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
                final ArrayList<String> ret = new ArrayList<String>();
                ret.add(restartBinary.getAbsolutePath());
                return ret;
            }
        }
        getLogger().info("No binary found! Will use JavaBinary!");
        final ArrayList<String> ret = new ArrayList<String>();
        ret.addAll(getJVMApplicationStartCommands(root));
        return ret;
    }

    @Override
    protected List<String> getJVMApplicationStartCommands(File root) {
        final java.util.List<String> jvmParameter = new ArrayList<String>();
        jvmParameter.add(CrossSystem.getJavaBinary());
        final List<String> lst = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (final String h : lst) {
            if (h.equals("exit")) {
                getLogger().info("Workaround: removing buggy exit parameter");
                /* JDownloaderExp.exe appends exit to jvm parameter list -> java cannot find mainclass anymore */
                continue;
            } else if (h.startsWith("-agentlib:")) {
                continue;
            } else {
                jvmParameter.add(h);
            }
        }
        jvmParameter.add("-jar");
        jvmParameter.add(Application.getJarName(RestartController.class));
        return jvmParameter;

    }

}
