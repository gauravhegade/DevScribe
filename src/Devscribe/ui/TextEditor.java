package Devscribe.ui;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.folding.CurlyFoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rsyntaxtextarea.templates.CodeTemplate;
import org.fife.ui.rsyntaxtextarea.templates.StaticCodeTemplate;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import Devscribe.io.ReadFile;
import Devscribe.io.UserConfig;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;

import static Devscribe.io.UserConfig.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;

/**
 * This code defines a `TextEditor` class that extends `JPanel` and is used to
 * create a text editor component in a Java application.
 * 
 * The `TextEditor` class represents a text editor panel with features like
 * syntax highlighting, code folding, and more.
 * 
 * It has two constructors, one accepting a `File` and an `EditorUI` parent
 * and the other additionally accepting an initial cursor position.
 * 
 * The class uses `RSyntaxTextArea` for text editing, which is configured with
 * various settings, including syntax highlighting and code templates.
 * 
 * It tracks cursor position and updates a cursor position panel accordingly.
 * 
 * The text editor provides common text editing operations like copy, cut,
 * paste, undo, redo, and select all.
 * 
 * It manages the text content and file association, allowing users to save
 * and reload files.
 * 
 * The class also handles file-related operations such as checking if a file
 * has been modified.
 * 
 * It supports theming with different color schemes and allows customization
 * of the font.
 * 
 * Additionally, it provides methods to enable or disable line numbers and
 * indentation guides.
 * 
 * The class defines code templates for code generation within the editor.
 * 
 * It is a versatile component for creating a text editor with various
 * features in a Java application.
 */

public class TextEditor extends JPanel {
    private File file;

    private RSyntaxTextArea textArea;
    private RTextScrollPane scroll;

    private JPanel editorPanel;
    private JPanel cursorPositionPanel;

    private EditorUI editorUI;

    static int fontSize = 16;

    // Constructor for TextEditor.
    TextEditor(File file, EditorUI editorUI) {
        this(file, 0, editorUI);
    }

    // Overloaded constructor for TextEditor.
    TextEditor(File file, int cursorPosition, final EditorUI editorUI) {
        setLayout(new GridBagLayout());

        this.editorUI = editorUI;
        this.file = file;

        // Create and configure the RSyntaxTextArea.
        textArea = new RSyntaxTextArea();
        textArea.setTabSize(4);
        textArea.setFocusable(true);
        textArea.setMarkOccurrences(true);
        textArea.setCodeFoldingEnabled(true);
        textArea.setClearWhitespaceLinesEnabled(false);

        // Add a fold parser for curly braces.
        FoldParserManager.get().addFoldParserMapping("text/sl", new CurlyFoldParser());

        // Configure token maker for syntax highlighting.
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/sl", "Devscribe.ui.SLTokenMaker");
        textArea.setSyntaxEditingStyle("text/sl");
        textArea.setPaintTabLines(UserConfig.getBoolean(KEY_INDENT_GUIDES));

        // Configure input map for code templates.
        InputMap map = textArea.getInputMap();
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK);
        String actionName = RSyntaxTextAreaEditorKit.rstaPossiblyInsertTemplateAction;
        map.put(ks, actionName);

        // Set text content and caret position.
        textArea.setText(ReadFile.read(file));
        textArea.setCaretPosition(cursorPosition);
        textArea.requestFocus();

