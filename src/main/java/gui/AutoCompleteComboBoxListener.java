package gui;


import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class AutoCompleteComboBoxListener<T> {

    private ComboBox<T> comboBox;
    private StringBuilder sb;
    private int lastLength;

    public AutoCompleteComboBoxListener(ComboBox<T> comboBox) {
        this.comboBox = comboBox;
        sb = new StringBuilder();

        this.comboBox.setEditable(true);
        this.comboBox.setOnKeyReleased(event -> {
            // this variable is used to bypass the auto complete process if the length is the same.
            // this occurs if user types fast, the length of textfield will record after the user
            // has typed after a certain delay.
            if (lastLength != (comboBox.getEditor().getLength() - comboBox.getEditor().getSelectedText().length()))
                lastLength = comboBox.getEditor().getLength() - comboBox.getEditor().getSelectedText().length();

            if (event.isControlDown() || event.getCode() == KeyCode.BACK_SPACE ||
                    event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT ||
                    event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.HOME ||
                    event.getCode() == KeyCode.END || event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.ENTER

                    )
                return;

            if (event.getCode().equals(KeyCode.DOWN)) {
                comboBox.show();
                return;
            }

            IndexRange ir = comboBox.getEditor().getSelection();
            sb.delete(0, sb.length());
            sb.append(comboBox.getEditor().getText());
            // remove selected string index until end so only unselected text will be recorded
            try {
                sb.delete(ir.getStart(), sb.length());
            } catch (Exception ignored) { }

            ObservableList<T> items = comboBox.getItems();
            for (int i=0; i<items.size(); i++) {
                if (items.get(i) != null && comboBox.getEditor().getText() != null && items.get(i).toString().toLowerCase().startsWith(comboBox.getEditor().getText().toLowerCase())) {
                    try {
                        comboBox.getEditor().setText(sb.toString() + items.get(i).toString().substring(sb.toString().length()));
                        comboBox.setValue(items.get(i));
                        comboBox.getSelectionModel().select(i);
                    } catch (Exception e) {
                        comboBox.getEditor().setText(sb.toString());
                    }
                    comboBox.getEditor().positionCaret(sb.toString().length());
                    comboBox.getEditor().selectEnd();
                    break;
                }
            }
        });

        // add a focus listener such that if not in focus, reset the filtered typed keys
//        this.comboBox.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
//            if (!newValue) {
//                lastLength = 0;
//                sb.delete(0, sb.length());
//                selectClosestResultBasedOnTextFieldValue(false, false);
//            }
//        });

        this.comboBox.setOnMouseClicked(event -> selectClosestResultBasedOnTextFieldValue(true, true));
    }

    /*
     * selectClosestResultBasedOnTextFieldValue() - selects the item and scrolls to it when
     * the popup is shown.
     *
     * parameters:
     *  affect - true if combobox is clicked to show popup so text and caret position will be readjusted.
     *  inFocus - true if combobox has focus. If not, programmatically press enter key to add new entry to list.
     *
     */
    private void selectClosestResultBasedOnTextFieldValue(boolean affect, boolean inFocus) {
        ObservableList items = AutoCompleteComboBoxListener.this.comboBox.getItems();
        boolean found = false;
        if(items == null)return;

        for (int i=0; i<items.size(); i++) {
            if (items.get(i) != null && AutoCompleteComboBoxListener.this.comboBox.getEditor().getText() != null && AutoCompleteComboBoxListener.this.comboBox.getEditor().getText().toLowerCase().equals(items.get(i).toString().toLowerCase())) {
                try {
                    ListView lv = ((ComboBoxListViewSkin) AutoCompleteComboBoxListener.this.comboBox.getSkin()).getListView();
                    lv.getSelectionModel().clearAndSelect(i);
                    lv.scrollTo(lv.getSelectionModel().getSelectedIndex());
                    found = true;
                    break;
                } catch (Exception ignored) { }
            }
        }

        String s = comboBox.getEditor().getText();
        System.out.println("Found? " + found);
        if (!found && affect) {
            comboBox.getSelectionModel().clearSelection();
            comboBox.getEditor().setText(s);
            comboBox.getEditor().end();
        }

        if (!found) {
            comboBox.getEditor().setText(null);
            comboBox.getSelectionModel().select(null);
            comboBox.setValue(null);
        }
//            KeyEvent ke = new KeyEvent(comboBox, KeyCode.ENTER.toString(), KeyCode.ENTER.getName(), KeyCode.ENTER.impl_getCode(), false, false, false, false, KeyEvent.KEY_RELEASED);

        if (!inFocus && comboBox.getEditor().getText() != null && comboBox.getEditor().getText().trim().length() > 0) {
            // press enter key programmatically to have this entry added
            KeyEvent ke = new KeyEvent(KeyEvent.KEY_RELEASED, KeyCode.ENTER.toString(), KeyCode.ENTER.toString(), KeyCode.ENTER, false, false, false, false);
            comboBox.fireEvent(ke);
        }
    }

}