package backend;


public class PathCalculator {
    public int addition(int a, int b) {
        return a + b;
    }

    public int division(int a, int b) {
        if (b == 0) throw new IllegalArgumentException("Division par zéro interdite");
        return a / b;
    }
}