package org.jdownloader.updatev2;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

public class InstallLog {
    private HashSet<File> sourcePackages;

    public InstallLog() {
        modifiedDirects = new HashSet<String>();
        modifiedFiles = new HashSet<String>();
        modifiedPlugins = new HashSet<String>();
        modifiedExtensionFiles = new HashSet<String>();
        modifiedRestartRequiredFiles = new HashSet<String>();
        sourcePackages = new HashSet<File>();
    }

    public HashSet<File> getSourcePackages() {
        return sourcePackages;
    }

    public Collection<String> getModifiedFiles() {
        return modifiedFiles;
    }

    private Collection<String> modifiedFiles;

    private Collection<String> modifiedPlugins;
    private Collection<String> modifiedDirects;

    public Collection<String> getModifiedPlugins() {
        return modifiedPlugins;
    }

    public Collection<String> getModifiedDirects() {
        return modifiedDirects;
    }

    public Collection<String> getModifiedRestartRequiredFiles() {
        return modifiedRestartRequiredFiles;
    }

    public Collection<String> getModifiedExtensionFiles() {
        return modifiedExtensionFiles;
    }

    private Collection<String> modifiedRestartRequiredFiles;
    private Collection<String> modifiedExtensionFiles;

    public void add(String relPath) {
        modifiedFiles.add(relPath);
        if (relPath.equals("build.json")) {
            modifiedDirects.add(relPath);
            return;
        }
        if (relPath.endsWith(".lng")) {
            modifiedDirects.add(relPath);
            return;
        }

        if (relPath.endsWith(".class") && relPath.toLowerCase(Locale.ENGLISH).startsWith("jd/plugins")) {
            modifiedPlugins.add(relPath);
            return;
        }

        if (relPath.startsWith("extensions/")) {
            modifiedExtensionFiles.add(relPath);
            modifiedRestartRequiredFiles.add(relPath);
            return;
        }
        modifiedRestartRequiredFiles.add(relPath);
    }

    public void merge(InstallLog installLog) {
        modifiedDirects.addAll(installLog.modifiedDirects);
        modifiedFiles.addAll(installLog.modifiedFiles);
        modifiedPlugins.addAll(installLog.modifiedPlugins);
        modifiedExtensionFiles.addAll(installLog.modifiedExtensionFiles);
        modifiedRestartRequiredFiles.addAll(installLog.modifiedRestartRequiredFiles);
    }

}
