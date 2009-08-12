package jd.gui.swing.laf.synthetica;

import java.io.IOException;
import java.text.ParseException;

import jd.controlling.JDLogger;
import jd.utils.JDUtilities;
import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;

/**
 * <p>
 * JSynthLaf Copyright (C) 2009 JD-Team support@jdownloader.org JSynthLaf is
 * original based on Lafaello which is based itself on Synthetica (javasoft.de)
 * 1.41. See http://svn.jdownloader.org for all changes made by JD-Team
 * 
 * Lafaello Copyright (C) 2009 (based on GPL version 1.4 of Synthetica -
 * http://www.javasoft.de/jsf/public/products/synthetica)
 * </p>
 * 
 * <p>
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * </p>
 * 
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * </p>
 * 
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see {@link http://www.gnu.org/licenses}.
 * </p>
 * 
 * @author lafaello@gmail.com
 * 
 */
public class SyntheticaJayDownLookAndFeel extends SyntheticaLookAndFeel {

    private static final long serialVersionUID = -1032317968282284025L;

    public SyntheticaJayDownLookAndFeel() throws ParseException, IOException {
        super("blackmoon/synth.xml");
        long start = System.currentTimeMillis();
        Class<?> clazz = SyntheticaLookAndFeel.class;
        load(JDUtilities.getResourceURL("jd/img/synthetica/jaydown/synth.xml").openStream(), clazz);
        try {
            String syntheticaFileName = "Synthetica.xml";
            load(clazz.getResourceAsStream((new StringBuilder("/")).append(syntheticaFileName).toString()), clazz);

            JDLogger.getLogger().finest((new StringBuilder("[Info] Found '")).append(syntheticaFileName).append("' configuration file.").toString());
        } catch (IllegalArgumentException illegalargumentexception) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        String className = getClass().getName();
        try {
            String syntheticaFileName = (new StringBuilder(String.valueOf(className.substring(className.lastIndexOf(".") + 1)))).append(".xml").toString();
            load(clazz.getResourceAsStream((new StringBuilder("/")).append(syntheticaFileName).toString()), clazz);

            JDLogger.getLogger().finest((new StringBuilder("[Info] Found '")).append(syntheticaFileName).append("' configuration file.").toString());
        } catch (IllegalArgumentException illegalargumentexception1) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        long stop = System.currentTimeMillis();
        JDLogger.getLogger().finest((new StringBuilder("Time for loading LAF: ")).append(stop - start).append("ms").toString());

    }

    @Override
    public String getID() {
        return "SyntheticaJayDownLookAndFeel";
    }

    @Override
    public String getName() {
        return "Synthetica JayDown Look and Feel";
    }
}
