package com.example.btchat;

import java.lang.ref.WeakReference;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

public class BTCommHandler extends Handler {
    public final static int MESSAGE_CONNECTION_STARTED = 1;
    public final static int MESSAGE_CONNECTION_ESTABLISHED = 2;
    public final static int MESSAGE_SERVER_STARTED = 3;
    public final static int MESSAGE_SERVER_CONNECTED = 4;
    public final static int MESSAGE_SERVER_TIMEOUT = 5;
    public final static int MESSAGE_RECEIVED = 6;
    public final static int MESSAGE_ERROR = 7;

    public interface Listener {
        public void onConnectionStarted(BluetoothDevice device);
        public void onConnectionEstablished(BluetoothDevice device);
        public void onServerStarted();
        public void onServerConnected();
        public void onServerTimeout();
        public void onReceived(String text);
        public void onReceptionError();
    }

    private WeakReference<Listener> listenerRef;

    public BTCommHandler(Listener listener) {
        listenerRef = new WeakReference<Listener>(listener);
    }

    @Override
    public void handleMessage(Message message) {
        Listener listener = listenerRef.get();
        if (listener == null)
            return;
        switch (message.what) {
        case MESSAGE_CONNECTION_STARTED:
            listener.onConnectionStarted((BluetoothDevice)message.obj);
            break;
        case MESSAGE_CONNECTION_ESTABLISHED:
            listener.onConnectionEstablished((BluetoothDevice)message.obj);
            break;
        case MESSAGE_SERVER_STARTED:
            listener.onServerStarted();
            break;
        case MESSAGE_SERVER_CONNECTED:
            listener.onServerConnected();
            break;
        case MESSAGE_SERVER_TIMEOUT:
            listener.onServerTimeout();
            break;
        case MESSAGE_RECEIVED:
            listener.onReceived((String)message.obj);
            break;
        case MESSAGE_ERROR:
            listener.onReceptionError();
            break;
        }
    }
}
