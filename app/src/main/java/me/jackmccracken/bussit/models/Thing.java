package me.jackmccracken.bussit.models;

/**
 * Created by jackmccracken on 2015-05-31.
 */
public abstract class Thing {
    protected String thingId;
    protected String type;

    public Thing(String thingId, String type) {
        this.thingId = thingId;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getThingId() {
        return thingId;
    }
}
