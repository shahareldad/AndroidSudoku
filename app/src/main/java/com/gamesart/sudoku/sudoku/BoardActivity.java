package com.gamesart.sudoku.sudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

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
    private ArrayList<CellData> _cells = null;

    private AdView _adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate started");

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
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onStop onResume");
        super.onResume();

        if (_level == -1) {
            _level = 1;
            _cells = TryLoadSavedGame();
        }
        if (_cells != null){
            StartNewGame(_mainGridLayout, _level, false);
        }
        else{
            _cells = new ArrayList<>(81);
            StartNewGame(_mainGridLayout, _level, true);
        }

        InitKeyboard(_mainGridLayout);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onStop started");
        super.onPause();
        SaveCurrentBoardState();
    }

    private ArrayList<CellData> TryLoadSavedGame() {
        Log.d(TAG, "TryLoadSavedGame started");

        byte[] fileData = null;
        BufferedReader br = null;
        StringBuilder builder = null;
        InputStream stream = null;

        try{
            stream = openFileInput(FILENAME);
            br = new BufferedReader(new InputStreamReader(stream));
            builder = new StringBuilder();

            String line = "";
            while ((line = br.readLine()) != null){
                builder.append(line);
            }
            stream.close();
        }
        catch (IOException ex){
            Log.e(TAG, "SaveCurrentBoardState.IOException: " + ex.getMessage());
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (fileData == null){
            return null;
        }

        Gson gson = new GsonBuilder().create();
        ArrayList<CellData> cells = new ArrayList<>();
        cells = gson.fromJson(builder.toString(), cells.getClass());

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

    private void SaveCurrentBoardState() {
        Log.d(TAG, "SaveCurrentBoardState started");

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
        Log.d(TAG, "Gsoned the CellData list: " + result);

        FileOutputStream fos = null;
        try{
            fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(result.getBytes());
            fos.close();
        }
        catch (FileNotFoundException ex){
            Log.e(TAG, "SaveCurrentBoardState.FileNotFoundException: " + ex.getMessage());
        }
        catch (IOException ex){
            Log.e(TAG, "SaveCurrentBoardState.IOException: " + ex.getMessage());
        }
        finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void InitKeyboard(final GridLayout mainGridLayout) {
        Log.d(TAG, "InitKeyboard started");

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
        Log.d(TAG, "SetKeyboardKeysWidth started");

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
        Log.d(TAG, "SetKeyboardClick started");

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
        Log.d(TAG, "CaseClearSelected started");

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
        Log.d(TAG, "CaseDigitSelected started");

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
        Log.d(TAG, "CheckWinState started");

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
        Log.d(TAG, "ShowAlertDialogOnGameOver started");

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
        Log.d(TAG, "GameOverMenuItemClicked started");

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

    private int[][] deepCopy(int[][] original) {
        Log.d(TAG, "deepCopy started");

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
        Log.d(TAG, "StartNewGame started");

        mainGridLayout.removeAllViews();

        if (newBoard){
            SudokuGenerator generator = new SudokuGenerator();
            _board = generator.GetBoard(level);
            _cells.clear();
            for (int row = 0; row < 9; row++){
                for (int col = 0; col < 9; col++){
                    CellData item = new CellData();
                    item.setRow(row);
                    item.setColumn(col);
                    item.setCellDigit(_board[row][col]);
                    if (_board[row][col] == 0)
                        item.setIsCellConst(false);
                    else
                        item.setIsCellConst(true);
                    _cells.add(item);
                }
            }
        }

        int cellDimension = _screenWidth / 9;
        Log.d("BoardActivity", "cellDimension Pixel measurement: " + String.valueOf(cellDimension));

        for(int index = 0; index < 81; index++){
            class CreateCell implements Runnable{

                private final int _digit;
                private final int _row;
                private final int _col;
                private final int _cellDimension;
                private final boolean _isCellConst;

                CreateCell (int digit, int row, int col, boolean isCellConst, int px){
                    _digit = digit;
                    _row = row;
                    _col = col;
                    _isCellConst = isCellConst;
                    _cellDimension = px;
                }

                @Override
                public void run() {
                    if (_digit == 0)
                        _counter++;
                    String strDigit = String.valueOf(_digit);
                    TextView cell = CreateTextViewCell(_row, _col, strDigit, _isCellConst, _cellDimension);

                    Message completeMessage = mHandler.obtainMessage(1, cell);
                    completeMessage.sendToTarget();
                }
            }
            CellData item = _cells.get(index);
            Thread t = new Thread(new CreateCell(item.getCellDigit(), item.getRow(), item.getColumn(), item.getIsCellConst(), cellDimension));
            t.start();
        }
    }

    private TextView CreateTextViewCell(int row, int col, String strDigit, boolean isCellConst, int cellDimension) {
        Log.d(TAG, "CreateTextViewCell started");

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

            if (isCellConst){
                cell.setTextColor(Color.GRAY);
            }
            else{
                cell.setTextColor(Color.BLACK);
            }

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
        Log.d(TAG, "OnCellClicked started");

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
