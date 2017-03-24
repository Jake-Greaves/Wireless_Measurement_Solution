/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.analog.learningandroid;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String SERIAL_PORT_SERVICE_UUID = "0783b03e-8535-b5a0-7140-a304d2495cb7";
    public static String SPS_SERVER_TX_UUID = "0783b03e-8535-b5a0-7140-a304d2495cb8";

    static {
        //Services
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute");
        attributes.put(SERIAL_PORT_SERVICE_UUID, "Serial Port Service");

        // Sample Characteristics.
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "Device Name");
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "Appearance");
        attributes.put("00002a02-0000-1000-8000-00805f9b34fb", "Peripheral Privacy Flag");
        attributes.put("00002a05-0000-1000-8000-00805f9b34fb", "Service Changed");
        attributes.put(SPS_SERVER_TX_UUID, "SPS_SERVER_TX");
        attributes.put("0783b03e-8535-b5a0-7140-a304d2495cba", "SPS_SERVER_RX");
        attributes.put("0783b03e-8535-b5a0-7140-a304d2495cb9", "SPS_FLOW_CTRL");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
