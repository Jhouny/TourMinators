import static org.junit.jupiter.api.Assertions.assertEquals;

import example.util.Calculator;

import org.junit.jupiter.api.Test;

class PathCalculatorTest {

    private PathCalculator calc;
    private list<Edge> troncons;
    private list<Node> sommets;
    private Features features = new Features();

    @BeforeEach
    void setUp() {
        calc = new PathCalculator();
    }

    @Test
    void test1packet() {
        sommets = new list<Node>();
        sommets.add(new Node(1, 45, 4));
        sommets.add(new Node(2, 46, 5));
        sommets.add(new Node(3, 43, 0));


        troncons = new list<Edge>();
        troncons.add(new Edge(1, 2, "",

        int resultat = calc.addition(2, 3);
        assertEquals(5, resultat, "2 + 3 devrait faire 5");
    }

    @Test
    void testDivision() {
        int resultat = calc.division(10, 2);
        assertEquals(5, resultat);
    }

    @Test
    void testDivisionParZero() {
        assertThrows(IllegalArgumentException.class, () -> calc.division(10, 0));
    }
}