package com.gnoemes.shikimori.di.app.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * The GraphQL client that sends the user's token.
 *
 * Separate from {@link GraphqlApi}, which is deliberately anonymous: the catalog's `mylist` filter
 * needs authentication, and an anonymous call **silently ignores** it rather than failing, so the
 * filter would quietly stop filtering.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthGraphqlApi {
}
