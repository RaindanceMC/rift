package de.raindancer.modules.rift.model;

/**
 * What an owner decided about pages, as the page rule reads it.
 *
 * @param freeEveryHours active hours per free page; zero or less for none
 */
public record PageTerms(int starting, int most, int freeEveryHours, int mostFree) {
}
