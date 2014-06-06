package com.example.btchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity implements BTCommHandler.Listener {
    private final static String TAG = "MainActivity";

    private ArrayList<String> chatList = null;
    private final static String CHATLIST_KEY = "com.example.btchat.MainActivity.chatList";

    private TextView connectionStatusView;
    private ProgressBar connectionProgress;
    private ListView chatView;
    private ArrayAdapter<String> chatAdapter;
    private EditText inputText;
    private Button sendButton;

    private BluetoothAdapter btAdapter;

    private final static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private final UUID uuid = UUID.fromString(SPP_UUID);

    private final static int REQUEST_ENABLE_BT = 1111;
    private final static int REQUEST_SELECT_DEVICE = 2222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
            chatList = savedInstanceState.getStringArrayList(CHATLIST_KEY);
        if (chatList == null)
            chatList = new ArrayList<String>();

        setupUI();
        disableInput();
        setupBT();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
        outState.putStringArrayList(CHATLIST_KEY, chatList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case R.id.action_clear:
            Log.i(TAG, "menu: Clear");
            chatAdapter.clear();
            return true;
        case R.id.action_start_server:
            Log.i(TAG, "menu: Start Server");
            startServer();
            return true;
        case R.id.action_connect:
            Log.i(TAG, "menu: Connect");
            startActivityForResult(new Intent(this, BTSelectionActivity.class),
                    REQUEST_SELECT_DEVICE);
            return true;
        case R.id.action_disconnect:
            Log.i(TAG, "menu: Disconnect");
            if (socket != null && out != null) {
                out.println("DISCONNECT:");
                stopReception();
                disconnect();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupUI() {
        connectionStatusView = (TextView) findViewById(R.id.connection_status);

        connectionProgress = (ProgressBar) findViewById(R.id.connection_progress);
        connectionProgress.setIndeterminate(false);

        chatView = (ListView) findViewById(R.id.chat_view);
        chatAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, chatList);
        chatView.setAdapter(chatAdapter);
        chatView.smoothScrollToPosition(chatAdapter.getCount());

        inputText = (EditText) findViewById(R.id.input_text);

        sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString();
                if (text.length() > 0)
                    sendText(text);
            }
        });
    }

    private void enableInput() {
        sendButton.setEnabled(true);
    }

    private void disableInput() {
        sendButton.setEnabled(false);
    }

    private void setupBT() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null)
            FatalErrorDialogFragment.show(this, R.string.no_bt_alert_message);
        else if (!btAdapter.isEnabled())
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT);
        else
            setupBT1();
    }

    private void setupBT1() {}

    public void onActivityResult(int reqCode, int resCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        switch (reqCode) {
        case REQUEST_ENABLE_BT:
            if (resCode == Activity.RESULT_OK)
                setupBT1();
            else if (resCode == Activity.RESULT_CANCELED)
                FatalErrorDialogFragment.show(this, R.string.bt_needed_alert_message);
            break;
        case REQUEST_SELECT_DEVICE:
            if (resCode == Activity.RESULT_OK) {
                BluetoothDevice device = (BluetoothDevice) data
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "device=" + device);
                connect(device);
            }
            break;
        }
    }

    // Handling Connection

    private Handler handler = new BTCommHandler(this);
    private BluetoothSocket socket = null;
    private BufferedReader in = null;
    private PrintWriter out = null;

    private void connect(final BluetoothDevice device) {
        ensureDisconnected();
        new Thread() {
            public void run() {
                Message message;
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                    message = handler.obtainMessage(BTCommHandler.MESSAGE_CONNECTION_STARTED);
                    message.obj = device;
                    handler.sendMessage(message);
                    socket.connect();
                    message = handler.obtainMessage(BTCommHandler.MESSAGE_CONNECTION_ESTABLISHED);
                    message.obj = device;
                    handler.sendMessage(message);
                }
                catch (IOException e) {}
            }
        }.start();
    }

    public void onConnectionStarted(BluetoothDevice device) {
        Log.i(TAG, "onConnectionStarted");
        connectionStatusView.setText("Connecting to " + device.getName());
        connectionProgress.setIndeterminate(true);
    }

    public void onConnectionEstablished(BluetoothDevice device) {
        Log.i(TAG, "onConnectionEstablished");
        connectionStatusView.setText("Connected to " + device.getName());
        connectionProgress.setIndeterminate(false);
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (IOException e) {}
        enableInput();
        startReception();
    }

    private void disconnect() {
        ensureDisconnected();
        disableInput();
        connectionStatusView.setText("Disconnected");
    }

    private void ensureDisconnected() {
        Log.i(TAG, "ensureDisconnected");
        try {
            if (socket != null && socket.isConnected())
                socket.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
        catch (IOException e) {}
        socket = null;
        in = null;
        out = null;
    }

    private void sendText(String text) {
        chatAdapter.add(btAdapter.getName() + ": " + text);
        chatView.smoothScrollToPosition(chatAdapter.getCount());
        inputText.getText().clear();
        out.println("TEXT:" + text);
    }

    private ReaderThread reader = null;

    private void startReception() {
        Log.i(TAG, "startReception");
        reader = new ReaderThread();
        reader.start();
    }

    private void stopReception() {
        Log.i(TAG, "stopReception");
        if (reader != null)
            reader.terminate();
        reader = null;
    }

    private class ReaderThread extends Thread {
        private boolean stopped = false;

        public void run() {
            try {
                while (true) {
                    while (!stopped && !in.ready())
                        Thread.sleep(100);
                    if (stopped)
                        break;
                    String text = in.readLine();
                    Message message = handler.obtainMessage(BTCommHandler.MESSAGE_RECEIVED);
                    message.obj = text;
                    handler.sendMessage(message);
                }
                Log.i(TAG, "reader terminated");
            }
            catch (Exception e) {
                e.printStackTrace();
                Message message = handler.obtainMessage(BTCommHandler.MESSAGE_ERROR);
                handler.sendMessage(message);
            }
        }

        public void terminate() {
            stopped = true;
        }
    }

    public void onReceived(String line) {
        Log.i(TAG, "onReceived: line=" + line);
        if (line.startsWith("TEXT:")) {
            String from = socket.getRemoteDevice().getName();
            chatAdapter.add(from + ": " + line.substring(5));
            chatView.smoothScrollToPosition(chatAdapter.getCount());
        }
        else if (line.startsWith("DISCONNECT:")) {
            stopReception();
            disconnect();
        }
    }

    public void onReceptionError() {
        Log.i(TAG, "onReceptionError");
        stopReception();
        disconnect();
    }

    private void startServer() {
        if (btAdapter.isDiscovering())
            btAdapter.cancelDiscovery();
        ensureDisconnected();
        new Thread() {
            public void run() {
                Message message;
                try {
                    message = handler.obtainMessage(BTCommHandler.MESSAGE_SERVER_STARTED);
                    handler.sendMessage(message);
                    BluetoothServerSocket servSock = btAdapter.listenUsingRfcommWithServiceRecord(
                            btAdapter.getName(), uuid);
                    socket = servSock.accept(30000);
                    message = handler.obtainMessage(BTCommHandler.MESSAGE_SERVER_CONNECTED);
                    handler.sendMessage(message);
                    servSock.close();
                }
                catch (IOException e) {
                    message = handler.obtainMessage(BTCommHandler.MESSAGE_SERVER_TIMEOUT);
                    handler.sendMessage(message);
                }
            }
        }.start();
    }

    public void onServerStarted() {
        Log.i(TAG, "onServerStarted");
        connectionStatusView.setText("Server Started");
        connectionProgress.setIndeterminate(true);
    }

    public void onServerConnected() {
        Log.i(TAG, "onServerConnected");
        onConnectionEstablished(socket.getRemoteDevice());
    }

    public void onServerTimeout() {
        Log.i(TAG, "onServerTimeout");
        connectionStatusView.setText("Server Timeout");
        connectionProgress.setIndeterminate(false);
    }
}
