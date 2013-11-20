package ca.mudar.patinoires.ui.view;

import android.database.Cursor;

/**
 * Created by mudar on 19/11/13.
 */
public interface IMultiChoiceModeAdapter {
    public void setNewSelection(int id, boolean checked);

    public int getSelectionSize();

    public String[] getSelectionItems();

    public void clearSelection();

    public Cursor getCursor();
}
