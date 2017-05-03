package com.nrs.nsnik.stripepayment;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nrs.nsnik.stripepayment.fragments.dialogFragments.LoadingDialogFragment;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.mainCardWidget) CardInputWidget mMainCadWidget;
    @BindView(R.id.mainToolbar) Toolbar mMainToolbar;
    @BindView(R.id.mainPay) Button mPay;
    @BindView(R.id.mainRelativeLayout) RelativeLayout mCoordinatorLayout;
    LoadingDialogFragment mLoadingDialog;
    private static final String TEST_PUB_API_KEY = "pk_test_TmY7jM0kEY3V7wi1dNBWItKJ";
    private static final String TEST_SEC_API_KEY = "sk_test_HOD340Hji6fxpxqcZn14AegJ";
    private static final String LIVE_PUB_API_KEY = "N/A";
    private static final String LIVE_SEC_API_KEY = "N/A";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initialize();
    }

    private void initialize() {
        setSupportActionBar(mMainToolbar);
        addOnConnection();
    }

    private void addOnConnection() {
        if(checkConnection()){
            mPay.setEnabled(true);
            listeners();
        }else {
            removeOffConnection();
        }
    }

    private void removeOffConnection() {
      Snackbar.make(mCoordinatorLayout,"No Internet",Snackbar.LENGTH_INDEFINITE).setAction("Retry", new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              addOnConnection();
          }
      }).setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.colorPrimary)).show();
        mPay.setEnabled(false);
    }

    private void listeners() {
        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pay();
            }
        });

    }

    private void pay() {
        Card mCard = mMainCadWidget.getCard();
        if (mCard == null) {
            messageDialog("Invalid Card Data");
        } else {
            try {
                mLoadingDialog = new LoadingDialogFragment();
                mLoadingDialog.show(getSupportFragmentManager(),"wait");
                Stripe stripe = new Stripe(getApplicationContext(), TEST_PUB_API_KEY);
                stripe.createToken(mCard,
                        new TokenCallback() {
                            public void onSuccess(Token token) {
                                new ChargeAsync().execute(token.getId());
                            }

                            public void onError(Exception error) {
                                toastView(error.getLocalizedMessage(), Toast.LENGTH_LONG);
                            }
                        }
                );
            }catch (IllegalArgumentException e){
                mLoadingDialog.dismiss();
                e.printStackTrace();
            }

        }

    }


    private class ChargeAsync extends AsyncTask<String,Void,String>{



        @Override
        protected String doInBackground(String... params) {
           return charge(TEST_SEC_API_KEY,params[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mLoadingDialog.dismiss();
            messageDialog(s);
        }
    }

    private String charge(String apiKey,String token){
        Map<String, Object> params = new HashMap<>();
        params.put("amount", 100);
        params.put("currency", "usd");
        params.put("description", "Example charge");
        params.put("source", token);
        try {
            Charge charge = Charge.create(params,apiKey);
            return charge.getDescription();
            //Charge charge= Charge.create(params);
        } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException | APIException e) {
            messageDialog(e.toString());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void messageDialog(String message) {
        if(message!=null) {
            AlertDialog.Builder messageDialog = new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message);
            messageDialog.create().show();
        }else {
            toastView("Null",Toast.LENGTH_SHORT);
        }
    }

    private boolean checkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void toastView(String message, final int length) {
        Toast.makeText(getApplicationContext(), message, length).show();
    }
}
