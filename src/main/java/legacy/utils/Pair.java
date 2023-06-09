package legacy.utils;

public class Pair<S, T> {
    public S first;
    public T second;

    public Pair(S first, T second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "{" + first + ", " + second + '}';
    }
}
