class SimpleClass {
    public String method() {
        throw new UnsupportedOperationException("Removed by DD");
    }

    public String field = "test".transform(s -> {
        throw new UnsupportedOperationException("Removed by DD");
    })
}