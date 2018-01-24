package com.gamesart.sudoku.sudoku;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class FirstViewActivity extends AppCompatActivity {

    public static String LevelParamName = "level";
    public static String LoadGameParamName = "loadGame";

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
    }

    private void startNewGameByLevel(int level, boolean loadGame) {
        Intent i = new Intent(FirstViewActivity.this, BoardActivity.class);
        i.putExtra(LevelParamName, level);
        i.putExtra(LoadGameParamName, loadGame);
        startActivity(i);
    }
}
