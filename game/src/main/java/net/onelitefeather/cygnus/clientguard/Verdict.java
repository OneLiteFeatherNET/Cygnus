package net.onelitefeather.cygnus.clientguard;

import java.util.Objects;

/**
 * The outcome of checking a client: it may stay, or it is blocked and the reason says why.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public sealed interface Verdict permits Verdict.Allowed, Verdict.Blocked {

    /**
     * The client passed every check.
     */
    record Allowed() implements Verdict {
    }

    /**
     * The client failed a check.
     *
     * @param reason a human readable reason, for the log. It never reaches the client, so it may name the brand
     *               or channel the client sent.
     */
    record Blocked(String reason) implements Verdict {

        /**
         * Checks the reason.
         *
         * @throws NullPointerException if the reason is missing
         */
        public Blocked {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
