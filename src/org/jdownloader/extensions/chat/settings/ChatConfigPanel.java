package org.jdownloader.extensions.chat.settings;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.TextArea;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.chat.ChatConfig;
import org.jdownloader.extensions.chat.ChatExtension;
import org.jdownloader.extensions.chat.translate.T;

public class ChatConfigPanel extends ExtensionConfigPanel<ChatExtension> {

    private static final long serialVersionUID = 1L;
    private ChatConfig        config;
    private TextInput         nick;
    private Checkbox          userColors;
    private ComboBox<String>  userPosition;
    private TextArea          perform;

    public ChatConfigPanel(ChatExtension chatExtension, ChatConfig config) {
        super(chatExtension);
        this.config = config;
        initComponents();
        layoutPanel();
    }

    private void initComponents() {
        nick = new TextInput();
        userColors = new Checkbox();
        userPosition = new ComboBox<String>(T._.settings_userlist_position_right(), T._.settings_userlist_position_left());
        perform = new TextArea();
    }

    protected void layoutPanel() {

        addPair(T._.settings_nick(), nick);
        addPair(T._.settings_enabled_userlist_colors(), userColors);
        addPair(T._.settings_userlist_position(), userPosition);
        addPair(T._.settings_perform(), perform);

    }

    @Override
    public void save() {
        boolean changes = false;
        if (!nick.getText().equals(config.getNick())) {
            config.setNick(nick.getText());
            changes = true;
        }
        if (userColors.isSelected() != config.isUserColorEnabled()) {
            config.setUserColorEnabled(userColors.isSelected());
            changes = true;
        }
        if (userPosition.getSelectedIndex() != config.getUserListPosition()) {
            config.setUserListPosition(userPosition.getSelectedIndex());
            changes = true;
        }

        config.setPerformOnLoginCommands(perform.getText());
        if (changes) showRestartRequiredMessage();
    }

    @Override
    public void updateContents() {

        nick.setText(config.getNick());
        userColors.setSelected(config.isUserColorEnabled());
        userPosition.setSelectedIndex(config.getUserListPosition());
        perform.setText(config.getPerformOnLoginCommands());

    }

}
