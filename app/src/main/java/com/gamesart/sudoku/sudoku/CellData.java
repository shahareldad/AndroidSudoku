package com.gamesart.sudoku.sudoku;

/**
 * Created by shaharel on 22/01/2018.
 */

public class CellData {
    private int CellDigit;
    private int Row;
    private int Column;
    private String IsCellConst;

    public int getCellDigit() {
        return CellDigit;
    }

    public void setCellDigit(int cellDigit) {
        CellDigit = cellDigit;
    }

    public int getRow() {
        return Row;
    }

    public void setRow(int row) {
        Row = row;
    }

    public int getColumn() {
        return Column;
    }

    public void setColumn(int column) {
        Column = column;
    }

    public String getIsCellConst() {
        return IsCellConst;
    }

    public void setIsCellConst(String cellConst) {
        IsCellConst = cellConst;
    }
}
