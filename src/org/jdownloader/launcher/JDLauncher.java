package org.jdownloader.launcher;

import org.jdownloader.updatev2.JDClassLoaderLauncher;

public class JDLauncher extends JDClassLoaderLauncher {
    public static void main(String[] args) {
        new JDLauncher().main(args, "org.jdownloader.launcher.StandaloneLauncher");
    }
}
