package com.gamesart.sudoku.sudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.GridLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoardActivity extends AppCompatActivity {

    private String TAG = "BoardActivity";
    private String FILENAME = "games_art_sudoku_saved_board";

    private Handler mHandler = null;
    private TextView lastSelectedCell = null;
    private int[][] _board = null;
    private int _screenWidth = 0;
    private int _screenHeight = 0;
    private Integer _level;
    private SudokuSolver _solver = null;
    private int[][] _solvedBoard = null;
    private int _counter = 0;
    private GridLayout _mainGridLayout = null;

    private AdView _adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        _adView = findViewById(R.id.adView);
        AdRequest request = new AdRequest.Builder().build();
        _adView.loadAd(request);

        _level = Integer.valueOf(getIntent().getIntExtra(FirstViewActivity.LevelParamName, 1));

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        _screenWidth = size.x;
        _screenHeight = size.y;

        _mainGridLayout = findViewById(R.id.mainGridLayout);
        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                TextView textView = (TextView)msg.obj;
                _mainGridLayout.addView(textView);
            }
        };

        _solver = new SudokuSolver();

        ArrayList<CellData> cells = TryLoadSavedGame();
        if (cells != null){
            StartNewGame(_mainGridLayout, _level, false);
            SetUserDigitsOnBoard(cells);
        }
        else{
            StartNewGame(_mainGridLayout, _level, true);
        }

        InitKeyboard(_mainGridLayout);
    }

    private void SetUserDigitsOnBoard(ArrayList<CellData> cells) {

    }

    private ArrayList<CellData> TryLoadSavedGame() {
        byte[] fileData = null;

        try{
            FileInputStream fos = openFileInput(FILENAME);
            fos.read(fileData);
            fos.close();
        }
        catch (FileNotFoundException ex){
            Log.e(TAG, "SaveCurrentBoardState.FileNotFoundException: " + ex.getMessage());
        }
        catch (IOException ex){
            Log.e(TAG, "SaveCurrentBoardState.IOException: " + ex.getMessage());
        }

        if (fileData == null){
            return null;
        }

        String gsoned = "";

        try{
            gsoned = new String(fileData, "UTF-8");
        }catch (UnsupportedEncodingException ex){
            Log.e(TAG, "SaveCurrentBoardState.UnsupportedEncodingException: " + ex.getMessage());
        }

        Gson gson = new GsonBuilder().create();
        ArrayList<CellData> cells = new ArrayList<>();
        cells = gson.fromJson(gsoned, cells.getClass());

        _board = new int[9][9];
        int length = cells.size();
        if (length != 81){
            Log.e(TAG, "TryLoadSavedGame.Length of loaded game array is not 81. Something went wrong on last save. Stopping load process.");
            return null;
        }
        for (int index = 0; index < length; index++){
            CellData temp = cells.get(index);
            if (temp.getIsCellConst())
                _board[temp.getRow()][temp.getColumn()] = temp.getCellDigit();
            else
                _board[temp.getRow()][temp.getColumn()] = 0;
        }

        return cells;
    }

    @Override
    protected void onStop() {
        super.onStop();

        SaveCurrentBoardState();
    }

    private void SaveCurrentBoardState() {
        ArrayList<CellData> cells = new ArrayList<>(81);

        for (int index = 0; index < 81; index++){
            CellData item = new CellData();
            TextView cell = (TextView)_mainGridLayout.getChildAt(index);
            String cellStringValue = cell.getText().toString();
            if (cellStringValue.equals(""))
                item.setCellDigit(0);
            else
                item.setCellDigit(Integer.valueOf(cellStringValue));

            String tag = String.valueOf(cell.getTag());
            int row = Character.getNumericValue(tag.charAt(0));
            int col = Character.getNumericValue(tag.charAt(1));
            int isConst = Character.getNumericValue(tag.charAt(2));
            item.setRow(row);
            item.setColumn(col);
            item.setIsCellConst(isConst == 1);
            cells.add(item);
        }
        Gson gson = new GsonBuilder().create();
        String result = gson.toJson(cells);

        try{
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(result.getBytes("UTF-8"));
            fos.close();
        }
        catch (FileNotFoundException ex){
            Log.e(TAG, "SaveCurrentBoardState.FileNotFoundException: " + ex.getMessage());
        }
        catch (IOException ex){
            Log.e(TAG, "SaveCurrentBoardState.IOException: " + ex.getMessage());
        }
    }

    private void InitKeyboard(final GridLayout mainGridLayout) {
        GridLayout keyboardGrid = findViewById(R.id.gameKeyboard);
        int length = keyboardGrid.getChildCount();
        int height = _screenWidth / 9;
        int numericWidth = height * 7 / 5;
        int commandWidth = height * 2;
        for (int index = 0; index < length; index++){
            TextView keyboardKey = (TextView)keyboardGrid.getChildAt(index);
            keyboardKey.setHeight(height);
            SetKeyboardClick(mainGridLayout, keyboardKey);
            SetKeyboardKeysWidth(numericWidth, commandWidth, keyboardKey);
        }
    }

    private void SetKeyboardKeysWidth(int numericWidth, int commandWidth, TextView keyboardKey) {
        String text = String.valueOf(keyboardKey.getText());
        switch (text){
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
            case "Clear":
                keyboardKey.setWidth(numericWidth);
                break;
            case "New Game":
            case "Reset":
                keyboardKey.setWidth(commandWidth);
                break;
        }
    }

    private void SetKeyboardClick(final GridLayout mainGridLayout, final TextView keyboardKey) {
        keyboardKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = String.valueOf(keyboardKey.getText());
                String currentText = "";
                char isConst = ' ';
                if (lastSelectedCell != null){
                    currentText = String.valueOf(lastSelectedCell.getText());
                    isConst = String.valueOf(lastSelectedCell.getTag()).charAt(3);
                }

                switch (text){
                    case "1":
                    case "2":
                    case "3":
                    case "4":
                    case "5":
                    case "6":
                    case "7":
                    case "8":
                    case "9":
                        CaseDigitSelected(text, currentText, isConst, mainGridLayout);
                        break;
                    case "Clear":
                        CaseClearSelected(currentText, isConst);
                        break;
                    case "New Game":
                        StartNewGame(mainGridLayout, _level, true);
                        break;
                    case "Reset":
                        StartNewGame(mainGridLayout, _level, false);
                        break;
                }
            }
        });
    }

    private void CaseClearSelected(String currentText, char isConst) {
        if (lastSelectedCell == null)
            return;
        if (isConst == '1')
            return;
        if (currentText.equals(""))
            return;
        _counter++;
        lastSelectedCell.setText("", TextView.BufferType.EDITABLE);
    }

    private void CaseDigitSelected(String text, String currentText, char isConst, GridLayout mainGridLayout) {
        if (lastSelectedCell == null)
            return;
        if (isConst == '1')
            return;
        if (currentText.equals(""))
            _counter--;
        lastSelectedCell.setText(text, TextView.BufferType.EDITABLE);
        CheckWinState(mainGridLayout);
    }

    private void CheckWinState(final GridLayout mainGridLayout){
        if (_counter != 0)
            return;

        int[][] temp = deepCopy(_board);
        _solver.Solve(temp);
        _solvedBoard = _solver.GetSolvedBoard();

        boolean isWinStateTrue = true;
        for (int index = 0; index < 81; index++){
            TextView cell = (TextView)mainGridLayout.getChildAt(index);
            String cellStringValue = cell.getText().toString();
            int cellValue = Integer.valueOf(cellStringValue);
            String tag = String.valueOf(cell.getTag());
            int row = Character.getNumericValue(tag.charAt(0));
            int col = Character.getNumericValue(tag.charAt(1));
            int solvedDigit = _solvedBoard[row][col];

            if (cellValue != solvedDigit){
                isWinStateTrue = false;
                break;
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (isWinStateTrue) {
            builder.setTitle(R.string.youWin);
            final String[] items = new String[]{
                    getString(R.string.startNew),
                    getString(R.string.selectLevel)
            };
            ShowAlertDialogOnGameOver(mainGridLayout, builder, items);
        }
        else{
            builder.setTitle(R.string.solutionIsIncorrect);
            final String[] items = new String[]{
                    getString(R.string.continueCurrent),
                    getString(R.string.resetCurrent),
                    getString(R.string.startNew),
                    getString(R.string.selectLevel)
            };
            ShowAlertDialogOnGameOver(mainGridLayout, builder, items);
        }
    }

    private void ShowAlertDialogOnGameOver(final GridLayout mainGridLayout, AlertDialog.Builder builder, final String[] items) {
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selection = Arrays.asList(items).get(i);
                GameOverMenuItemClicked(dialogInterface, selection, mainGridLayout);
            }
        });

        builder.create().show();
    }

    private void GameOverMenuItemClicked(DialogInterface dialogInterface, String selection, GridLayout mainGridLayout) {
        if (selection.equals(getString(R.string.startNew))){
            StartNewGame(mainGridLayout, _level, true);
        }
        if (selection.equals(getString(R.string.selectLevel))){
            finish();
        }
        if (selection.equals(getString(R.string.resetCurrent))){
            StartNewGame(mainGridLayout, _level, false);
        }
        if (selection.equals(getString(R.string.continueCurrent))){
            dialogInterface.dismiss();
        }
    }

    public static int[][] deepCopy(int[][] original) {
        if (original == null) {
            return null;
        }

        final int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return result;
    }

    private void StartNewGame(GridLayout mainGridLayout, int level, boolean newBoard) {
        mainGridLayout.removeAllViews();

        if (newBoard){
            SudokuGenerator generator = new SudokuGenerator();
            _board = generator.GetBoard(level);
        }

        int rowCount = mainGridLayout.getRowCount();
        int columnCount = mainGridLayout.getColumnCount();

        int cellDimension = _screenWidth / 9;
        Log.d("BoardActivity", "cellDimension Pixel measurement: " + String.valueOf(cellDimension));

        for (int row = 0; row < rowCount; row++){
            for (int col = 0; col < columnCount; col++){

                class CreateCell implements Runnable{

                    private final int[][] _board;
                    private final int _row;
                    private final int _col;
                    private final int _cellDimension;

                    CreateCell (int[][] board, int row, int col, int px){
                        _board = board;
                        _row = row;
                        _col = col;
                        _cellDimension = px;
                    }

                    @Override
                    public void run() {
                        int digit = _board[_row][_col];
                        if (digit == 0)
                            _counter++;
                        String strDigit = String.valueOf(digit);
                        TextView cell = CreateTextViewCell(_row, _col, strDigit, _cellDimension);

                        Message completeMessage = mHandler.obtainMessage(1, cell);
                        completeMessage.sendToTarget();
                    }
                }
                Thread t = new Thread(new CreateCell(_board, row, col, cellDimension));
                t.start();
            }
        }
    }

    private TextView CreateTextViewCell(int row, int col, String strDigit, int cellDimension) {
        try{
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout layout = (RelativeLayout)inflater.inflate(R.layout.cell_view, null);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(row);
            params.columnSpec = GridLayout.spec(col);

            TextView cell = (TextView)layout.getChildAt(0);
            cell.setWidth(cellDimension);
            cell.setHeight(cellDimension);
            layout.removeViewAt(0);
            cell.setLayoutParams(params);

            String cellTag = String.valueOf(row) + String.valueOf(col);

            if ((row >= 0 && row <= 2) || (row >= 6 && row <= 8)){
                if (col >= 3 && col <= 5){
                    cell.setBackground(getDrawable(R.drawable.sudoku_cell));
                    cellTag += "1";
                }
                else{
                    cell.setBackground(getDrawable(R.drawable.sudoku_cell_alt));
                    cellTag += "0";
                }
            }
            else{
                if (col < 3 || col > 5){
                    cell.setBackground(getDrawable(R.drawable.sudoku_cell));
                    cellTag += "1";
                }
                else{
                    cell.setBackground(getDrawable(R.drawable.sudoku_cell_alt));
                    cellTag += "0";
                }
            }

            if (!strDigit.equals("0")) {
                cell.setTextColor(Color.GRAY);
                cell.setText(strDigit, TextView.BufferType.EDITABLE);
                cellTag += "1";
            }
            else{
                cell.setTextColor(Color.BLACK);
                cellTag += "0";
            }
            cell.setTag(cellTag);

            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OnCellClicked((TextView) view);
                }
            });

            return cell;
        }catch (Exception ex){
            Log.e(TAG, "CreateTextViewCell Failed: " + ex.getMessage());
            return null;
        }
    }

    private void OnCellClicked(TextView view) {
        if (lastSelectedCell != null){
            char backgroundState = String.valueOf(lastSelectedCell.getTag()).charAt(2);
            if (backgroundState == '1'){
                lastSelectedCell.setBackground(getDrawable(R.drawable.sudoku_cell));
            }
            else{
                lastSelectedCell.setBackground(getDrawable(R.drawable.sudoku_cell_alt));
            }
        }

        char backgroundState = String.valueOf(view.getTag()).charAt(2);
        if (backgroundState == '1'){
            view.setBackground(getDrawable(R.drawable.sudoku_cell_selected));
        }
        else{
            view.setBackground(getDrawable(R.drawable.sudoku_cell_alt_selected));
        }
        lastSelectedCell = view;

        GridLayout grid = findViewById(R.id.gameKeyboard);
        int length = grid.getChildCount();
        char isConst = String.valueOf(lastSelectedCell.getTag()).charAt(3);
        for (int index = 0; index <length; index++){
            TextView key = (TextView)grid.getChildAt(index);
            String text = String.valueOf(key.getText());
            switch (text){
                case "1":
                case "2":
                case "3":
                case "4":
                case "5":
                case "6":
                case "7":
                case "8":
                case "9":
                case "Clear":
                    if (isConst == '1'){
                        key.setEnabled(false);
                    }
                    else{
                        key.setEnabled(true);
                    }
                    break;
            }
        }
    }
}
