package org.jdownloader.gui.laf.jddefault;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.jdownloader.images.NewTheme;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import de.javasoft.util.OS;

public class JDDefaultLookAndFeel extends SyntheticaLookAndFeel {

    private static final long serialVersionUID = -6728461590292436336L;

    // Construtor
    public JDDefaultLookAndFeel() throws java.text.ParseException {
        // load synth.xml from custom package
        super("/org/jdownloader/gui/laf/jddefault/synth.xml");
        OS.getCurrentOS();
    }

    // return an unique LAF id
    public String getID() {
        return "SyntheticaJDVisionLookAndFeel";
    }

    private final AtomicBoolean newThemeAvailable = new AtomicBoolean(false);

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
