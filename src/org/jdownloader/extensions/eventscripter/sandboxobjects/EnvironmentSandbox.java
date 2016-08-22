package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;

import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;

public class EnvironmentSandbox {

    public EnvironmentSandbox() {
    }

    public boolean isWindows() {
        return CrossSystem.isWindows();
    }

    public boolean isLinux() {
        return CrossSystem.isLinux();
    }

    public boolean isMac() {
        return CrossSystem.isMac();
    }

    public boolean isBSD() {
        return CrossSystem.isBSD();
    }

    public boolean is64BitOS() {
        return CrossSystem.is64BitOperatingSystem();
    }

    public boolean is64BitArch() {
        return CrossSystem.is64BitArch();
    }

    public boolean is64BitJava() {
        return Application.is64BitJvm();
    }

    public boolean isHeadless() {
        return Application.isHeadless();
    }

    public String getARCHFamily() {
        return String.valueOf(CrossSystem.getARCHFamily());
    }

    public String getOS() {
        return String.valueOf(CrossSystem.getOS());
    }

    public String getOSFamily() {
        return String.valueOf(CrossSystem.getOSFamily());
    }

    public String getNewLine() {
        return String.valueOf(CrossSystem.getNewLine());
    }

    public String getPathSeparator() {
        return File.separatorChar + "";
    }

    public long getJavaVersion() {
        return Application.getJavaVersion();
    }

}
