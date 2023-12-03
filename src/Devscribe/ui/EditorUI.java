package Devscribe.ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Devscribe.io.ReadFile;
import Devscribe.io.UserConfig;
import Devscribe.io.WriteFile;
import Devscribe.util.StateFile;

import static Devscribe.io.UserConfig.*;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorUI extends JFrame {
    private JTabbedPane jTabbedPane;

    private JMenuItem jMenuItemNewFile, jMenuItemOpenFile, jMenuItemSave, jMenuItemSaveAs, jMenuItemSaveAll,
            jMenuItemCloseTab,
            jMenuItemExit, jMenuItemUndo, jMenuItemRedo, jMenuItemCopy, jMenuItemCut, jMenuItemPaste,
            jMenuItemSelectAll, jMenuItemIncreaseFont,
            jMenuItemDecreaseFont, jMenuItemOriginalSize;
    private JCheckBoxMenuItem jCheckBoxGuidesIndentation, jCheckBoxLineNumbers, jCheckBoxToolBar, jCheckBoxWordWrap;

    private JButton jButtonNewFile, jButtonSaveAll, jButtonRedo, jButtonUndo, jButtonCopy, jButtonPaste, jButtonCut,
            jButtonOpenNewFile;

    private MenuItemEvents menuItemEvents;

    private JToolBar jToolBar;

    private Map<String, StateFile> fileStatusList;
    private boolean internalWindowActivated = true;

    public EditorUI() {
        super("DevScribe");
        // Set the window state to maximized.
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        // Set the window size to the screen size.
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(new ImageIcon(getClass().getResource("icon/Devscribe.png")).getImage());
        addWindowListener(new WindowEvents());

        fileStatusList = new HashMap<>();
        menuItemEvents = new MenuItemEvents();

        // this makes it possible to drag and drop files into the editor
        jTabbedPane = new JTabbedPane();
        new DropTarget(jTabbedPane, new DragAndDropEvent());
        jTabbedPane.setFocusable(false);
        jTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                autoReload();
            }
        });

        add(jTabbedPane, BorderLayout.CENTER);

        // loads the default toolbar initially then adds the user config
        jToolBar = new JToolBar();
        jToolBar.setFloatable(false);
        jToolBar.setFocusable(false);
        jToolBar.setBorder(BorderFactory.createEtchedBorder());
        jToolBar.setVisible(UserConfig.getBoolean(KEY_TOOLBAR));
        configureToolBar();

        add(jToolBar, BorderLayout.NORTH);

        setJMenuBar(configureMenuBar());

        setVisible(true);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                restoreOpenedFiles();
            }
        });
    }

    private void restoreOpenedFiles() {
        List<String> filePathList = UserConfig.getFilePaths();
        File filePath;

        for (String filePathString : filePathList) {
            filePath = new File(filePathString);

            if (filePath.exists()) {
                insertNewEditor(new TextEditor(filePath, this));
            }
        }
    }

    // user configured toolbar with various options
    private void configureToolBar() {
        ToolbarEvents toolbarEvents = new ToolbarEvents();

        jButtonNewFile = new JButton(new ImageIcon(getClass().getResource("icon/new_file.png")));
        jButtonNewFile.setToolTipText("New File (Ctrl+N)");
        jButtonNewFile.setFocusable(false);
        jButtonNewFile.addActionListener(toolbarEvents);

        jButtonOpenNewFile = new JButton(new ImageIcon(getClass().getResource("icon/open.png")));
        jButtonOpenNewFile.setToolTipText("Open (Ctrl+O)");
        jButtonOpenNewFile.setFocusable(false);
        jButtonOpenNewFile.addActionListener(toolbarEvents);

        jButtonSaveAll = new JButton(new ImageIcon(getClass().getResource("icon/save_all.png")));
        jButtonSaveAll.setToolTipText("Save All (Ctrl+Shift+S)");
        jButtonSaveAll.setFocusable(false);
        jButtonSaveAll.addActionListener(toolbarEvents);

        jButtonCut = new JButton(new ImageIcon(getClass().getResource("icon/cut.png")));
        jButtonCut.setToolTipText("Cut (Ctrl+X)");
        jButtonCut.setFocusable(false);
        jButtonCut.addActionListener(toolbarEvents);

        jButtonCopy = new JButton(new ImageIcon(getClass().getResource("icon/copy.png")));
        jButtonCopy.setToolTipText("Copy (Ctrl+C)");
        jButtonCopy.setFocusable(false);
        jButtonCopy.addActionListener(toolbarEvents);

        jButtonPaste = new JButton(new ImageIcon(getClass().getResource("icon/paste.png")));
        jButtonPaste.setToolTipText("Paste (Ctrl+V)");
        jButtonPaste.setFocusable(false);
        jButtonPaste.addActionListener(toolbarEvents);

        jButtonRedo = new JButton(new ImageIcon(getClass().getResource("icon/redo.png")));
        jButtonRedo.setToolTipText("Redo (Ctrl+Y)");
        jButtonRedo.setFocusable(false);
        jButtonRedo.addActionListener(toolbarEvents);

        jButtonUndo = new JButton(new ImageIcon(getClass().getResource("icon/undo.png")));
        jButtonUndo.setToolTipText("Undo (Ctrl+Z)");
        jButtonUndo.setFocusable(false);
        jButtonUndo.addActionListener(toolbarEvents);

        jToolBar.add(jButtonNewFile);
        jToolBar.add(jButtonOpenNewFile);
        jToolBar.add(jButtonSaveAll);
        jToolBar.addSeparator();
        jToolBar.add(jButtonUndo);
        jToolBar.add(jButtonRedo);
        jToolBar.addSeparator();
        jToolBar.add(jButtonCut);
        jToolBar.add(jButtonCopy);
        jToolBar.add(jButtonPaste);
    }

    void updateMenuItem(boolean undo, boolean redo) {
        jMenuItemRedo.setEnabled(redo);
        jMenuItemUndo.setEnabled(undo);

        jButtonRedo.setEnabled(redo);
        jButtonUndo.setEnabled(undo);
    }

    private JMenuBar configureMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();

        // Menus
        JMenu jmenuNewFile = new JMenu("File");
        JMenu jmenuEdit = new JMenu("Edit");
        JMenu jmenuView = new JMenu("View");
        // JMenu jmenuHelp = new JMenu("Help");

        confMenuFile(jmenuNewFile);
        confMenuEdit(jmenuEdit);
        confMenuView(jmenuView);

        jMenuBar.add(jmenuNewFile);
        jMenuBar.add(jmenuEdit);
        jMenuBar.add(jmenuView);
        // jMenuBar.add(jmenuHelp);

        activateMenuItem(false);

        return jMenuBar;
    }

    private void confMenuEdit(JMenu menu) {
        jMenuItemUndo = new JMenuItem("Undo");
        jMenuItemUndo.addActionListener(menuItemEvents);
        jMenuItemUndo.setActionCommand("jMenuItemUndo");
        jMenuItemUndo.setIcon(new ImageIcon(getClass().getResource("icon/undo.png")));
        jMenuItemUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));

        jMenuItemRedo = new JMenuItem("Redo");
        jMenuItemRedo.addActionListener(menuItemEvents);
        jMenuItemRedo.setActionCommand("jMenuItemRedo");
        jMenuItemRedo.setIcon(new ImageIcon(getClass().getResource("icon/redo.png")));
        jMenuItemRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));

        updateMenuItem(false, false);

        jMenuItemCopy = new JMenuItem("Copy");
        jMenuItemCopy.addActionListener(menuItemEvents);
        jMenuItemCopy.setActionCommand("jMenuItemCopy");
        jMenuItemCopy.setIcon(new ImageIcon(getClass().getResource("icon/copy.png")));
        jMenuItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));

        jMenuItemCut = new JMenuItem("Cut");
        jMenuItemCut.addActionListener(menuItemEvents);
        jMenuItemCut.setActionCommand("jMenuItemCut");
        jMenuItemCut.setIcon(new ImageIcon(getClass().getResource("icon/cut.png")));
        jMenuItemCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));

        jMenuItemPaste = new JMenuItem("Paste");
        jMenuItemPaste.addActionListener(menuItemEvents);
        jMenuItemPaste.setActionCommand("jMenuItemPaste");
        jMenuItemPaste.setIcon(new ImageIcon(getClass().getResource("icon/paste.png")));
        jMenuItemPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));

        jMenuItemSelectAll = new JMenuItem("Select All");
        jMenuItemSelectAll.addActionListener(menuItemEvents);
        jMenuItemSelectAll.setActionCommand("jMenuItemSelectAll");
        jMenuItemSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));

        menu.add(jMenuItemRedo);
        menu.add(jMenuItemUndo);
        menu.addSeparator();
        menu.add(jMenuItemCopy);
        menu.add(jMenuItemCut);
        menu.add(jMenuItemPaste);
        menu.add(jMenuItemSelectAll);
    }

    private void confMenuView(JMenu menu) {
        int fontSize = UserConfig.getInt(KEY_FONT_SIZE);

        jMenuItemIncreaseFont = new JMenuItem("Zoom In");
        jMenuItemIncreaseFont.setIcon(new ImageIcon(getClass().getResource("icon/zoomAum.png")));
        jMenuItemIncreaseFont
                .setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                                InputEvent.CTRL_DOWN_MASK));
        jMenuItemIncreaseFont.setActionCommand("jMenuItemIncreaseFont");
        jMenuItemIncreaseFont.setEnabled(fontSize != 30);
        jMenuItemIncreaseFont.addActionListener(menuItemEvents);

        jMenuItemDecreaseFont = new JMenuItem("Zoom Out");
        jMenuItemDecreaseFont.setIcon(new ImageIcon(getClass().getResource("icon/zoomDism.png")));
        jMenuItemDecreaseFont.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        jMenuItemDecreaseFont.setActionCommand("jMenuItemDecreaseFont");
        jMenuItemDecreaseFont.setEnabled(fontSize != 8);
        jMenuItemDecreaseFont.addActionListener(menuItemEvents);

        jMenuItemOriginalSize = new JMenuItem("Reset zoom");
        jMenuItemOriginalSize.setActionCommand("jMenuItemOriginalSize");
        jMenuItemOriginalSize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
        jMenuItemOriginalSize.setEnabled(fontSize != 12);
        jMenuItemOriginalSize.addActionListener(menuItemEvents);

        jCheckBoxGuidesIndentation = new JCheckBoxMenuItem("Indentation Guides",
                UserConfig.getBoolean(KEY_INDENT_GUIDES));
        jCheckBoxGuidesIndentation.setActionCommand("jCheckBoxGuidesIndentation");
        jCheckBoxGuidesIndentation.addActionListener(menuItemEvents);

        jCheckBoxLineNumbers = new JCheckBoxMenuItem("Line Numbers", UserConfig.getBoolean(KEY_LINE_NUMBERS));
        jCheckBoxLineNumbers.setActionCommand("jCheckBoxLineNumbers");
        jCheckBoxLineNumbers.addActionListener(menuItemEvents);

        jCheckBoxToolBar = new JCheckBoxMenuItem("Toggle Toolbar", UserConfig.getBoolean(KEY_TOOLBAR));
        jCheckBoxToolBar.setActionCommand("jCheckBoxToolBar");
        jCheckBoxToolBar.addActionListener(menuItemEvents);

        jCheckBoxWordWrap = new JCheckBoxMenuItem("Toggle Word Wrap", false);
        jCheckBoxWordWrap.setActionCommand("jCheckBoxWordWrap");
        jCheckBoxWordWrap.addActionListener(menuItemEvents);

        JMenu jmenuTheme = new JMenu("Select Theme");
        confMenuTheme(jmenuTheme);

        menu.add(jMenuItemIncreaseFont);
        menu.add(jMenuItemDecreaseFont);
        menu.add(jMenuItemOriginalSize);
        menu.addSeparator();
        menu.add(jmenuTheme);
        menu.addSeparator();
        menu.add(jCheckBoxGuidesIndentation);
        menu.add(jCheckBoxLineNumbers);
        menu.add(jCheckBoxToolBar);
        menu.add(jCheckBoxWordWrap);

    }

    private void confMenuTheme(JMenu menu) {
        String themeName = UserConfig.getString(KEY_THEME);
        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem menuItem;

        menuItem = new JRadioButtonMenuItem("IntelliJ");
        menuItem.setActionCommand("intelli-j");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Dracula");
        menuItem.setActionCommand("darcula");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Dark");
        menuItem.setActionCommand("dark");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Darkii");
        menuItem.setActionCommand("darkii");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("RSTA 1");
        menuItem.setActionCommand("default");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("RSTA 2");
        menuItem.setActionCommand("default-alt");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Eclipse");
        menuItem.setActionCommand("eclipse");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Idle");
        menuItem.setActionCommand("idle");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Monokai");
        menuItem.setActionCommand("monokai");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Visual Studio");
        menuItem.setActionCommand("vs");
        menuItem.setSelected(menuItem.getActionCommand().equals(themeName));
        menuItem.addActionListener(menuItemEvents);
        group.add(menuItem);
        menu.add(menuItem);
    }

    private void confMenuFile(JMenu menu) {
        jMenuItemNewFile = new JMenuItem("New file");
        jMenuItemNewFile.addActionListener(menuItemEvents);
        jMenuItemNewFile.setActionCommand("jMenuItemNewFile");
        jMenuItemNewFile.setIcon(new ImageIcon(getClass().getResource("icon/new_file.png")));
        jMenuItemNewFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));

        jMenuItemOpenFile = new JMenuItem("Open file");
        jMenuItemOpenFile.addActionListener(menuItemEvents);
        jMenuItemOpenFile.setActionCommand("jMenuItemOpenFile");
        jMenuItemOpenFile.setIcon(new ImageIcon(getClass().getResource("icon/open.png")));
        jMenuItemOpenFile
                .setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));

        jMenuItemSave = new JMenuItem("Save");
        jMenuItemSave.addActionListener(menuItemEvents);
        jMenuItemSave.setActionCommand("jMenuItemSave");
        jMenuItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        jMenuItemSaveAs = new JMenuItem("Save as");
        jMenuItemSaveAs.setActionCommand("jMenuItemSaveAs");
        jMenuItemSaveAs.addActionListener(menuItemEvents);

        jMenuItemSaveAll = new JMenuItem("Save all");
        jMenuItemSaveAll.addActionListener(menuItemEvents);
        jMenuItemSaveAll.setActionCommand("jMenuItemSaveAll");
        jMenuItemSaveAll.setIcon(new ImageIcon(getClass().getResource("icon/save_all.png")));
        jMenuItemSaveAll
                .setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

        jMenuItemCloseTab = new JMenuItem("Close tab");
        jMenuItemCloseTab.addActionListener(menuItemEvents);
        jMenuItemCloseTab.setActionCommand("jMenuItemCloseTab");
        jMenuItemCloseTab.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));

        jMenuItemExit = new JMenuItem("Exit");
        jMenuItemExit.setActionCommand("jMenuItemExit");
        jMenuItemExit.addActionListener(menuItemEvents);

        menu.add(jMenuItemNewFile);
        menu.add(jMenuItemOpenFile);
        menu.addSeparator();
        menu.add(jMenuItemSave);
        menu.add(jMenuItemSaveAs);
        menu.add(jMenuItemSaveAll);
        menu.addSeparator();
        menu.add(jMenuItemCloseTab);
        menu.add(jMenuItemExit);
    }

    private JPanel insertTab(TextEditor editor) {
        JPanel jpTab = new JPanel();
        jpTab.setOpaque(false);
        jpTab.setLayout(new BoxLayout(jpTab, BoxLayout.X_AXIS));

        JLabel fileName = new JLabel(editor.getFile().getName());

        final JLabel jlClose = new JLabel(new ImageIcon(getClass().getResource("icon/c1.png")));
        jlClose.setName(editor.getFile().getAbsolutePath());
        jlClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JLabel jlClose = (JLabel) e.getSource();

                    TextEditor TextEditor;
                    for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
                        TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);

                        if (TextEditor.getFile().getAbsolutePath().equals(jlClose.getName())) {
                            tabClose(TextEditor);
                            break;
                        }
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                jlClose.setIcon(new ImageIcon(getClass().getResource("icon/c2.png")));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                jlClose.setIcon(new ImageIcon(getClass().getResource("icon/c1.png")));
            }
        });
        jpTab.add(fileName);
        jpTab.add(Box.createHorizontalStrut(5));
        jpTab.add(jlClose);

        return jpTab;
    }

    private void tabClose(TextEditor editor) {
        if (editor.isFileModified()) {
            switch (showDialogSaveChanges(editor.getFile().getName())) {
                case 0:
                    new WriteFile(this).saveFile(editor.getFile(), editor.getText());
                    break;

                case 2:
                    return;
            }
        }

        jTabbedPane.remove(editor);
        fileStatusList.remove(editor.getFile().getAbsolutePath());
        if (jTabbedPane.getTabCount() == 0) {
            activateMenuItem(false);
            updateMenuItem(internalWindowActivated, rootPaneCheckingEnabled);
        }
    }

    private void tabUpdate(String title, int index) {
        JPanel panel = (JPanel) jTabbedPane.getTabComponentAt(index);
        JLabel jlFileName = (JLabel) panel.getComponent(0);
        jlFileName.setText(title);
    }

    private void insertNewEditor(TextEditor editor) {
        new DropTarget(editor.getComponent(), new DragAndDropEvent());
        jTabbedPane.add(editor);

        int index = jTabbedPane.getTabCount() - 1;
        jTabbedPane.setTabComponentAt(index, insertTab(editor));
        jTabbedPane.setToolTipTextAt(index, editor.getFile().getAbsolutePath());
        jTabbedPane.setSelectedIndex(index);

        if (jTabbedPane.getTabCount() == 1)
            activateMenuItem(true);
    }

    private void activateMenuItem(boolean state) {
        jMenuItemSave.setEnabled(state);
        jMenuItemSaveAs.setEnabled(state);
        jMenuItemSaveAll.setEnabled(state);
        jMenuItemCloseTab.setEnabled(state);

        jMenuItemCut.setEnabled(state);
        jMenuItemCopy.setEnabled(state);
        jMenuItemPaste.setEnabled(state);
        jMenuItemSelectAll.setEnabled(state);

        jButtonSaveAll.setEnabled(state);
        jButtonCut.setEnabled(state);
        jButtonCopy.setEnabled(state);
        jButtonPaste.setEnabled(state);

        if (state) {
            int fontSize = UserConfig.getInt(KEY_FONT_SIZE);

            jMenuItemIncreaseFont.setEnabled(fontSize != 30);
            jMenuItemDecreaseFont.setEnabled(fontSize != 8);
            jMenuItemOriginalSize.setEnabled(fontSize != 16);

        } else {
            jMenuItemDecreaseFont.setEnabled(false);
            jMenuItemIncreaseFont.setEnabled(false);
            jMenuItemOriginalSize.setEnabled(false);
        }
    }

    void saveFile(TextEditor editor) {
        new WriteFile(this).saveFile(editor.getFile(), editor.getText());
    }

    private void closeProgram() {
        List<String> listArchivesRoute = new ArrayList<>(jTabbedPane.getTabCount());
        List<TextEditor> list = new ArrayList<>();
        TextEditor TextEditor;

        for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
            TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);
            listArchivesRoute.add(TextEditor.getFile().getAbsolutePath());

            if (TextEditor.isFileModified())
                list.add(TextEditor);
        }

        switch (list.size()) {
            case 1:
                TextEditor editor = list.get(0);
                switch (showDialogSaveChanges(editor.getFile().getName())) {
                    // 0 means user agreed to save the file
                    case 0:
                        new WriteFile(this).saveFile(editor.getFile(), editor.getText());
                        break;

                    // 2 means user cancelled the operation
                    case 2:
                        return;
                }

            case 0:
                UserConfig.putFilePaths(listArchivesRoute);
                System.exit(0);

            default:
                internalWindowActivated = true;
                int resp = SaveMultipleDialog.showDialog(EditorUI.this, list);
                if (resp == SaveMultipleDialog.OPERATION_COMPLETED) {
                    UserConfig.putFilePaths(listArchivesRoute);
                    System.exit(0);
                }
        }
    }

    private int showDialogSaveChanges(String file_name) {
        internalWindowActivated = true;
        String message = "The file " + file_name + " has been modified. Save changes?";
        String[] bt = { "Save", "Don't Save", "Cancel" };

        return DialogBox.show(this, message, "Save Changes", bt, DialogBox.INFORMATION);
    }

    private int showDialogReloadFile(String file_name) {
        internalWindowActivated = true;
        String message = "The file " + file_name + " has been modified. Do you want to reload it?";
        String[] bt = { "Yes", "No" };

        return DialogBox.show(this, message, "Reload file", bt, DialogBox.INFORMATION);
    }

    private void newFile() {
        internalWindowActivated = true;
        WriteFile writer = new WriteFile(this);
        if (writer.saveFileValidateExtension(null, ""))
            insertNewEditor(new TextEditor(writer.getFile(), 0, this));
    }

    private void openFile() {
        internalWindowActivated = true;
        ReadFile read = new ReadFile(this);
        if (read.fileRead()) {
            TextEditor TextEditor;
            int i, tabCount = jTabbedPane.getTabCount();

            for (i = 0; i < tabCount; i++) {
                TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);
                if (TextEditor.getFile().getAbsolutePath().equals(read.getFile().getAbsolutePath())) {
                    if (TextEditor.isFileModified())
                        TextEditor.setText(read.getText());

                    jTabbedPane.setSelectedIndex(i);
                    break;
                }
            }
            if (tabCount == i)
                insertNewEditor(new TextEditor(read.getFile(), this));
        }
    }

    private void saveAll() {
        TextEditor TextEditor;
        for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
            TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);

            if (TextEditor.isFileModified())
                new WriteFile(EditorUI.this).saveFile(TextEditor.getFile(), TextEditor.getText());
        }
    }

    private void activeEditorTheme(String themeName) {
        TextEditor TextEditor;
        for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
            TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);

            TextEditor.updateTheme(themeName);
        }
    }

    private void autoReload() {
        if (jTabbedPane.getTabCount() != 0) {
            TextEditor editor = (TextEditor) jTabbedPane.getSelectedComponent();

            if (fileStatusList.containsKey(editor.getFile().getAbsolutePath())) {
                String textFile = ReadFile.read(editor.getFile());

                StateFile textFileOnExit = fileStatusList.remove(editor.getFile().getAbsolutePath());
                String textoEditor = editor.getText();

                if (textFileOnExit.isUpdated()) {
                    if (!textoEditor.equals(textFile))
                        editor.setText(textFile);

                } else {
                    if (!textFile.equals(textFileOnExit.getContenido()) && !textoEditor.equals(textFile)) {
                        if (showDialogReloadFile(editor.getFile().getName()) == 0)
                            editor.setText(textFile);
                    }
                }
            }
        }
    }

    private void existsFile() {
        int tabCount = jTabbedPane.getTabCount();

        if (tabCount != 0) {
            TextEditor editor;

            for (int i = tabCount - 1; i >= 0; i--) {
                editor = (TextEditor) jTabbedPane.getComponentAt(i);

                if (!editor.getFile().exists()) {
                    jTabbedPane.remove(i);
                    fileStatusList.remove(editor.getFile().getAbsolutePath());
                }
            }
        }
    }

    // setting the same font to all tabs in the editor
    private void actSourceFromAllEditors() {
        TextEditor TextEditor;
        for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
            TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);

            TextEditor.updateFont();
        }
    }

    private class MenuItemEvents implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getActionCommand()) {
                case "jMenuItemNewFile":
                    newFile();
                    break;

                case "jMenuItemOpenFile":
                    openFile();
                    break;

                default:
                    TextEditor editor = (TextEditor) jTabbedPane.getSelectedComponent();

                    switch (e.getActionCommand()) {
                        case "jMenuItemOpenFile":
                            saveFile(editor);
                            break;

                        case "jMenuItemSaveAs":
                            internalWindowActivated = true;
                            WriteFile escribe = new WriteFile(EditorUI.this);
                            if (escribe.saveFile(editor.getText())) {
                                tabUpdate(escribe.getFile().getName(), jTabbedPane.getSelectedIndex());
                                editor.setFile(escribe.getFile());

                                // Se actualiza el identificador del archivo.
                                JPanel panel = (JPanel) jTabbedPane.getTabComponentAt(jTabbedPane.getSelectedIndex());
                                panel.getComponent(2).setName(escribe.getFile().getAbsolutePath());
                            }
                            break;

                        case "jMenuItemSaveAll":
                            saveAll();
                            break;

                        case "jMenuItemCloseTab":
                            tabClose(editor);
                            break;

                        case "jMenuItemExit":
                            closeProgram();
                            break;

                        case "jMenuItemCopy":
                            editor.copy();
                            break;

                        case "jMenuItemCut":
                            editor.cut();
                            break;

                        case "jMenuItemPaste":
                            editor.paste();
                            break;

                        case "jMenuItemSelectAll":
                            editor.selectAll();
                            break;

                        case "jMenuItemRedo":
                            editor.redo();
                            break;

                        case "jMenuItemUndo":
                            editor.undo();
                            break;

                        case "darcula":
                        case "dark":
                        case "darkii":
                        case "default":
                        case "default-alt":
                        case "eclipse":
                        case "idle":
                        case "intelli-j":
                        case "monokai":
                        case "vs":
                            UserConfig.putString(KEY_THEME, e.getActionCommand());
                            activeEditorTheme(e.getActionCommand());
                            break;

                        case "jMenuItemIncreaseFont":
                            if (TextEditor.fontSize < 30) {
                                TextEditor.fontSize++;

                                actSourceFromAllEditors();

                                if (TextEditor.fontSize == 30)
                                    jMenuItemIncreaseFont.setEnabled(false);

                                UserConfig.putInt(KEY_FONT_SIZE, TextEditor.fontSize);
                            }

                            if (!jMenuItemDecreaseFont.isEnabled())
                                jMenuItemDecreaseFont.setEnabled(true);

                            if (TextEditor.fontSize != 12) {
                                if (!jMenuItemOriginalSize.isEnabled())
                                    jMenuItemOriginalSize.setEnabled(true);

                            } else {
                                jMenuItemOriginalSize.setEnabled(false);
                            }
                            break;

                        case "jMenuItemDecreaseFont":
                            if (TextEditor.fontSize > 8) {
                                TextEditor.fontSize--;

                                actSourceFromAllEditors();

                                if (TextEditor.fontSize == 8)
                                    jMenuItemDecreaseFont.setEnabled(false);

                                UserConfig.putInt(KEY_FONT_SIZE, TextEditor.fontSize);
                            }

                            if (!jMenuItemIncreaseFont.isEnabled())
                                jMenuItemIncreaseFont.setEnabled(true);

                            if (TextEditor.fontSize != 12) {
                                if (!jMenuItemOriginalSize.isEnabled())
                                    jMenuItemOriginalSize.setEnabled(true);

                            } else {
                                jMenuItemOriginalSize.setEnabled(false);
                            }
                            break;

                        case "jMenuItemOriginalSize":
                            TextEditor.fontSize = 12;

                            actSourceFromAllEditors();

                            jMenuItemOriginalSize.setEnabled(false);
                            if (!jMenuItemIncreaseFont.isEnabled())
                                jMenuItemIncreaseFont.setEnabled(true);
                            if (!jMenuItemDecreaseFont.isEnabled())
                                jMenuItemDecreaseFont.setEnabled(true);

                            // Se guarda nuevo tamaño de fuente.
                            UserConfig.putInt(KEY_FONT_SIZE, TextEditor.fontSize);
                            break;

                        case "jCheckBoxLineNumbers":
                            boolean state = jCheckBoxLineNumbers.isSelected();

                            UserConfig.putBoolean(KEY_LINE_NUMBERS, state);

                            TextEditor TextEditor;
                            for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
                                TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);

                                TextEditor.enableLineNumbers(state);
                            }
                            break;

                        case "jCheckBoxToolBar":
                            boolean est = jCheckBoxToolBar.isSelected();
                            jToolBar.setVisible(est);
                            UserConfig.putBoolean(KEY_TOOLBAR, est);
                            break;

                        case "jCheckBoxGuidesIndentation":
                            boolean value = jCheckBoxGuidesIndentation.isSelected();

                            UserConfig.putBoolean(KEY_INDENT_GUIDES, value);

                            for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
                                editor = (TextEditor) jTabbedPane.getComponentAt(i);
                                editor.enableIndentationGuides(value);
                            }
                            break;

                        case "jCheckBoxWordWrap":
                            boolean status = jCheckBoxWordWrap.isSelected();
                            UserConfig.putBoolean(KEY_WORD_WRAP, status);
                            editor.setWordWrap(status);
                            break;
                    }
            }
        }
    }

    private class ToolbarEvents implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == jButtonNewFile) {
                newFile();

            } else if (e.getSource() == jButtonOpenNewFile) {
                openFile();

            } else {
                TextEditor editor = (TextEditor) jTabbedPane.getSelectedComponent();

                if (e.getSource() == jButtonSaveAll)
                    saveAll();

                else if (e.getSource() == jButtonUndo)
                    editor.undo();

                else if (e.getSource() == jButtonRedo)
                    editor.redo();

                else if (e.getSource() == jButtonCut)
                    editor.cut();

                else if (e.getSource() == jButtonCopy)
                    editor.copy();

                else if (e.getSource() == jButtonPaste)
                    editor.paste();
            }
        }
    }

    private class WindowEvents extends WindowAdapter {
        @Override
        public void windowActivated(WindowEvent e) {
            if (!internalWindowActivated) {
                existsFile();
                autoReload();

            } else
                internalWindowActivated = false;
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            if (!internalWindowActivated) {
                int tabCount = jTabbedPane.getTabCount();

                if (tabCount != 0) {
                    TextEditor TextEditor;
                    StateFile estadoArch;
                    String contenido;

                    for (int i = 0; i < tabCount; i++) {
                        TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);

                        estadoArch = fileStatusList.get(TextEditor.getFile().getAbsolutePath());
                        if (estadoArch != null && estadoArch.isUpdated())
                            continue;

                        if ((contenido = TextEditor.getFileModifiedContent()) != null)
                            estadoArch = new StateFile(TextEditor.getFile().getAbsolutePath(), contenido);

                        else
                            estadoArch = new StateFile(TextEditor.getFile().getAbsolutePath());

                        fileStatusList.put(TextEditor.getFile().getAbsolutePath(), estadoArch);
                    }
                }
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            closeProgram();
        }
    }

    private class DragAndDropEvent extends DropTargetAdapter {

        @Override
        public void drop(DropTargetDropEvent e) {
            try {
                e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                List list = (List) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                for (int k = 0; k < list.size(); k++) {
                    File file = (File) list.get(k);

                    if (file.isFile()) {
                        TextEditor TextEditor;
                        int tabCount = jTabbedPane.getTabCount();
                        int i;

                        for (i = 0; i < tabCount; i++) {
                            TextEditor = (TextEditor) jTabbedPane.getComponentAt(i);
                            // Compruebo si el arhivo que se desea abrir ya esta abierto.
                            if (TextEditor.getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                                if (TextEditor.isFileModified())
                                    TextEditor.setText(ReadFile.read(file)); // Recargar el archivo.

                                if (k == list.size() - 1)
                                    jTabbedPane.setSelectedIndex(i);
                                break;
                            }
                        }

                        if (tabCount == i) // Compruebo si el archivo no ha sido abierto.
                            insertNewEditor(new TextEditor(file, EditorUI.this));
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}