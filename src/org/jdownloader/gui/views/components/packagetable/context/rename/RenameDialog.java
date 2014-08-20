package org.jdownloader.gui.views.components.packagetable.context.rename;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class RenameDialog extends AbstractDialog<Object> {

    private SelectionInfo selection;
    private ExtTextField  txtSearch;
    private ExtTextField  txtReplace;
    private ExtCheckBox   cbRegex;

    public RenameDialog(final SelectionInfo selection) {
        super(0, _GUI._.RenameDialog_RenameDialog(selection.getChildren().size()), new AbstractIcon(IconKey.ICON_EDIT, 32), _GUI._.lit_continue(), null);
        this.selection = selection;
        setLeftActions(new AppAction() {
            {
                setName(_GUI._.lit_preview());
            }

            @Override
            public void actionPerformed(ActionEvent e1) {

                try {

                    boolean regex = cbRegex.isSelected();
                    CFG_GUI.CFG.setRenameActionRegexEnabled(regex);
                    Pattern pattern = createPattern(txtSearch.getText(), regex);
                    String rep = txtReplace.getText();

                    ArrayList<Result> list = new ArrayList<Result>();
                    for (Object l : selection.getChildren()) {
                        String name = null;
                        if (l instanceof CrawledLink) {
                            name = ((CrawledLink) l).getName();
                            String newName = pattern.matcher(name).replaceAll(rep);
                            list.add(new Result(name, newName, l));
                        } else if (l instanceof DownloadLink) {
                            name = ((DownloadLink) l).getName();
                            String newName = pattern.matcher(name).replaceAll(rep);
                            list.add(new Result(name, newName, l));
                        }

                    }
                    TestWaitDialog d = new TestWaitDialog(regex, pattern, rep, list);
                    UIOManager.I().show(null, d);
                } catch (Throwable e) {
                    Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e.getMessage(), e);
                }
            }

        });
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    public static Pattern createPattern(String regex, boolean useRegex) {
        if (useRegex) {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        } else {
            String[] parts = regex.split("\\*+");
            StringBuilder sb = new StringBuilder();
            if (regex.startsWith("*")) {
                sb.append("(.*)");
            }
            int actualParts = 0;
            for (int i = 0; i < parts.length; i++) {

                if (parts[i].length() != 0) {
                    if (actualParts > 0) {
                        sb.append("(.*)");
                    }
                    sb.append(Pattern.quote(parts[i]));
                    actualParts++;
                }
            }
            if (sb.length() == 0) {
                sb.append("(.*)");
            } else {
                if (regex.endsWith("*")) {
                    sb.append("(.*)");
                }

            }
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            try {
                boolean regex = cbRegex.isSelected();
                CFG_GUI.CFG.setRenameActionRegexEnabled(regex);
                Pattern pattern = createPattern(txtSearch.getText(), regex);
                String rep = txtReplace.getText();
                for (Object l : selection.getChildren()) {
                    String name = null;
                    if (l instanceof CrawledLink) {
                        name = ((CrawledLink) l).getName();
                        String newName = pattern.matcher(name).replaceAll(rep);
                        ((CrawledLink) l).setName(newName);
                    } else if (l instanceof DownloadLink) {
                        name = ((DownloadLink) l).getName();
                        String newName = pattern.matcher(name).replaceAll(rep);
                        DownloadWatchDog.getInstance().renameLink((DownloadLink) l, newName);
                    }

                }
            } catch (Exception e) {
                Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e.getMessage(), e);
            }
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        String allRegex = null;
        String allReplace = null;
        int length = 0;
        for (Object l : selection.getChildren()) {
            String name = null;
            if (l instanceof CrawledLink) {
                name = ((CrawledLink) l).getName();
            } else if (l instanceof DownloadLink) {
                name = ((DownloadLink) l).getName();
            }
            if (name != null) {
                allRegex = merge(name, allRegex);
                length = name.length();
            }
        }
        boolean regex = CFG_GUI.CFG.isRenameActionRegexEnabled();
        if (StringUtils.isEmpty(allRegex)) {
            allRegex = regex ? "(.*)" : "*";
            allReplace = "$1";
        } else {
            if (length == allRegex.length()) {
                allReplace = allRegex;
                allRegex = quote(allRegex, regex);

            } else {
                allReplace = Matcher.quoteReplacement(allRegex) + "$1";
                allRegex = quote(allRegex, regex) + (regex ? "(.*)" : "*");

            }

        }
        txtSearch = new ExtTextField();
        txtSearch.setText(allRegex);
        txtReplace = new ExtTextField();
        cbRegex = new ExtCheckBox();
        txtReplace.setText(allReplace);
        cbRegex.setSelected(regex);
        MigPanel p = new MigPanel("ins 0,wrap 2", "[right][grow,fill]", "[][]");
        p.add(SwingUtils.toBold(new JLabel(_GUI._.RenameDialog_layoutDialogContent_search())));
        p.add(txtSearch);
        p.add(SwingUtils.toBold(new JLabel(_GUI._.RenameDialog_layoutDialogContent_replace())));
        p.add(txtReplace);
        p.add(SwingUtils.toBold(new JLabel(_GUI._.RenameDialog_layoutDialogContent_regex())));
        p.add(cbRegex);

        return p;
    }

    @Override
    protected void initFocus(JComponent focus) {
        this.txtReplace.selectAll();
        this.txtReplace.requestFocusInWindow();
    }

    @Override
    protected int getPreferredWidth() {
        return 500;
    }

    private String quote(String allRegex, boolean regex) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < allRegex.length(); i++) {

            char c = allRegex.charAt(i);
            if (regex) {
                switch (c) {
                case '|':
                case '$':
                case '\\':
                case '\'':

                case '.':

                case '[':

                case '{':

                case '(':

                case '*':

                case '+':

                case '?':

                case '^':

                    sb.append("\\");

                }
            } else {
                switch (c) {

                case '\\':

                    sb.append("\\");

                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String merge(String name, String allRegex) {
        if (allRegex == null) {
            return name;
        }

        return StringUtils.getCommonalities(name, allRegex);
    }

}
