**Definition of Done**
* Components (Service, Repository, Converter, Mapper, etc) are covered by Unit Tests
* Business Logic in Components are covered by Unit Tests
* Clean Code (DRY, KISS, SOLID)
* There are no Errors or Warning on Compile
* Code is formatted (Default Formatter) (use Save Action from IDE)
* Code does not contains
    * Dead Code 
    * Commented Code
    * Unused Code
    * //TODO (Create Jira Issue)
    * //FIXME
**Guidelines**
* Package by Feature
* Never use wildcard imports
* Follow Java Guidelines
    * https://google.github.io/styleguide/javaguide.html
* Follow Clean Code Guidelines
    * https://guide.freecodecamp.org/cplusplus/clean-code-guidelines/

**Commit message policy**

The basic Commit message is:

```ISSUE-KEY ISSUE-DEDSCRIPTION #comment <COMMENT-STRING>```

Example

```DX-31 CI/CD jenkins Pipeline integration in CBP Project #comment update README.md```

**Pull-Request**
* Primary Reviewer: all
* Required min 1 reviewer
* Changes after approved Pull-Request should reset the Pull-Request status to *unapproved*

**Test Coverage threshold**
## 95%
