package org.example.ebnfFormatter.match;

public sealed interface BoundValue permits RawValue, AppliedRuleValue {
    Object legacyValue();
}