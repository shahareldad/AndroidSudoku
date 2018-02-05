package com.gamesart.sudoku.sudoku;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class QueryPurchasedItems extends AsyncTask {

    private final AppCompatActivity _activity;
    private final IInAppBillingService _service;
    private final Handler _alertDialogBuilderHandler;
    private final int _productsSize;
    private final String _coinsPrice;
    private String _token;

    QueryPurchasedItems(IInAppBillingService service, AppCompatActivity context, Handler alertDialogBuilderHandler, int productsSize, String coinsPrice){
        _activity = context;
        _service = service;
        _alertDialogBuilderHandler = alertDialogBuilderHandler;
        _productsSize = productsSize;
        _coinsPrice = coinsPrice;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        try{
            Bundle ownedItems = _service.getPurchases(3, _activity.getPackageName(), "inapp", null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == BoardActivity.BILLING_RESPONSE_RESULT_OK) {
                ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    String purchaseData = purchaseDataList.get(i);
                    try{
                        JSONObject o = new JSONObject(purchaseData);
                        _token = o.getString("purchaseToken");
                    }catch (JSONException ex){

                    }
                }

                // if continuationToken != null, call getPurchases again
                // and pass in the token to retrieve more items
            }
        }catch (RemoteException ex){

        }

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        new ConsumePurchase(_service, _activity, _alertDialogBuilderHandler, _productsSize, _coinsPrice, _token).execute();
    }
}
