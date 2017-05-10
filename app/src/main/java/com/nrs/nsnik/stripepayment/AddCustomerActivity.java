package com.nrs.nsnik.stripepayment;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.nrs.nsnik.stripepayment.adapters.AdapterCardList;
import com.nrs.nsnik.stripepayment.fragments.dialogFragments.LoadingDialogFragment;
import com.nrs.nsnik.stripepayment.interfaces.InterfaceClick;
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
import com.stripe.model.ExternalAccount;
import com.stripe.model.ExternalAccountCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddCustomerActivity extends AppCompatActivity implements InterfaceClick{

    @BindView(R.id.customerCardWidget) CardInputWidget mCustomerCardWidget;
    @BindView(R.id.customerAdd) Button mAddCustomer;
    @BindView(R.id.customerConatiner) LinearLayout mCustomerContainer;
    @BindView(R.id.customerToolbar) Toolbar mCustomerToolbar;
    @BindView(R.id.customerCardList) ListView mCardList;
    LoadingDialogFragment mLoadingDialog;
    private static final String TEST_PUB_API_KEY = "pk_test_cHZ8p6lv1KldUz7RkWC50VEO";
    private static final String NULL_VALUE = "N/A";

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
                mLoadingDialog =  new LoadingDialogFragment();
                mLoadingDialog.show(getSupportFragmentManager(), "wait");
                com.stripe.android.Stripe stripe = new com.stripe.android.Stripe(getApplicationContext(), TEST_PUB_API_KEY);
                stripe.createToken(mCard,
                        new TokenCallback() {
                            public void onSuccess(Token token) {
                                SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(AddCustomerActivity.this);
                                String custId = spf.getString(getResources().getString(R.string.prefcustid),NULL_VALUE);
                                if(custId.equalsIgnoreCase(NULL_VALUE)){
                                    new createCustomerAsync().execute(token.getId());
                                }else {
                                   new addCardAsync().execute(custId,token.getId());
                                }

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

    @Override
    public void removedCard() {
       loadCardList();
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
                SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(AddCustomerActivity.this);
                spf.edit().putString(getResources().getString(R.string.prefcustid),s).apply();
                loadCardList();
                toastView("Customer Created Successfully",Toast.LENGTH_SHORT);
            }else {
                toastView(s,Toast.LENGTH_LONG);
            }
        }
    }

    private class getCardList extends AsyncTask<String,Void,List<com.stripe.model.Card>>{
        @Override
        protected List<com.stripe.model.Card> doInBackground(String... params) {
            Stripe.apiKey = "sk_test_vb9Wu57BSwTRcxB7wqa0tDjC";
            Map<String, Object> cardParams = new HashMap<>();
            cardParams.put("object", "card");
            //List<String> cardLast4List = new ArrayList<>();
            List<com.stripe.model.Card> cardList = new ArrayList<>();
            try {
                Customer customer = Customer.retrieve(params[0]);
                ExternalAccountCollection accountCollection = customer.getSources().all(cardParams);
                List<ExternalAccount> externalAccountList = accountCollection.getData();
                for(int i=0;i<externalAccountList.size();i++){
                    com.stripe.model.Card source = (com.stripe.model.Card) customer.getSources().retrieve(externalAccountList.get(i).getId());
                    cardList.add(source);
                    //cardLast4List.add(source.getLast4());
                }
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException | CardException e) {
                e.printStackTrace();
            }
            return cardList;
        }

        @Override
        protected void onPostExecute(List<com.stripe.model.Card> cards) {
            super.onPostExecute(cards);
            mLoadingDialog.dismiss();
            AdapterCardList adapterCardList = new AdapterCardList(AddCustomerActivity.this,cards,AddCustomerActivity.this);
            mCardList.setAdapter(adapterCardList);
        }
    }

    private class addCardAsync extends AsyncTask<String,Void,Void>{

        @Override
        protected Void doInBackground(String... params) {
            Stripe.apiKey = "sk_test_vb9Wu57BSwTRcxB7wqa0tDjC";
            try {
                Customer customer = Customer.retrieve(params[0]);
                Map<String, Object> parameter = new HashMap<>();
                parameter.put("source", params[1]);
                customer.getSources().create(parameter);
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException | APIException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mLoadingDialog.dismiss();
            loadCardList();
            super.onPostExecute(aVoid);
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
        mCardList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                toastView(position+"",Toast.LENGTH_LONG);
            }
        });
    }

    private void initialize() {
        setSupportActionBar(mCustomerToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
            loadCardList();
        }else {
            removeOffConnection();
        }
    }

    private void loadCardList(){
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(AddCustomerActivity.this);
        String custId = spf.getString(getResources().getString(R.string.prefcustid),NULL_VALUE);
        if(!custId.equalsIgnoreCase(NULL_VALUE)){
            mLoadingDialog =  new LoadingDialogFragment();
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.show(getSupportFragmentManager(),"dialog");
            new getCardList().execute(custId);
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
