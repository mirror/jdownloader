package org.jdownloader.updatev2.gui;

import java.awt.Toolkit;
import java.net.URL;

import org.appwork.utils.Application;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.os.CrossSystem;

public enum LookAndFeelType {
    FLATLAF_LIGHT("flatlaf-themes", "com.formdev.flatlaf.FlatLightLaf", JVMVersion.JAVA_1_8),
    FLATLAF_MAC_LIGHT("flatlaf-themes", "com.formdev.flatlaf.themes.FlatMacLightLaf", JVMVersion.JAVA_1_8),
    FLATLAF_DARK("flatlaf-themes", "com.formdev.flatlaf.FlatDarkLaf", JVMVersion.JAVA_1_8),
    FLATLAF_MAC_DARK("flatlaf-themes", "com.formdev.flatlaf.themes.FlatMacDarkLaf", JVMVersion.JAVA_1_8),
    FLATLAF_INTELLIJ("flatlaf-themes", "com.formdev.flatlaf.FlatIntelliJLaf", JVMVersion.JAVA_1_8),
    FLATLAF_DRACULA("flatlaf-themes", "com.formdev.flatlaf.FlatDarculaLaf", JVMVersion.JAVA_1_8),
    JAVA_METAL(null, "javax.swing.plaf.metal.MetalLookAndFeel"),
    JAVA_SYSTEM(null, null) {
        @Override
        public String getClazz() {
            if (!Application.isHeadless()) {
                try {
                    final Toolkit toolkit = Toolkit.getDefaultToolkit();
                    switch (CrossSystem.getOSFamily()) {
                    case WINDOWS:
                        return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
                    case LINUX:
                        if (ReflectionUtils.isInstanceOf("sun.awt.SunToolkit", toolkit) && Boolean.TRUE.equals(ReflectionUtils.invoke("sun.awt.SunToolkit", "isNativeGTKAvailable", toolkit, Boolean.class))) {
                            return "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
                        }
                        break;
                    case MAC:
                        if (ReflectionUtils.isInstanceOf("sun.lwawt.macosx.LWCToolkit", toolkit)) {
                            return "com.apple.laf.AquaLookAndFeel";
                        }
                        break;
                    default:
                        break;
                    }
                } catch (Throwable ignore) {
                }
            }
            return "javax.swing.plaf.metal.MetalLookAndFeel";
        }
    },
    JAVA_NIMBUS(null, "javax.swing.plaf.nimbus.NimbusLookAndFeel"),
    ALU_OXIDE("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaAluOxideLookAndFeel"),
    BLACK_EYE("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel"),
    BLACK_MOON("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel"),
    BLACK_STAR("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel"),
    BLUE_ICE("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel"),
    BLUE_LIGHT("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlueLightLookAndFeel"),
    BLUE_MOON("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel"),
    BLUE_STEEL("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlueSteelLookAndFeel"),
    CLASSY("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaClassyLookAndFeel"),
    GREEN_DREAM("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaGreenDreamLookAndFeel"),
    MAUVE_METALLIC("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel"),
    ORANGE_METALLIC("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaOrangeMetallicLookAndFeel"),
    DARK("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaDarkLookAndFeel"),
    PLAIN("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaPlainLookAndFeel"),
    SILVER_MOON("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel"),
    SIMPLE_2D(null, "de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel"),
    SKY_METALLIC("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel"),
    STANDARD("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel"),
    WHITE_VISION("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel"),
    JD_PLAIN("theme-plain", "org.jdownloader.gui.laf.plain.PlainLookAndFeel"),
    DEFAULT(null, "org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel");

    private final String clazz;
    private final String extensionID;
    private final long   minimumJVMVersoin;

    public final String getExtensionID() {
        return extensionID;
    }

    public String getClazz() {
        return clazz;
    }

    public final long getMinimumJVMVersion() {
        return minimumJVMVersoin;
    }

    private LookAndFeelType(String extensionID, String clazz, long minimumJVMVersoin) {
        this.clazz = clazz;
        this.extensionID = extensionID;
        this.minimumJVMVersoin = minimumJVMVersoin;
    }

    private LookAndFeelType(String extensionID, String clazz) {
        this(extensionID, clazz, JVMVersion.JAVA_1_6);
    }

    public boolean isSupported() {
        return JVMVersion.isMinimum(getMinimumJVMVersion());
    }

    public boolean isAvailable() {
        try {
            if (isSupported()) {
                // do not use Class.forName here since this would load the class
                final String path = "/" + getClazz().replace(".", "/") + ".class";
                final URL classPath = getClass().getResource(path);
                return classPath != null;
            } else {
                return false;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
}