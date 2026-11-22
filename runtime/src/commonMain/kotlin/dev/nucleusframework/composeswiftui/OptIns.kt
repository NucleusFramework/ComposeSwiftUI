package dev.nucleusframework.composeswiftui

/**
 * Experimental API of Compose SwiftUI: may change or go away in a minor release, without a deprecation cycle.
 * Opt in with `@OptIn(ExperimentalComposeSwiftUIApi::class)`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING, message = "Experimental Compose SwiftUI API: it may change in a minor release.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS, AnnotationTarget.CONSTRUCTOR)
public annotation class ExperimentalComposeSwiftUIApi

/**
 * Used by the generated bindings, the Swift renderer and the IDE tooling, not by apps: may change in any release.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Internal Compose SwiftUI API, for the generated bindings and tooling.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS, AnnotationTarget.CONSTRUCTOR)
public annotation class InternalComposeSwiftUIApi
