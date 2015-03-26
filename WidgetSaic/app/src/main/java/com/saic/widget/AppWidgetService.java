package com.saic.widget;

/**
 * Created by J.Brandon on 26/03/2015.
 */


import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileFilter;

public class AppWidgetService extends Service {

    public final static String APPWIDGET_SERVICE = "AppWidgetService";

    private static final int INVALID_ID = -1;

    private class MetaData {
        long id = INVALID_ID;
        String artist = null;
        String album = null;
        String title = null;
        String filename = null;
        long length = 0;
        Bitmap artBmp = null;
    }

    MetaData currentMeta = new MetaData();
    private boolean currentlyPlaying = false;

    private static UpdateMetaTask previousMeta = null;

    BroadcastReceiver receiver = null;



    public void onCreate() {
        makeReceiver();
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        makeReceiver();
        return START_STICKY;
    }

    public void onDestroy() {
        if (receiver != null)
            unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    private void makeReceiver() {
        if (receiver != null)
            unregisterReceiver(receiver);

        Config config = new Config(getApplicationContext());
        IntentFilter filter = new IntentFilter();
        filter.addAction(config.getPlayer().getMetaChangedAction());
        filter.addAction(config.getPlayer().getPlaystateChangedAction());

        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Config config = new Config(context);
                PlayerConfig player = config.getPlayer();
                String action = intent.getAction();

                if (player.getMetaChangedAction().equals(action)) {
                    updateMeta(config, context, intent);
                }

                if (player.getPlaystateChangedAction().equals(action)) {
                    updatePlaystate(config, context, intent);
                }

            }
        };
        registerReceiver(receiver, filter);
    }



    private void updateMeta(Config config,
                            Context context,
                            Intent intent) {
        if (previousMeta != null)
            previousMeta.cancel(true);

        UpdateMetaArgs args = new UpdateMetaArgs();
        args.config = config;
        args.context = context;
        args.intent = intent;

        previousMeta = new UpdateMetaTask();
        previousMeta.execute(args);
    }


    private void updatePlaystate(Config config,
                                 Context context,
                                 Intent intent) {
        try {
            String playState = config.getPlayer().getPlaystateChangedPlaying();
            currentlyPlaying = intent.getBooleanExtra(playState, false);
            Bitmap playButton = getPlayButton(currentlyPlaying);
            updateWidgetPlay(playButton);
        } catch (Exception e) {
            Log.e("AppWidgetCup", "Error updating playstate.", e);
        }
    }

    private Bitmap getPlayButton(boolean playing) {
        int imgId = playing ? R.drawable.ic_pause : R.drawable.ic_play;
        return BitmapFactory.decodeResource(getResources(), imgId);
    }

    private void updateWidget(String artist,
                              String title,
                              Bitmap artBmp) {
        AppWidgetManager appWidgetManager
                = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(),
                R.layout.appwidget);

        views.setTextViewText(R.id.artistView, artist);
        views.setTextViewText(R.id.titleView,  title);

        ExampleAppWidgetProvider.addButtonsToRemoteViews(this, views);

        if (artBmp != null)
            views.setImageViewBitmap(R.id.albumArtButton, artBmp);

        ComponentName thisWidget = new ComponentName(this, ExampleAppWidgetProvider.class);

        appWidgetManager.updateAppWidget(thisWidget, views);
    }


    private void updateWidgetPlay(Bitmap playButton) {
        AppWidgetManager appWidgetManager
                = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(),
                R.layout.appwidget);

        views.setImageViewBitmap(R.id.playPauseButton, playButton);

        ExampleAppWidgetProvider.addButtonsToRemoteViews(this, views);

        ComponentName thisWidget = new ComponentName(this, ExampleAppWidgetProvider.class);
        appWidgetManager.updateAppWidget(thisWidget, views);
    }


    private void resetWidget() {
        String artist = getResources().getString(R.string.noartist);
        String title = getResources().getString(R.string.notitle);
        Bitmap artBmp = getDefaultArt(this);

        updateWidget(artist, title, artBmp);
    }

    private Bitmap getDefaultArt(Context context) {
        return BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_blankalbum);
    }



    private class UpdateMetaArgs {
        Config config;
        Context context;
        Intent intent;
    }


    private class UpdateMetaTask extends AsyncTask<UpdateMetaArgs, Void, Void> {

        protected Void doInBackground(UpdateMetaArgs... args) {
            try {
                updateMeta(args[0].config,
                        args[0].context,
                        args[0].intent);
            } catch (Exception e) {
                Log.e("TeaCupReceiver", "Error updating meta.", e);
            }
            return null;
        }

        private void updateMeta(Config config, Context context, Intent intent) {
            MetaData meta = null;
            synchronized (currentMeta) {
                meta = getMeta(config, context, intent);
                if (meta.artist != null &&
                        meta.title != null &&
                        !isCancelled()) {
                    updateWidget(meta.artist,
                            meta.title,
                            null);
                }
                currentMeta = meta;
            }

            if (meta != null &&
                    meta.artist != null &&
                    meta.title != null &&
                    !isCancelled()) {
                Bitmap artBmp = getArtBmp(config, context, meta);

                synchronized (currentMeta) {
                    if (!isCancelled() &&
                            meta == currentMeta) {
                        currentMeta.artBmp = artBmp;
                        updateWidget(meta.artist,
                                meta.title,
                                meta.artBmp);
                    }
                }
            }

        }

        private MetaData getMeta(Config config,
                                 Context context,
                                 Intent intent) {
            String idField = config.getPlayer().getMetaChangedId();
            long id = intent.getLongExtra(idField, INVALID_ID);

            MetaData meta = new MetaData();

            if (id  != INVALID_ID) {
                String selectionArgs[] = {
                        Long.toString(id)
                };
                String projection[] = {
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DURATION
                };
                String selection = MediaStore.Audio.Media._ID + " = ?";
                ContentResolver resolver = context.getContentResolver();
                Cursor result = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null);
                if (result.getCount() > 0) {
                    result.moveToFirst();
                    meta = new MetaData();
                    meta.id = id;
                    meta.artist = result.getString(0);
                    meta.album = result.getString(1);
                    meta.title = result.getString(2);
                    meta.filename = result.getString(3);
                    meta.length = result.getLong(4);
                }
            }

            return meta;
        }

        private Bitmap getArtBmp(Config config,
                                 Context context,
                                 MetaData meta) {
            Bitmap artBmp = null;

            boolean getEmbeddedArt = config.getEmbeddedArt();
            boolean getDirectoryArt = config.getDirectoryArt();

            if (getEmbeddedArt)
                artBmp = getFileEmbeddedArt(meta.filename);
            if (artBmp == null && getDirectoryArt)
                artBmp = getImageFromDirectory(meta.filename);
            if (artBmp == null) {
                artBmp = getDefaultArt(context);
            }

            return artBmp;
        }

        private Bitmap getImageFromDirectory(String filename) {
            Bitmap artBmp = null;

            File file = new File(filename);
            String directory = file.getParent();

            if (directory != null) {
                FileFilter imageFilter = new FileFilter() {
                    boolean found = false;

                    public boolean accept(File file) {
                        if (found) {
                            return false;
                        } else {
                            String filename = file.getName();
                            found = filename.endsWith(".jpg") ||
                                    filename.endsWith(".jpeg") ||
                                    filename.endsWith(".bmp") ||
                                    filename.endsWith(".png") ||
                                    filename.endsWith(".gif");
                            return found;
                        }
                    }
                };

                File[] files = new File(directory).listFiles(imageFilter);
                for (int i = 0; i < files.length && artBmp == null; ++i) {
                    artBmp = AlbumArtFactory.readFile(files[i]);
                }
            }
            return artBmp;
        }


        private Bitmap getFileEmbeddedArt(String filename) {
            Bitmap artBmp = null;

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(filename);
            byte[] artArray = retriever.getEmbeddedPicture();
            if (artArray != null) {
                artBmp = AlbumArtFactory.readBytes(artArray);
            }

            return artBmp;
        }


    }



}

