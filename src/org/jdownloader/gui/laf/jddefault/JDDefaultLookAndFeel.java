package org.jdownloader.gui.laf.jddefault;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.images.NewTheme;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import de.javasoft.util.OS;

public class JDDefaultLookAndFeel extends SyntheticaLookAndFeel {
    private static final long serialVersionUID = -6828461590292436336L;
    private boolean           initDone         = false;

    // Construtor
    public JDDefaultLookAndFeel() throws java.text.ParseException {
        // load synth.xml from custom package
        super("/org/jdownloader/gui/laf/jddefault/synth.xml");
        OS.getCurrentOS();
        initDone = true;
    }

    // return an unique LAF id
    public String getID() {
        return "SyntheticaJDVisionLookAndFeel";
    }

    private final AtomicBoolean newThemeAvailable = new AtomicBoolean(false);

    @Override
    public void load(InputStream input, Class<?> resourceBase) throws ParseException {
        FileInputStream fis = null;
        if (!initDone) {
            try {
                File custom = Application.getResource("cfg/laf/" + JDDefaultLookAndFeel.class.getSimpleName() + ".xml");
                LoggerFactory.getDefaultLogger().info("Custom LAF XML: " + custom);
                if (custom.exists()) {
                    super.load(new BufferedInputStream(fis = new FileInputStream(custom)), resourceBase);
                    input.close();
                    return;
                }
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (Throwable e) {
                    LoggerFactory.getDefaultLogger().log(e);
                }
            }
        }
        super.load(input, resourceBase);
    }

    public Icon getDisabledIcon(JComponent component, Icon icon) {
        if (!newThemeAvailable.get()) {
            try {
                final Class<?> newTheme = getClass().getClassLoader().loadClass("org.jdownloader.images.NewTheme");
                if (newTheme != null) {
                    newThemeAvailable.set(true);
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        if (newThemeAvailable.get()) {
            return NewTheme.I().getDisabledIcon(component, icon);
        } else {
            return super.getDisabledIcon(component, icon);
        }
    }

    // return the LAF name - readable for humans
    public String getName() {
        return "SyntheticaJDVisionLookAndFeel Custom Look and Feel";
    }
}
