package com.hmdm.launcher.helper;

/**
 * Web Filter: what to do with the Android private DNS on this sync (design D10). Pure logic, no Android calls.
 */
public final class PrivateDnsDecision {

    public enum Action { APPLY, REMOVE, UNSUPPORTED, NONE }

    // Value of UserManager.DISALLOW_CONFIG_PRIVATE_DNS (API 29)
    public static final String RESTRICTION = "disallow_config_private_dns";

    public final Action action;
    public final String host;
    /** For REMOVE: whether the private DNS restriction may be lifted (the profile does not impose it itself). */
    public final boolean releaseRestriction;

    private PrivateDnsDecision(Action action, String host, boolean releaseRestriction) {
        this.action = action;
        this.host = host;
        this.releaseRestriction = releaseRestriction;
    }

    public static PrivateDnsDecision decide(int sdkInt, boolean deviceOwner, String receivedHost,
                                            String lastAppliedHost, String profileRestrictions) {
        String host = receivedHost == null || receivedHost.trim().isEmpty() ? null : receivedHost.trim();
        if (host != null) {
            if (sdkInt < 29 || !deviceOwner) {
                return new PrivateDnsDecision(Action.UNSUPPORTED, host, false);
            }
            // Applied again on every sync: cheap, and it repairs a DNS changed behind the launcher's back
            return new PrivateDnsDecision(Action.APPLY, host, false);
        }
        if (lastAppliedHost != null && sdkInt >= 29 && deviceOwner) {
            boolean profileImposes = profileRestrictions != null && profileRestrictions.contains(RESTRICTION);
            return new PrivateDnsDecision(Action.REMOVE, null, !profileImposes);
        }
        return new PrivateDnsDecision(Action.NONE, null, false);
    }
}
