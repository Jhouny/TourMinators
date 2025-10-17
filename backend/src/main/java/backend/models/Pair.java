package backend.models;

import java.util.Objects;

public class Pair<K, V> {
    private K left;
    private V right;

    public Pair(K left, V right) {
        this.left = left;
        this.right = right;
    }

    public K getLeft() {
        return left;
    }

    public V getRight() {
        return right;
    }
    @Override
    public String toString(){
        return "("+left.toString()+", "+right.toString()+")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> p = (Pair<?, ?>) o;
        return Objects.equals(left, p.left) && Objects.equals(right, p.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}