package com.example.btchat;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class BTSelectionActivity extends Activity implements BTDevDialogFragment.Listener {
    private final static String TAG = "ScanningActivity";

    private ArrayList<BluetoothDevice> devList = null;
    private final static String DEVLIST_KEY = "com.example.btchat.ScanningActivity.devList";

    private TextView scanningStatusView;
    private ProgressBar scanningProgress;
    private ListView devListView;
    private ArrayAdapter<BluetoothDevice> devListAdapter;
    private Button startStopButton;

    private BluetoothAdapter btAdapter = null;
    private BroadcastReceiver btSearchReceiver = null;

    private final static int REQUEST_ENABLE_BT = 1111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_scanning);

        if (savedInstanceState != null)
            devList = savedInstanceState.getParcelableArrayList(DEVLIST_KEY);
        if (devList == null)
            devList = new ArrayList<BluetoothDevice>();

        setupUI();
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (!btAdapter.isEnabled())
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT);
        else
            setupBT();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if (btAdapter != null)
            btAdapter.cancelDiscovery();
        if (btSearchReceiver != null)
            unregisterReceiver(btSearchReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
        outState.putParcelableArrayList(DEVLIST_KEY, devList);
    }

    public void onActivityResult(int reqCode, int resCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        switch (reqCode) {
        case REQUEST_ENABLE_BT:
            if (resCode == Activity.RESULT_OK)
                setupBT();
            else if (resCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, R.string.error_bluetooth_must_be_enabled, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            break;
        }
        super.onActivityResult(reqCode, resCode, data);
    }

    private void setupUI() {
        scanningStatusView = (TextView) findViewById(R.id.scanning_status);

        scanningProgress = (ProgressBar) findViewById(R.id.scanning_progress);
        scanningProgress.setIndeterminate(false);

        devListView = (ListView) findViewById(R.id.dev_list_view);
        devListAdapter = new ArrayAdapter<BluetoothDevice>(this, 0, devList) {
            @Override
            public View getView(int pos, View view, ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                BluetoothDevice dev = getItem(pos);
                String bonded = dev.getBondState() == BluetoothDevice.BOND_BONDED ? " *" : "";
                ((TextView) view.findViewById(android.R.id.text2)).setText(dev.getAddress());
                ((TextView) view.findViewById(android.R.id.text1)).setText(dev.getName() + bonded);
                return view;
            }
        };
        devListView.setAdapter(devListAdapter);
        devListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                BluetoothDevice device = devList.get(pos);
                Log.i(TAG, "onItemClick: dev=" + device);
                BTDevDialogFragment.newInstance(BTSelectionActivity.this, device).show(
                        getFragmentManager(), "BTDevDialogFragment");
            }
        });
        devListView.smoothScrollToPosition(devListAdapter.getCount());

        startStopButton = (Button) findViewById(R.id.start_stop_button);
        startStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btAdapter.isDiscovering())
                    btAdapter.cancelDiscovery();
                else {
                    devListAdapter.clear();
                    btAdapter.startDiscovery();
                }
            }
        });
    }

    private void setupBT() {
        Set<BluetoothDevice> devs = btAdapter.getBondedDevices();
        devListAdapter.addAll(devs);

        btSearchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.i(TAG, "onReceive: " + action);
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devListAdapter.add(device);
                    devListAdapter.notifyDataSetChanged();
                    devListView.smoothScrollToPosition(devListAdapter.getCount());
                }
                else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                    scanningProgress.setIndeterminate(true);
                    scanningStatusView.setText(R.string.status_scanning);
                    startStopButton.setText(R.string.stop_scanning_button_label);
                }
                else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                    scanningProgress.setIndeterminate(false);
                    scanningStatusView.setText(R.string.status_stopped_scanning);
                    startStopButton.setText(R.string.start_scanning_button_label);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(btSearchReceiver, filter);
    }

    public void onDeviceSelected(BluetoothDevice device) {
        if (btAdapter.isDiscovering())
            btAdapter.cancelDiscovery();
        Intent data = new Intent();
        data.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
