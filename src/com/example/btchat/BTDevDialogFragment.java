package com.example.btchat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;

public class BTDevDialogFragment extends DialogFragment {
    public interface Listener {
        public void onDeviceSelected(BluetoothDevice device);
    }

    private Listener listener;
    private BluetoothDevice device;

    public static BTDevDialogFragment newInstance(Listener listener, BluetoothDevice device) {
        BTDevDialogFragment frag = new BTDevDialogFragment();
        frag.listener = listener;
        frag.device = device;
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String message = "Name: " + device.getName() + "\nBonded: "
                + (device.getBondState() == BluetoothDevice.BOND_BONDED ? "Yes" : "No")
                + "\nAddress: " + device.getAddress();
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.device_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok_button_label,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                listener.onDeviceSelected(device);
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel_button_label,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        }).create();
    }
}
