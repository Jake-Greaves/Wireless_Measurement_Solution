package com.analog.learningandroid;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//MAIN
public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter; //Basic BLE operations object declaration
    private BluetoothLeScanner mBluetoothLeScanner; //BLE scanner object declaration

    private boolean mScanning; //flag for current BLE scanning status

    private static final int RQS_ENABLE_BLUETOOTH = 1; //request number for activating Bluetooth if disabled. Used by callback once request is complete
    private static final int RQS_LOCATION_PERMISSION = 1; //request number for permission to use Location. Needed for BLE. Used by callback once request is complete

    Button btnScan; //Scan button object declaration
    ListView listViewLE; //List of found devices object declaration

    List<String> listBluetoothDevice; //return list from BLE scan
    ListAdapter adapterLeScanResult; //bridge between ListView and the data to be displayed

    private Handler mHandler; //handler object declaration. Used to implement time limited BLE scan due to drain on battery
    private static final long SCAN_PERIOD = 5000; //defines scan period after button is pressed. Declared in ms (10000 = 10s)

    boolean bleActivateEventOccured = false;

    Runnable r;

    HashMap<String, BluetoothDevice> DeviceHash = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {//entry point to program. On creation of MainActivity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,
                    "BLUETOOTH_LE not supported in this device!",
                    Toast.LENGTH_SHORT).show();
            finish(); //close app as BLE is unavailable
            return;
        }

        //Location services needed to use BLE
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                RQS_LOCATION_PERMISSION);

        getBluetoothAdapterAndLeScanner(); //initialize the declared BLE manager, adapter and scanner

        if (!mBluetoothAdapter.isEnabled()) {
            //to avoid event occurring twice in onResume
                bleActivateEventOccured = true;
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
        }

        // Ensure bluetooth adapter is not null
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show();
            finish(); //close app as BLE is unavailable
            return;
        }

        //set listener for scan button
        btnScan = (Button)findViewById(R.id.scan); //find button in resource class
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true); //begin device scan
            }
        });

        listViewLE = (ListView)findViewById(R.id.lelist); //find listView in resource class

        listBluetoothDevice = new ArrayList<>(); //instantiate scan results array
        adapterLeScanResult = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, listBluetoothDevice);
        listViewLE.setAdapter(adapterLeScanResult); //set adapter that maintains data
        listViewLE.setOnItemClickListener(scanResultOnItemClickListener); //callback when an item is clicked

        mHandler = new Handler(); //initialize handler for scanning

    } //onCreate

    //callback for item click handler
    AdapterView.OnItemClickListener scanResultOnItemClickListener =
            new AdapterView.OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final BluetoothDevice device = DeviceHash.get(parent.getItemAtPosition(position)); //get clicked device

                    //build display for small menu when item is clicked
                    String msg = device.getAddress() + "\n"
                            + device.getBluetoothClass().toString() + "\n"
                            + getBTDeviceType(device);

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(device.getName())
                            .setMessage(msg)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .setNeutralButton("CONNECT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final Intent intent = new Intent(MainActivity.this,
                                            ControlActivity.class);
                                    intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME,
                                            device.getName());
                                    intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS,
                                            device.getAddress());

                                    if (mScanning) {
                                        mBluetoothLeScanner.stopScan(scanCallback);
                                        mHandler.removeCallbacks(r);
                                        mScanning = false;
                                        btnScan.setEnabled(true);
                                    }
                                    startActivity(intent);
                                }
                            })
                            .show();

                }
            };

    //determine device type for display
    private String getBTDeviceType(BluetoothDevice d){
        String type;

        switch (d.getType()){
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;
            default:
                type = "unknown...";
        }

        return type;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled() && !bleActivateEventOccured) {
            bleActivateEventOccured = false;
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
            }
        }
    }

    //handles if request to enable bluetooth has been rejected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RQS_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        getBluetoothAdapterAndLeScanner(); //initialize the declared BLE manager, adapter and scanner

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    //method declares BLE manager, adapter and scanner
    private void getBluetoothAdapterAndLeScanner(){
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanning = false;
    }

    /*
    to call startScan (ScanCallback callback),
    Requires BLUETOOTH_ADMIN permission.
    Must hold ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get results.
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            listBluetoothDevice.clear(); //remove all found devices to begin fresh scan
            listViewLE.invalidateViews();

            // Stops scanning after a pre-defined scan period.
            r = new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(scanCallback);
                    listViewLE.invalidateViews();

                    Toast.makeText(MainActivity.this,
                            "Scan timeout",
                            Toast.LENGTH_LONG).show();

                    mScanning = false;
                    btnScan.setEnabled(true);
                }
            };
            mHandler.postDelayed(r, SCAN_PERIOD);

            mBluetoothLeScanner.startScan(scanCallback);
            mScanning = true;
            btnScan.setEnabled(false);
        } else {
            mBluetoothLeScanner.stopScan(scanCallback);
            mScanning = false;
            btnScan.setEnabled(true);
        }
    }

    //scan callback on scan completion
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            addBluetoothDevice(result.getDevice()); //add device to list
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(ScanResult result : results){
                addBluetoothDevice(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(MainActivity.this,
                    "onScanFailed: " + String.valueOf(errorCode),
                    Toast.LENGTH_LONG).show();
        }

        private void addBluetoothDevice(BluetoothDevice device){
            if(!listBluetoothDevice.contains(device.getName())){
                DeviceHash.put(device.getName(), device);
                listBluetoothDevice.add(device.getName());
                listViewLE.invalidateViews();
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RQS_LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!

                } else {

                    finish();
                }
                return;
            }
        }
    }
}