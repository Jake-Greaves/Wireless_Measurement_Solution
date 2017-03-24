package com.analog.learningandroid;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jtgre on 21/03/2017.
 */

public class XmlSchemaParser {

    private static final String ns = null;
    // We don't use namespaces

    public List<Device> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readContext(parser);
        } finally {
            in.close();
        }
    }

    private List<Device> readContext(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Device> device = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "context");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the device tag
            if (name.equals("device")) {
                device.add(readDevice(parser));
            } else {
                skip(parser);
            }
        }
        return device;
    }

    // This class represents a single entry (post) in the XML context
    // It includes the data members "channel", "attribute"
    public static class Device {
        public final String deviceName;
        public final List<Channel> channels;
        public final List<String> attributes;

        private Device(String contextName, List<Channel> channels, List<String> attributes) {
            this.deviceName = contextName;
            this.channels = channels;
            this.attributes = attributes;
        }
    }

    // This class represents a single entry (post) in the XML feed.
    // It includes the data members "title," "link," and "summary."
    public static class Channel {
        public final String channelName;
        public final List<String> attributes;

        private Channel(String channelName, List<String> attributes) {
            this.channelName = channelName;
            this.attributes = attributes;
        }
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    private Device readDevice(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "device");
        List<Channel> channel = new ArrayList<>();
        List<String> attribute = new ArrayList<>();
        String deviceName;

        deviceName = parser.getAttributeValue(null, "name");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equals("channel")) {
                channel.add(readChannel(parser));
            } else if (name.equals("attribute")) {
                attribute.add(readAttribute(parser));
            } else {
                skip(parser);
            }
        }
        return new Device(deviceName, channel, attribute);
    }

    // Processes title tags in the feed.
    private Channel readChannel(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "channel");

        String channelName;
        List<String> attribute = new ArrayList<>();

        channelName = parser.getAttributeValue(null, "name");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (name.equals("attribute")) {
                attribute.add(readAttribute(parser));
            } else {
                skip(parser);
            }
        }
        return new Channel(channelName, attribute);
    }

    // For the tags title and summary, extracts their text values.
    private String readAttribute(XmlPullParser parser) throws IOException, XmlPullParserException {
        String name = "";
        parser.require(XmlPullParser.START_TAG, ns, "attribute");
        String tag = parser.getName();

        if (tag.equals("attribute")) {
            name = parser.getAttributeValue(null, "name");
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, "attribute");
        return name;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
