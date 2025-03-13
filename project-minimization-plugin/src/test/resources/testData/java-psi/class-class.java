class SimpleClass {
    class InnerClass1 {
        public void method1() {
            System.out.println("first method");
        }
    }

    public void method2() {
        class InnerClass2 {
            // Inside a method -> not collected
            public void uncollected() {
                System.out.println("uncollected");
            }
        }

        InnerClass2 inner = new InnerClass2();
        inner.uncollected();
    }

    public boolean field = Stream.of("test").allMatch(new Predicate<String>() {
        // collected
        public boolean test(String s) {
            return false;
        }
    })
}