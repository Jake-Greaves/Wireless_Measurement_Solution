package com.analog.learningandroid;

import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jtgre on 22/03/2017.
 */

public class MeasurementDevice {

    public MeasurementDevice() {
        String[] PowerModeOptions = {"Low", "Mid", "Full"};
        String[] OperationalModeOptions = {"One_Conv", "Continuous"};
        String[] FilterTypeOptions = {"Sinc4", "FIR"};
        String[] FirFrequencyOptions = {"20SPS", "25SPS"};
        String[] FsOptions = {"384", "96", "48"};
        String[] TemperatureUnitOptions = {"Celsius", "Fahrenheit"};

        attributeOptions.put("PowerMode", PowerModeOptions);
        attributeOptions.put("OperationalMode", OperationalModeOptions);
        attributeOptions.put("FilterType", FilterTypeOptions);
        attributeOptions.put("FirFrequency", FirFrequencyOptions);
        attributeOptions.put("FS", FsOptions);
        attributeOptions.put("TemperatureUnit", TemperatureUnitOptions);
    }

    private static class Channel {
        public HashMap<String, String> channelAttributes = new HashMap<>();
        private HashMap<String, String[]> attributeOptions = new HashMap<>();

        public void changeAttribute(String attribute, String value) {
            channelAttributes.put(attribute, value);
        }

        public String getAttributeValue(String attribute) {
            return channelAttributes.get(attribute);
        }

        public void readAttribute(String ChannelName, String attName) {
            ControlActivity.mBluetoothLeService.readAttribute(ChannelName, attName);
        }

        public String[] getAttributeOptions(String channel, String attribute) {
                return attributeOptions.get(attribute);
        }

        public void readAllAttributes(String channelName) {
            if(channelName.equals("cold_junction")) {
                String[] SensorOptions = {"SensorOff", "RTD3Wire", "RTD4Wire", "Thermistor", "DigitalIC", "Other"};
                String[] SensorTypeOptions = {"PT100", "PT1000"};
                String[] GainOptions = {"1", "8", "16", "32"};
                String[] ExcitationCurrentOptions = {"0", "100", "250", "500", "750"};
                String[] ReferenceResistorOptions = {"USER_SELECT"};
                String[] VBiasEnableOptions = {"0", "1"};
                String[] TemperatureMax = {"USER_SELECT"};
                String[] TemperatureMin = {"USER_SELECT"};

                attributeOptions.put("Sensor", SensorOptions);
                attributeOptions.put("SensorType", SensorTypeOptions);
                attributeOptions.put("Gain", GainOptions);
                attributeOptions.put("ExcitationCurrent", ExcitationCurrentOptions);
                attributeOptions.put("ReferenceResistor", ReferenceResistorOptions);
                attributeOptions.put("VBiasEnable", VBiasEnableOptions);
                attributeOptions.put("TemperatureMax", TemperatureMax);
                attributeOptions.put("TemperatureMin", TemperatureMin);

            } else if(channelName.equals("thermocouple")) {
                String[] SensorOptions = {"thermocouple"};
                String[] SensorTypeOptions = {"T", "J", "K", "E", "S", "R", "N", "B"};
                String[] GainOptions = {"32", "64", "128"};
                String[] VBiasEnableOptions = {"0", "1"};
                String[] TemperatureMax = {"USER_SELECT"};
                String[] TemperatureMin = {"USER_SELECT"};

                attributeOptions.put("Sensor", SensorOptions);
                attributeOptions.put("SensorType", SensorTypeOptions);
                attributeOptions.put("Gain", GainOptions);
                attributeOptions.put("VBiasEnable", VBiasEnableOptions);
                attributeOptions.put("TemperatureMax", TemperatureMax);
                attributeOptions.put("TemperatureMin", TemperatureMin);
            }

            for(HashMap.Entry<String, String> attribute : channelAttributes.entrySet()) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {

                }
                    String attName = attribute.getKey();
                    readAttribute(channelName, attName);
                }
            }
        }

    private HashMap<String, String> deviceAttributes = new HashMap<>();
    private final HashMap<String, Channel> deviceChannels = new HashMap<>();
    private final HashMap<String, String[]> attributeOptions = new HashMap<>();

    public String[] getAttributeOptions(String channel, String attribute) {
        if (channel == null) {
            return attributeOptions.get(attribute);
        } else {
            return deviceChannels.get(channel).getAttributeOptions(null, attribute);
        }
    }

    public void changeAttribute(String channelName, String attribute, String value) {
        if(channelName == null)
        {
            deviceAttributes.put(attribute, value);
        } else {
            Channel currChannel = deviceChannels.get(channelName);
            if(currChannel == null) {
                deviceChannels.put(channelName, new Channel());
                currChannel = deviceChannels.get(channelName);
            }
            currChannel.changeAttribute(attribute, value);
        }
    }

    public String getAttributeValue(String channelName, String attribute) {
        if (channelName == null) {
            return deviceAttributes.get(attribute);
        } else {
            Channel currChannel = deviceChannels.get(channelName);
            return currChannel.getAttributeValue(attribute);
        }
    }


    public void readAllAttributes() {
        for(HashMap.Entry<String, Channel> channelEntry : deviceChannels.entrySet()) {
            channelEntry.getValue().readAllAttributes(channelEntry.getKey());
        }

        for(HashMap.Entry<String, String> attribute : deviceAttributes.entrySet()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                //pls just work
            }
            String attName = attribute.getKey();
            readAttribute(null, attName);
        }
    }

    private void readAttribute(String ChannelName, String attName) {
        ControlActivity.mBluetoothLeService.readAttribute(ChannelName, attName);
    }

}
