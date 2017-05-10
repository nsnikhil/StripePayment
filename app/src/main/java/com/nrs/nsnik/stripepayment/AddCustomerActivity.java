package com.nrs.nsnik.stripepayment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nrs.nsnik.stripepayment.fragments.dialogFragments.LoadingDialogFragment;
import com.stripe.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Customer;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddCustomerActivity extends AppCompatActivity {

    @BindView(R.id.customerCardWidget) CardInputWidget mCustomerCardWidget;
    @BindView(R.id.customerAdd) Button mAddCustomer;
    @BindView(R.id.customerConatiner) LinearLayout mCustomerContainer;
    @BindView(R.id.customerToolbar) Toolbar mCustomerToolbar;
    LoadingDialogFragment mLoadingDialog;
    private static final String TEST_PUB_API_KEY = "pk_test_cHZ8p6lv1KldUz7RkWC50VEO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customer);
        ButterKnife.bind(this);
        initialize();
    }

    private void createCustomer() {
        Card mCard = mCustomerCardWidget.getCard();
        if (mCard == null) {
            messageDialog("Invalid Card Data");
        } else {
            try {
                mLoadingDialog.show(getSupportFragmentManager(), "wait");
                com.stripe.android.Stripe stripe = new com.stripe.android.Stripe(getApplicationContext(), TEST_PUB_API_KEY);
                stripe.createToken(mCard,
                        new TokenCallback() {
                            public void onSuccess(Token token) {
                                new createCustomerAsync().execute(token.getId());
                            }
                            public void onError(Exception error) {
                                toastView(error.getLocalizedMessage(), Toast.LENGTH_LONG);
                                mLoadingDialog.dismiss();
                            }
                        }
                );
            } catch (IllegalArgumentException e) {
                mLoadingDialog.dismiss();
                e.printStackTrace();
            }

        }
    }

    private class createCustomerAsync extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... params) {
            String id;
            Stripe.apiKey = "sk_test_vb9Wu57BSwTRcxB7wqa0tDjC";
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("description", "test Description");
            customerParams.put("source", params[0]);
            try {
                Customer c = Customer.create(customerParams);
                id = c.getId();
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException | APIException e) {
                e.printStackTrace();
                id = e.getMessage();
            }
            return id;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mLoadingDialog.dismiss();
            if(s.contains("cus_")){
                toastView("Customer Created Successfully",Toast.LENGTH_SHORT);
            }else {
                toastView(s,Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    private void listeners() {
        mAddCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createCustomer();
            }
        });
    }

    private void initialize() {
        setSupportActionBar(mCustomerToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mLoadingDialog = new LoadingDialogFragment();
        addOnConnection();
    }

    private void messageDialog(String message) {
        if (message != null) {
            AlertDialog.Builder messageDialog = new AlertDialog.Builder(AddCustomerActivity.this)
                    .setMessage(message);
            messageDialog.create().show();
        } else {
            toastView("Null", Toast.LENGTH_SHORT);
        }
    }



    private void addOnConnection() {
        if(checkConnection()){
            mAddCustomer.setEnabled(true);
            listeners();
        }else {
            removeOffConnection();
        }
    }

    private void removeOffConnection() {
        Snackbar.make(mCustomerContainer,"No Internet",Snackbar.LENGTH_INDEFINITE).setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOnConnection();
            }
        }).setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.colorPrimary)).show();
        mAddCustomer.setEnabled(true);
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
