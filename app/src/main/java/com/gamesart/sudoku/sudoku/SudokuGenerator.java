package com.gamesart.sudoku.sudoku;

import java.util.Random;

/**
 * Created by shaharel on 11/01/2018.
 */

public class SudokuGenerator {

    private int[][] _board;
    private SudokuSolver _solver = new SudokuSolver();

    public SudokuGenerator()
    {
        _board = new int[][]
        {
            {7,3,5,6,1,4,8,9,2},
            {8,4,2,9,7,3,5,6,1},
            {9,6,1,2,8,5,3,7,4},
            {2,8,6,3,4,9,1,5,7},
            {4,1,3,8,5,7,9,2,6},
            {5,7,9,1,2,6,4,3,8},
            {1,5,7,4,9,2,6,8,3},
            {6,9,4,7,3,8,2,1,5},
            {3,2,8,5,6,1,7,4,9}
        };
    }

    public int[][] GetBoard(int level)
    {
        ShuffleBoard();
        return MaskCells(level);
    }

    private int[][] MaskCells(int level)
    {
        int getMaskLevel = GetMaskingValuePerLevel(level);
        Random rnd = new Random();

        for (int index = 0; index < getMaskLevel; index++)
        {
            int row = rnd.nextInt(9);
            int col = rnd.nextInt(9);
            if (_board[row][col] == 0)
            {
                index--;
                continue;
            }
            int temp = _board[row][col];
            _board[row][col] = 0;
            if (_solver.Solve(deepCopyIntMatrix(_board)) == 0)
            {
                _board[row][col] = temp;
                index--;
            }
        }
        return _board;
    }

    private int[][] deepCopyIntMatrix(int[][] input) {
        if (input == null)
            return null;
        int[][] result = new int[input.length][];
        for (int r = 0; r < input.length; r++) {
            result[r] = input[r].clone();
        }
        return result;
    }

    private int GetMaskingValuePerLevel(int level)
    {
        int result;
        switch (level)
        {
            case 0:             // Difficulty level very easy
                result = new Random().nextInt(10) + 50;
                break;

            case 1:                 // Difficulty level easy
                result = new Random().nextInt(13) + 41;
                if (BoardActivity.DEBUG_MODE == 1){
                    result = 80;
                }
                break;

            case 2:               // Difficulty level medium
                result = new Random().nextInt(8) + 32;
                break;

            case 3:                 // Difficulty level hard
                result = new Random().nextInt(3) + 28;
                break;

            default:                                    // Default expert level.
                result = new Random().nextInt(5) + 22;
                break;
        }
        return 81 - result;
    }

    private void ShuffleBoard()
    {
        for (int times = 0; times < 1000; times++)
        {
            Random rnd = new Random();
            int rndCol = rnd.nextInt(9);
            int colStart = rndCol / 3 * 3;
            int rndSwitchCol = rnd.nextInt(2) + colStart;
            int rndRow = rnd.nextInt(9);
            int RowStart = rndRow / 3 * 3;
            int rndSwitchRow = rnd.nextInt(2) + RowStart;

            for (int index = 0; index < 9; index++)
            {
                int temp = _board[rndCol][index];
                _board[rndCol][index] = _board[rndSwitchCol][index];
                _board[rndSwitchCol][index] = temp;
            }

            for (int index = 0; index < 9; index++)
            {
                int temp = _board[rndRow][index];
                _board[rndRow][index] = _board[rndSwitchRow][index];
                _board[rndSwitchRow][index] = temp;
            }
        }
    }
}
