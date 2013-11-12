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

package jd.gui.swing.jdgui.components.premiumbar;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;

public class TinyProgressBar extends MigPanel {

    private static final long    serialVersionUID = 8385631080915257786L;

    private ExtButton            bt;

    private ServicePanel         owner;

    private ServiceCollection<?> serviceCollection;

    public TinyProgressBar(ServicePanel owner, final ServiceCollection<?> serviceCollection) {
        super("ins 0", "[]", "[]");
        this.owner = owner;
        this.serviceCollection = serviceCollection;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt = new ExtButton() {

            @Override
            public boolean isTooltipWithoutFocusEnabled() {
                return true;
            }

            @Override
            public boolean isTooltipDisabledUntilNextRefocus() {

                return false;
            }

            @Override
            public ExtTooltip createExtTooltip(Point mousePosition) {
                return serviceCollection.createTooltip(TinyProgressBar.this.owner);

            }

        };
        bt.setTooltipsEnabled(true);
        bt.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                ToolTipController.getInstance().show(bt);
            }

        });
        bt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        bt.setBorderPainted(false);
        add(bt, "width 20!,height 20!");

        bt.setIcon(serviceCollection.getIcon());
        this.setVisible(true);
        setEnabled(serviceCollection.isEnabled());
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        bt.setEnabled(enabled);
    }

}
