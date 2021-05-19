/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.reflect.validation

import groovy.transform.CompileStatic
import org.gradle.internal.reflect.problems.ValidationProblemId
import org.gradle.util.GradleVersion
import org.gradle.util.internal.TextUtil
import spock.lang.Specification

import java.util.regex.Pattern

/**
 * This test renders exactly what a user would see when a validation message is printed
 */
class ValidationMessageCheckerTest extends Specification implements ValidationMessageChecker {
    @Delegate
    private VerificationFixture condition

    @ValidationTestFor(
        ValidationProblemId.VALUE_NOT_SET
    )
    def "tests output of missingValueMessage"() {
        when:
        render(missingValueMessage {
            type('SomeType')
            property('someProperty')
        })

        then:
        outputEquals """
Type 'SomeType' property 'someProperty' doesn't have a configured value.

Reason: This property isn't marked as optional and no value has been configured.

Possible solutions:
  1. Assign a value to 'someProperty'.
  2. Mark property 'someProperty' as optional.
"""

        when:
        render missingValueMessage {
            type('SomeType')
            property('someProperty')
            includeLink()
        }

        then:
        outputEquals """
Type 'SomeType' property 'someProperty' doesn't have a configured value.

Reason: This property isn't marked as optional and no value has been configured.

Possible solutions:
  1. Assign a value to 'someProperty'.
  2. Mark property 'someProperty' as optional.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#value_not_set for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.IGNORED_ANNOTATIONS_ON_METHOD
    )
    def "tests outputof methodShouldNotBeAnnotatedMessage"() {
        when:
        render methodShouldNotBeAnnotatedMessage {
            type('MyTask')
            kind('method')
            annotation('SomeAnnotation')
            method('setFoo')
            includeLink()
        }

        then:
        outputEquals """
Type 'MyTask' method 'setFoo()' should not be annotated with: @SomeAnnotation.

Reason: Input/Output annotations are ignored if they are placed on something else than a getter.

Possible solutions:
  1. Remove the annotations.
  2. Rename the method.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#ignored_annotations_on_method for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.PRIVATE_GETTER_MUST_NOT_BE_ANNOTATED
    )
    def "tests outputof privateGetterAnnotatedMessage"() {
        when:
        render privateGetterAnnotatedMessage {
            type('Foo').property('someProperty')
            annotation('Some')
            includeLink()
        }

        then:
        outputEquals """
Type 'Foo' property 'someProperty' is private and annotated with @Some.

Reason: Annotations on private getters are ignored.

Possible solutions:
  1. Make the getter public.
  2. Annotate the public version of the getter.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#private_getter_must_not_be_annotated for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.IGNORED_PROPERTY_MUST_NOT_BE_ANNOTATED
    )
    def "tests outputof ignoredAnnotatedPropertyMessage"() {
        when:
        render ignoredAnnotatedPropertyMessage {
            type('SomeType').property('someProperty')
            ignoring('Internal')
            alsoAnnotatedWith('Foo', 'Bar')
            includeLink()
        }

        then:
        outputEquals """
Type 'SomeType' property 'someProperty' annotated with @Internal should not be also annotated with @Foo, @Bar.

Reason: A property is ignored but also has input annotations.

Possible solutions:
  1. Remove the input annotations.
  2. Remove the @Internal annotation.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#ignored_property_must_not_be_annotated for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.CONFLICTING_ANNOTATIONS
    )
    def "tests outputof conflictingAnnotationsMessage"() {
        when:
        render conflictingAnnotationsMessage {
            type('Whatever').property('prop')
            inConflict('Input', 'Output')
            includeLink()
        }

        then:
        outputEquals """
Type 'Whatever' property 'prop' has conflicting type annotations declared: @Input, @Output.

Reason: The different annotations have different semantics and Gradle cannot determine which one to pick.

Possible solution: Choose between one of the conflicting annotations.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#conflicting_annotations for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.ANNOTATION_INVALID_IN_CONTEXT
    )
    def "tests output of annotationInvalidInContext"() {
        when:
        render annotationInvalidInContext {
            type('SomeType').property('prop')
            annotation('Invalid')
            forTransformAction()
            includeLink()
        }

        then:
        outputEquals """
Type 'SomeType' property 'prop' is annotated with invalid property type @Invalid.

Reason: The '@Invalid' annotation cannot be used in this context.

Possible solutions:
  1. Remove the property.
  2. Use a different annotation, e.g one of @Inject, @InputArtifact or @InputArtifactDependencies.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#annotation_invalid_in_context for more details about this problem.
"""
        when:
        render annotationInvalidInContext {
            type('SomeType').property('prop')
            annotation('Invalid')
            forTransformParameters()
        }

        then:
        outputEquals """
Type 'SomeType' property 'prop' is annotated with invalid property type @Invalid.

Reason: The '@Invalid' annotation cannot be used in this context.

Possible solutions:
  1. Remove the property.
  2. Use a different annotation, e.g one of @Console, @Inject, @Input, @InputDirectory, @InputFile, @InputFiles, @Internal, @Nested or @ReplacedBy.
"""

        when:
        render annotationInvalidInContext {
            type('SomeType').property('prop')
            annotation('Invalid')
            forTask()
        }

        then:
        outputEquals """
Type 'SomeType' property 'prop' is annotated with invalid property type @Invalid.

Reason: The '@Invalid' annotation cannot be used in this context.

Possible solutions:
  1. Remove the property.
  2. Use a different annotation, e.g one of @Console, @Destroys, @Inject, @Input, @InputDirectory, @InputFile, @InputFiles, @Internal, @LocalState, @Nested, @OptionValues, @OutputDirectories, @OutputDirectory, @OutputFile, @OutputFiles or @ReplacedBy.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.MISSING_ANNOTATION
    )
    def "tests ouput of missingAnnotationMessage"() {
        when:
        render missingAnnotationMessage {
            type('Task').property('prop')
            kind('something cool')
            includeLink()
        }

        then:
        outputEquals """
Type 'Task' property 'prop' is missing something cool.

Reason: A property without annotation isn't considered during up-to-date checking.

Possible solutions:
  1. Add something cool.
  2. Mark it as @Internal.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#missing_annotation for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.IGNORED_ANNOTATIONS_ON_FIELD
    )
    def "tests output of ignoredAnnotationOnField"() {
        when:
        render ignoredAnnotationOnField {
            type('Bear').property('claws')
            annotatedWith("Harmless")
            includeLink()
        }

        then:
        outputEquals """
Type 'Bear' field 'claws' without corresponding getter has been annotated with @Harmless.

Reason: Annotations on fields are only used if there's a corresponding getter for the field.

Possible solutions:
  1. Add a getter for field 'claws'.
  2. Remove the annotations on 'claws'.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#ignored_annotations_on_field for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.INCOMPATIBLE_ANNOTATIONS
    )
    def "tests output of incompatibleAnnotations"() {
        when:
        render incompatibleAnnotations {
            type('SuperHero').property('fireballs')
            annotatedWith("Boring")
            incompatibleWith("SuperPower")
            includeLink()
        }

        then:
        outputEquals """
Type 'SuperHero' property 'fireballs' is annotated with @Boring but that is not allowed for 'SuperPower' properties.

Reason: This modifier is used in conjunction with a property of type 'SuperPower' but this doesn't have semantics.

Possible solution: Remove the '@Boring' annotation.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#incompatible_annotations for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.INCORRECT_USE_OF_INPUT_ANNOTATION
    )
    def "tests output of incorrectUseOfInputAnnotation"() {
        when:
        render incorrectUseOfInputAnnotation {
            type('Task').property('input')
            propertyType('FileCollection')
            includeLink()
        }

        then:
        outputEquals """
Type 'Task' property 'input' has @Input annotation used on property of type 'FileCollection'.

Reason: A property of type 'FileCollection' annotated with @Input cannot determine how to interpret the file.

Possible solutions:
  1. Annotate with @InputFile for regular files.
  2. Annotate with @InputDirectory for directories.
  3. If you want to track the path, return File.absolutePath as a String and keep @Input.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#incorrect_use_of_input_annotation for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.MISSING_NORMALIZATION_ANNOTATION
    )
    def "tests output of missingNormalizationStrategy"() {
        when:
        render missingNormalizationStrategy {
            type('SomeCacheableTask').property('inputFiles')
            annotatedWith('InputFiles')
            includeLink()
        }

        then:
        outputEquals """
Type 'SomeCacheableTask' property 'inputFiles' is annotated with @InputFiles but missing a normalization strategy.

Reason: If you don't declare the normalization, outputs can't be re-used between machines or locations on the same machine, therefore caching efficiency drops significantly.

Possible solution: Declare the normalization strategy by annotating the property with either @PathSensitive, @Classpath or @CompileClasspath.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#missing_normalization_annotation for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.UNRESOLVABLE_INPUT
    )
    def "tests output of unresolvableInput"() {
        when:
        render unresolvableInput {
            type('Task').property("inputFiles")
            conversionProblem("cannot convert butter into gold")
            includeLink()
        }

        then:
        outputEquals """
Type 'Task' property 'inputFiles' cannot be resolved: cannot convert butter into gold.

Reason: An input file collection couldn't be resolved, making it impossible to determine task inputs.

Possible solution: Consider using Task.dependsOn instead.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#unresolvable_input for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.IMPLICIT_DEPENDENCY
    )
    def "tests output of implicitDependency"() {
        def location = new File(".").absoluteFile
        when:
        render implicitDependency {
            consumer('consumer')
            producer('producer')
            at(location)
            includeLink()
        }

        then:
        outputEquals """
Gradle detected a problem with the following location: '${location}'.

Reason: Task 'consumer' uses this output of task 'producer' without declaring an explicit or implicit dependency. This can lead to incorrect results being produced, depending on what order the tasks are executed.

Possible solutions:
  1. Declare task 'producer' as an input of 'consumer'.
  2. Declare an explicit dependency on 'producer' from 'consumer' using Task#dependsOn.
  3. Declare an explicit dependency on 'producer' from 'consumer' using Task#mustRunAfter.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#implicit_dependency for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.INPUT_FILE_DOES_NOT_EXIST
    )
    def "tests output of inputDoesNotExist"() {
        def location = dummyLocation()
        when:
        render inputDoesNotExist {
            property('prop')
            file(location)
            includeLink()
        }

        then:
        outputEquals """
Property 'prop' specifies file '${location}' which doesn't exist.

Reason: An input file was expected to be present but it doesn't exist.

Possible solutions:
  1. Make sure the file exists before the task is called.
  2. Make sure that the task which produces the file is declared as an input.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#input_file_does_not_exist for more details about this problem.
"""

        render inputDoesNotExist {
            property('prop')
            dir(location)
            includeLink()
        }

        then:
        outputEquals """
Property 'prop' specifies directory '${location}' which doesn't exist.

Reason: An input file was expected to be present but it doesn't exist.

Possible solutions:
  1. Make sure the directory exists before the task is called.
  2. Make sure that the task which produces the directory is declared as an input.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#input_file_does_not_exist for more details about this problem.
"""

    }

    @ValidationTestFor(
        ValidationProblemId.UNEXPECTED_INPUT_FILE_TYPE
    )
    def "tests output of unexpectedInputType"() {
        def location = new File(".").absoluteFile

        when:
        render unexpectedInputType {
            type('SomeTask').property('tada')
                .kind("file")
                .missing(location)
                .includeLink()
        }

        then:
        outputEquals """
Type 'SomeTask' property 'tada' file '${location}' is not a file.

Reason: Expected an input to be a file but it was a directory.

Possible solutions:
  1. Use a file as an input.
  2. Declare the input as a directory instead.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#unexpected_input_file_type for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_WRITE_OUTPUT
    )
    def "tests output of cannotWriteToDir"() {
        def location = dummyLocation('/tmp/foo/bar')
        def ancestor = dummyLocation('/tmp/foo')

        when:
        render cannotWriteToDir {
            type('Writer').property('output')
            dir(location)
            isNotDirectory()
            includeLink()
        }

        then:
        outputEquals """
Type 'Writer' property 'output' is not writable because '${location}' is not a directory.

Reason: Expected '${location}' to be a directory but it's a file.

Possible solution: Make sure that the 'output' is configured to a directory.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#cannot_write_output for more details about this problem.
"""

        when:
        render cannotWriteToDir {
            type('Writer').property('output')
            dir(location)
            ancestorIsNotDirectory(ancestor)
            includeLink()
        }

        then:
        outputEquals """
Type 'Writer' property 'output' is not writable because '${location}' ancestor '${ancestor}' is not a directory.

Reason: Expected '${ancestor}' to be a directory but it's a file.

Possible solution: Make sure that the 'output' is configured to a directory.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#cannot_write_output for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_WRITE_OUTPUT
    )
    def "tests output of cannotWriteToFile"() {
        def location = dummyLocation('/tmp/foo/bar')
        def ancestor = dummyLocation('/tmp/foo')

        when:
        render cannotWriteToFile {
            type('Writer').property('output')
            file(location)
            isNotFile()
            includeLink()
        }

        then:
        outputEquals """
Type 'Writer' property 'output' is not writable because '${location}' is not a file.

Reason: Cannot write a file to a location pointing at a directory.

Possible solution: Configure 'output' to point to a file, not a directory.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#cannot_write_output for more details about this problem.
"""

        when:
        render cannotWriteToFile {
            type('Writer').property('output')
            file(location)
            ancestorIsNotDirectory(ancestor)
            includeLink()
        }

        then:
        outputEquals """
Type 'Writer' property 'output' is not writable because '${location}' ancestor '${ancestor}' is not a directory.

Reason: Cannot write a file to a location pointing at a directory.

Possible solution: Configure 'output' to point to a file, not a directory.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#cannot_write_output for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_WRITE_TO_RESERVED_LOCATION
    )
    def "tests output of cannotWriteToReservedLocation"() {
        def reserved = dummyLocation()

        when:
        render cannotWriteToReservedLocation {
            type('SomeTransform').property('mixed')
            forbiddenAt(reserved)
            includeLink()
        }

        then:
        outputEquals """
Type 'SomeTransform' property 'mixed' points to '${reserved}' which is managed by Gradle.

Reason: Trying to write an output to a read-only location which is for Gradle internal use only.

Possible solution: Select a different output location.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#cannot_write_to_reserved_location for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_WRITE_OUTPUT
    )
    def "tests output of cannotCreateRootOfFileTree"() {
        def location = dummyLocation('/tmp/foo/bar')

        when:
        render cannotCreateRootOfFileTree {
            type('Writer').property('output')
            dir(location)
            includeLink()
        }

        then:
        outputEquals """
Type 'Writer' property 'output' is not writable because '${location}' is not a directory.

Reason: Expected the root of the file tree '${location}' to be a directory but it's a file.

Possible solution: Make sure that the root of the file tree 'output' is configured to a directory.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#cannot_write_output for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.UNSUPPORTED_NOTATION
    )
    def "tests output of unsupportedNotation"() {
        when:
        render unsupportedNotation {
            type('Human').property('face')
            value('visible', 'person')
            candidates("a mask", "a vaccine")
            cannotBeConvertedTo('safe person')
            includeLink()
        }

        then:
        outputEquals """
Type 'Human' property 'face' has unsupported value 'visible'.

Reason: Type 'person' cannot be converted to a safe person.

Possible solutions:
  1. Use a mask.
  2. Use a vaccine.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#unsupported_notation for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.INVALID_USE_OF_CACHEABLE_ANNOTATION
    )
    def "tests output of invalidUseOfCacheableAnnotation"() {
        when:
        render invalidUseOfCacheableAnnotation {
            type('SomeTransform')
            invalidAnnotation('CacheableTask')
            onlyMakesSenseOn('Task')
            includeLink()
        }

        then:
        outputEquals """
Type 'SomeTransform' is incorrectly annotated with @CacheableTask.

Reason: This annotation only makes sense on Task types.

Possible solution: Remove the annotation.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#invalid_use_of_cacheable_annotation for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.CANNOT_USE_OPTIONAL_ON_PRIMITIVE_TYPE
    )
    def "tests output of optionalOnPrimitive"() {
        when:
        render optionalOnPrimitive {
            type('Person').property('age')
            primitive(int.class)
            includeLink()
        }

        then:
        outputEquals """
Type 'Person' property 'age' of type int shouldn't be annotated with @Optional.

Reason: Properties of primitive type cannot be optional.

Possible solutions:
  1. Remove the @Optional annotation.
  2. Use the java.lang.Integer type instead.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#cannot_use_optional_on_primitive_types for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.REDUNDANT_GETTERS
    )
    def "tests output of redundantGetters"() {
        when:
        render redundantGetters {
            type('Person').property('nice')
            includeLink()
        }

        then:
        outputEquals """
Type 'Person' property 'nice' has redundant getters: 'getNice()' and 'isNice()'.

Reason: Boolean property 'nice' has both an `is` and a `get` getter.

Possible solutions:
  1. Remove one of the getters.
  2. Annotate one of the getters with @Internal.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#redundant_getters for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.MUTABLE_TYPE_WITH_SETTER
    )
    def "tests output of mutableSetter"() {
        when:
        render mutableSetter {
            property('someProperty')
            propertyType('Property<String>')
            includeLink()
        }

        then:
        outputEquals """
Property 'someProperty' of mutable type 'Property<String>' is writable.

Reason: Properties of type 'Property<String>' are already mutable.

Possible solution: Remove the 'setSomeProperty' method.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#mutable_type_with_setter for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.CACHEABLE_TRANSFORM_CANT_USE_ABSOLUTE_SENSITIVITY
    )
    def "tests output of invalidUseOfAbsoluteSensitivity"() {
        when:
        render invalidUseOfAbsoluteSensitivity {
            type('Hello').property('world')
            includeLink()
        }

        then:
        outputEquals """
Type 'Hello' property 'world' is declared to be sensitive to absolute paths.

Reason: This is not allowed for cacheable transforms.

Possible solution: Use a different normalization strategy via @PathSensitive, @Classpath or @CompileClasspath.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#cacheable_transform_cant_use_absolute_sensitivity for more details about this problem.
"""
    }

    @ValidationTestFor(
        ValidationProblemId.UNKNOWN_IMPLEMENTATION
    )
    def "tests output of unknown implementation of nested property implemented by lambda"() {
        when:
        render implementationUnknown(true) {
            nestedProperty('action')
            implementedByLambda('LambdaAction')
            includeLink()
        }

        then:
        outputEquals """
Property 'action' was implemented by the Java lambda 'LambdaAction\$\$Lambda\$<non-deterministic>'. Using Java lambdas is not supported, use an (anonymous) inner class instead.

Reason: Gradle cannot track inputs when it doesn't know their implementation.

Possible solution: Use an (anonymous) inner class instead.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#implementation_unknown for more details about this problem."""
    }

