class TestClass {
    public Runnable r1 = () -> System.out.println("Expression");
    public Runnable r2 = () -> {
        System.out.println("Block");
    };
    public String str = Stream.of("test")
        .filter(s -> s.length() > 0)
        .filter(s -> {
            return s.length() < 42
        }).findFirst().orElse("empty");
}

