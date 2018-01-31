package com.gamesart.sudoku.sudoku;

/**
 * Created by shaharel on 31/01/2018.
 */

public class TipsEngine {
    private int _currentNumberOfTips = 1000000;

    public int getCurrentNumberOfTips() {
        return _currentNumberOfTips;
    }

    public void decreaseTipsAmount() {
        if (_currentNumberOfTips <= 0){
            return;
        }

        this._currentNumberOfTips--;
    }

    public void userWonGame(){
        _currentNumberOfTips += 5;
    }
}