        // Add caret listener to track cursor position.
        textArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                updateCursorPosition(textArea.getCaretPosition());
                TextEditor.this.editorUI.updateMenuItem(textArea.canUndo(), textArea.canRedo());
            }
        });

        // Add undoable edit listener to track changes for undo/redo.
        textArea.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                TextEditor.this.editorUI.updateMenuItem(textArea.canUndo(), textArea.canRedo());
            }
        });

        configureEditorPanel();
        updateTheme(UserConfig.getString(KEY_THEME));
        configureCursorPositionPanel();
        updateCursorPosition(textArea.getCaretPosition());

        RSyntaxTextArea.setTemplatesEnabled(true);
        CodeTemplateManager ctm = RSyntaxTextArea.getCodeTemplateManager();

        // Add code templates.
        for (CodeTemplate ct : getCodeTemplates()) {
            ctm.addTemplate(ct);
        }

        GridBagConstraints constraints = new GridBagConstraints();

        // Configuration of the component in row 0, column 0.
        constraints.gridx = constraints.gridy = 0;
        constraints.weightx = constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;

        add(editorPanel, constraints);

        // Configuration of the component in row 1, column 0.
        constraints.gridy = 1;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        add(cursorPositionPanel, constraints);
    }

    private void updateCursorPosition(int caretPosition) {
        // Extract the label contained in the 'cursorPositionPanel'.
        JLabel cursorPosLabel = (JLabel) cursorPositionPanel.getComponent(1);

        try {
            int line = textArea.getLineOfOffset(caretPosition);
            int column = caretPosition - textArea.getLineStartOffset(line);

            line++;
            column++;

            cursorPosLabel.setText(line + " : " + column);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private void configureEditorPanel() {
        scroll = new RTextScrollPane(textArea);
        scroll.getGutter().setBorder(new Gutter.GutterBorder(0, 0, 0, 5));
        scroll.getGutter().setLayout(new BorderLayout(10, 0));
        scroll.setFoldIndicatorEnabled(true);
        scroll.setLineNumbersEnabled(UserConfig.getBoolean(KEY_LINE_NUMBERS));
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        editorPanel = new JPanel(new GridLayout(1, 1));
        editorPanel.add(scroll);
    }

    private void configureCursorPositionPanel() {
        cursorPositionPanel = new JPanel();
        cursorPositionPanel.setLayout(new BoxLayout(cursorPositionPanel, BoxLayout.X_AXIS));

        cursorPositionPanel.add(Box.createHorizontalGlue());
        cursorPositionPanel.add(new JLabel());
        cursorPositionPanel.add(Box.createRigidArea(new Dimension(10, 18)));
    }

    void copy() {
        textArea.copy();
    }

    void cut() {
        textArea.cut();
    }

    void paste() {
        textArea.paste();
    }

    void selectAll() {
        textArea.selectAll();
    }

    File getFile() {
        return file;
    }

    void setFile(File file) {
        this.file = file;
    }

    void setText(String text) {
        int caretPos = 0;

        // Check if the file is going to be reloaded to save the cursor position.
        if (text.length() >= textArea.getText().length())
            caretPos = textArea.getCaretPosition();

        textArea.setText(text);
        textArea.setCaretPosition(caretPos);
    }

    String getText() {
        return textArea.getText();
    }

    void redo() {
        textArea.redoLastAction();
    }

    void undo() {
        textArea.undoLastAction();
    }

    public String toString() {
        return file.getName();
    }

    public boolean equals(Object o) {
        if (o instanceof TextEditor) {
            if (file.getAbsolutePath().equals(((TextEditor) o).getFile().getAbsolutePath()))
                return true;
        }

        return false;
    }

    public int hashCode() {
        return file.getAbsolutePath().hashCode();
    }

    void enableLineNumbers(boolean state) {
        scroll.setLineNumbersEnabled(state);
    }

    void enableIndentationGuides(boolean state) {
        textArea.setPaintTabLines(state);
    }

    boolean isFileModified() {
        String savedText = ReadFile.read(file);

        return !savedText.equals(textArea.getText());
    }

    String getFileModifiedContent() {
        String savedText = ReadFile.read(file);
        String editorText = textArea.getText();

        if (savedText.equals(editorText))
            return null;

        return savedText;
    }

    void updateTheme(String themeName) {
        try {
            InputStream in = getClass().getResourceAsStream("/Devscribe/theme/" + themeName + ".xml");
            Theme theme = Theme.load(in);
            theme.apply(textArea);
            updateFont();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void setWordWrap(boolean status) {
        textArea.setWrapStyleWord(status);
        textArea.setLineWrap(status);
    }

    void updateFont() {
        Font font = new Font("JetBrains Mono", Font.PLAIN, fontSize);

        textArea.setFont(font);
        scroll.getGutter().setLineNumberFont(font);
    }

    JComponent getComponent() {
        return textArea;
    }

    private CodeTemplate[] getCodeTemplates() {
        CodeTemplate[] ct = {
                new StaticCodeTemplate("for", "for (int i = 0; i < ", "; i++) {\n\t\n}"),
                new StaticCodeTemplate("while", "while (", ") {\n\t\n}"),
                new StaticCodeTemplate("jmain", "public class ",
                        "{\n\tpublic static void main(String args[]){\n\t\t\n\t}\n}"),
                new StaticCodeTemplate("cmain", "#include<stdio.h>\n\nint main(){ \n\t", "\n\treturn 0;\n}"),
                new StaticCodeTemplate("pf", "printf(", ");"),
                new StaticCodeTemplate("sf", "scanf(", ");"),
                new StaticCodeTemplate("if", "if (", ") {\n\t\n}"),
                new StaticCodeTemplate("ie", "if (", ") {\n\t\n}else{\n\t\n}"),
                new StaticCodeTemplate("iee", "if (", ") {\n\t\n}else if (){\n\t\n}else{\n\t\n}"),
                new StaticCodeTemplate("func", "void ", "(){\n\n\treturn;\n}")
        };

        return ct;
    }
}
