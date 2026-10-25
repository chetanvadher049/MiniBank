package service;

public final class DemoResource implements AutoCloseable {
    public void use() {
        System.out.println("Using a closable banking resource.");
    }

    @Override
    public void close() {
        System.out.println("Banking resource closed.");
    }
}
