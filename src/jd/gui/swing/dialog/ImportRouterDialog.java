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

package jd.gui.swing.dialog;

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

import jd.controlling.reconnect.HTTPLiveHeader;
import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.gui.swing.dialog.AbstractDialog;
import jd.nutils.encoding.Encoding;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class ImportRouterDialog extends AbstractDialog {

    public static String[] showDialog() {
        ImportRouterDialog dialog = new ImportRouterDialog();
        if (UserIO.isOK(dialog.getReturnValue())) return dialog.getResult();
        return null;
    }

    private static final long serialVersionUID = 2043825047691368115L;

    private ArrayList<String[]> scripts;

    private JList list;

    private ImportRouterDialog() {
        super(UserIO.NO_COUNTDOWN, JDL.L("gui.config.liveheader.dialog.importrouter", "Import Router"), JDTheme.II("gui.images.search", 32, 32), null, null);

        scripts = HTTPLiveHeader.getLHScripts();
        Collections.sort(scripts, new Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return (a[0] + " " + a[1]).compareToIgnoreCase(b[0] + " " + b[1]);
            }
        });

        init();
    }

    public String[] getResult() {
        String selected = list.getSelectedValue().toString();
        int id = Integer.parseInt(selected.split("\\.")[0]);
        return scripts.get(id);
    }

    @Override
    public JComponent contentInit() {
        HashMap<String, Boolean> ch = new HashMap<String, Boolean>();
        for (int i = scripts.size() - 1; i >= 0; i--) {
            if (ch.containsKey(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2])) {
                scripts.remove(i);
            } else {
                ch.put(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2], true);
            }
        }

        final String[] d = new String[scripts.size()];
        for (int i = 0; i < d.length; i++) {
            d[i] = i + ". " + Encoding.htmlDecode(scripts.get(i)[0] + " : " + scripts.get(i)[1]);
        }

        JPanel panel = new JPanel(new MigLayout("ins 10,wrap 3", "[grow 30,fill]5[grow 0,fill]10[grow,fill,300!]", "[fill]5[]5[fill,grow]"));
        final DefaultListModel defaultListModel = new DefaultListModel();
        final String text = JDL.L("gui.config.reconnect.selectrouter", "Search Router Model");
        final JTextField searchField = new JTextField();

        list = new JList(defaultListModel);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
            }

            public void insertUpdate(DocumentEvent e) {
                refreshList();
            }

            private void refreshList() {
                String search = searchField.getText().toLowerCase();
                String[] hits = search.split(" ");
                defaultListModel.removeAllElements();
                for (int i = 0; i < d.length; i++) {
                    for (int j = 0; j < hits.length; j++) {
                        if (!d[i].toLowerCase().contains(hits[j])) break;
                        if (j == hits.length - 1) {
                            defaultListModel.addElement(d[i]);
                        }
                    }
                }
                list.setModel(defaultListModel);
            }

            public void removeUpdate(DocumentEvent e) {
                refreshList();
            }
        });
        searchField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(text)) {
                    searchField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().equals("")) {
                    searchField.setText(text);
                    for (String element : d) {
                        defaultListModel.addElement(element);
                    }
                }
            }
        });
        final JTextArea preview = new JTextArea();
        preview.setFocusable(true);

        JButton reset = Factory.createButton(null, JDTheme.II("gui.images.undo", 16, 16), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchField.setForeground(Color.lightGray);
                searchField.setText(text);
                preview.setText("");
                for (String element : d) {
                    defaultListModel.addElement(element);
                }
            }
        });
        reset.setBorder(null);
        searchField.setText(text);

        list.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                String selected = (String) list.getSelectedValue();
                if (selected != null) {
                    int id = Integer.parseInt(selected.split("\\.")[0]);
                    String[] data = scripts.get(id);

                    preview.setText(data[2]);
                }
            }

        });
        JLabel example = new JLabel(JDL.L("gui.config.reconnect.selectrouter.example", "Example: 3Com ADSL"));

        for (String element : d) {
            defaultListModel.addElement(element);
        }

        panel.add(searchField);
        panel.add(reset);
        panel.add(new JScrollPane(preview), "spany");

        panel.add(example, "spanx 2");
        panel.add(new JScrollPane(list), "spanx 2");

        return panel;
    }

    @Override
    protected void packed() {
        setMinimumSize(new Dimension(700, 500));
    }

}
