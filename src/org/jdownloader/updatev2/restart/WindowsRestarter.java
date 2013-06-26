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
        ArrayList<String> lst = new ArrayList<String>();
        File restartBinary = new File(root, "JDownloader.exe");
        if (!restartBinary.exists() || !restartBinary.isFile()) restartBinary = new File(root, "JDownloader2.exe");
        if (!restartBinary.exists() || !restartBinary.isFile()) restartBinary = new File(root, "JDownloader 2.exe");
        if (restartBinary.exists() && restartBinary.isFile()) {
            getLogger().info("Found binary: " + restartBinary + " for restart");
            try {
                String binaryHash = Hash.getMD5(restartBinary);
                if ("a08b3424355c839138f326c06a964b9e".equals(binaryHash)) {
                    getLogger().info("Workaround: found locking binary " + binaryHash + "! Will use JavaBinary as workaround!");
                    restartBinary = null;
                }
            } catch (final Throwable e) {
                getLogger().log(e);
            }
        } else {
            restartBinary = null;
        }
        if (restartBinary != null) {
            lst.add(restartBinary.getAbsolutePath());
        } else {
            getLogger().info("No binary found! Will use JavaBinary!");
            lst.addAll(getJVMApplicationStartCommands(root));
        }
        return lst;
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
