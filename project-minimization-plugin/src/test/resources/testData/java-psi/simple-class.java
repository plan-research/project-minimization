class SimpleClass {
    // Constructor -> not collected
    public SimpleClass() {}

    private String method1() {
        return "test";
    }

    public void method2() {
    }

    static int method3() {
        return 42;
    }

    // No body -> not collected
    protected int uncollectedMethod()
}