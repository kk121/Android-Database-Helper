import javax.swing.*;
import java.awt.event.*;

public class TableDetailsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea textArea1;
    private TableCreationQueryDialogListener dialogListener;

    public TableDetailsDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

//        editorTextField = new EditorTextField();
//        contentPane.add(editorTextField);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        // add your code here
        dispose();
        if (dialogListener != null) {
            dialogListener.onDialogResult(textArea1.getText());
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        /*Messages.showMessageDialog("\n main called", "Information", Messages.getInformationIcon());
        TableDetailsDialog dialog = new TableDetailsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.out.print("main");
        System.exit(0);*/
    }

    public void setDilogListener(TableCreationQueryDialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }

    public interface TableCreationQueryDialogListener {
        void onDialogResult(String result);
    }
}
