fun lambdaAsDefaultParameterShouldNotBeReplacebal(f: (Int) -> Unit = {x -> println(x)}, x: Int) {
    f(x)
}
