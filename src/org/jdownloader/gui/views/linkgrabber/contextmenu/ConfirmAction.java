package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmAction extends AppAction {

    private ArrayList<AbstractNode> selection;

    public ConfirmAction(ArrayList<AbstractNode> selection) {
        this.selection = selection;
        if (LinkFilterSettings.LINKGRABBER_AUTO_START_ENABLED.getValue()) {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_start());
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("add", 12);

            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, 0, 0, 9, 10)));
        } else {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add());

            setSmallIcon(NewTheme.I().getIcon("add", 20));
        }

    }

    public void actionPerformed(ActionEvent e) {
    }

}
