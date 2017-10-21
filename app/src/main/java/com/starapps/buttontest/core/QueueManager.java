package com.starapps.buttontest.core;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueManager {

    private static final String TAG = "QueueManager";

    private Queue<QueueItem> queue = new ConcurrentLinkedQueue<>();
    private ConnectionManager manager;
    private static QueueManager instance;
    private List<QueueObserver> observers = new ArrayList<>();

    private QueueManager() {
    }

    public static QueueManager getInstance() {
        if (instance == null) {
            instance = new QueueManager();
        }
        return instance;
    }

    public void addObserver(QueueObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(QueueObserver observer) {
        if (observers.contains(observer)) {
            observers.remove(observer);
        }
    }

    public String insert(QueueItem item) {
        queue.offer(item);
        int size = queue.size();
        Log.d(TAG, "insert: " + size + ", manager " + manager);
        if (size == 1 && manager != null) {
            manager.writeMessage(item.getData());
        }
        return item.getUuId();
    }

    public void setManager(ConnectionManager manager) {
        if (this.manager == null) {
            this.manager = manager;
        }
    }

    public QueueItem getCurrent() {
        return queue.peek();
    }

    public QueueItem deQueue() {
        QueueItem item = queue.poll();
        QueueItem next = queue.peek();
        Log.d(TAG, "deQueue: " + queue.size());
        if (next != null) {
            manager.writeMessage(next.getData());
        }
        if (queue.size() == 0) {
            notifyObservers();
        }
        return item;
    }

    public void clearQueue() {
        queue.clear();
    }

    public void notifyQueue() {
        QueueItem item = getCurrent();
        Log.d(TAG, "notifyQueue: " + item + ", " + manager);
        if (item != null && manager != null) manager.writeMessage(item.getData());
    }

    private void notifyObservers() {
        for (QueueObserver observer : observers) {
            observer.onDeQueue();
        }
    }

    public interface QueueObserver {
        void onDeQueue();
    }
}