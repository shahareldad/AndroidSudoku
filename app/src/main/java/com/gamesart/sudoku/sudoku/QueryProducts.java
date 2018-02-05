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

public class QueryProducts extends AsyncTask {

    private final Handler _alertDialogBuilderHandler;
    private final AppCompatActivity _activity;
    private final IInAppBillingService _service;
    private final TipsEngine _tipsEngine;
    private int _productsSize = 0;
    private String _coinsPrice = "0";

    QueryProducts(IInAppBillingService service, AppCompatActivity context, Handler alertDialogBuilderHandler, TipsEngine tipsEngine){
        _activity = context;
        _service = service;
        _alertDialogBuilderHandler = alertDialogBuilderHandler;
        _tipsEngine = tipsEngine;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try{
            ArrayList<String> skuList = new ArrayList<> ();
            skuList.add(BoardActivity.PRODUCT_ID);
            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
            Bundle skuDetails = _service.getSkuDetails(3, _activity.getPackageName(), BoardActivity.ITEM_TYPE_INAPP, querySkus);
            int responseCode = skuDetails.getInt("RESPONSE_CODE");
            if (responseCode == BoardActivity.BILLING_RESPONSE_RESULT_OK){
                ArrayList<String> products = skuDetails.getStringArrayList("DETAILS_LIST");
                if (products != null){
                    for (String item: products){
                        try {
                            JSONObject obj = new JSONObject(item);
                            String sku = obj.getString("productId");
                            String price = obj.getString("price");
                            if (sku.equals(BoardActivity.PRODUCT_ID)){
                                _coinsPrice = price;
                            }

                        }catch (JSONException ex){

                        }
                    }
                    _productsSize = products.size();
                }
            }

        }catch (RemoteException ex) {
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        new QueryPurchasedItems(_service, _activity, _alertDialogBuilderHandler, _productsSize, _coinsPrice).execute();
    }
}
