package com.saic.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by J.Brandon on 26/03/2015.
 */
public class ServiceStarter extends BroadcastReceiver {

    public void onReceive(Context context, Intent intext) {
        restartService(context);
    }

    static public void restartService(Context context) {
        Intent i = new Intent(AppWidgetService.APPWIDGET_SERVICE);
        i.setClass(context, AppWidgetService.class);
        context.stopService(i);


        // only start if there are widgets to receive
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, ExampleAppWidgetProvider.class);
        if (manager.getAppWidgetIds(provider).length > 0)
            context.startService(i);
        else
            Log.d("AppWidget", "but there are no widgets...");
    }

    static public void stopService(Context context) {
        Intent i = new Intent(AppWidgetService.APPWIDGET_SERVICE);
        i.setClass(context, AppWidgetService.class);
        context.stopService(i);
    }
}

