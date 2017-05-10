package com.nrs.nsnik.stripepayment.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nrs.nsnik.stripepayment.R;
import com.nrs.nsnik.stripepayment.interfaces.InterfaceClick;
import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Customer;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class AdapterCardList extends BaseAdapter{

    private Context mContext;
    private List<com.stripe.model.Card> mList;
    private static final String NULL_VALUE = "N/A";
    InterfaceClick mClick;

    public AdapterCardList(Context context, List<com.stripe.model.Card> cardList,InterfaceClick click){
        mContext = context;
        mList = cardList;
        mClick = click;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MyViewHolder myViewHolder;
        if(convertView==null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.single_item_layout,parent,false);
            myViewHolder = new MyViewHolder(convertView);
            convertView.setTag(myViewHolder);
        }else {
            myViewHolder = (MyViewHolder) convertView.getTag();
        }
        myViewHolder.mCardNo.setText(mList.get(position).getLast4());
        myViewHolder.mCardEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buildDialog("Dou you want to edit this card",0,position);
            }
        });
        myViewHolder.mCardDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buildDialog("Dou you want to delete this card",1,position);
            }
        });
        return convertView;
    }

    private void buildDialog(final String message, final int key, final int position){
        AlertDialog.Builder card = new AlertDialog.Builder(mContext);
        card.setTitle("Warning").setMessage(message)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(key==0){
                    Toast.makeText(mContext,"Edit",Toast.LENGTH_SHORT).show();
                }else if(key==1){
                    SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(mContext);
                    new removeCardAsync().execute(String.valueOf(position), spf.getString(mContext.getResources().getString(R.string.prefcustid),NULL_VALUE));
                }
            }
        });
        card.create().show();
    }

    private class removeCardAsync extends AsyncTask<String,Void,Void>{

        @Override
        protected Void doInBackground(String... params) {
            Stripe.apiKey = "sk_test_vb9Wu57BSwTRcxB7wqa0tDjC";
            try {
                Customer customer = Customer.retrieve(params[1]);
                customer.getSources().retrieve(mList.get(Integer.parseInt(params[0])).getId()).delete();
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException | APIException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mClick.removedCard();
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.singleCardNo)
        TextView mCardNo;
        @BindView(R.id.singleCardEdit)
        ImageView mCardEdit;
        @BindView(R.id.singleCardDelete)
        ImageView mCardDelete;

        public MyViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }


}
