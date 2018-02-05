package com.gamesart.sudoku.sudoku;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

import com.android.vending.billing.IInAppBillingService;

public class ConsumePurchase extends AsyncTask {

    private final Handler _alertDialogBuilderHandler;
    private final int _productsSize;
    private final String _coinsPrice;
    private final String _token;
    private IInAppBillingService _service;
    private AppCompatActivity _activity;

    public ConsumePurchase(IInAppBillingService service, AppCompatActivity activity, Handler alertDialogBuilderHandler, int productsSize, String coinsPrice, String token){
        _service = service;
        _activity = activity;
        _alertDialogBuilderHandler = alertDialogBuilderHandler;
        _productsSize = productsSize;
        _coinsPrice = coinsPrice;
        _token = token;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try{
            _service.consumePurchase(3, _activity.getPackageName(), _token);
        }catch (RemoteException ex){
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        new PurchaseProducts(_service, _activity, _alertDialogBuilderHandler, _productsSize, _coinsPrice).execute();
    }
}
