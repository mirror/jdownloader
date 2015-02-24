package org.jdownloader.extensions.extraction.multi;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.appwork.utils.os.CrossSystem;

public class FilePermission17 {

    public static void setFilePermission(final File file, final FilePermissionSet filePermissionSet) throws IOException {
        if (file != null && file.exists() && filePermissionSet != null) {
            if (CrossSystem.isLinux()) {
                final Set<java.nio.file.attribute.PosixFilePermission> permissions = new HashSet<java.nio.file.attribute.PosixFilePermission>();
                // add owners permission
                if (filePermissionSet.isUserRead()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_READ);
                }
                if (filePermissionSet.isUserWrite()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_WRITE);
                }
                if (filePermissionSet.isUserExecute()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE);
                }
                // add group permissions
                if (filePermissionSet.isGroupRead()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.GROUP_READ);
                }
                if (filePermissionSet.isGroupWrite()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.GROUP_WRITE);
                }
                if (filePermissionSet.isGroupExecute()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE);
                }
                // add others permissions
                if (filePermissionSet.isOtherRead()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.OTHERS_READ);
                }
                if (filePermissionSet.isOtherWrite()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE);
                }
                if (filePermissionSet.isOtherExecute()) {
                    permissions.add(java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE);
                }
                java.nio.file.Files.setPosixFilePermissions(file.toPath(), permissions);
            } else {
                if (!file.setExecutable(true, filePermissionSet.isOtherExecute() == false && filePermissionSet.isOtherExecute() == false)) {
                    throw new IOException("Failed to set " + filePermissionSet + " to " + file);
                }
            }
        }
    }
}
