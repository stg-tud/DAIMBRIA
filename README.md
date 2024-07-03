# DAIMBRIA

## Motivation

In modern distributed systems, maintaining different schema versions of data models is a common challenge.
As systems evolve, their underlying data structures also change, creating incompatibilities between services or clients
working with different schema versions. Without an efficient solution, developers are often forced to maintain
multiple versions of the same API or data structure, leading to increased complexity and duplicate codebases.

[Cambria](https://www.inkandswitch.com/cambria/) has developed a solution to this problem in the form of Lenses, which
are bidirectional transformations
between schema versions. With Lenses, data can be automatically converted from one schema version to another,
enabling seamless communication between services that use different schema versions. This approach eliminates
the need to maintain separate API versions and enables dynamic handling of schema development through a lens graph.

Building on [Cambria](https://www.inkandswitch.com/cambria/)'s approach and the corresponding
[TypeScript project](https://github.com/inkandswitch/cambria-project), DAIMBRIA offers a Kotlin-based library for
managing schema transformation. It allows developers to reduce the overhead when maintaining different versions for the
same data model.

Given the following legacy data model

```kotlin
data class UserDto(
    val username: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String
)
```

A change has been introduced and the new model looks like this:

```kotlin
data class UserDto(
    val username: String,
    val passwordHash: String,
    val name: String
)
```

This new model leads to issues with older clients, as (previously valid) requests to the endpoints are rejected, as
the `firstName` and `lastName` are no longer available, and the `name` field is required but null.

To solve this problem, developers often provide multiple versions to support legacy clients, leading to a lot of
duplicated code and data management.

DAIMBRA aims to eliminate this problem by providing a library to convert incoming payloads automatically, before they
are parsed by the server.
Additionally, DAIMBRA is able to intercept outgoing requests, so older clients can communicate with newer server
versions without touching their data models.

This allows developers to reduce the overhead, as they can define transformations for their DTO and continue using their
existing data structures.

### Features

DAIMBRA implements various features to provide a slim but powerful library.

##### Transformations

DAIMBRA does support the following transformations:

- Add Fields
- Remove Fields
- Rename Fields
- Copy Fields
- Convert Fields
- Hoist Fields (moving outside of an object)
- Plunge Fields (move inside of object)
- Wrap Fields (as an array)
- Head Fields (take first element of an array)

Please be aware that some operations are lossy - for example, removed fields cannot be restored backwards, and are
filled with a default value.

##### Validation

DAIMBRIA includes compile time and runtime checks to validate whether the defined lenses are properly defined.
For example, copying an undefined field will result in an exception.

Each Transformation graph starts with an initial definition, which defines the fields of the very first lens.
Afterwards, each lens can modify, add or remove fields.
This ensures that each conversion is well-defined.

Moreover, DAIMBRIA supports validation for types based on the types defined in the JSON specification.
This allows you to verify that your data conversion is successful, for example when converting a string to an Integer.
Additionally, this ensures that the incoming payload is well-defined, as mismatches between types (for example between
string and integers) are validated when defining each lens.

DAIMBRIA focuses on validating lenses as soon as possible.
Hence, each lens will be validated as soon as possible (usually during startup), and DAIMBRIA will enforce the
correctness of each lens.
This also applies to correct type checks.

### Installation

DAIMBRIA is published as a Gradle Package.

You can include it as any other gradle package in your package manager.

### Usage

##### Initialization

You can create a new LensGraph by initializing it with the root lens of the graph.
If you have just started using DAIMBRIA, this lens should contain the current fields of your object.
Otherwise, the initial lens may date back to the very first version of your object.
The root lens defines the start of each graph and is used to determine, whether the subsequent transformations are
valid.

```kotlin
val root: Lens = rootLens(version = "1.0") {
    add(name = "name", type = STRING, default = "Max Mustermann")
    // more field definitions
}

val lens1 = lens(from = "1.0", to = "2.0") {
    rename(from = "name", to = "nickname")
}

val graph = lensGraph(rootLens = root) {
    +lens1
    // add more lenses here
}
```

Btw: named parameters, e.g. `version = "1.0"`, may be omitted in Kotlin if order is the same as defined.

##### Transformation

Each Lens consists of one or multiple transformations, called lens operations.
DAIMBRIA provides a domain-specific language to specify the various operations included within a lens.

```kotlin
// transformation from version 1.0 to 2.0
lens("1.0", "2.0") {
    // adds field with type string, default value not set (default is null)
    add("name", STRING)

    // removes field (type information necessary for reverse operation)
    // default value not set (default is null), necessary for reverse operation
    remove("name", STRING)

    // rename field from 'name' to 'nickname'
    rename("name", "nickname")

    // copy field value to new field
    copy("age", "yearsLived")

    // move field 'value' outside of object 'result'
    hoist("value", "result")

    // move field 'height' into object 'properties'
    plunge("height", "properties")

    // wraps field inside an array with it as single value
    wrap("results")

    // take first element of array as scalar
    head("favorites")

    // applies lens operations inside an object field
    lensIn("object") {
        ...
    }

    // apply lens operations inside an array (in) for each element (map)
    lensIn("array") {
        lensMap {
            ...
        }
    }

    /* 
     * convert field name
     * defines mapping from string to string
     * reverse mapping has to be provided
     * type declaration necessary for validation of mapping functions
     */
    convert("name") {
        STRING mapsTo STRING
        mapping { from: String -> from.split(" ").first() }
        reverseMapping { from: String -> from }
    }

}
```

The `TransformationEngine` applies Lens operations to a Json object but it doesn't validate anything but the bare
minimum.
Doing so, we can use the TransformationEngine standalone to just convert Json documents without paying attention to
other depending lenses.
Validation happens during compile time so that errors in defined Lenses can be caught beforehand.

##### Validation

DAIMBRIA aims to automatically validate the types based on the type information provided when adding, removing or
converting fields.
Hence, manually validating the graph - DAIMBRIA does validate each graph during initialization.

##### Validation pays attention to following points:

- The fields referenced exist
- Declared fields are stored with their type to be able to validate for following lens operations
- The referenced json nodes are of correct type (e.g. object / array)
- Convert function parameter and return types

### Design Choices

#### Kotlin vs. YAML Lenses & TypeScript

In the initial Cambria project, the lenses were defined in a YAML dsl. We chose Kotlin because of its great support
for writing custom Dsl in code without the need to parse an external document.
Moreover, exploring the capabilities of a well typed high level language,
especially on the JVM, regarding porting Lenses to the JVM was a point for choosing Kotlin.

#### Logic Convert (not just structural)

The main drawback of Cambrias convert operator was, that it is purely structural. Therefore defining hard-coded mappings
e.g. from boolean to enums or vice versa. More complex transformations like changing the format of a timestamp was not
realizable.

Using Kotlin higher order functions (lambdas), we can define more complex convert mappings.
The downside is, that the convert operator can no longer be serialized (see following).

#### Non-Serializable Lenses

In the Cambria paper, there was introduced use-case where lenses could be sent to other instances of an application
to ensure forward compatibility of instances with an older version (without shared lenses).

Generally, this could be realized with our implementation BUT without the functionality of convert using Kotlin
functions.
Exploring the field of serializable functions could make this possible.

If not, static mappings have to be introduced to make lens operators serializable.
This could be realized by setting a flag to only allow serializable convert variants (if they were added).

#### Validation on build time

Lenses are validated on build-time so that falsy lenses are detected and can be corrected before application runs and
makes use of them.
Doing so removes the need to run extensive validation on run-time.

### Example Application

An example application is available in the `demo` folder in the project.
Here, you can find a server implementation and a client application, which have two different versions of the same entity.
Both applications are based on Spring Boot, which is one of the most used frameworks for both Java and Kotlin.

The client sends a request to the server, using its outdated version of the entity.
The server then receives the request, transforms it into the latest version using DAIMBRIA, handles the request and then returns the response in the appropriate version for the client.

The example outlines the possibilities and some of the limitations of DAIMBRIA.
For example, the loss of data due to the use of lossy operations is demonstrated. 

To start the server, you can run the ServerDemoApplication.kt file (or depending on your setup, run the main function located there), located at `demo/src/main/kotlin/de/daimpl/demo/server`.
To start the client, you can run the ClientDemoApplication.kt file (or depending on your setup, run the main function located there), located at `demo/src/main/kotlin/de/daimpl/demo/client`.

### Known Limitations

- No serializable lenses at the moment (because of convert)
- Lossy connections still unexplored

### Future Work

- Serializable functions for convert
- Introduce flag for using serializable convert operations
- How to force writing 'shortcuts' in graph when too much loss exists in path
    - keep track of introduced loss?
- Better error messages for debugging -> show Json model where validation failed
- Null handling (regarding if json field is null, especially for convert operator)
- Validate Kotlin class against internal json object in Graph