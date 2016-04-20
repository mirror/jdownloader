package org.jdownloader.api.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcMounts {

    private final String device;
    private final String fileSystem;

    public String getFileSystem() {
        return fileSystem;
    }

    public String getDevice() {
        return device;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    private final String mountPoint;

    private ProcMounts(final String device, final String mountPoint, final String fileSystem) {
        this.device = device;
        this.mountPoint = mountPoint;
        this.fileSystem = fileSystem;
    }

    @Override
    public String toString() {
        return "Device:" + getDevice() + "|MountPoint:" + getMountPoint() + "|FileSystem:" + getFileSystem();
    }

    public static List<ProcMounts> list() throws IOException {
        FileInputStream fis = null;
        try {
            final File procMounts = new File("/proc/mounts");
            final List<ProcMounts> ret = new ArrayList<ProcMounts>();
            if (procMounts.exists()) {
                fis = new FileInputStream(procMounts);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    String[] infos = line.split(" ");
                    if (infos.length >= 6) {
                        final ProcMounts mount = new ProcMounts(infos[0], infos[1].replaceAll("\\\\040", " "), infos[2]);
                        ret.add(mount);
                    }
                }
                fis.close();
                fis = null;
            }
            return ret;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
}
