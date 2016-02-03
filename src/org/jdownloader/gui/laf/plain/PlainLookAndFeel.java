package org.jdownloader.gui.laf.plain;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.jdownloader.images.NewTheme;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import de.javasoft.util.OS;

public class PlainLookAndFeel extends SyntheticaLookAndFeel {

    private static final long serialVersionUID = -6728461590292436336L;

    // Construtor
    public PlainLookAndFeel() throws java.text.ParseException {
        // load synth.xml from custom package
        super("/org/jdownloader/gui/laf/plain/synth.xml");
        OS.getCurrentOS();
    }

    // return an unique LAF id
    public String getID() {
        return "SyntheticaJDPlainLookAndFeel";
    }

    public Icon getDisabledIcon(JComponent component, Icon icon) {
        return NewTheme.I().getDisabledIcon(icon);
    }

    // return the LAF name - readable for humans
    public String getName() {
        return "SyntheticaJDPlainLookAndFeel Custom Look and Feel";
    }

}
