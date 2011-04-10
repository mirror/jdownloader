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

package jd.controlling.reconnect.plugins.liveheader;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.controlling.reconnect.plugins.liveheader.translate.T;
import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.nutils.encoding.Encoding;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;

public class ImportRouterDialog extends AbstractDialog<Integer> {

    private static final long         serialVersionUID = 2043825047691368115L;

    private final ArrayList<String[]> scripts;
    private JList                     list;

    public ImportRouterDialog(final ArrayList<String[]> scripts) {
        super(UserIO.NO_COUNTDOWN, T._.gui_config_liveheader_dialog_importrouter(), JDTheme.II("gui.images.search", 32, 32), null, null);

        this.scripts = scripts;
        Collections.sort(this.scripts, new Comparator<String[]>() {
            public int compare(final String[] a, final String[] b) {
                return (a[0] + " " + a[1]).compareToIgnoreCase(b[0] + " " + b[1]);
            }
        });

    }

    @Override
    protected Integer createReturnValue() {
        return this.getReturnmask();
    }

    public String[] getResult() {
        final String selected = this.list.getSelectedValue().toString();
        final int id = Integer.parseInt(selected.split("\\.")[0]);
        return this.scripts.get(id);
    }

    @Override
    public JComponent layoutDialogContent() {
        final HashMap<String, Boolean> ch = new HashMap<String, Boolean>();
        for (int i = this.scripts.size() - 1; i >= 0; i--) {
            if (ch.containsKey(this.scripts.get(i)[0] + this.scripts.get(i)[1] + this.scripts.get(i)[2])) {
                this.scripts.remove(i);
            } else {
                ch.put(this.scripts.get(i)[0] + this.scripts.get(i)[1] + this.scripts.get(i)[2], true);
            }
        }

        final String[] d = new String[this.scripts.size()];
        for (int i = 0; i < d.length; i++) {
            d[i] = i + ". " + Encoding.htmlDecode(this.scripts.get(i)[0] + " : " + this.scripts.get(i)[1]);
        }

        final JPanel panel = new JPanel(new MigLayout("ins 10,wrap 3", "[grow 30,fill]5[grow 0,fill]10[grow,fill,300!]", "[fill]5[]5[fill,grow]"));
        final DefaultListModel defaultListModel = new DefaultListModel();
        final String text = T._.gui_config_reconnect_selectrouter();
        final JTextField searchField = new JTextField();

        this.list = new JList(defaultListModel);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) {
            }

            public void insertUpdate(final DocumentEvent e) {
                this.refreshList();
            }

            private void refreshList() {
                final String search = searchField.getText().toLowerCase();
                final String[] hits = search.split(" ");
                defaultListModel.removeAllElements();
                for (int i = 0; i < d.length; i++) {
                    for (int j = 0; j < hits.length; j++) {
                        if (!d[i].toLowerCase().contains(hits[j])) {
                            break;
                        }
                        if (j == hits.length - 1) {
                            defaultListModel.addElement(d[i]);
                        }
                    }
                }
                ImportRouterDialog.this.list.setModel(defaultListModel);
            }

            public void removeUpdate(final DocumentEvent e) {
                this.refreshList();
            }
        });
        searchField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(final FocusEvent e) {
                if (searchField.getText().equals(text)) {
                    searchField.setText("");
                }
            }

            @Override
            public void focusLost(final FocusEvent e) {
                if (searchField.getText().equals("")) {
                    searchField.setText(text);
                    for (final String element : d) {
                        defaultListModel.addElement(element);
                    }
                }
            }
        });
        final JTextArea preview = new JTextArea();
        preview.setFocusable(true);

        final JButton reset = Factory.createButton(null, JDTheme.II("gui.images.undo", 16, 16), new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                searchField.setForeground(Color.lightGray);
                searchField.setText(text);
                preview.setText("");
                for (final String element : d) {
                    defaultListModel.addElement(element);
                }
            }
        });
        reset.setBorder(null);
        searchField.setText(text);

        this.list.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                final String selected = (String) ImportRouterDialog.this.list.getSelectedValue();
                if (selected != null) {
                    final int id = Integer.parseInt(selected.split("\\.")[0]);
                    final String[] data = ImportRouterDialog.this.scripts.get(id);

                    preview.setText(data[2]);
                }
            }

        });
        final JLabel example = new JLabel(T._.gui_config_reconnect_selectrouter_example());

        for (final String element : d) {
            defaultListModel.addElement(element);
        }

        panel.add(searchField);
        panel.add(reset);
        panel.add(new JScrollPane(preview), "spany");

        panel.add(example, "spanx 2");
        panel.add(new JScrollPane(this.list), "spanx 2");

        return panel;
    }

    @Override
    protected void packed() {
        this.setMinimumSize(new Dimension(700, 500));
    }

}