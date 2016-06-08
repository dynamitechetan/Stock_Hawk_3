package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;


import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;


public class QuoteWidgetRemoteViewsService extends RemoteViewsService {

    private final String LOT_TAG=QuoteWidgetRemoteViewsService.class.getSimpleName();

    private static final String[] QUOTE_COLUMNS = {
            QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
            QuoteColumns.BIDPRICE,
            QuoteColumns.ISUP,
            QuoteColumns.ISCURRENT
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_SYMBOL = 1;
    private static final int INDEX_PERCENT_CHANGE = 2;
    private static final int INDEX_CHANGE = 3;
    private static final int INDEX_BIDPRICE = 4;
    private static final int INDEX_ISUP = 5;
    private static final int INDEX_ISCURRENT = 6;



    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.d(LOT_TAG, "onGetViewFactory called");
        return new QuoteWidgetRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    class QuoteWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private Cursor Data;
        private Context mContext;
        private int mAppWidgetId;

        public QuoteWidgetRemoteViewsFactory(Context context, Intent intent) {
            Log.d(LOT_TAG, "QuoteWidgetRemoteViewsFactory constructor called");
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {
            Log.d(LOT_TAG, "onDataSetChanged called");

            if (Data != null) {
                Data.close();
            }

            final long identityToken = Binder.clearCallingIdentity();

            Data = getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,QUOTE_COLUMNS,
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);

            Binder.restoreCallingIdentity(identityToken);

        }

        @Override
        public void onDestroy() {
            Log.d(LOT_TAG, "onDestroy called");
            if (Data != null) {
                Data.close();
            }
        }

        @Override
        public int getCount() {
            Log.d(LOT_TAG, "getCount called");
            return Data.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Log.d(LOT_TAG, "getViewAt called");
            // Get the layout
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            Data.moveToPosition(position);
            views.setTextViewText(R.id.stock_symbol, Data.getString(INDEX_SYMBOL));

            if (Data.getInt(Data.getColumnIndex(QuoteColumns.ISUP)) == 1) {
                views.setInt(R.id.change, getResources().getString(R.string.string_set_background_resource), R.drawable.percent_change_pill_green);
            } else {
                views.setInt(R.id.change, getResources().getString(R.string.string_set_background_resource), R.drawable.percent_change_pill_red);
            }

            if (Utils.showPercent) {
                views.setTextViewText(R.id.change, Data.getString(Data.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
            } else {
                views.setTextViewText(R.id.change, Data.getString(Data.getColumnIndex(QuoteColumns.CHANGE)));
            }

            final Intent fillInIntent = new Intent();
            fillInIntent.putExtra(getResources().getString(R.string.stock_symbol), Data.getString(Data.getColumnIndex(QuoteColumns.SYMBOL)));
            views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }


}
