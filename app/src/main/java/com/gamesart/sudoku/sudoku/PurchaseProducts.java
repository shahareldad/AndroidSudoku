package com.gamesart.sudoku.sudoku;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

import com.android.vending.billing.IInAppBillingService;

/**
 * Created by shaharel on 05/02/2018.
 */

public class PurchaseProducts extends AsyncTask {

    private final AppCompatActivity _activity;
    private final int _productsSize;
    private final String _coinsPrice;
    private final Handler _alertDialogBuilderHandler;
    private final IInAppBillingService _service;

    PurchaseProducts(IInAppBillingService service, AppCompatActivity activity, Handler alertDialogBuilderHandler, int productsSize, String coinsPrice){
        _activity = activity;
        _alertDialogBuilderHandler = alertDialogBuilderHandler;
        _productsSize = productsSize;
        _coinsPrice = coinsPrice;
        _service = service;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        publishProgress("PurchaseProducts.doInBackground started");
        if (_productsSize > 0) {
            publishProgress("PurchaseProducts.doInBackground _productsSize > 0");
            AlertDialog.Builder builder = new AlertDialog.Builder(_activity);
            String priceQuestion = _activity.getString(R.string.buyCoins) + " " +  _coinsPrice + "?";
            builder.setTitle(R.string.noCoins);
            builder.setMessage(priceQuestion);
            builder.setPositiveButton(R.string.okButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try{
                        Bundle buyIntentBundle = _service.getBuyIntent(3, _activity.getPackageName(), BoardActivity.PRODUCT_ID, BoardActivity.ITEM_TYPE_INAPP, null);
                        PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                        if (pendingIntent != null) {
                            _activity.startIntentSenderForResult(pendingIntent.getIntentSender(), BoardActivity.REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
                        }
                    }catch (RemoteException | IntentSender.SendIntentException ex){
                    }
                }
            });
            builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            _alertDialogBuilderHandler.obtainMessage(0, builder).sendToTarget();
        }
        return null;
    }
}
