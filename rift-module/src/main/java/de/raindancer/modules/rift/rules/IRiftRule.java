package de.raindancer.modules.rift.rules;

/** A rule belonging to this module: decides, and does nothing else. No side effects, safe from any thread. */
public interface IRiftRule {

    String describe();
}
