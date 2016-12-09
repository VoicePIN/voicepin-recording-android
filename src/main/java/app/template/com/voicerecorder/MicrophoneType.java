package app.template.com.voicerecorder;

/**
 * Created by mcol on 2015-07-27.
 */
public enum MicrophoneType {
    BUILT_IN("built-in"),
    LINE_IN("line-in"),
    HEADSET("headset"),
    BLUETOOTH_HFP("bluetooth-HFP"),
    USB("USB"),
    CAR("car");

    String type;

    MicrophoneType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
