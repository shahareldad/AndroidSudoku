package com.gamesart.sudoku.sudoku;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class TipsEngine {

    private static final String FILENAME = "games_art_sudoku_saved_coins";
    private final AppCompatActivity _activity;
    private int _currentNumberOfTips = 5;

    TipsEngine(AppCompatActivity activity){
        _activity = activity;

        BufferedReader br = null;
        InputStream stream = null;

        try{
            stream = _activity.openFileInput(FILENAME);
            br = new BufferedReader(new InputStreamReader(stream));
            String coins = br.readLine();
            _currentNumberOfTips = Integer.valueOf(coins);
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

    }

    int getCurrentNumberOfTips() {
        return _currentNumberOfTips;
    }

    void decreaseTipsAmount() {
        if (_currentNumberOfTips <= 0){
            return;
        }

        this._currentNumberOfTips--;
        saveCoinsState();
    }

    void userSawVideo(){
        _currentNumberOfTips += 5;

        saveCoinsState();
    }

    void userPurchasedCoins(){
        _currentNumberOfTips += 100;

        saveCoinsState();
    }

    private void saveCoinsState() {
        FileOutputStream fos = null;
        try{
            fos = _activity.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(String.valueOf(_currentNumberOfTips).getBytes());
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

    boolean hasCoins(){
        return _currentNumberOfTips > 0;
    }
}
