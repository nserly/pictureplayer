package top.nserly.SoftwareCollections_API.Collections;


public class SameThreadCollections<T> {
    private final Thread t1;
    private T map;

    public SameThreadCollections() {
        t1 = Thread.currentThread();
    }

    public SameThreadCollections(T object) {
        t1 = Thread.currentThread();
        map = object;
    }

    public Thread GetCrateThread() {
        return t1;
    }

    public boolean isNull() {
        return map == null;
    }

    public void Add(T object) {
        map = object;
    }

    public T Get() {
        return map;
    }
}
