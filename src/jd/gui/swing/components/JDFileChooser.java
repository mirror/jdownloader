//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.components;

import java.io.File;

import javax.swing.JFileChooser;

import jd.GeneralSettings;
import jd.config.Configuration;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;

/**
 * Ein Wrapper um JFileChooser
 * 
 * @author JD-Team
 */
public class JDFileChooser extends JFileChooser {

    private static final long serialVersionUID = 3315263822025280362L;
    private final String      id;

    public JDFileChooser() {
        this(null);
    }

    /**
     * Über die ID kann eine ID für den Auswahldialog ausgewählt werden. Der
     * {@link JDFileChooser} merkt sich für diese ID den zuletzt verwendeten
     * Pfad.
     * 
     * @param id
     */
    public JDFileChooser(String id) {
        super();

        this.id = id;

        setCurrentDirectory(getCurrentWorkingDirectory(id));
    }

    @Override
    public File getSelectedFile() {
        File ret = super.getSelectedFile();

        if (ret == null) return null;
        if (ret.isDirectory()) {
            setCurrentWorkingDirectory(ret, id);
        } else {
            setCurrentWorkingDirectory(ret.getParentFile(), id);
        }
        return ret;
    }

    @Override
    public File[] getSelectedFiles() {
        File[] ret = super.getSelectedFiles();

        if (ret == null || ret.length == 0) return ret;
        if (ret[0].isDirectory()) {
            setCurrentWorkingDirectory(ret[0], id);
        } else {
            setCurrentWorkingDirectory(ret[0].getParentFile(), id);
        }
        return ret;
    }

    private static File getCurrentWorkingDirectory(final String id) {
        final String lastDir = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + (id == null ? "" : id), null);
        if (lastDir != null) return new File(lastDir);

        final String dlDir = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        if (dlDir != null) return new File(dlDir);

        return new File("");
    }

    private static void setCurrentWorkingDirectory(final File f, final String id) {
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CURRENT_BROWSE_PATH + (id == null ? "" : id), f.getAbsolutePath());
        JDUtilities.getConfiguration().save();
    }

}