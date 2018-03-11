package com.gamesart.sudoku.sudoku;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoardActivity extends AppCompatActivity implements RewardedVideoAdListener {

    private static final String TAG = "BoardActivity";
    private static final String FILENAME = "games_art_sudoku_saved_board";
    private static final String FILENAME_GAME_DATA = "games_art_sudoku_game_data";
    private static int _videoAdCounter = 5;

    public static final int DEBUG_MODE = 0;
    public static final int REQUEST_CODE = 1001;
    public static final String PRODUCT_ID = "solve_random_cell_coins";
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final int BILLING_RESPONSE_RESULT_OK = 0;

    private IInAppBillingService _service;

    ServiceConnection _serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            _service = IInAppBillingService.Stub.asInterface(service);
            _setupDone = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            _service = null;
        }
    };

    private boolean _setupDone = false;
    private int _sectionColRowLength = 9;
    private int _fullBoardLength = 81;
    private TextView lastSelectedCell = null;
    private int[][] _board = null;
    private TextView[][] _textViews = null;
    private int _screenWidth = 0;
    private Integer _level;
    private int _counter = 0;
    private ArrayList<CellData> _cells = null;
    private SettingsData _settings;
    private TipsEngine _tipsEngine;
    private TextView _solveCellBtn;
    private TextView _findErrorBtn;
    private final AppCompatActivity _activity = this;
    private Handler _alertDialogBuilderHandler;
    private TextView _currentCoinsTitle;
    private TextView _buyCoinsBtn;
    private RewardedVideoAd mRewardedVideoAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_board);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        MobileAds.initialize(this, "ca-app-pub-8402023979328526~3171238260");

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);
        loadRewardedVideoAd();

        _alertDialogBuilderHandler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj != null){
                    AlertDialog.Builder builder = (AlertDialog.Builder)msg.obj;
                    builder.create().show();
                }
            }
        };

        _tipsEngine = new TipsEngine(this);
        _solveCellBtn = findViewById(R.id.solveCellBtn);
        _solveCellBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _solveCellBtn.setEnabled(false);
                if (_tipsEngine.hasCoins()){
                    SolveRandomCell();
                }
                else{
                    ShowGetCoinsDialog();
                }
            }
        });
        _findErrorBtn = findViewById(R.id.findErrorBtn);
        _findErrorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FindCellWithError();
            }
        });
        _buyCoinsBtn = findViewById(R.id.buyCoinsBtn);
        _buyCoinsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowGetCoinsDialog();
            }
        });

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        List<ResolveInfo> intentServices = getPackageManager().queryIntentServices(serviceIntent, 0);
        if (intentServices != null && !intentServices.isEmpty()) {
            bindService(serviceIntent, _serviceConnection, Context.BIND_AUTO_CREATE);
        }

        LoadSettingsData();

        _level = getIntent().getIntExtra(FirstViewActivity.LevelParamName, 1);
        boolean _loadGame = getIntent().getBooleanExtra(FirstViewActivity.LoadGameParamName, false);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        _screenWidth = size.x;

        LoadAllTextViewsToArray(_screenWidth);

        if (!_loadGame) {
            _cells = new ArrayList<>(_fullBoardLength);
            StartNewGame(true, false);
        }
        else {
            TryLoadSavedGame();
            TryLoadGameData();
            StartNewGame(false, false);
        }

        _currentCoinsTitle = findViewById(R.id.currentCoinsTitle);
        _currentCoinsTitle.setText(getString(R.string.currentCoins) + " " + _tipsEngine.getCurrentNumberOfTips());
        if (_counter == 0){
            _solveCellBtn.setEnabled(false);
        }
        else{
            _solveCellBtn.setEnabled(true);
        }
        if (_tipsEngine.getCurrentNumberOfTips() == 0){
            _buyCoinsBtn.setVisibility(View.VISIBLE);
        }

        InitKeyboard();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _solveCellBtn.setEnabled(true);
        if (requestCode == REQUEST_CODE){
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            if (responseCode == BILLING_RESPONSE_RESULT_OK){
                _tipsEngine.userPurchasedCoins();
                _currentCoinsTitle.setText(getString(R.string.currentCoins) + " " + _tipsEngine.getCurrentNumberOfTips());
                _buyCoinsBtn.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        mRewardedVideoAd.resume(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mRewardedVideoAd.pause(this);
        super.onPause();

        SaveCurrentBoardState();
        SaveCurrentGameData();
    }

    @Override
    protected void onDestroy() {
        mRewardedVideoAd.destroy(this);
        super.onDestroy();
        if (_serviceConnection != null){
            unbindService(_serviceConnection);
        }
        _serviceConnection = null;
        _service = null;
    }

    private void ShowGetCoinsDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.solutionIsIncorrect);
        final String[] items = new String[]{
                getString(R.string.buy100coins),
                getString(R.string.watchVideo)
        };
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selection = Arrays.asList(items).get(i);

                if (selection.equals(getString(R.string.buy100coins))){
                    if (_setupDone){
                        new QueryProducts(_service, _activity, _alertDialogBuilderHandler, _tipsEngine).execute();
                    }
                }
                if (selection.equals(getString(R.string.watchVideo))){
                    ShowVideoAd();
                }
            }
        });

        builder.create().show();
    }

    private void ShowVideoAd() {
        if (mRewardedVideoAd.isLoaded()) {
            mRewardedVideoAd.show();
        }else{
            loadRewardedVideoAd();
            if (mRewardedVideoAd.isLoaded()) {
                mRewardedVideoAd.show();
            }
        }
    }

    private void loadRewardedVideoAd() {
        if (DEBUG_MODE == 1){
            mRewardedVideoAd.loadAd("ca-app-pub-3940256099942544/5224354917",
                    new AdRequest.Builder().build());
        }else{
            mRewardedVideoAd.loadAd("ca-app-pub-8402023979328526/7489955284",
                    new AdRequest.Builder().build());
        }
    }

    private void SolveRandomCell(){
        Lock l = new ReentrantLock();
        l.lock();
        try{
            int currentTipsAmount = _tipsEngine.getCurrentNumberOfTips();
            if (currentTipsAmount == 0)
                return;

            ArrayList<TextView> _emptyTextViews = GetEmptyTextViews();
            TextView requiredCell = GetRandomCell(_emptyTextViews);
            if (requiredCell == null){
                ShowWinStateDialog();
                return;
            }

            int[][] solvedBoard = GetSolvedBoard();
            int row = Integer.valueOf(String.valueOf(String.valueOf(requiredCell.getTag()).charAt(0)));
            int col = Integer.valueOf(String.valueOf(String.valueOf(requiredCell.getTag()).charAt(1)));
            int solvedDigit = solvedBoard[row][col];
            String tag = String.valueOf(requiredCell.getTag());
            updateCell(requiredCell, solvedDigit, tag);
            UpdateCellDataWithConst(solvedDigit, row, col);
            _counter--;
            if (DEBUG_MODE != 1){
                _tipsEngine.decreaseTipsAmount();
            }
            _currentCoinsTitle.setText(getString(R.string.currentCoins) + " " + _tipsEngine.getCurrentNumberOfTips());

            if (_counter == 0){
                _solveCellBtn.setEnabled(false);
                ShowWinStateDialog();
            }
            else{
                _solveCellBtn.setEnabled(true);
            }
            if (_tipsEngine.getCurrentNumberOfTips() == 0){
                _buyCoinsBtn.setVisibility(View.VISIBLE);
            }
        }
        finally {
            l.unlock();
        }
    }

    private void UpdateCellDataWithConst(int solvedDigit, int row, int col) {
        for (int index = 0; index < _fullBoardLength; index++){
            CellData cd = _cells.get(index);
            if (cd.getRow() == row && cd.getColumn() == col){
                cd.setIsCellConst("1");
                cd.setCellDigit(solvedDigit);
            }
        }
    }

    private void EnableTipButtons(boolean enable){
        _solveCellBtn.setEnabled(enable);
        _findErrorBtn.setEnabled(enable);
    }

    private int[][] GetSolvedBoard() {
        int[][] temp = DeepCopy(_board);
        SudokuSolver solver = new SudokuSolver();
        solver.Solve(temp);
        return solver.GetSolvedBoard();
    }

    private TextView GetRandomCell(ArrayList<TextView> textViews) {
        int length = textViews.size();
        if (length <= 0)
            return null;
        int randomNumber = new Random().nextInt(length);
        return textViews.get(randomNumber);
    }

    @NonNull
    private ArrayList<TextView> GetEmptyTextViews() {
        ArrayList<TextView> _emptyTextViews = new ArrayList<>();
        for (int row = 0; row < _sectionColRowLength; row++) {
            for (int col = 0; col < _sectionColRowLength; col++) {
                TextView temp = _textViews[row][col];
                SetDefaultBackground(temp);
                String text = String.valueOf(temp.getText());
                if ("".equals(text)){
                    _emptyTextViews.add(temp);
                }
            }
        }
        return _emptyTextViews;
    }

    private void updateCell(TextView requiredCell, int solvedDigit, String tag) {
        requiredCell.setText(String.valueOf(solvedDigit));
        requiredCell.setTextColor(Color.GRAY);
        String newTag = tag.substring(0,3) + "1";
        requiredCell.setTag(newTag);
        String regularOrAlternate = String.valueOf(tag.charAt(2));
        if ("1".equals(regularOrAlternate)){
            requiredCell.setBackground(getDrawable(R.drawable.sudoku_cell_tip_solve));
        }
        else{
            requiredCell.setBackground(getDrawable(R.drawable.sudoku_cell_alt_tip_solve));
        }
    }

    private void FindCellWithError(){
        Lock l = new ReentrantLock();
        l.lock();
        try{
            int[][] solvedBoard = GetSolvedBoard();
            boolean isFoundError = false;
            for (int row = 0; row < _sectionColRowLength; row++) {
                for (int col = 0; col < _sectionColRowLength; col++) {
                    TextView temp = _textViews[row][col];
                    String tag = String.valueOf(temp.getTag());
                    if ("1".equals(String.valueOf(tag.charAt(3)))){
                        continue;
                    }
                    String text = String.valueOf(temp.getText());
                    if (text.equals(String.valueOf(solvedBoard[row][col]))){
                        continue;
                    }
                    if ("".equals(text)){
                        continue;
                    }
                    isFoundError = true;
                    String regularOrAlternate = String.valueOf(tag.charAt(2));
                    if ("1".equals(regularOrAlternate)){
                        temp.setBackground(getDrawable(R.drawable.sudoku_cell_error));
                    }
                    else{
                        temp.setBackground(getDrawable(R.drawable.sudoku_cell_alt_error));
                    }
                    break;
                }
                if (isFoundError){
                    break;
                }
            }
            if (!isFoundError){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.noErrors);
                builder.setMessage(R.string.goodJob);
                builder.setPositiveButton(R.string.okButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // DO NOTHING
                    }
                });
                builder.create().show();
            }
        }
        finally {
            l.unlock();
        }
    }

    private void LoadSettingsData() {
        BufferedReader br = null;
        StringBuilder builder = null;
        InputStream stream = null;

        try{
            stream = openFileInput(FirstViewActivity.SETTINGS_FILENAME);
            br = new BufferedReader(new InputStreamReader(stream));
            builder = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null){
                builder.append(line);
            }
            stream.close();
        }
        catch (IOException ex){
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

        _settings = new SettingsData();
        _settings.setSameNumbers(false);
        _settings.setGreenCross(false);
        _settings.setRedSquares(false);
        Gson gson = new GsonBuilder().create();
        if (builder != null) {
            _settings = gson.fromJson(builder.toString(), new TypeToken<SettingsData>() {}.getType());
        }
    }

    private void LoadAllTextViewsToArray(int screenWidth) {
        GridLayout mainGridLayout = findViewById(R.id.mainGridLayout);
        _textViews = new TextView[_sectionColRowLength][_sectionColRowLength];
        int cellSide = screenWidth / _sectionColRowLength;
        for (int index = 0; index < _sectionColRowLength; index++){
            GridLayout layout = (GridLayout)((RelativeLayout)mainGridLayout.getChildAt(index)).getChildAt(0);
            for (int textViewIndex = 0; textViewIndex < _sectionColRowLength; textViewIndex++){
                TextView child = (TextView)layout.getChildAt(textViewIndex);
                String tag = String.valueOf(child.getTag());
                String row = String.valueOf(tag.charAt(0));
                String col = String.valueOf(tag.charAt(1));
                _textViews[Integer.valueOf(row)][Integer.valueOf(col)] = child;
                child.setWidth(cellSide);
                child.setHeight(cellSide);
            }
        }
    }

    private void TryLoadSavedGame() {

        BufferedReader br = null;
        StringBuilder builder = null;
        InputStream stream = null;

        try{
            stream = openFileInput(FILENAME);
            br = new BufferedReader(new InputStreamReader(stream));
            builder = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null){
                builder.append(line);
            }
            stream.close();
        }
        catch (IOException ex){

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

        if (builder == null) {
            return;
        }

        String result = builder.toString();
        Gson gson = new GsonBuilder().create();
        _cells = gson.fromJson(result, new TypeToken<ArrayList<CellData>>(){}.getType());

        _board = new int[_sectionColRowLength][_sectionColRowLength];
        for (int index = 0; index < _fullBoardLength; index++){
            CellData temp = _cells.get(index);
            if (temp.getIsCellConst().equals("1"))
                _board[temp.getRow()][temp.getColumn()] = temp.getCellDigit();
            else
                _board[temp.getRow()][temp.getColumn()] = 0;
        }
    }

    private void TryLoadGameData() {

        BufferedReader br = null;
        StringBuilder builder = null;
        InputStream stream = null;

        try{
            stream = openFileInput(FILENAME_GAME_DATA);
            br = new BufferedReader(new InputStreamReader(stream));
            builder = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null){
                builder.append(line);
            }
            stream.close();
        }
        catch (IOException ex){

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

        if (builder == null) {
            return;
        }

        String result = builder.toString();
        Gson gson = new GsonBuilder().create();
        SavedGameData sgd = gson.fromJson(result, new TypeToken<SavedGameData>(){}.getType());
        _level = sgd.getLevel();
    }

    private void SaveCurrentGameData() {
        SavedGameData sgd = new SavedGameData();
        sgd.setLevel(_level);

        Gson gson = new GsonBuilder().create();
        String result = gson.toJson(sgd);

        FileOutputStream fos = null;
        try{
            fos = openFileOutput(FILENAME_GAME_DATA, Context.MODE_PRIVATE);
            fos.write(result.getBytes());
            fos.close();
        }catch (IOException ex){

        }finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void SaveCurrentBoardState() {

        ArrayList<CellData> cells = new ArrayList<>(_fullBoardLength);

        for (int row = 0; row < _sectionColRowLength; row++) {
            for (int col = 0; col < _sectionColRowLength; col++) {
                CellData item = new CellData();
                TextView cell = _textViews[row][col];
                String cellStringValue = cell.getText().toString();
                if (cellStringValue.equals(""))
                    item.setCellDigit(0);
                else
                    item.setCellDigit(Integer.valueOf(cellStringValue));

                String tag = String.valueOf(cell.getTag());
                String isConst = String.valueOf(tag.charAt(3));
                item.setRow(row);
                item.setColumn(col);
                item.setIsCellConst(isConst);
                cells.add(item);
            }
        }

        Gson gson = new GsonBuilder().create();
        String result = gson.toJson(cells);

        FileOutputStream fos = null;
        try{
            fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(result.getBytes());
            fos.close();
        }catch (IOException ex){

        }finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void InitKeyboard() {

        GridLayout keyboardGrid = findViewById(R.id.gameKeyboard);
        int length = keyboardGrid.getChildCount();
        int height = _screenWidth / _sectionColRowLength;
        int numericWidth = height * 7 / 5;
        int commandWidth = height * 2;
        for (int index = 0; index < length; index++){
            TextView keyboardKey = (TextView)keyboardGrid.getChildAt(index);
            keyboardKey.setHeight(height);
            SetKeyboardClick(keyboardKey);
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

    private void SetKeyboardClick(final TextView keyboardKey) {

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
                        CaseDigitSelected(text, currentText, isConst);
                        break;
                    case "Clear":
                        CaseClearSelected(currentText, isConst);
                        break;
                    case "New Game":
                        NewGameRestartClicked(true, false);
                        break;
                    case "Reset":
                        NewGameRestartClicked(false, true);
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
        OnCellClicked(lastSelectedCell);
    }

    private void CaseDigitSelected(String text, String currentText, char isConst) {

        if (lastSelectedCell == null)
            return;
        if (isConst == '1')
            return;
        if (currentText.equals(""))
            _counter--;
        lastSelectedCell.setText(text, TextView.BufferType.EDITABLE);
        OnCellClicked(lastSelectedCell);
        CheckWinState();
    }

    private void CheckWinState(){
        if (_counter != 0)
            return;

        int[][] temp = DeepCopy(_board);
        boolean isWinStateTrue = IsUserBoardValid(temp);

        if (isWinStateTrue) {
            //_tipsEngine.userWonGame();
            //_currentCoinsTitle.setText(_tipsEngine.getCurrentNumberOfTips());
            ShowWinStateDialog();
        }
        else{
            ShowLoseStateDialog();
        }
    }

    private void ShowLoseStateDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.solutionIsIncorrect);
        final String[] items = new String[]{
                getString(R.string.continueCurrent),
                getString(R.string.resetCurrent),
                getString(R.string.startNew),
                getString(R.string.selectLevel)
        };
        ShowAlertDialogOnGameOver(builder, items);
    }

    private void ShowWinStateDialog() {
        EnableTipButtons(false);
        setKeyboardEnabledDisabled(false);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.youWin);
        final String[] items = new String[]{
                getString(R.string.startNew),
                getString(R.string.selectLevel)
        };
        ShowAlertDialogOnGameOver(builder, items);
    }

    private boolean IsUserBoardValid(int[][] board) {
        //Check rows and columns
        for (int i = 0; i < board.length; i++) {
            BitSet bsRow = new BitSet(_sectionColRowLength);
            BitSet bsColumn = new BitSet(_sectionColRowLength);
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == 0 || board[j][i] == 0) continue;
                if (bsRow.get(board[i][j] - 1) || bsColumn.get(board[j][i] - 1))
                    return false;
                else {
                    bsRow.set(board[i][j] - 1);
                    bsColumn.set(board[j][i] - 1);
                }
            }
        }
        //Check within 3 x 3 grid
        for (int rowOffset = 0; rowOffset < _sectionColRowLength; rowOffset += 3) {
            for (int columnOffset = 0; columnOffset < _sectionColRowLength; columnOffset += 3) {
                BitSet threeByThree = new BitSet(_sectionColRowLength);
                for (int i = rowOffset; i < rowOffset + 3; i++) {
                    for (int j = columnOffset; j < columnOffset + 3; j++) {
                        if (board[i][j] == 0) continue;
                        if (threeByThree.get(board[i][j] - 1))
                            return false;
                        else
                            threeByThree.set(board[i][j] - 1);
                    }
                }
            }
        }
        return true;
    }

    private void ShowAlertDialogOnGameOver(AlertDialog.Builder builder, final String[] items) {

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selection = Arrays.asList(items).get(i);
                GameOverMenuItemClicked(dialogInterface, selection);
            }
        });

        builder.create().show();

        if (_videoAdCounter == 0){
            _videoAdCounter = 5;
            ShowVideoAd();
        }
        else{
            _videoAdCounter--;
        }
    }

    private void GameOverMenuItemClicked(DialogInterface dialogInterface, String selection) {

        if (selection.equals(getString(R.string.startNew))){
            StartNewGame(true, false);
        }
        if (selection.equals(getString(R.string.selectLevel))){
            finish();
        }
        if (selection.equals(getString(R.string.resetCurrent))){
            StartNewGame(false, true);
        }
        if (selection.equals(getString(R.string.continueCurrent))){
            dialogInterface.dismiss();
        }
    }

    private int[][] DeepCopy(int[][] original) {

        if (original == null) {
            return null;
        }

        final int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
        }
        return result;
    }

    private void NewGameRestartClicked(final boolean newBoard, final boolean isResetRequested){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.newGameRestart));
        builder.setMessage(getString(R.string.areYouSure));
        builder.setPositiveButton(getString(R.string.yepp), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                StartNewGame(newBoard, isResetRequested);
            }
        });
        builder.setNegativeButton(getString(R.string.nope), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Do nothing
            }
        });
        builder.create().show();
    }

    private void StartNewGame(boolean newBoard, boolean isResetRequested) {

        _counter = 0;
        EnableTipButtons(true);
        if (newBoard || (_cells == null)){
            SudokuGenerator generator = new SudokuGenerator();
            _board = generator.GetBoard(_level);
            if (_cells == null)
                _cells = new ArrayList<>();
            else
                _cells.clear();
            for (int row = 0; row < _sectionColRowLength; row++){
                for (int col = 0; col < _sectionColRowLength; col++){
                    CellData item = new CellData();
                    item.setRow(row);
                    item.setColumn(col);
                    item.setCellDigit(_board[row][col]);
                    if (_board[row][col] == 0)
                        item.setIsCellConst("0");
                    else
                        item.setIsCellConst("1");
                    _cells.add(item);
                }
            }
        }

        int cellsCount = _cells.size();
        for(int index = 0; index < cellsCount; index++){
            CellData item = _cells.get(index);
            if (isResetRequested && item.getIsCellConst().equals("0"))
                item.setCellDigit(0);
            int digit = item.getCellDigit();
            int row = item.getRow();
            int col = item.getColumn();
            SetDefaultBackground(_textViews[row][col]);
            UpdateTextViewCell(_textViews[row][col], item.getIsCellConst());
            if (item.getIsCellConst().equals("0")) {
                _textViews[row][col].setTextColor(Color.BLACK);
                if (digit != 0)
                    _textViews[row][col].setText(String.valueOf(digit));
                else {
                    _textViews[row][col].setText("");
                    _counter++;
                }

                continue;
            }
            _textViews[row][col].setText(String.valueOf(digit));
            _textViews[row][col].setTextColor(Color.GRAY);
        }
    }

    private void UpdateTextViewCell(TextView cell, String isConst) {

        String cellTag = String.valueOf(cell.getTag());
        if (cellTag.length() == 4){
            cellTag = cellTag.substring(0, 3);
        }
        cellTag += isConst;
        cell.setTag(cellTag);

        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnCellClicked((TextView) view);
            }
        });
    }

    private void OnCellClicked(TextView view) {
        String selectedRow = String.valueOf(String.valueOf(view.getTag()).charAt(0));
        String selectedCol = String.valueOf(String.valueOf(view.getTag()).charAt(1));
        String selectedCellDigit = String.valueOf(view.getText());
        int selectedRowInt = Integer.valueOf(selectedRow);
        int selectedColInt = Integer.valueOf(selectedCol);
        int sectionRowStart = selectedRowInt / 3 * 3;
        int sectionColStart = selectedColInt / 3 * 3;

        for (int row = 0; row < _sectionColRowLength; row++) {
            for (int col = 0; col < _sectionColRowLength; col++) {
                TextView currentWorkCell = _textViews[row][col];
                String currentWorkCellDigit = String.valueOf(currentWorkCell.getText());

                if (selectedRow.equals(String.valueOf(row)) || selectedCol.equals(String.valueOf(col))){
                    if (_settings.showGreenCross()) {
                        currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell_selected_row_col));
                    }
                    else{
                        SetDefaultBackground(currentWorkCell);
                    }
                    if (currentWorkCell == view){
                        currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell_selected));
                        continue;
                    }
                    if (currentWorkCellDigit.equals(selectedCellDigit) && !currentWorkCellDigit.equals("")){
                        if (_settings.showRedSquares()) {
                            if (_settings.showGreenCross()){
                                currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell_selected_row_col_error));
                            }
                            else{
                                currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell_selected_same_number_error));
                            }
                        }
                        else{
                            if (_settings.showGreenCross()) {
                                currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell_selected_row_col));
                            }
                            else{
                                SetDefaultBackground(currentWorkCell);
                            }
                        }
                    }
                }
                else{
                    if (currentWorkCellDigit.equals(selectedCellDigit) && !currentWorkCellDigit.equals("")){
                        if (_settings.showSameNumbers()) {
                            currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell_selected_same_number));
                        }
                        else{
                            SetDefaultBackground(currentWorkCell);
                        }
                        if ((row >= sectionRowStart && row <= (sectionRowStart + 2)) && (col >= sectionColStart && col <= (sectionColStart + 2))){
                            if (currentWorkCellDigit.equals(selectedCellDigit) && !currentWorkCellDigit.equals("")){
                                if (_settings.showRedSquares()) {
                                    currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell_selected_same_number_error));
                                }
                            }
                        }
                    }
                    else{
                        SetDefaultBackground(currentWorkCell);
                    }
                }
            }
        }

        lastSelectedCell = view;

        char isConst = String.valueOf(view.getTag()).charAt(3);
        if (isConst == '1'){
            setKeyboardEnabledDisabled(false);
        }else{
            setKeyboardEnabledDisabled(true);
        }

    }

    private void setKeyboardEnabledDisabled(boolean setKeyboardEnabled) {
        GridLayout grid = findViewById(R.id.gameKeyboard);
        int length = grid.getChildCount();
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
                    key.setEnabled(setKeyboardEnabled);
                    break;
            }
        }
    }

    private void SetDefaultBackground(TextView currentWorkCell) {
        char backgroundState = String.valueOf(currentWorkCell.getTag()).charAt(2);
        if (backgroundState == '1'){
            currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell));
        }
        else{
            currentWorkCell.setBackground(getDrawable(R.drawable.sudoku_cell_alt));
        }
    }

    @Override
    public void onRewardedVideoAdLoaded() {

    }

    @Override
    public void onRewardedVideoAdOpened() {

    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoAdClosed() {
        loadRewardedVideoAd();
    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        _tipsEngine.userSawVideo();
        _currentCoinsTitle.setText(getString(R.string.currentCoins) + " " + _tipsEngine.getCurrentNumberOfTips());
        _buyCoinsBtn.setVisibility(View.INVISIBLE);
        _solveCellBtn.setEnabled(true);
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {
        loadRewardedVideoAd();
    }
}
