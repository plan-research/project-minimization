## Title (hint)

The title's format is following: `JBRes-#### <Some description>`

The title should contain a related ticket number from YT, and a short description of your changes or a new functionality.

For example:
> JBRes-1877 Kotlinc Exceptions Transformers

## Description

Here should be more info about PR:
- context/purpose for implementing changes;
- detailed descriptions of the changes made;
- link to a documentation.

## How to test

### Automated tests

Please specify the _automated tests_ for your code changes: you should either mention the existing tests or add the new ones.

For example:
> Translation and parsing of Kotlinc errors verified with tests: `project-minimization-plugin/src/test/kotlin/KotlincExceptionTranslatorTests.kt`

### Manual tests

If it's impossible to provide the automated tests, please provide a description of how to verify the changes manually. 

## Self-check list

- [ ] PR **title** and **description** are clear and aligned with a format.
- [ ] I've added enough **comments** to my code, particularly in hard-to-understand areas. 
- [ ] The functionality I've repaired, changed or added is covered with **automated tests**.
- [ ] **Manual tests** have been provided _optionally_.
- [ ] The **documentation** for the functionality I've been working on is up-to-date/provided.
- [ ] The **link** to this PR is commented on in the corresponding **YT ticket**.

Hint: [x] is a marked item
