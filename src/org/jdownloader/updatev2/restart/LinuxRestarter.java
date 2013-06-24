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
        ArrayList<String> lst = new ArrayList<String>();
        // TODO: user start of install4j
        lst.addAll(getJVMApplicationStartCommands(root));
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
