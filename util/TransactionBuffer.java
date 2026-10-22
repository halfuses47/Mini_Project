package util;

public class TransactionBuffer {

    private final Runnable[] buffer;
    private int count = 0;
    private int in = 0;
    private int out = 0;
    private final int capacity;

    public TransactionBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Runnable[capacity];
    }

    public synchronized void produce(Runnable task) throws InterruptedException {
        while (count == capacity) {
            wait();
        }
        buffer[in] = task;
        in = (in + 1) % capacity;
        count++;
        System.out.println("[PRODUCER] Queued transaction. Buffer count: " + count + "/" + capacity);
        notify();
    }

    public synchronized Runnable consume() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        Runnable task = buffer[out];
        out = (out + 1) % capacity;
        count--;
        System.out.println("[CONSUMER] Retrieved transaction. Buffer count: " + count + "/" + capacity);
        notify();
        return task;
    }

    public synchronized int getCount() {
        return count;
    }
}
