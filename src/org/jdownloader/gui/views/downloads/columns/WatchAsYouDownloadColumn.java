/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.gui.views.downloads.columns;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

/**
 *
 * @author Shashank Tulsyan
 */
public class WatchAsYouDownloadColumn extends ExtColumn<AbstractNode> {

    public WatchAsYouDownloadColumn() {
        super(_GUI._.WatchAsYouDownloadColumn_WatchAsYouDownloadColumn(), null);
    }

    @Override
    public void configureEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
        //
    }

    @Override
    public void configureRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getCellEditorValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JComponent getEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JComponent getRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resetEditor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resetRenderer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setValue(Object value, AbstractNode object) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private static final class Renderer {
        private JCheckBox checkBox;
        public Renderer() {
            checkBox = new JCheckBox(NewTheme.I().getIcon("mediaplayer", 24), true);
        }
        
    }
}
