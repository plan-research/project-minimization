class SimpleClass {
    public String method() {
        return "test";
    }

    public String field = "test".transform(s -> s + "passed")
}