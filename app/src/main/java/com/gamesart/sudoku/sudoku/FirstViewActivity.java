package com.gamesart.sudoku.sudoku;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class FirstViewActivity extends AppCompatActivity {

    public static String LevelParamName = "level";
    public static String LoadGameParamName = "loadGame";
    public static String SETTINGS_FILENAME = "SUDOKU_SETTINGS";
    private SettingsData _settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_view);

        MobileAds.initialize(this, "ca-app-pub-8402023979328526~3171238260");

        AdView adViewTop = findViewById(R.id.adViewTop);
        AdRequest requestTop = new AdRequest.Builder().build();
        adViewTop.loadAd(requestTop);

        AdView adViewBottom = findViewById(R.id.adViewBottom);
        AdRequest requestBottom = new AdRequest.Builder().build();
        adViewBottom.loadAd(requestBottom);

        Button easyBtn = findViewById(R.id.easyGame);
        Button mediumBtn = findViewById(R.id.mediumGame);
        Button hardBtn = findViewById(R.id.hardGame);
        Button loadGame = findViewById(R.id.loadGame);
        Button gameSettings = findViewById(R.id.gameSettings);

        easyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewGameByLevel(1, false);
            }
        });

        mediumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewGameByLevel(2, false);
            }
        });

        hardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewGameByLevel(3, false);
            }
        });

        loadGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewGameByLevel(1, true);
            }
        });

        LoadSettingsData();
        final boolean[] checked = new boolean[3];
        checked[0] = _settings.showRedSquares();
        checked[1] = _settings.showGreenCross();
        checked[2] = _settings.showSameNumbers();
        final SettingsData settings = new SettingsData();
        settings.setRedSquares(_settings.showRedSquares());
        settings.setGreenCross(_settings.showGreenCross());
        settings.setSameNumbers(_settings.showSameNumbers());
        final Context context = this;
        gameSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.settings);
                final String[] items = new String[]{
                        getString(R.string.redSquares),
                        getString(R.string.greenCross),
                        getString(R.string.sameNumbers)
                };
                builder.setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int indexSelected, boolean isChecked) {
                        UpdateSettingssData(indexSelected, isChecked, items, settings);
                    }
                });
                builder.setPositiveButton(R.string.okButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SaveSettingsData(settings);
                        _settings.setRedSquares(settings.showRedSquares());
                        _settings.setGreenCross(settings.showGreenCross());
                        _settings.setSameNumbers(settings.showSameNumbers());
                    }
                });
                builder.create().show();
            }
        });
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

    private void SaveSettingsData(SettingsData settings) {
        Gson gson = new GsonBuilder().create();
        String result = gson.toJson(settings);

        FileOutputStream fos = null;
        try{
            fos = openFileOutput(FirstViewActivity.SETTINGS_FILENAME, Context.MODE_PRIVATE);
            fos.write(result.getBytes());
            fos.close();
        }
        catch (FileNotFoundException ex){
        }
        catch (IOException ex){
        }
        finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void UpdateSettingssData(int indexSelected, boolean isChecked, String[] items, SettingsData settings) {
        String selection = Arrays.asList(items).get(indexSelected);
        if (selection.equals(getString(R.string.redSquares))){
            settings.setRedSquares(isChecked);
        }
        if (selection.equals(getString(R.string.greenCross))){
            settings.setGreenCross(isChecked);
        }
        if (selection.equals(getString(R.string.sameNumbers))){
            settings.setSameNumbers(isChecked);
        }
    }

    private void startNewGameByLevel(int level, boolean loadGame) {
        Intent i = new Intent(FirstViewActivity.this, BoardActivity.class);
        i.putExtra(LevelParamName, level);
        i.putExtra(LoadGameParamName, loadGame);
        startActivity(i);
    }
}
