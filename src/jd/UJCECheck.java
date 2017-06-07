package jd;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Security;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;

public class UJCECheck {
    // Version 07-06-2017 12:00
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
        final String name = System.getProperty("java.runtime.name");
        final String vendor = System.getProperty("java.vendor");
        return (vendor != null && vendor.contains("Oracle")) || "Java(TM) SE Runtime Environment".equals(name);
    }

    private static final AtomicBoolean checked    = new AtomicBoolean(false);
    private static final AtomicBoolean successful = new AtomicBoolean(false);

    public static boolean isSuccessful() {
        return successful.get();
    }

    public static final void check() {
        if (checked.compareAndSet(false, true)) {
            if (isOracleJVM() && Boolean.TRUE.equals(isRestrictedCryptography())) {
                if (true) {
                    removeCryptographyRestrictions();
                } else {
                    // final String javaHome = System.getProperty("java.home", null);
                    // final File securityFolder = new File(javaHome, "lib" + File.separatorChar + "security");
                    // if (securityFolder.exists() && securityFolder.isDirectory()) {
                    // final long javaVersion = Application.getJavaVersion();
                    // final File uJCE;
                    // if (javaVersion >= Application.JAVA19) {
                    // try {
                    // // JDK1.9
                    // Security.setProperty("crypto.policy", "unlimited");
                    // } catch (final Throwable e) {
                    // }
                    // uJCE = null;
                    // } else if (javaVersion >= Application.JAVA18) {
                    // uJCE = Application.getResource("security/ujce8");
                    // } else if (javaVersion >= Application.JAVA17) {
                    // uJCE = Application.getResource("security/ujce7");
                    // } else if (javaVersion >= Application.JAVA16) {
                    // uJCE = Application.getResource("security/ujce6");
                    // } else {
                    // return;
                    // }
                    // if (uJCE != null && uJCE.exists() && uJCE.isDirectory()) {
                    // ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
                    // @Override
                    // public void onShutdown(ShutdownRequest shutdownRequest) {
                    // try {
                    // IO.copyFolderRecursive(uJCE, securityFolder, true);
                    // } catch (final Throwable e) {
                    // e.printStackTrace();
                    // }
                    // }
                    // });
                    // }
                    // }
                }
            }
        }
    }

    private static void modifyRestricted(Field isRestrictedField) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        /**
         * JceSecurity.isRestricted = false;
         */
        if (Modifier.isFinal(isRestrictedField.getModifiers())) {
            // >=1.8U102
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
            isRestrictedField.setAccessible(true);
            isRestrictedField.set(null, false);
            isRestrictedField.setAccessible(false);
        } else {
            isRestrictedField.setAccessible(true);
            isRestrictedField.set(null, false);
            isRestrictedField.setAccessible(false);
        }
    }

    public static void main(String[] args) {
        removeCryptographyRestrictions();
        if (isRestrictedCryptography()) {
            System.out.println("Hello World!");
        } else {
            System.out.println("Sad world :(");
        }
        // System.out.println("JavaVersion:" + Application.getJavaVersion());
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
            // JDK1.9
            Security.setProperty("crypto.policy", "unlimited");
        } catch (final Throwable e) {
        }
        try {
            try {
                final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
                final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
                final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");
                final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
                final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
                final Field perms = cryptoPermissions.getDeclaredField("perms");
                final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
                modifyRestricted(isRestrictedField);
                defaultPolicyField.setAccessible(true);
                final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);
                /**
                 * JceSecurity.defaultPolicy.perms.clear();
                 */
                perms.setAccessible(true);
                ((Map<?, ?>) perms.get(defaultPolicy)).clear();
                /**
                 * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
                 */
                instance.setAccessible(true);
                defaultPolicy.add((Permission) instance.get(null));
            } catch (final ClassNotFoundException e) {
                try {
                    // Java 6 has obfuscated JCE classes
                    final Class<?> jceSecurity = Class.forName("javax.crypto.SunJCE_b");
                    final Class<?> cryptoPermissions = Class.forName("javax.crypto.SunJCE_d");
                    final Class<?> cryptoAllPermission = Class.forName("javax.crypto.SunJCE_k");
                    final Field defaultPolicyField = jceSecurity.getDeclaredField("c");
                    final Field perms = cryptoPermissions.getDeclaredField("a");
                    final Field isRestrictedField = jceSecurity.getDeclaredField("g");
                    final Field instance = cryptoAllPermission.getDeclaredField("b");
                    if (Boolean.TRUE.equals(isRestrictedField.get(null))) {
                        /**
                         * JceSecurity.isRestricted = false;
                         */
                        if (Modifier.isFinal(isRestrictedField.getModifiers())) {
                            // >=1.8U102
                            Field modifiers = Field.class.getDeclaredField("modifiers");
                            modifiers.setAccessible(true);
                            modifiers.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
                            isRestrictedField.setAccessible(true);
                            isRestrictedField.set(null, false);
                            isRestrictedField.setAccessible(false);
                        } else {
                            isRestrictedField.setAccessible(true);
                            isRestrictedField.set(null, false);
                            isRestrictedField.setAccessible(false);
                        }
                    }
                    defaultPolicyField.setAccessible(true);
                    final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);
                    /**
                     * JceSecurity.defaultPolicy.perms.clear();
                     */
                    perms.setAccessible(true);
                    ((Map<?, ?>) perms.get(defaultPolicy)).clear();
                    /**
                     * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
                     */
                    instance.setAccessible(true);
                    defaultPolicy.add((Permission) instance.get(null));
                } catch (final ClassNotFoundException e2) {
                    throw e;
                }
            }
            successful.set(true);
            System.out.println("Successfully removed cryptography restrictions");
        } catch (final Throwable e) {
            System.out.println("Failed to remove cryptography restrictions:" + e);
        }
    }
}
