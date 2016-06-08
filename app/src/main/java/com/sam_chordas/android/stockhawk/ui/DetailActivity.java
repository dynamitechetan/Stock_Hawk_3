package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DetailActivity extends Activity {

    ChartView chartView;
    String[] mLabels;
    float[] mValues;
    String LOG_TAG = DetailActivity.class.getSimpleName();
    String stockSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        stockSymbol = getIntent().getStringExtra("symbol");

        chartView = (ChartView) findViewById(R.id.linechart);
        new getDataFromInternet().execute(stockSymbol);


    }

    private void fillChartWithData() {
        LineSet dataSet = new LineSet(mLabels, mValues);
        dataSet.setColor(getResources().getColor(android.R.color.primary_text_dark));
        chartView.setLabelsColor(getResources().getColor(android.R.color.primary_text_dark));
        chartView.setAxisColor(getResources().getColor(android.R.color.primary_text_dark));
        chartView.addData(dataSet);

        //SOO much math
        /**
         * int step must divide difference between max and min
         */
        final int STEP_COUNT= 10; //optional(min) step count = 10, but no more than 20=2*10 (19 is max)
        int max = getMax(mValues);
        int min = getMin(mValues);

        Log.d(LOG_TAG,"min "+min+", max "+max);
        int step;
        if ((max-min)<(2*STEP_COUNT)){
            step=1;
        }else {
            int difference=max-min;
            int rawStep = difference/STEP_COUNT;
            if ((difference % rawStep)==0){
                step=rawStep;
            }else {
                int remainder = difference % rawStep;
                // add one to remainder until it's rawStep
                while (remainder!=rawStep){
                    max=max+1;
                    remainder=remainder+1;
                    if (remainder==rawStep){
                        break;
                    }
                    min=min-1;
                    remainder=remainder+1;
                }
                step=rawStep;
            }
        }
        Log.d(LOG_TAG,"min "+min+", max "+max);

        chartView.setAxisBorderValues(min, max, step);
        chartView.show();
    }


    private class getDataFromInternet extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String symbol = params[0];

            StringBuilder urlStringBuilder = new StringBuilder();
            try {
                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol =\"" + symbol + "\"", "UTF-8"));
                urlStringBuilder.append(URLEncoder.encode("and startDate = \"" + getDateBefore30days() + "\" and endDate = \"" + getCurrentDate() + "\"", "UTF-8"));
                //            urlStringBuilder.append("&diagnostics=true&env=store://datatables.org/alltableswithkeys");
                // finalize the URL for the API query.
                urlStringBuilder.append("&format=json&env=store%3A%2F%2Fdatatables."
                        + "org%2Falltableswithkeys&callback=");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "StringBuilder unsuccessful");
            }

            String string = urlStringBuilder.toString();
            Log.d(LOG_TAG, "Get " + string);

            String getResponse = null;
            try {
                getResponse = fetchData(string);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (getResponse != null) {
                Log.d(LOG_TAG, "StringBuilder string = " + getResponse);
                return getResponse;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(LOG_TAG, "String " + s);
            if (s != null) {
                fillDataValues(s);
                fillChartWithData();
            } else {
                Toast.makeText(DetailActivity.this, getResources().getString(R.string.unable_to_get_data), Toast.LENGTH_SHORT).show();
                finish();
            }

        }

        private String getCurrentDate() {
            Calendar c = GregorianCalendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd");
            String today = df.format(c.getTime());
            return today;
        }

        private String getDateBefore30days() {
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -30);
            Date daysBeforeDate = cal.getTime();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return df.format(daysBeforeDate);
        }

        private String fetchData(String url) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void fillDataValues(String JSON) {

            JSONObject jsonObject = null;
            JSONArray resultsArray = null;
            try {
                jsonObject = new JSONObject(JSON);
                if (jsonObject != null && jsonObject.length() != 0) {
                    jsonObject = jsonObject.getJSONObject("query");
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
                    mLabels = new String[resultsArray.length()];
                    mValues = new float[resultsArray.length()];
                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            try {
                                String dateString = jsonObject.getString("Date");
                                float stockPriceAtCloseFloat = Float.parseFloat(jsonObject.getString("Close"));
                                //mLabels[i]=dateString;// too much info on axis
                                //mLabels[resultsArray.length()-i-1]= String.valueOf(-i);
                                if ((resultsArray.length() - i - 1) % 7 == 0) {
                                    mLabels[resultsArray.length() - i - 1] = dateString;
                                } else {
                                    mLabels[resultsArray.length() - i - 1] = "";
                                }
                                mValues[resultsArray.length() - i - 1] = stockPriceAtCloseFloat;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                    if (mLabels.length > 0) {
                        mLabels[mLabels.length - 1] = getResources().getString(R.string.today);
                    }
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "String to JSON failed: " + e);
            }
        }

    }

    private int getMax(float[] floats) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < floats.length; i++) {
            if (floats[i] > max) {
                max = (int) floats[i] + 1;
            }
        }
        return max;
    }

    private int getMin(float[] floats) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < floats.length; i++) {
            if (floats[i] < min) {
                min = (int) floats[i];
            }
        }
        return min;
    }


}
