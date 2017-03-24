package com.analog.learningandroid;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.LinearLayout;

/**
 * Created by jtgre on 16/02/2017.
 */

public class GattDebugFragment extends DialogFragment {

    private LinearLayout parent;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        parent = new LinearLayout(getContext());

        builder.setTitle("GATT Services");
        //builder.setView(inflater.inflate(R.layout.fragment_gatt_debug, null));
        parent.addView(ControlActivity.mGattServicesList);
        builder.setView(parent);


        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        parent.removeAllViews();
    }
}
