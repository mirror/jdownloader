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
    // Version 09-05-2019 17:40
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
        return UJCECheck.successful.get() || Boolean.FALSE.equals(isRestrictedCryptography());
    }

    public static final void check() {
        if (UJCECheck.checked.compareAndSet(false, true)) {
            if (UJCECheck.isOracleJVM() && Boolean.TRUE.equals(UJCECheck.isRestrictedCryptography())) {
                UJCECheck.removeCryptographyRestrictions();
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
        if (Boolean.FALSE.equals(UJCECheck.isRestrictedCryptography())) {
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
        if (Boolean.FALSE.equals(UJCECheck.isRestrictedCryptography())) {
            System.out.println("Cryptography restrictions removal not needed");
        } else {
            try {
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
}