    @ValidationTestFor(
        ValidationProblemId.UNKNOWN_IMPLEMENTATION
    )
    def "tests output of unknown implementation of additional task action implemented by lambda"() {
        when:
        render implementationUnknown(true) {
            additionalTaskAction(':myTask')
            implementedByLambda('LambdaAction')
            includeLink()
        }

        then:
        outputEquals """
Additional action of task ':myTask' was implemented by the Java lambda 'LambdaAction\$\$Lambda\$<non-deterministic>'. Using Java lambdas is not supported, use an (anonymous) inner class instead.

Reason: Gradle cannot track inputs when it doesn't know their implementation.

Possible solution: Use an (anonymous) inner class instead.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#implementation_unknown for more details about this problem."""
    }

    @ValidationTestFor(
        ValidationProblemId.UNKNOWN_IMPLEMENTATION
    )
    def "tests output of unknown implementation with unknown classloader"() {
        when:
        render implementationUnknown(true) {
            implementationOfTask(':myTask')
            unknownClassloader('Unknown')
            includeLink()
        }

        then:
        outputEquals """
Implementation of task ':myTask' was loaded with an unknown classloader (class 'Unknown').

Reason: Gradle cannot track inputs when it doesn't know their implementation.

Possible solution: Use an (anonymous) inner class instead.

Please refer to https://docs.gradle.org/current/userguide/validation_problems.html#implementation_unknown for more details about this problem."""
    }

