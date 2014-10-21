package org.jdownloader.updatev2.restart;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.updatev2.RestartController;

public class LinuxRestarter extends Restarter {
    @Override
    protected List<String> getApplicationStartCommands(File root) {
        final ArrayList<String> lst = new ArrayList<String>();
        final String exe4JModuleName = System.getProperty("exe4j.moduleName");
        if (exe4JModuleName != null && exe4JModuleName.length() > 0) {
            try {
                final File exe4JLauncher = new File(exe4JModuleName);
                if (exe4JLauncher.isFile() && exe4JLauncher.exists() && exe4JLauncher.canExecute() && exe4JLauncher.length() > 0) {
                    lst.add(exe4JModuleName);
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        } else {
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
            if (h.startsWith("-agentlib:")) {
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
