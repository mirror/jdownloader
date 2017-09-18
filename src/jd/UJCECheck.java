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
    // Version 14-09-2017 17:00
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
        return vendor != null && vendor.contains("Oracle") || "Java(TM) SE Runtime Environment".equals(name);
    }

    private static final AtomicBoolean checked    = new AtomicBoolean(false);
    private static final AtomicBoolean successful = new AtomicBoolean(false);

    public static boolean isSuccessful() {
        return UJCECheck.successful.get();
    }

    public static final void check() {
        if (UJCECheck.checked.compareAndSet(false, true)) {
            if (UJCECheck.isOracleJVM() && Boolean.TRUE.equals(UJCECheck.isRestrictedCryptography())) {
                if (true) {
                    UJCECheck.removeCryptographyRestrictions();
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
        UJCECheck.removeCryptographyRestrictions();
        if (!UJCECheck.isRestrictedCryptography()) {
            System.out.println("Hello World!");
        } else {
            System.out.println("Sad world :(");
        }
    }

    private static void removeCryptographyRestrictions_M1() throws Throwable {
        final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
        final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
        final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");
        final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
        final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
        final Field perms = cryptoPermissions.getDeclaredField("perms");
        final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
        UJCECheck.modifyRestricted(isRestrictedField);
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
    }

    private static void removeCryptographyRestrictions_M2() throws Throwable {
        // IBM has obfuscated JCE classes
        final Class<?> jceSecurity = Class.forName("javax.crypto.d");
        final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");
        final Field defaultPolicyField = jceSecurity.getDeclaredField("a");
        final Field instance = cryptoAllPermission.getDeclaredField("h");
        defaultPolicyField.setAccessible(true);
        final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);
        final Field perms = defaultPolicy.getClass().getDeclaredField("a");
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
    }

    private static void removeCryptographyRestrictions_M3() throws Throwable {
        // Java 6 has obfuscated JCE classes
        final Class<?> jceSecurity = Class.forName("javax.crypto.SunJCE_b");
        final Class<?> cryptoPermissions = Class.forName("javax.crypto.SunJCE_d");
        final Class<?> cryptoAllPermission = Class.forName("javax.crypto.SunJCE_k");
        final Field defaultPolicyField = jceSecurity.getDeclaredField("c");
        final Field perms = cryptoPermissions.getDeclaredField("a");
        final Field isRestrictedField = jceSecurity.getDeclaredField("g");
        final Field instance = cryptoAllPermission.getDeclaredField("b");
        UJCECheck.modifyRestricted(isRestrictedField);
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
    }

    /**
     * http://stackoverflow.com/questions/1179672/how-to-avoid-installing-unlimited-strength-jce-policy-files-when-deploying-an
     *
     * http://stackoverflow.com/questions/18435227/java-patching-client-side-security-policy-from-applet-for-aes256
     */
    private static void removeCryptographyRestrictions() {
        if (!UJCECheck.isRestrictedCryptography()) {
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
                UJCECheck.removeCryptographyRestrictions_M1();
            } catch (final ClassNotFoundException e) {
                e.printStackTrace();
                try {
                    UJCECheck.removeCryptographyRestrictions_M2();
                } catch (final ClassNotFoundException e2) {
                    e2.printStackTrace();
                    try {
                        UJCECheck.removeCryptographyRestrictions_M3();
                    } catch (final ClassNotFoundException e3) {
                        e3.printStackTrace();
                        throw e;
                    }
                }
            }
            UJCECheck.successful.set(true);
            System.out.println("Successfully removed cryptography restrictions");
        } catch (final Throwable e) {
            System.out.println("Failed to remove cryptography restrictions:" + e);
        }
    }
}
