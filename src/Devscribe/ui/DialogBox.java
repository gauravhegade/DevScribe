package Devscribe.ui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// inheritance and interface implementation
public class DialogBox extends JDialog implements ActionListener {
    private int buttonIndex;
    public static final int INFORMATION = 0, ERROR = 1, WARNING = 2;

    /**
     * Private constructor to create a DialogBox instance.
     * 
     * @param parent      The parent JFrame of the dialog.
     * @param message     The message to display in the dialog.
     * @param title       The title of the dialog.
     * @param options     An array of button labels.
     * @param messageType The type of message (INFORMATION, ERROR, or WARNING).
     */

    // polymorphism - overrides the default JDialog constructor
    private DialogBox(JFrame parent, String message, String title, String[] options, int messageType) {
        super(parent, true); // Create a modal dialog box.
        setTitle(title);
        setResizable(false); // Prevent resizing.
        setLayout(new GridBagLayout()); // Use a GridBagLayout for components.
        setIconImage(new ImageIcon(getClass().getResource("icon/Devscribe.png")).getImage()); // Set the dialog icon.

        // Add a window listener to handle dialog closing.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                buttonIndex = 2; // Set a default button index.
                dispose(); // Close the dialog.
            }
        });

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = constraints.gridy = 0; // Component in row 0, column 0.
        constraints.weighty = 1.0;
        constraints.insets = new Insets(30, 10, 30, 10); // Padding.

        JLabel icon;
        // Determine the icon based on the messageType.
        switch (messageType) {
            case INFORMATION:
                icon = new JLabel(new ImageIcon(getClass().getResource("icon/dialogInfo.png")));
                break;
            case ERROR:
                icon = new JLabel(new ImageIcon(getClass().getResource("icon/dialogError.png")));
                break;
            default:
                icon = new JLabel(new ImageIcon(getClass().getResource("icon/dialogWarning.png")));
                break;
        }
        add(icon, constraints);

        constraints.gridx = 1; // Component in row 0, column 1.
        constraints.insets = new Insets(30, 0, 30, 10); // Padding.

        JLabel messageLabel = new JLabel(message);
        add(messageLabel, constraints);

        constraints.gridx = 0; // Component in row 1, column 0.
        constraints.gridy = 1;
        constraints.gridwidth = 2; // Span two columns.
        constraints.weighty = 0.0;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 10, 10, 10); // Padding.
        constraints.fill = GridBagConstraints.HORIZONTAL;

        add(buttonsPanel(options), constraints); // Add the buttons panel.

        pack(); // Pack the dialog to fit its components.
        setLocationRelativeTo(parent); // Locate the dialog in the middle of its parent container.
        setVisible(true); // Make the dialog visible.
    }

    /**
     * Create a JPanel containing buttons.
     * 
     * @param options An array of button labels.
     * @return JPanel containing buttons.
     */
    private JPanel buttonsPanel(String[] options) {
        JPanel panel = new JPanel();

        JButton button;
        for (int i = 0; i < options.length; i++) {
            button = new JButton(options[i]);
            button.addActionListener(this);
            button.setActionCommand("" + i);
            panel.add(button);
        }

        return panel;
    }

    /**
     * Show a custom dialog box and return the index of the selected button.
     * 
     * @param parent      The parent JFrame of the dialog.
     * @param message     The message to display in the dialog.
     * @param title       The title of the dialog.
     * @param options     An array of button labels.
     * @param messageType The type of message (INFORMATION, ERROR, or WARNING).
     * @return The index of the selected button.
     */
    public static int show(JFrame parent, String message, String title, String[] options, int messageType) {
        return new DialogBox(parent, message, title, options, messageType).buttonIndex;
    }

    /**
     * ActionListener implementation for handling button clicks.
     * 
     * @param e The ActionEvent representing the button click.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        buttonIndex = Integer.parseInt(e.getActionCommand()); // Get the index of the clicked button.
        dispose(); // Close the dialog.
    }
}
