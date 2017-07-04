/*
 * Copyright (C) 2017 Félix Pedrozo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package rabbit.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import rabbit.io.ConfDeUsuario;
import rabbit.io.LeerArchivo;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.*;
import java.io.File;

import static rabbit.io.ConfDeUsuario.*;

public class EditorDeTexto extends JPanel {
    private String archivoRuta, nombreArchivo;

    private RSyntaxTextArea textArea;
    private RTextScrollPane scroll;

    private JPanel jpEditor;
    private JPanel jpPosicionCursor;

    private EditorUI editorUI;

    static int fontSize;

    static {
        //Se obtiene el tamaño de la fuente guardado.
        fontSize = ConfDeUsuario.getInt(KEY_FUENTE_TAMANIO);
    }

    EditorDeTexto (String archivoRuta, String text, EditorUI editorUI) {
        this (new File(archivoRuta), text, editorUI);
    }

    public EditorDeTexto (File file, String text, final EditorUI editorUI) {
        setLayout(new GridBagLayout());

        this.editorUI = editorUI;

        archivoRuta = file.getAbsolutePath();
        nombreArchivo = file.getName();

        textArea = new RSyntaxTextArea();
        textArea.setTabSize(4);
        textArea.setFocusable(true);
        textArea.setMarkOccurrences(true);
        textArea.setCodeFoldingEnabled(true);
        textArea.setPaintTabLines(ConfDeUsuario.getBoolean(KEY_GUIAS_IDENTACION));
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));

        if (text == null) {
            textArea.setText("inicio\n\tcls()\n\t\nfin"); //Texto que tendra por defecto.
            textArea.setCaretPosition(17); //Se configura la posición del cursor.

        } else {
            textArea.setText(text);
            textArea.setCaretPosition(0);
        }
        textArea.requestFocus();

        textArea.addCaretListener(e -> {
            actualizarPosCursor(textArea.getCaretPosition());
            EditorDeTexto.this.editorUI.actualizarMenuItem (textArea.canUndo(), textArea.canRedo());
        });

        textArea.getDocument().addUndoableEditListener(e ->
             EditorDeTexto.this.editorUI.actualizarMenuItem (textArea.canUndo(), textArea.canRedo())
        );

        //confMenuEmergente();
        confPanelEditor();
        confPanelPosicionCursor ();
        //confEstiloYFormato();
        actualizarPosCursor(textArea.getCaretPosition());

        GridBagConstraints conf = new GridBagConstraints();

        //Configuración del componente en la fila 0 columna 0.
        conf.gridx = conf.gridy = 0;
        conf.weightx = conf.weighty = 1.0;
        conf.fill = GridBagConstraints.BOTH;

        add(jpEditor, conf);

        //Configuración del componente en la fila 1 columna 0.
        conf.gridy = 1;
        conf.weighty = 0.0;
        conf.fill = GridBagConstraints.HORIZONTAL;

        add (jpPosicionCursor, conf);
    }

    private void actualizarPosCursor (int caretPosc) {
        //Extraigo el label que contiene el panel 'jpPosicionCursor'.
        JLabel jlPosCursor = (JLabel) jpPosicionCursor.getComponent(1);

        int fila = obtenerFila(caretPosc);
        int colum = obtenerColum(caretPosc);

        //Actualizo posición del cursor.
        jlPosCursor.setText(fila + " : " + colum);
    }

    private void confPanelEditor () {
        scroll = new RTextScrollPane(textArea);
        scroll.getGutter().setBorder(new Gutter.GutterBorder(0, 0, 0, 5));
        scroll.getGutter().setLayout(new BorderLayout(10, 0));
        scroll.setFoldIndicatorEnabled(true);
        scroll.setLineNumbersEnabled(ConfDeUsuario.getBoolean(KEY_NUM_LINEA));
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        jpEditor  = new JPanel(new GridLayout(1, 1));
        jpEditor.add(scroll);
    }

    private void confPanelPosicionCursor () {
        jpPosicionCursor = new JPanel();
        jpPosicionCursor.setLayout(new BoxLayout(jpPosicionCursor, BoxLayout.X_AXIS));

        jpPosicionCursor.add(Box.createHorizontalGlue());
        jpPosicionCursor.add(new JLabel());
        jpPosicionCursor.add(Box.createRigidArea(new Dimension(10, 18)));
    }

    void copiar () {
        textArea.copy();
    }

    void cortar () {
        textArea.cut();
    }

    void pegar () {
        textArea.paste();
    }

    void seleccTodo () {
        textArea.selectAll();
    }

    void setArchivoRuta(String archivoRuta) {
        this.archivoRuta = archivoRuta;
    }

    String getArchivoRuta() {
        return archivoRuta;
    }

    void setText(String text) {
        int caretPos = 0;

        //Compruebo si el archivo va ha ser recargado para guardar la posición del cursor.
        if (text.length() >= textArea.getText().length())
            caretPos = textArea.getCaretPosition();

        textArea.setText(text);
        textArea.setCaretPosition(caretPos);
    }

    String getText() {
        return textArea.getText();
    }

    String getNombreArchivo() {
        return nombreArchivo;
    }

    void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    void rehacer() {
        textArea.redoLastAction();
    }

    void deshacer() {
        textArea.undoLastAction();
    }

    public String toString () {
        return nombreArchivo;
    }

    public boolean equals (Object o) {
        if (o instanceof EditorDeTexto) {
            if (archivoRuta.equals(((EditorDeTexto)o).getArchivoRuta()))
                return true;
        }

        return false;
    }

    public int hashCode () {
        return archivoRuta.hashCode();
    }

    void habilitarNumLineas(boolean state) {
        scroll.setLineNumbersEnabled(state);
    }

    void habilitarGuiasDeIdentacion(boolean state) {
        textArea.setPaintTabLines(state);
    }

    boolean archivoModificado() {
        String textGuardado = LeerArchivo.leer(archivoRuta);

        return !textGuardado.equals(textArea.getText());
    }

    String archivoModifRetornaContenido() {
        String textGuardado = LeerArchivo.leer(archivoRuta);
        String textEditor = textArea.getText();

        if (textGuardado.equals(textEditor)) return null;

        return textGuardado;
    }

    void actualizarTema() {

    }

    void actualizarFuente() {
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);

        textArea.setFont(font);
        scroll.getGutter().setLineNumberFont(font);
    }

    private int obtenerFila (int posc) {
        int numFila = 0;

        try {
            for (int offset = posc; offset >= 0;) {
                offset = Utilities.getRowStart(textArea, offset) - 1;
                numFila ++;
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return numFila;
    }

    private int obtenerColum (int posc) {
        int colNum = 0;

        try {
            colNum = posc - Utilities.getRowStart(textArea, posc) + 1;

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return colNum;
    }
}
