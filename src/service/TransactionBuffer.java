package service;

public final class TransactionBuffer {
    private final Runnable[] buffer;
    private int count;

    public TransactionBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be positive.");
        }
        buffer = new Runnable[capacity];
    }

    public synchronized void put(Runnable task) throws InterruptedException {
        while (count == buffer.length) {
            wait();
        }
        buffer[count++] = task;
        notifyAll();
    }

    public synchronized Runnable take() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        Runnable task = buffer[0];
        System.arraycopy(buffer, 1, buffer, 0, --count);
        buffer[count] = null;
        notifyAll();
        return task;
    }
}
