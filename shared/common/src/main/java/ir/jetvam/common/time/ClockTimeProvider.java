package ir.jetvam.common.time;

import java.time.Clock;
import java.util.Objects;

/** Default framework-neutral {@link TimeProvider} backed by a Java clock.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class ClockTimeProvider implements TimeProvider {

    private final Clock clock;

    public ClockTimeProvider(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Clock clock() {
        return clock;
    }
}
