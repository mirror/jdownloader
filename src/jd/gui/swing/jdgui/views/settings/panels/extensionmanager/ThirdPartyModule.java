package jd.gui.swing.jdgui.views.settings.panels.extensionmanager;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules.Module;

import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.extensions.Header;
import org.jdownloader.images.NewTheme;

public abstract class ThirdPartyModule extends Module {
    private Header    header;
    private JLabel    icon;

    private JTextArea txt;

    public ThirdPartyModule() {

        super("ins 0,wrap 1", "[grow,fill]", "[]");

        header = new Header(getTitle(), null);

        icon = new JLabel(NewTheme.I().getIcon(getIconKey(), 32));
        txt = new JTextArea();
        SwingUtils.setOpaque(txt, false);
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setFocusable(false);
        // txt.setEnabled(false);

        txt.setText(getDescription());
        add(header, "gapbottom 5");

        add(icon, "split 2,width 32!,gapleft 22,gapright 10");
        // txt.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
        add(txt, "gaptop 0,spanx,growx,pushx,gapbottom 15,wmin 10, aligny top");

    }

    protected abstract String getTitle();

    protected abstract String getIconKey();

    protected abstract String getDescription();

}
