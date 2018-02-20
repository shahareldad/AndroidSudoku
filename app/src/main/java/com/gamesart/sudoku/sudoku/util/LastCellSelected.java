package com.gamesart.sudoku.sudoku.util;

import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

/**
 * Created by shaharel on 20/02/2018.
 */

public class LastCellSelected {
    private TextView _cell;
    private boolean _isInEditMode;
    private GridLayout _notes;

    public TextView getCell() {
        return _cell;
    }

    public void setNoteNumber(String text) {
        TextView note = (TextView)_notes.getChildAt(Integer.valueOf(text) - 1);
        String currentText = String.valueOf(note.getText());
        if (currentText.equals(text)){
            note.setText("");
        }else {
            note.setText(text);
        }
    }

    public void clearNotes(){
        for(int index = 0; index < 9; index++){
            TextView note = (TextView)_notes.getChildAt(index);
            note.setText("");
        }
    }

    public void setCell(TextView cell, GridLayout notes) {
        _cell = cell;
        _notes = notes;
    }

    public boolean isInEditMode() {
        return _isInEditMode;
    }

    public void reverseEditMode() {
        setIsInEditMode(!_isInEditMode);
    }

    public void setIsInEditMode(boolean isInEditMode) {
        _isInEditMode = isInEditMode;
        if (_isInEditMode){
            _cell.setVisibility(View.INVISIBLE);
            _cell.setText("");
            _notes.setVisibility(View.VISIBLE);
        }else{
            _cell.setVisibility(View.VISIBLE);
            clearNotes();
            _notes.setVisibility(View.INVISIBLE);
        }
    }
}
