package com.analog.learningandroid;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * Created by jtgre on 20/03/2017.
 */

public class ConfigureDeviceFragment extends DialogFragment {

    private LinearLayout parent;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        parent = new LinearLayout(getContext());
        parent.setOrientation(LinearLayout.VERTICAL);

        builder.setTitle("Configure");
        parent.addView(ControlActivity.mDevConfigureList);
        parent.addView(ControlActivity.devAttributesListView);

        builder.setView(parent);

        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ControlActivity.pauseReads = false;
        ControlActivity.readLoop.run();
        parent.removeAllViews();
    }
}
