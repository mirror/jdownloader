package org.jdownloader.extensions.schedulerV2.actions;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.plugins.Account;

public class AccountListRenderer implements ListCellRenderer<Object> {
    final ListCellRenderer<Object> original;

    public AccountListRenderer(ListCellRenderer<Object> original) {
        this.original = original;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (!(value instanceof Account)) {
            return new JLabel("?");
        } else {
            final Account acc = (Account) value;
            final JLabel label = (JLabel) original.getListCellRendererComponent(list, acc.getUser(), index, isSelected, cellHasFocus);
            label.setIcon(acc.getDomainInfo().getIcon(16));
            return label;
        }
    }
}
