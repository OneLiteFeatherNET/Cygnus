/**
 * OpenTelemetry traces of a Cygnus service.
 * <p>
 * Only the tracing <em>API</em> is used. The OpenTelemetry javaagent supplies the SDK and the
 * exporter in production, and without it every call here lands on a no-op implementation. Nothing in
 * this package reads {@code GlobalOpenTelemetry}: {@link net.onelitefeather.cygnus.telemetry.CygnusTracing}
 * is built once by the composition root and handed to whatever creates spans, which is what lets
 * the tests run against a private SDK.
 * </p>
 */
@NotNullByDefault
package net.onelitefeather.cygnus.telemetry;

import org.jetbrains.annotations.NotNullByDefault;
