package com.nrs.nsnik.stripepayment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.nrs.nsnik.stripepayment.fragments.dialogFragments.LoadingDialogFragment;
import com.stripe.Stripe;
import com.stripe.android.model.Card;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Account;
import com.stripe.model.AccountCollection;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCardCollection;
import com.stripe.model.CustomerCollection;
import com.stripe.model.ExternalAccount;
import com.stripe.model.ExternalAccountCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {


    @BindView(R.id.mainToolbar)
    Toolbar mMainToolbar;
    @BindView(R.id.mainPay)
    Button mPay;
    @BindView(R.id.mainRelativeLayout)
    RelativeLayout mMainContainer;
    @BindView(R.id.mainAccountList)
    Spinner mAccountList;
    @BindView(R.id.mainCustomerList)
    Spinner mCustomerList;
    @BindView(R.id.mainAmount)
    TextInputEditText mAmount;
    @BindView(R.id.mainFee)
    TextInputEditText mFee;
    @BindView(R.id.mainAddCustomer)
    FloatingActionButton mAddCustomer;
    LoadingDialogFragment mLoadingDialog;
    List<String> mAccountIdList;
    private static final String TEST_PUB_API_KEY = "pk_test_cHZ8p6lv1KldUz7RkWC50VEO";
    private static final String TEST_SEC_API_KEY = "sk_test_vb9Wu57BSwTRcxB7wqa0tDjC";
    private static final String LIVE_PUB_API_KEY = "N/A";
    private static final String LIVE_SEC_API_KEY = "N/A";
    private static final String NULL_VALUE = "N/A";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initialize();
    }

    private void initialize() {
        setSupportActionBar(mMainToolbar);
        mLoadingDialog = new LoadingDialogFragment();
        addOnConnection();
    }

    private void addOnConnection() {
        if (checkConnection()) {
            mPay.setEnabled(true);
            listeners();
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.show(getSupportFragmentManager(), "wait");
            new createList().execute();
            SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            String custId = spf.getString(getResources().getString(R.string.prefcustid),NULL_VALUE);
            if(!custId.equalsIgnoreCase(NULL_VALUE)){
                new getCustomerData().execute(custId);
            }else {
               toastView("No account found add one!",Toast.LENGTH_LONG);
            }
        } else {
            removeOffConnection();
        }
    }

    private void removeOffConnection() {
        Snackbar.make(mMainContainer, "No Internet", Snackbar.LENGTH_INDEFINITE).setAction("Retry", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOnConnection();
            }
        }).setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary)).show();
        mPay.setEnabled(false);
    }

    private void listeners() {
        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validDetails()) {
                    SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    String custId = spf.getString(getResources().getString(R.string.prefcustid),NULL_VALUE);
                    if(!custId.equalsIgnoreCase(NULL_VALUE)){
                        chargeAccount account = new chargeAccount(mAmount.getText().toString(), mFee.getText().toString()
                                , mAccountIdList.get(mAccountList.getSelectedItemPosition()), custId);
                        account.execute();
                    }else {
                        toastView("No account found add one!",Toast.LENGTH_LONG);
                    }

                }
            }
        });
        mAddCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,AddCustomerActivity.class));
            }
        });
    }

    private class getCustomerData extends AsyncTask<String,Void,List<String>>{
        @Override
        protected List<String> doInBackground(String... params) {
            Stripe.apiKey = "sk_test_vb9Wu57BSwTRcxB7wqa0tDjC";
            Map<String, Object> cardParams = new HashMap<>();
            cardParams.put("object", "card");
            List<String> cardLast4List = new ArrayList<>();
            try {
                Customer customer = Customer.retrieve(params[0]);
                ExternalAccountCollection accountCollection = customer.getSources().all(cardParams);
                List<ExternalAccount> externalAccountList = accountCollection.getData();
                for(int i=0;i<externalAccountList.size();i++){
                    com.stripe.model.Card source = (com.stripe.model.Card) customer.getSources().retrieve(externalAccountList.get(i).getId());
                    cardLast4List.add(source.getLast4());
                }
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException | CardException e) {
                e.printStackTrace();
            }
            return cardLast4List;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, strings);
            mCustomerList.setAdapter(adapter);
        }
    }

    private class createList extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... params) {
            com.stripe.Stripe.apiKey = "sk_test_vb9Wu57BSwTRcxB7wqa0tDjC";
            Map<String, Object> accountParams = new HashMap<>();
            List<String> idList = new ArrayList<>();
            mAccountIdList = new ArrayList<>();
            try {
                AccountCollection accountCollection = Account.list(accountParams);
                List<Account> accountList = accountCollection.getData();
                for (Integer i = 0; i < accountList.size(); i++) {
                    if (accountList.get(i).getManaged()) {
                        idList.add(accountList.get(i).getEmail());
                        mAccountIdList.add(accountList.get(i).getId());
                    }
                }
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException | APIException e) {
                e.printStackTrace();
            }
            return idList;
        }

        @Override
        protected void onPostExecute(List<String> idList) {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, idList);
            mAccountList.setAdapter(arrayAdapter);
            mLoadingDialog.dismiss();
        }
    }

    private class createCustomerList extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... params) {
            Map<String, Object> customerParamas = new HashMap<>();
            List<String> customerId = new ArrayList<>();
            try {
                CustomerCollection collection = Customer.list(customerParamas);
                List<Customer> customerList = collection.getData();
                for (int i = 0; i < customerList.size(); i++) {
                    customerId.add(customerList.get(i).getId());
                }
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException | APIException e) {
                e.printStackTrace();
            }
            return customerId;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, strings);
            mCustomerList.setAdapter(adapter);
            mLoadingDialog.dismiss();
        }
    }

    private boolean validDetails() {
        if (mAmount.getText().toString().isEmpty() && mAmount.getText().toString().length() <= 0) {
            mAmount.requestFocus();
            mAmount.setError("Enter a amount to pay");
            return false;
        } else if (mFee.getText().toString().isEmpty() && mFee.getText().toString().length() <= 0) {
            mFee.requestFocus();
            mFee.setError("Enter application fee");
            return false;
        } else if (Integer.parseInt(mAmount.getText().toString()) < Integer.parseInt(mFee.getText().toString())) {
            toastView("Fee cannot be greater than amount", Toast.LENGTH_LONG);
            return false;
        }
        return true;
    }


    private class chargeAccount extends AsyncTask<String, Void, Void> {

        String mAmount,mFee,mAccId,mCustId;

        chargeAccount(String amount,String fee,String accId,String custId){
            mAmount = amount;
            mFee = fee;
            mAccId = accId;
            mCustId = custId;
        }

        @Override
        protected Void doInBackground(String... params) {
            com.stripe.Stripe.apiKey = "sk_test_vb9Wu57BSwTRcxB7wqa0tDjC";
            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", mAmount);
            chargeParams.put("currency", "usd");
            chargeParams.put("application_fee", mFee);
            chargeParams.put("description", "testcharge");
            chargeParams.put("on_behalf_of",   mAccId);
            chargeParams.put("customer", mCustId);

            Map<String, Object> destinationParams = new HashMap<>();
            destinationParams.put("account",  mAccId);
            chargeParams.put("destination", destinationParams);

            try {
                Charge.create(chargeParams);
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException | APIException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mLoadingDialog.dismiss();
        }
    }


    private void messageDialog(String message) {
        if (message != null) {
            AlertDialog.Builder messageDialog = new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message);
            messageDialog.create().show();
        } else {
            toastView("Null", Toast.LENGTH_SHORT);
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
