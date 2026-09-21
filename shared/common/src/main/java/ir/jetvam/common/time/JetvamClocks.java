package ir.jetvam.common.time;

import java.time.Clock;

/** Central definitions for clocks used by Jetvam runtime and utilities. */
public final class JetvamClocks {

    private static final Clock SYSTEM_UTC = Clock.systemUTC();

    private JetvamClocks() {
    }

    public static Clock systemUtc() {
        return SYSTEM_UTC;
    }
}
