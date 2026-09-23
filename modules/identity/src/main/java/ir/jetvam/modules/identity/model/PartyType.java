package ir.jetvam.modules.identity.model;

/**
 * Distinguishes natural persons from legal organizations in the party registry.
 * Authentication accounts reference a party instead of duplicating identity data.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public enum PartyType {
    INDIVIDUAL,
    ORGANIZATION
}
