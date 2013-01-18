package org.jdownloader.updatev2.restart;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.updatev2.RestartController;

public class LinuxRestarter extends Restarter {
    @Override
    protected List<String> getApplicationStartCommands() {
        ArrayList<String> lst = new ArrayList<String>();
        // TODO: user start of install4j
        lst.addAll(getJVMApplicationStartCommands());
        return lst;

    }

    @Override
    protected List<String> getJVMApplicationStartCommands() {

        final java.util.List<String> jvmParameter = new ArrayList<String>();

        jvmParameter.add(CrossSystem.getJavaBinary());

        final List<String> lst = ManagementFactory.getRuntimeMXBean().getInputArguments();

        boolean xmsset = false;
        boolean useconc = false;
        boolean minheap = false;
        boolean maxheap = false;

        for (final String h : lst) {
            if (h.contains("Xmx")) {
                if (Runtime.getRuntime().maxMemory() < 533000000) {
                    jvmParameter.add("-Xmx512m");
                    continue;
                }
            } else if (h.contains("xms")) {
                xmsset = true;
                jvmParameter.add(h);
            } else if (h.contains("XX:+useconc")) {
                useconc = true;
                jvmParameter.add(h);
            } else if (h.contains("minheapfree")) {
                minheap = true;
                jvmParameter.add(h);
            } else if (h.contains("maxheapfree")) {
                maxheap = true;
                jvmParameter.add(h);
            } else if (h.startsWith("-agentlib:")) {
                continue;
            }

        }
        if (!xmsset) {

            jvmParameter.add("-Xms64m");
        }
        if (CrossSystem.isLinux()) {

            if (!useconc) {
                jvmParameter.add("-XX:+UseConcMarkSweepGC");
            }
            if (!minheap) {
                jvmParameter.add("-XX:MinHeapFreeRatio=0");
            }
            if (!maxheap) {
                jvmParameter.add("-XX:MaxHeapFreeRatio=0");
            }
        }
        jvmParameter.add("-jar");
        jvmParameter.add(Application.getJarName(RestartController.class));
        return jvmParameter;

    }
}
