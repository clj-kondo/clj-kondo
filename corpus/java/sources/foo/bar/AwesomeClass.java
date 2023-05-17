package foo.bar;

import java.io.File;
import java.util.List;

public class AwesomeClass {

    private String foo1;
    private final  Double foo2 = 1.0;
    private final static Double foo3 = 1.0;

    public Double bar1;
    public final Double bar2 = 1.0;
    public static final Double bar3 = 1.0;

    public AwesomeClass(double a) {
        System.out.println("Initializing..." + a);
    }

    public int coolSum1(double a, double b) {
        return (int) a + (int) b;
    }

    /*
     * Some cool doc
     * @param filenames
     * @return list of files
     */
    public static File[] coolParse(List<String> filenames) {
        return null;
    }
}
