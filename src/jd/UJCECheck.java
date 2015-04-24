package jd;

import java.io.File;
import java.lang.reflect.Field;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.Application;
import org.appwork.utils.IO;

public class UJCECheck {

    private static Boolean isRestrictedCryptography() {
        try {
            final int strength = Cipher.getMaxAllowedKeyLength("AES");
            if (strength > 128) {
                return false;
            } else {
                return true;
            }

        } catch (Throwable e) {
            return null;
        }
    }

    private static boolean isOracleJVM() {
        return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
    }

    private static final AtomicBoolean checked = new AtomicBoolean(false);

    public static final void check() {
        if (checked.compareAndSet(false, true)) {
            if (isOracleJVM() && Boolean.TRUE.equals(isRestrictedCryptography())) {
                if (true) {
                    removeCryptographyRestrictions();
                } else {
                    final String javaHome = System.getProperty("java.home", null);
                    final File securityFolder = new File(javaHome, "lib" + File.separatorChar + "security");
                    if (securityFolder.exists() && securityFolder.isDirectory()) {
                        final long javaVersion = Application.getJavaVersion();
                        final File uJCE;
                        if (javaVersion >= Application.JAVA18) {
                            uJCE = Application.getResource("security/ujce8");
                        } else if (javaVersion >= Application.JAVA17) {
                            uJCE = Application.getResource("security/ujce7");
                        } else if (javaVersion >= Application.JAVA16) {
                            uJCE = Application.getResource("security/ujce6");
                        } else {
                            return;
                        }
                        if (uJCE.exists() && uJCE.isDirectory()) {
                            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                                @Override
                                public void onShutdown(ShutdownRequest shutdownRequest) {
                                    try {
                                        IO.copyFolderRecursive(uJCE, securityFolder, true);
                                    } catch (final Throwable e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }
                    }
                }
            }
        }
    }

    /**
     * http://stackoverflow.com/questions/1179672/how-to-avoid-installing-unlimited-strength-jce-policy-files-when-deploying-an
     *
     * http://stackoverflow.com/questions/18435227/java-patching-client-side-security-policy-from-applet-for-aes256
     */
    private static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            System.out.println("Cryptography restrictions removal not needed");
            return;
        }
        try {
            try {
                final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
                final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");

                /**
                 * JceSecurity.isRestricted = false;
                 */
                isRestrictedField.setAccessible(true);
                isRestrictedField.set(null, false);

                final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
                defaultPolicyField.setAccessible(true);
                final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

                /**
                 * JceSecurity.defaultPolicy.perms.clear();
                 */
                final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
                final Field perms = cryptoPermissions.getDeclaredField("perms");
                perms.setAccessible(true);
                ((Map<?, ?>) perms.get(defaultPolicy)).clear();

                /**
                 * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
                 */
                final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");
                final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
                instance.setAccessible(true);
                defaultPolicy.add((Permission) instance.get(null));
            } catch (final ClassNotFoundException e) {
                try {
                    // Java 6 has obfuscated JCE classes
                    final Class<?> jceSecurity = Class.forName("javax.crypto.SunJCE_b");
                    final Field isRestrictedField = jceSecurity.getDeclaredField("g");

                    /**
                     * JceSecurity.isRestricted = false;
                     */
                    isRestrictedField.setAccessible(true);
                    isRestrictedField.set(null, false);

                    final Field defaultPolicyField = jceSecurity.getDeclaredField("c");
                    defaultPolicyField.setAccessible(true);
                    final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

                    /**
                     * JceSecurity.defaultPolicy.perms.clear();
                     */
                    final Class<?> cryptoPermissions = Class.forName("javax.crypto.SunJCE_d");
                    final Field perms = cryptoPermissions.getDeclaredField("a");
                    perms.setAccessible(true);
                    ((Map<?, ?>) perms.get(defaultPolicy)).clear();

                    /**
                     * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
                     */
                    final Class<?> cryptoAllPermission = Class.forName("javax.crypto.SunJCE_k");
                    final Field instance = cryptoAllPermission.getDeclaredField("b");
                    instance.setAccessible(true);
                    defaultPolicy.add((Permission) instance.get(null));
                } catch (final ClassNotFoundException e2) {
                    throw e;
                }
            }
            System.out.println("Successfully removed cryptography restrictions");
        } catch (final Throwable e) {
            System.out.println("Failed to remove cryptography restrictions:" + e);
        }
    }

}
