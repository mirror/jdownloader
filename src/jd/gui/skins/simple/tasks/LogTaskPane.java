//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class LogTaskPane extends TaskPanel implements ActionListener {

    private static final long serialVersionUID = 1828963496367613790L;
    public static final int ACTION_SAVE = 1;
    public static final int ACTION_UPLOAD = 2;


    private JButton save;
    private JButton upload;
  

    public LogTaskPane(String string, ImageIcon ii) {
        super(string, ii, "logtask");

        initGui();
    }

    private void initGui() {
        save = this.createButton(JDLocale.L("gui.taskpanels.log.save", "Save as"), JDTheme.II("gui.images.save", 16, 16));
        upload = this.createButton(JDLocale.L("gui.taskpanels.log.upload", "Upload log"), JDTheme.II("gui.images.upload", 16, 16));
        add(save, D1_BUTTON_ICON);
        add(upload, D1_BUTTON_ICON);
    
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == save) {
            this.broadcastEvent(new ActionEvent(this, ACTION_SAVE, e.getActionCommand()));       
        } else if (e.getSource() == upload) {
            this.broadcastEvent(new ActionEvent(this, ACTION_UPLOAD, e.getActionCommand()));
        }

    }

}
