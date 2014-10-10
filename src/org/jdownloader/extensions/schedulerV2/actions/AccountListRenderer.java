package org.jdownloader.extensions.schedulerV2.actions;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;

import jd.plugins.Account;

import org.jdownloader.DomainInfo;

import de.javasoft.plaf.synthetica.classy.ComboListCellRenderer;

class AccountListRenderer extends ComboListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (!(value instanceof Account)) {
            return new JLabel("?");
        }
        Account acc = (Account) value;
        JLabel label = (JLabel) super.getListCellRendererComponent(list, acc.getUser(), index, isSelected, cellHasFocus);
        label.setIcon(DomainInfo.getInstance(acc.getHoster()).getIcon(16));
        return label;
    }
}
