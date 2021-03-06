package com.example.aravind.wififinal;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.example.aravind.wififinal.ListofDevices.DeviceActionListener;

public class MainActivity extends Activity implements ChannelListener, DeviceActionListener {

   public static final String TAG = "wifi_direct_demo";
   private WifiP2pManager manager;
   private boolean isWifiP2pEnabled = false;
   private boolean retryChannel = false;

   private final IntentFilter intentFilter = new IntentFilter();
   private Channel channel;
   private BroadcastReceiver receiver = null;
   public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
       this.isWifiP2pEnabled = isWifiP2pEnabled;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.main);
       intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
       intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
       intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
       intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

       manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
       channel = manager.initialize(this, getMainLooper(), null);
   }
   @Override
   public void onResume() {
       super.onResume();
       receiver = new broadcastReceiver(manager, channel, this);
       registerReceiver(receiver, intentFilter);
   }

   @Override
   public void onPause() {
       super.onPause();
       unregisterReceiver(receiver);
   }
   public void resetData() {
       ListofDevices fragmentList = (ListofDevices) getFragmentManager()
               .findFragmentById(R.id.frag_list);
       DeviceDetails fragmentDetails = (DeviceDetails) getFragmentManager()
               .findFragmentById(R.id.frag_detail);
       if (fragmentList != null) {
           fragmentList.clearPeers();
       }
       if (fragmentDetails != null) {
           fragmentDetails.resetViews();
       }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.action_items, menu);
       return true;
   }
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
           case R.id.atn_direct_enable:
               if (manager != null && channel != null) {
                   startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
               } else {
                   Log.e(TAG, "channel or manager is null");
               }
               return true;
           case R.id.atn_direct_discover:
               if (!isWifiP2pEnabled) {
                   Toast.makeText(MainActivity.this, R.string.p2p_off_warning,
                           Toast.LENGTH_SHORT).show();
                   return true;
               }
               final ListofDevices fragment = (ListofDevices) getFragmentManager()
                       .findFragmentById(R.id.frag_list);
               fragment.onInitiateDiscovery();
               manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                   @Override
                   public void onSuccess() {
                       Toast.makeText(MainActivity.this, "Discovery Initiated",
                               Toast.LENGTH_SHORT).show();
                   }

                   @Override
                   public void onFailure(int reasonCode) {
                       Toast.makeText(MainActivity.this, "Discovery Failed : " + reasonCode,
                               Toast.LENGTH_SHORT).show();
                   }
               });
               return true;
           default:
               return super.onOptionsItemSelected(item);
       }
   }

   @Override
   public void showDetails(WifiP2pDevice device) {
       DeviceDetails fragment = (DeviceDetails) getFragmentManager()
               .findFragmentById(R.id.frag_detail);
       fragment.showDetails(device);

   }

   @Override
   public void connect(WifiP2pConfig config) {
       manager.connect(channel, config, new ActionListener() {

           @Override
           public void onSuccess() {

           }

           @Override
           public void onFailure(int reason) {
               Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                       Toast.LENGTH_SHORT).show();
           }
       });
   }

   @Override
   public void disconnect() {
       final DeviceDetails fragment = (DeviceDetails) getFragmentManager()
               .findFragmentById(R.id.frag_detail);
       fragment.resetViews();
       manager.removeGroup(channel, new ActionListener() {

           @Override
           public void onFailure(int reasonCode) {
               Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
           }

           @Override
           public void onSuccess() {
               fragment.getView().setVisibility(View.GONE);
           }

       });
   }

   @Override
   public void onChannelDisconnected() {
       if (manager != null && !retryChannel) {
           Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
           resetData();
           retryChannel = true;
           manager.initialize(this, getMainLooper(), this);
       } else {
           Toast.makeText(this,
                   "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                   Toast.LENGTH_LONG).show();
       }
   }

   @Override
   public void cancelDisconnect() {
       if (manager != null) {
           final ListofDevices fragment = (ListofDevices) getFragmentManager()
                   .findFragmentById(R.id.frag_list);
           if (fragment.getDevice() == null
                   || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
               disconnect();
           } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                   || fragment.getDevice().status == WifiP2pDevice.INVITED) {

               manager.cancelConnect(channel, new ActionListener() {

                   @Override
                   public void onSuccess() {
                       Toast.makeText(MainActivity.this, "Aborting connection",
                               Toast.LENGTH_SHORT).show();
                   }

                   @Override
                   public void onFailure(int reasonCode) {
                       Toast.makeText(MainActivity.this,
                               "Connect abort request failed. Reason Code: " + reasonCode,
                               Toast.LENGTH_SHORT).show();
                   }
               });
           }
       }

   }
}
