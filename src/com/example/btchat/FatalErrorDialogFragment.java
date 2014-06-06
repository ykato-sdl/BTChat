package com.example.btchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class FatalErrorDialogFragment extends DialogFragment {
    private final static String ID_MESSAGE_KEY = "com.example.btchat.FatalErrorDialogFragment.id_message";

    public static FatalErrorDialogFragment newInstance(int id_message) {
        FatalErrorDialogFragment frag = new FatalErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ID_MESSAGE_KEY, id_message);
        frag.setArguments(args);
        frag.setCancelable(false);
        return frag;
    }

    public static void show(Activity activity, int id_message) {
        newInstance(id_message).show(activity.getFragmentManager(), "fatalErrorDialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        Bundle args = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.fatal_error_dialog_title);
        builder.setMessage(args.getInt(ID_MESSAGE_KEY));
        builder.setPositiveButton(R.string.dialog_ok_button_label,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                });
        return builder.create();
    }
}
