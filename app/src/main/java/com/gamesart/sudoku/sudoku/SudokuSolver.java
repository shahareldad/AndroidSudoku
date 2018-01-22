package com.gamesart.sudoku.sudoku;

/**
 * Created by shaharel on 11/01/2018.
 */

public class SudokuSolver {

    private int[][] _solvedBoard;

    public int[][] GetSolvedBoard(){
        return _solvedBoard;
    }

    public int Solve(int[][] board)
    {
        int result = FillEmptySlots(board, 0, 0);
        _solvedBoard = board;
        return result;
    }

    private int CheckNumberInPosition(int board[][], int row, int col, int num)
    {
        int rowStart = (row / 3) * 3;
        int colStart = (col / 3) * 3;

        for (int index = 0; index < 9; ++index)
        {
            if (board[row][index] == num)
            return 0;
            if (board[index][col] == num)
            return 0;
            if (board[rowStart + (index % 3)][ colStart + (index / 3)] == num)
            return 0;
        }
        return 1;
    }

    private int FillEmptySlots(int[][] board, int row, int col)
    {
        if (row < 9 && col < 9)
        {
            if (board[row][ col] != 0)
            {
                if ((col + 1) < 9)
                    return FillEmptySlots(board, row, col + 1);
                else if ((row + 1) < 9)
                    return FillEmptySlots(board, row + 1, 0);
                else
                    return 1;
            }
                else
            {
                for (int digit = 1; digit <= 9; ++digit)
                {
                    if (CheckNumberInPosition(board, row, col, digit) == 1)
                    {
                        board[row][ col] = digit;
                        if ((col) < 9)
                        {
                            if (FillEmptySlots(board, row, col) == 1)
                                return 1;
                            else
                                board[row][ col] = 0;
                        }
                        else if ((row) < 9)
                        {
                            if (FillEmptySlots(board, row, 0) == 1)
                                return 1;
                            else
                                board[row][ col] = 0;
                        }
                        else return 1;
                    }
                }
            }
            return 0;
        }
        else
            return 1;
    }
}
