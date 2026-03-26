package foo.bar;

public interface SampleInterface {
  void doSomething();
  String getName();
  int calculate(int a, int b);

  // Private helper method (Java 9+) - issue #2637
  private void helper() {
    System.out.println("I am private");
  }

  // Default method using the private helper
  default void doStuff() {
    helper();
  }
}
