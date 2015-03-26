package com.saic.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.Locale;
import java.util.Random;

/**
 * Created by J.Brandon on 26/03/2015.
 */
public class AppWidgetConfiguration extends Activity {

    private AppWidgetConfiguration self = this;

    private int teaCupId;

    private long lastFMCacheSize = 0;

    private static String[] scrobblers = {
            "evil geniuses",
            "malevolent dictators",
            "funky soulsters",
            "randy terrorists",
            "purple dinosaurs",
            "blue whales",
            "territorial armchairs",
            "musical nerds",
            "marvelous luvvies",
            "skin and bones",
            "fantastic foxes",
            "hipsters and hackers",
            "manx cats",
            "heroic friendsters",
            "teetotal auctioneers",
            "ticklish sneezes",
            "jive bunnies",
            "squeezed lemons",
            "kinder surprises"
    };

    private static final String LASTFM_URL = "http://www.lastfm.com";

    protected void onCreate(Bundle savedInstanceState) {
        Log.d("TeaCup", "configuration oncreate");
        super.onCreate(savedInstanceState);
        Log.d("TeaCup", "configuration done super");

        Intent launchIntent = getIntent();
        Bundle extras = launchIntent.getExtras();
        if (extras != null) {
            teaCupId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        } else {
            teaCupId = AppWidgetManager.INVALID_APPWIDGET_ID;
        }

        Log.d("TeaCup", "configuration set content view");
        setContentView(R.layout.configuration);

        Log.d("TeaCup", "configuration load config");
        Config config = new Config((Context)this);
        Log.d("TeaCup", "configuration write to activity");
        config.writeConfigToActivity(this);

        Log.d("TeaCup", "configuration adjust view");
        showHideCustomOptions(config.getPlayer().getPlayerId());


        Log.d("TeaCup", "set up buttons");

        Button ok = (Button) findViewById(R.id.okbutton);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Config config = new Config(self);
                config.writeConfigToSharedPreferences(self);

                ServiceStarter.restartService(getApplicationContext());

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        teaCupId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });

        Button cancel = (Button) findViewById(R.id.cancelbutton);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        teaCupId);

                setResult(RESULT_CANCELED, resultValue);
                finish();
            }
        });
    }

    public void onClickSelectPlayer(View view) {
        RadioGroup playerSelect = (RadioGroup) findViewById(R.id.selectPlayerRadioGroup);
        if (playerSelect.getVisibility() == View.VISIBLE)
            playerSelect.setVisibility(View.GONE);
        else
            playerSelect.setVisibility(View.VISIBLE);
    }

    public void onClickPlayerSelectRadioGroup(View view) {
        RadioGroup playerSelect = (RadioGroup) findViewById(R.id.selectPlayerRadioGroup);
        playerSelect.setVisibility(View.GONE);

        int checkedId = playerSelect.getCheckedRadioButtonId();
        RadioButton checkedButton = (RadioButton) findViewById(checkedId);

        TextView playerSelected = (TextView) findViewById(R.id.playerSelected);
        playerSelected.setText(checkedButton.getText());

        showHideCustomOptions(checkedId);
    }





    private void showHideCustomOptions(int selectedId) {
        View customPlayerOptions = (View) findViewById(R.id.customPlayerOptions);
        if (selectedId == R.id.customPlayer) {
            customPlayerOptions.setVisibility(View.VISIBLE);
        } else {
            customPlayerOptions.setVisibility(View.GONE);
        }
    }




}

