package com.gamesart.sudoku.sudoku;

/**
 * Created by shaharel on 22/01/2018.
 */

public class SettingsData {
    private boolean RedSquares;
    private boolean GreenCross;
    private boolean SameNumbers;

    public boolean showRedSquares() {
        return RedSquares;
    }

    public void setRedSquares(boolean redSquares) {
        RedSquares = redSquares;
    }

    public boolean showGreenCross() {
        return GreenCross;
    }

    public void setGreenCross(boolean greenCross) {
        GreenCross = greenCross;
    }

    public boolean showSameNumbers() {
        return SameNumbers;
    }

    public void setSameNumbers(boolean sameNumbers) {
        SameNumbers = sameNumbers;
    }
}
