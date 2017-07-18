package org.jdownloader.gui.views.components.packagetable.context.rename;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class RenameDialog extends AbstractDialog<Object> {
    private ExtTextField             txtSearch;
    private ExtTextField             txtReplace;
    private ExtCheckBox              cbRegex;
    private final List<AbstractNode> nodes = new ArrayList<AbstractNode>();

    public RenameDialog(final SelectionInfo<? extends AbstractPackageNode, ? extends AbstractPackageChildrenNode> selection) {
        super(0, "", new AbstractIcon(IconKey.ICON_EDIT, 32), _GUI.T.lit_continue(), null);
        if (selection.isPackageContext()) {
            setTitle(_GUI.T.RenameDialog_RenameDialog_Packages(selection.getPackageViews().size()));
            for (final PackageView<? extends AbstractPackageNode, ? extends AbstractPackageChildrenNode> packageView : selection.getPackageViews()) {
                nodes.add(packageView.getPackage());
            }
        } else {
            setTitle(_GUI.T.RenameDialog_RenameDialog(selection.getChildren().size()));
            nodes.addAll(selection.getChildren());
        }
        setLeftActions(new AppAction() {
            {
                setName(_GUI.T.lit_preview());
            }

            @Override
            public void actionPerformed(ActionEvent e1) {
                try {
                    final boolean regex = cbRegex.isSelected();
                    CFG_GUI.CFG.setRenameActionRegexEnabled(regex);
                    final Pattern pattern = createPattern(txtSearch.getText(), regex);
                    final String rep = txtReplace.getText();
                    final ArrayList<Result> list = new ArrayList<Result>();
                    for (final AbstractNode node : nodes) {
                        if (node instanceof CrawledLink) {
                            final CrawledLink link = ((CrawledLink) node);
                            final String name = link.getName();
                            final String newName = pattern.matcher(name).replaceAll(rep);
                            list.add(new Result(name, newName, node));
                        } else if (node instanceof DownloadLink) {
                            final DownloadLink link = ((DownloadLink) node);
                            final String name = link.getName();
                            final FilePackage fp = link.getFilePackage();
                            String newName = pattern.matcher(name).replaceAll(rep);
                            newName = PackagizerController.replaceDynamicTags(newName, fp.getName(), node);
                            list.add(new Result(name, newName, node));
                        } else if (node instanceof FilePackage) {
                            final FilePackage pkg = (FilePackage) node;
                            final String name = pkg.getName();
                            String newName = pattern.matcher(name).replaceAll(rep);
                            newName = PackagizerController.replaceDynamicTags(newName, name, node);
                            list.add(new Result(name, newName, node));
                        } else if (node instanceof CrawledPackage) {
                            final CrawledPackage pkg = (CrawledPackage) node;
                            final String name = pkg.getName();
                            final String newName = pattern.matcher(name).replaceAll(rep);
                            list.add(new Result(name, newName, node));
                        }
                    }
                    final TestWaitDialog d = new TestWaitDialog(regex, pattern, rep, list);
                    UIOManager.I().show(null, d);
                } catch (Throwable e) {
                    Dialog.getInstance().showExceptionDialog(_GUI.T.lit_error_occured(), e.getMessage(), e);
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
            regex = regex.replaceAll("\\*+", "*").trim();
            final StringBuilder sb = new StringBuilder();
            if (StringUtils.isEmpty(regex)) {
                sb.append("(.*)");
            } else if (regex.equals("*")) {
                sb.append("(.*)");
            } else {
                if (regex.startsWith("*")) {
                    sb.append("(.*)");
                }
                final String[] parts = regex.split("\\*+");
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
            }
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            try {
                final boolean regex = cbRegex.isSelected();
                CFG_GUI.CFG.setRenameActionRegexEnabled(regex);
                final Pattern pattern = createPattern(txtSearch.getText(), regex);
                final String rep = txtReplace.getText();
                for (final AbstractNode node : nodes) {
                    if (node instanceof CrawledLink) {
                        final CrawledLink link = ((CrawledLink) node);
                        final String name = link.getName();
                        final String newName = pattern.matcher(name).replaceAll(rep);
                        link.setName(newName);
                    } else if (node instanceof DownloadLink) {
                        final DownloadLink link = ((DownloadLink) node);
                        final String name = link.getName();
                        final FilePackage fp = link.getFilePackage();
                        String newName = pattern.matcher(name).replaceAll(rep);
                        newName = PackagizerController.replaceDynamicTags(newName, fp.getName(), node);
                        DownloadWatchDog.getInstance().renameLink(link, newName);
                    } else if (node instanceof FilePackage) {
                        final FilePackage pkg = (FilePackage) node;
                        final String name = pkg.getName();
                        final String newName = pattern.matcher(name).replaceAll(rep);
                        pkg.setName(newName);
                    } else if (node instanceof CrawledPackage) {
                        final CrawledPackage pkg = (CrawledPackage) node;
                        final String name = pkg.getName();
                        String newName = pattern.matcher(name).replaceAll(rep);
                        newName = PackagizerController.replaceDynamicTags(newName, name, node);
                        pkg.setName(newName);
                    }
                }
            } catch (Exception e) {
                Dialog.getInstance().showExceptionDialog(_GUI.T.lit_error_occured(), e.getMessage(), e);
            }
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        String allRegex = null;
        String allReplace = null;
        int length = 0;
        for (final AbstractNode node : nodes) {
            if (node instanceof AbstractNode) {
                final String name = node.getName();
                if (name != null) {
                    allRegex = merge(name, allRegex);
                    length = name.length();
                }
            }
        }
        final boolean regex = CFG_GUI.CFG.isRenameActionRegexEnabled();
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
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.RenameDialog_layoutDialogContent_search())));
        p.add(txtSearch);
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.RenameDialog_layoutDialogContent_replace())));
        p.add(txtReplace);
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.RenameDialog_layoutDialogContent_regex())));
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
                case '}':
                case '(':
                case ')':
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
        } else {
            return StringUtils.getCommonalities(name, allRegex);
        }
    }
}
