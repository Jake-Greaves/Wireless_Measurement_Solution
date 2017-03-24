package com.analog.learningandroid;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import org.xmlpull.v1.XmlPullParserException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class ControlActivity extends AppCompatActivity {

    private final static String TAG = ControlActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    public static BluetoothLeService mBluetoothLeService;
    static BluetoothGattCharacteristic rxServiceCharacteristic;

    TextView textViewState;
    static ExpandableListView mGattServicesList;
    static ExpandableListView mDevConfigureList;
    TextView dataLogView;

    DialogFragment gattDebug;
    DialogFragment configureDevice;

    //string buffer for bluetooth data
    String bleDataBuffer = "";
    String bleDataLog = "";
    String bleDataLogBuff;
    final int MAX_STRING_LENGTH = 3000;

    //graph variables
    private LineGraphSeries<DataPoint> series;
    private LineGraphSeries<DataPoint> series1;
    private LineGraphSeries<DataPoint> series2;
    private LineGraphSeries<DataPoint> series3;
    private LineGraphSeries<DataPoint> series4;
    private LineGraphSeries<DataPoint> series5;

    private static int maxX = 100;
    private static int minX = 0;
    private static int currX = 0;
    private boolean updateGraph = false;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<>();
    private MeasurementDevice Sense1000;

    List<String> devAttributes = new ArrayList<>(); //list of Strings to identify device attributes in config
    ListAdapter devAttributesAdapter; //bridge between ListView and the data to be displayed
    static ListView devAttributesListView;

    //xml schema for device. Will be read from bluetooth eventually
    String xml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE context [<!ELEMENT context "
            +"(device)*><!ELEMENT device (channel | attribute | debug-attribute)*><!ELEMENT "
            +"channel (scan-element?, attribute*)><!ELEMENT attribute EMPTY><!ELEMENT "
            +"scan-element EMPTY><!ELEMENT debug-attribute EMPTY><!ATTLIST context name "
            +"CDATA #REQUIRED description CDATA #IMPLIED><!ATTLIST device id CDATA "
            +"#REQUIRED name CDATA #IMPLIED><!ATTLIST channel id CDATA #REQUIRED type "
            +"(input|output) #REQUIRED name CDATA #IMPLIED><!ATTLIST scan-element index "
            +"CDATA #REQUIRED format CDATA #REQUIRED scale CDATA #IMPLIED><!ATTLIST "
            +"attribute name CDATA #REQUIRED filename CDATA #IMPLIED><!ATTLIST "
            +"debug-attribute name CDATA #REQUIRED>]><context name=\"tiny\" "
            +"description=\"Tiny IIOD\" >"

            +"<device id=\"iio:0\" name=\"TEMP_MODULE\" >"

            +"<channel id=\"temp0\" name=\"cold_junction\" type=\"input\" >"
            +"<scan-element index=\"0\" format=\"le:s32/32&gt;&gt;0\" />"
            +"<attribute name=\"Sensor\" />"
            +"<attribute name=\"SensorType\" />"
            +"<attribute name=\"Gain\" />"
            +"<attribute name=\"ExcitationCurrent\" />"
            +"<attribute name=\"ReferenceResistor\" />"
            +"<attribute name=\"TemperatureMax\" />"
            +"<attribute name=\"TemperatureMin\" />"
            +"</channel>"

            +"<channel id=\"temp1\" name=\"thermocouple\" type=\"input\" >"
            +"<scan-element index=\"1\" format=\"le:s32/32&gt;&gt;1\" />"
            +"<attribute name=\"Sensor\" />"
            +"<attribute name=\"SensorType\" />"
            +"<attribute name=\"Gain\" />"
            +"<attribute name=\"VBiasEnable\" />"
            +"<attribute name=\"TemperatureMin\" />"
            +"<attribute name=\"TemperatureMax\" />"
            +"</channel>"

            +"<attribute name=\"PowerMode\" />"
            +"<attribute name=\"OperationalMode\" />"
            +"<attribute name=\"FilterType\" />"
            +"<attribute name=\"FirFrequency\" />"
            +"<attribute name=\"FS\" />"
            +"<attribute name=\"TemperatureUnit\" />"

            +"<debug-attribute name=\"direct_reg_access\" />"
            +"</device></context>";

    InputStream in;

    //parsed xml list
    List<XmlSchemaParser.Device> xmlParsedList = new ArrayList<>();

    Handler handler;
    Handler streamHandler;
    static Runnable readLoop;
    static boolean dataReceived = false;
    static boolean pauseReads = false;
    boolean runnableStarted = false;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //handle gatt connection event
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState("Gatt Connected");
                displayData("Discovering Characteristics...");
            }
            //handle gatt disconnect event
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

                updateConnectionState("Gatt Disconnected");
                clearUI();
            }
            //handle gatt services discovered event
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface
                handleGattServices(mBluetoothLeService.getSupportedGattServices());

            }
            //handle characteristic changed event
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                //check if data received is the TX service
                String txFlag = intent.getStringExtra(BluetoothLeService.TX_DATA_FLAG);
                if(Boolean.parseBoolean(txFlag)) {
                    //store data to buffer
                    bleDataBuffer += intent.getStringExtra(BluetoothLeService.EXTRA_DATA);

                    //if data is complete
                    if(bleDataBuffer.contains("\n\r")) {

                        String dataString = bleDataBuffer.substring(0, bleDataBuffer.indexOf("\n")); //substring of complete data string w/out \n\r
                        //remove found data string from buffer
                        bleDataBuffer = bleDataBuffer.replaceFirst(dataString, "");
                        bleDataBuffer = bleDataBuffer.replaceFirst("\n\r", "");

                        //find tag value to determine nature of data. This is always the first char in string
                        String tagValue = dataString.substring(0,1);
                        //remove tag from data. If the tag isn't present, this data will be discarded anyway
                        dataString = dataString.replaceFirst(tagValue, "");

                        //determine action required based on tag. Tag and delimiter set here

                        switch (tagValue) {
                            case "s": streamDataReceived(dataString);
                                Log.d(TAG, "stream tag received");
                                break;
                            case "x": //xmlDataReceived(dataString);
                                Log.d(TAG, "xml tag received");
                                break;
                            case "r": readAttrReceived(dataString);
                                Log.d(TAG, "read Attribute tag received");
                                break;
                            default: Log.d(TAG, "Invalid tag received");
                        }
                        dataReceived = true;
                    }
                }
            }
        }
    };

    private void streamDataReceived(String dataString) {
        int dataCount = 0;
        int subStrPos = 0;

        //find all field delimiters ";" in string
        while(dataString.indexOf(";", subStrPos) >= 0) {
            subStrPos = dataString.indexOf(";", subStrPos) +1;
            dataCount++;
        }

        subStrPos = 0;
        bleDataLog += Integer.toString(currX) + ". ";

        for(int i = 0; i < dataCount; i++) {

            //cut down bleDataLog string to avoid large volumes of data
            if(bleDataLog.length() > MAX_STRING_LENGTH) {
                bleDataLogBuff = bleDataLog.substring(500);
                bleDataLog = bleDataLogBuff;
            }

            String dataVarBuffer = dataString.substring(subStrPos, dataString.indexOf(";"));
            dataString = dataString.replace(dataVarBuffer, "");
            dataString = dataString.replaceFirst(";", "");

            String graphLabel = dataVarBuffer.substring(0, dataVarBuffer.indexOf(":"));
            dataVarBuffer = dataVarBuffer.replace(graphLabel, "");
            Log.d(TAG, "onReceive: graphLabel = " + graphLabel);

            dataVarBuffer = dataVarBuffer.replace(":", "");
            String graphData = dataVarBuffer;
            Log.d(TAG, "onReceive: graphData = " + graphData);

            float currYValue = Float.parseFloat(graphData);

            bleDataLog += graphLabel + "\t" + graphData + "\t";


            //if graph and data is not froze for viewing
            if (!updateGraph) {
                //handle up to four separate sensor results
                if (i == 0) {
                    series.appendData(new DataPoint(currX, currYValue), true, maxX);
                    series.setTitle(graphLabel);
                } else if (i == 1) {
                    series1.appendData(new DataPoint(currX, currYValue), true, maxX);
                    series1.setTitle(graphLabel);
                } else if (i == 2) {
                    series2.appendData(new DataPoint(currX, currYValue), true, maxX);
                    series2.setTitle(graphLabel);
                } else if (i == 3) {
                    series3.appendData(new DataPoint(currX, currYValue), true, maxX);
                    series3.setTitle(graphLabel);
                } else if (i == 4) {
                    series4.appendData(new DataPoint(currX, currYValue), true, maxX);
                    series4.setTitle(graphLabel);
                } else if (i == 5) {
                    series5.appendData(new DataPoint(currX, currYValue), true, maxX);
                    series5.setTitle(graphLabel);
                }

                //add log data to log
                dataLogView.setText(bleDataLog);
            }
        }
        bleDataLog += "\r\n";

        if (!updateGraph) {
            //increment graph axis
            currX++;
        }
    }

    private void readAttrReceived(String dataString) {
        String device = null, channel = null, str, attr = null, value = null;
        //dump device value for now
        device = dataString.substring(0, dataString.indexOf(" "));
        dataString = dataString.replace(device, "");
        dataString = dataString.replaceFirst(" ", "");

        //check if read is channel or device attribute
        str = dataString.substring(0, dataString.indexOf(" "));

        if(str.equals("INPUT")) {
            dataString = dataString.replace(str, "");
            dataString = dataString.replaceFirst(" ", "");

            channel = dataString.substring(0, dataString.indexOf(" "));
            dataString = dataString.replace(channel, "");
            dataString = dataString.replaceFirst(" ", "");

            attr = dataString.substring(0, dataString.indexOf(" "));
            dataString = dataString.replace(attr, "");
            dataString = dataString.replaceFirst(" ", "");

            value = dataString;

            Sense1000.changeAttribute(channel, attr, value);
        }

        else {
            attr = str;
            dataString = dataString.replace(attr, "");
            dataString = dataString.replaceFirst(" ", "");

            value = dataString;

            Sense1000.changeAttribute(channel, attr, value);
        }
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
    }

    private void updateConnectionState(final String st) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewState.setText(st);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            textViewState.setText(data);
        }
    }

    // iterate through the supported GATT Services/Characteristics
    private void handleGattServices(List<BluetoothGattService> gattServices) {

        final String LIST_NAME = "NAME";
        final String LIST_UUID = "UUID";

        if (gattServices == null) return;
        String uuid;
        String unknownServiceString = "Unknown Service";
        String unknownCharaString = "Unknown Characteristic";
        String charaName;

        ArrayList<HashMap<String, String>> gattServiceData =
                new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();

                charaName = GattAttributes.lookup(uuid, unknownCharaString);
                currentCharaData.put(
                        LIST_NAME, charaName);

                if (charaName.equalsIgnoreCase("SPS_SERVER_TX")) {
                    //set data receive notification
                    mBluetoothLeService.setCharacteristicNotification(
                            gattCharacteristic, true);
                }

                if (charaName.equalsIgnoreCase("SPS_SERVER_RX")) {
                    rxServiceCharacteristic = gattCharacteristic;
                }

                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);

        displayData("Reading SENSE Attributes...");
        Sense1000.readAllAttributes();

        streamHandler = new Handler();

        readLoop = new Runnable() {
            @Override
            public void run() {
                if(dataReceived) {
                    mBluetoothLeService.read_Data();
                    dataReceived = false;
                }
                if(!pauseReads) {
                    streamHandler.postDelayed(this, 50);
                }

            }
        };
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                readLoop.run();
                dataReceived = true;
                runnableStarted = true;
            }
        }, 1000);


        displayData("Connected");
    }

    private final ExpandableListView.OnChildClickListener configureExpandableListClickListener =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    String str = parent.getExpandableListAdapter().getGroup(groupPosition).toString();
                    String name = "NAME=";
                    final String channel = str.substring(str.indexOf(name) + name.length(), str.indexOf("}", str.indexOf(name) + name.length()));

                    str = parent.getExpandableListAdapter().getChild(groupPosition, childPosition).toString();
                    final String attribute = str.substring(str.indexOf(name) + name.length(), str.indexOf("}", str.indexOf(name) + name.length()));

                    if (!Sense1000.getAttributeOptions(channel, attribute)[0].equals("USER_SELECT")) {
                        final ArrayAdapter<String> adp = new ArrayAdapter<>(ControlActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, Sense1000.getAttributeOptions(channel, attribute));

                        final Spinner sp = new Spinner(ControlActivity.this);
                        //sp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        sp.setAdapter(adp);
                        int spinnerPosition = adp.getPosition(Sense1000.getAttributeValue(channel, attribute));
                        sp.setSelection(spinnerPosition);


                        new AlertDialog.Builder(ControlActivity.this)
                                .setTitle(attribute)
                                .setView(sp)
                                .setPositiveButton("Write", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mBluetoothLeService.writeAttribute(channel, attribute, sp.getSelectedItem().toString());

                                        //read attribute after 100ms to avoid overloading Sense
                                        handler = new Handler();
                                        final Runnable r = new Runnable() {
                                            public void run() {
                                                mBluetoothLeService.readAttribute(channel, attribute);
                                                Toast.makeText(getApplicationContext(), channel + ": " + attribute + " updated."
                                                        , Toast.LENGTH_SHORT).show();
                                            }
                                        };
                                        handler.postDelayed(r, 100);
                                    }
                                })
                                .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                    }
                    else {
                        final EditText sp = new EditText(ControlActivity.this);
                        sp.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        sp.setText(Sense1000.getAttributeValue(channel, attribute));

                        new AlertDialog.Builder(ControlActivity.this)
                                .setTitle(attribute)
                                .setView(sp)
                                .setPositiveButton("Write", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mBluetoothLeService.writeAttribute(channel, attribute, sp.getText().toString());

                                        //read attribute after 100ms to avoid overloading Sense
                                        handler = new Handler();
                                        final Runnable r = new Runnable() {
                                            public void run() {
                                                mBluetoothLeService.readAttribute(channel, attribute);
                                                Toast.makeText(getApplicationContext(), channel + ": " + attribute + " updated."
                                                        , Toast.LENGTH_SHORT).show();
                                            }
                                        };
                                        handler.postDelayed(r, 100);
                                    }
                                })
                                .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                    }

                    mBluetoothLeService.readAttribute(channel, attribute);

                    return true;
                }
            };

    //callback for item click handler
    private final AdapterView.OnItemClickListener configureListClickListener =
            new AdapterView.OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final String attribute = parent.getItemAtPosition(position).toString(); //get clicked item name

                    final ArrayAdapter<String> adp = new ArrayAdapter<>(ControlActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, Sense1000.getAttributeOptions(null, attribute));

                    final Spinner sp = new Spinner(ControlActivity.this);
                    sp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    sp.setAdapter(adp);

                    int spinnerPosition = adp.getPosition(Sense1000.getAttributeValue(null, attribute));
                    sp.setSelection(spinnerPosition);

                    mBluetoothLeService.readAttribute(null, attribute);

                    new AlertDialog.Builder(ControlActivity.this)
                            .setTitle(attribute)
                            .setView(sp)
                            .setPositiveButton("Write", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mBluetoothLeService.writeAttribute(null, attribute, sp.getSelectedItem().toString());

                                    //read attribute after 100ms to avoid overloading Sense
                                    handler = new Handler();
                                    final Runnable r = new Runnable() {
                                        public void run() {
                                            mBluetoothLeService.readAttribute(null, attribute);
                                            Toast.makeText(getApplicationContext(), "Device: " + attribute + " updated."
                                                    , Toast.LENGTH_SHORT).show();
                                        }
                                    };
                                    handler.postDelayed(r, 500);
                                }
                            })
                            .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();

                }
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.gatt_debug:
                gattDebug.show(getSupportFragmentManager(), "GattDebug");
                return true;
            case R.id.configure:
                pauseReads = true;
                streamHandler.removeCallbacks(readLoop);

                devAttributesAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_list_item_1, devAttributes);

                devAttributesListView = new ListView(this); //instantiate new Listview for config dialog
                devAttributesListView.setAdapter(devAttributesAdapter); //set adapter that maintains data
                devAttributesListView.setOnItemClickListener(configureListClickListener);

                configureDevice.show(getSupportFragmentManager(), "configureDevice");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<>();
        graph.addSeries(series);
        series.setColor(Color.BLUE);
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getApplicationContext(), series.getTitle() + ": "
                        + dataPoint.getY() + " at sample number " + dataPoint.getX()
                        , Toast.LENGTH_LONG).show();
            }
        });

        series1 = new LineGraphSeries<>();
        graph.addSeries(series1);
        series1.setColor(Color.GREEN);
        series1.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getApplicationContext(), series1.getTitle() + ": "
                        + dataPoint.getY() + " at sample number " + dataPoint.getX()
                        , Toast.LENGTH_LONG).show();
            }
        });

        series2 = new LineGraphSeries<>();
        //graph.addSeries(series2);
        series2.setColor(Color.RED);
        series2.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getApplicationContext(), series2.getTitle() + ": "
                        + dataPoint.getY() + " at sample number " + dataPoint.getX()
                        , Toast.LENGTH_SHORT).show();
            }
        });

        series3 = new LineGraphSeries<>();
        //graph.addSeries(series3);
        series3.setColor(Color.MAGENTA);
        series3.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getApplicationContext(), series3.getTitle() + ": "
                        + dataPoint.getY() + " at sample number " + dataPoint.getX()
                        , Toast.LENGTH_SHORT).show();
            }
        });

        series4 = new LineGraphSeries<>();
        //graph.addSeries(series3);
        series4.setColor(Color.YELLOW);
        series4.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getApplicationContext(), series4.getTitle() + ": "
                        + dataPoint.getY() + " at sample number " + dataPoint.getX()
                        , Toast.LENGTH_SHORT).show();
            }
        });

        series5 = new LineGraphSeries<>();
        //graph.addSeries(series3);
        series5.setColor(Color.BLACK);
        series5.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getApplicationContext(), series5.getTitle() + ": "
                        + dataPoint.getY() + " at sample number " + dataPoint.getX()
                        , Toast.LENGTH_SHORT).show();
            }
        });

        //set default view
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(minX);
        graph.getViewport().setMaxX(maxX);
        graph.setTitle("Temp-Module");
        graph.setTitleTextSize(50);

        graph.getGridLabelRenderer().setVerticalAxisTitle("Value (°C)");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Sample");
        graph.getGridLabelRenderer().setLabelVerticalWidth(100);

        //enable legend
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        graph.getLegendRenderer().setWidth(150);

        // enable scaling and scrolling
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);

        //store BLE information from previous activity
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //setup display and connection info
        TextView textViewDeviceName = (TextView)findViewById(R.id.textDeviceName);
        TextView textViewDeviceAddr = (TextView)findViewById(R.id.textDeviceAddress);
        textViewState = (TextView)findViewById(R.id.textState);
        dataLogView = (TextView)findViewById(R.id.debug_window);
        dataLogView.setMovementMethod(new ScrollingMovementMethod());

        SwitchCompat freezeGraphButton = (SwitchCompat) findViewById(R.id.freeze_graph);
        freezeGraphButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateGraph = isChecked;
            }
        });

        textViewDeviceName.setText(mDeviceName);
        textViewDeviceAddr.setText(mDeviceAddress);

        //set debug fragment adapter and listener
        mGattServicesList = new ExpandableListView(this);

        //set config fragment adapter and listener
        mDevConfigureList = new ExpandableListView(this);
        mDevConfigureList.setOnChildClickListener(configureExpandableListClickListener);

        //instantiate xml parser
        XmlSchemaParser xmlParser = new XmlSchemaParser();

        //istream needed for xml parser
        //xml temporarily read from const string
        try{
            in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        } catch (IOException e) {
            Log.d(TAG, "IndexOutOfBoundsException: " + e.getMessage());
        }

        //parse xml Istream to List
        try {
            xmlParsedList = xmlParser.parse(in);
        } catch (IOException err) {
            Log.d(TAG, "IndexOutOfBoundsException: " + err.getMessage());
        } catch (XmlPullParserException err) {
            Log.d(TAG,"Caught IOException: " + err.getMessage());
        }

        //ArrayLists for expandablelistview
        ArrayList<HashMap<String, String>> configChannelData =
                new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> configAttrData
                = new ArrayList<>();

        Sense1000 = new MeasurementDevice();

        //parse all device attributes, channels and channel attributes to the config dialog fragment
        XmlSchemaParser.Device device = xmlParsedList.get(0);
        if(device != null) {
            for(String devAttribute: device.attributes){
                Sense1000.changeAttribute(null, devAttribute, "");
                devAttributes.add(devAttribute);
            }
            for(XmlSchemaParser.Channel channel:device.channels) {
                HashMap<String, String> ChannelMap = new HashMap<>();
                ArrayList<HashMap<String, String>> AttrList = new ArrayList<>();

                ChannelMap.put("NAME", channel.channelName);
                ChannelMap.put("DESC", "Channel");
                for(String channelAttribute:channel.attributes) {
                    HashMap<String, String> AttrMap = new HashMap<>();
                    AttrMap.put("NAME", channelAttribute);
                    AttrMap.put("VALUE", "");

                    Sense1000.changeAttribute(channel.channelName, channelAttribute, "");

                    AttrList.add(AttrMap);
                }
                configChannelData.add(ChannelMap);
                configAttrData.add(AttrList);
            }
        }

        //fill SimpleExpandableListAdapter with configuration options
        SimpleExpandableListAdapter mDevConfigureAdapter = new SimpleExpandableListAdapter(
                this,
                configChannelData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {"NAME", "DESC"},
                new int[] { android.R.id.text1, android.R.id.text2 },
                configAttrData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {"NAME", "VALUE"},
                new int[] { android.R.id.text1, android.R.id.text2}
        );

        mDevConfigureList.setAdapter(mDevConfigureAdapter);

        gattDebug = new GattDebugFragment();
        configureDevice = new ConfigureDeviceFragment();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        displayData("Connecting to Gatt...");
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        if(runnableStarted) {
            pauseReads = false;
            dataReceived = true;
            readLoop.run();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(runnableStarted) {
            pauseReads = true;
        }
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

        currX = 0;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}

