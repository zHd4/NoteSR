package app.notesr.service;

import lombok.Getter;

public class ServiceHandler<T> {
    @Getter
    private T service;
    private final Object lock = new Object();

    public void setService(T service) {
        synchronized (lock) {
            this.service = service;
            lock.notifyAll();
        }
    }

    public void waitForService() throws InterruptedException {
        synchronized (lock) {
            while (service == null) {
                lock.wait();
            }
        }
    }
}
