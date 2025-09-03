package net.onelitefeather.cygnus.template;

import net.onelitefeather.cygnus.event.GameFinishEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.List;
import java.util.stream.Stream;

/**
 * This class provides a test template for the {@link GameFinishEvent.Reason} enum.
 * It allows to define a {@link {@link GameFinishEvent.Reason} as a parameter for the test.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @see TestTemplate
 * @since 1.0.0
 */
public final class GameFinishReasonTemplate implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(@NotNull ExtensionContext context) {
        return true;  // Always support this test template
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(@NotNull ExtensionContext context) {
        return Stream.of(GameFinishEvent.Reason.values())
                .map(this::invocationContextForReason);
    }

    private TestTemplateInvocationContext invocationContextForReason(@NotNull GameFinishEvent.Reason reason) {
        return new TestTemplateInvocationContext() {

            @Override
            public String getDisplayName(int invocationIndex) {
                return String.format("Test GameFinishEvent.Reason: %s", reason.name());
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
                        return parameterContext.getParameter().getType() == GameFinishEvent.Reason.class;
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
                        return reason;
                    }
                });
            }
        };
    }
}