    @ValidationTestFor(
        ValidationProblemId.TEST_PROBLEM
    )
    def "tests output of dummyValidationProblem"() {
        when:
        render dummyValidationProblem('Foo', 'Bar', 'with some description', 'some reason')

        then:
        outputEquals """
Type 'Foo' property 'Bar' with some description.

Reason: Some reason.
"""
    }

    private File dummyLocation(String path = '/tmp/foo') {
        Stub(File) {
            getAbsolutePath() >> path
            toString() >> path
        }
    }

    private void render(String message) {
        condition = new VerificationFixture(message)
    }

    static String normalize(String input) {
        TextUtil.normaliseFileAndLineSeparators(input)
            .replaceAll(Pattern.quote(GradleVersion.current().version), "current")
            .trim()
    }

    @CompileStatic
    private static class VerificationFixture {
        private final String actualMessage

        VerificationFixture(String message) {
            actualMessage = normalize(message)
        }

        void outputEquals(String rendered) {
            String expected = normalize(rendered)
            if (actualMessage != expected) {
                printForCopyInTest()
            }
            assert actualMessage == expected
        }

        private void printForCopyInTest() {
            println("Incorrect test expectation. You might want to verify this message an copy this expectation in the test if you changed the rendering:")
            println()
            println('outputEquals """')
            println(actualMessage)
            println('"""')
        }
    }
}
