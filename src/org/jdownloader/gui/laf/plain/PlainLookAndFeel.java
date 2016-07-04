package org.jdownloader.gui.laf.plain;

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

public class PlainLookAndFeel extends SyntheticaLookAndFeel {

    private static final long serialVersionUID = -6728461590292436336L;
    private boolean           initDone         = false;

    // Construtor
    public PlainLookAndFeel() throws java.text.ParseException {
        // load synth.xml from custom package
        super("/org/jdownloader/gui/laf/plain/synth.xml");
        OS.getCurrentOS();
        initDone = true;
    }

    @Override
    protected void loadCustomXML() throws ParseException {
        super.loadCustomXML();
    }

    @Override
    public void load(InputStream input, Class<?> resourceBase) throws ParseException {
        FileInputStream fis = null;
        if (!initDone) {
            try {
                File custom = Application.getResource("cfg/laf/" + PlainLookAndFeel.class.getSimpleName() + ".xml");
                if (custom.exists()) {
                    LoggerFactory.getDefaultLogger().info("Custom LAF XML: " + custom);
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

    @Override
    protected void loadXMLConfig(String paramString) throws ParseException {
        // // SyntheticaLookAndFeel.class.getResourceAsStream(name)
        // getClass().getClassLoader().
        // URL url = Application.getRessourceURL("PlainLookAndFeelSynth.xml");
        // if (url != null) {
        // try {
        // Application.addUrlToClassPath(url, getClass().getClassLoader());
        //
        // super.loadXMLConfig(url + "");
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // } else {
        super.loadXMLConfig(paramString);
        // }
    }

    // return an unique LAF id
    public String getID() {
        return "SyntheticaJDPlainLookAndFeel";
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
        return "SyntheticaJDPlainLookAndFeel Custom Look and Feel";
    }

}
