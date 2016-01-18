package com.forkize.sdk.localstorage;

import com.forkize.sdk.ForkizeConfig;

import java.util.ArrayList;
import java.util.List;

public class EventMemoryStorage {

    private int eventMaxCount;
    private List<String> events;

    protected EventMemoryStorage() {
        this.events = new ArrayList<>();
        this.eventMaxCount = ForkizeConfig.getInstance().getMaxEventsPerFlush();
    }

    public boolean write(String data) throws Exception {
        if (this.events.size() < this.eventMaxCount) {
            this.events.add(data);
            return true;
        }
        return false;
    }

    public boolean write(String[] data) throws Exception {
        for (String dataItem : data) {
            if (!this.write(dataItem))
                return false;
        }
        return true;
    }

    public String[] read() throws Exception {
        return this.read(this.eventMaxCount);
    }

    public String[] read(int quantity) throws Exception {
        quantity = Math.min(quantity, this.events.size());
        String[] strings = new String[quantity];
        for (int i = 0; i < quantity; ++i) {
            strings[i] = this.events.get(i);
        }

        return strings;
    }

    public void flush(int eventCount) {
        eventCount = Math.min(eventCount, this.events.size());
        for (int i = eventCount - 1; i >= 0; --i) {
            this.events.remove(i);
        }
    }

    public void clear() {
        this.events.clear();
    }
}