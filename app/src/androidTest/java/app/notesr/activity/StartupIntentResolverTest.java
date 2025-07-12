package app.notesr.activity;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import org.junit.Test;

import java.util.List;
import java.util.function.Supplier;

public class StartupIntentResolverTest {
    @Test
    public void testResolveReturnsFirstNonNullIntent() {
        Intent intent1 = new Intent().setAction("action1");
        Intent intent2 = new Intent().setAction("action2");
        Intent defaultIntent = new Intent().setAction("default");

        List<Supplier<Intent>> suppliers = List.of(
                () -> null,
                () -> null,
                () -> intent1,
                () -> intent2
        );

        StartupIntentResolver resolver = new StartupIntentResolver(suppliers, defaultIntent);
        Intent result = resolver.resolve();

        assertEquals("Expected the first non-null intent to be returned", intent1, result);
    }

    @Test
    public void testResolveReturnsDefaultIntentIfAllAreNull() {
        Intent defaultIntent = new Intent().setAction("default");

        List<Supplier<Intent>> suppliers = List.of(
                () -> null,
                () -> null,
                () -> null
        );

        StartupIntentResolver resolver = new StartupIntentResolver(suppliers, defaultIntent);
        Intent result = resolver.resolve();

        assertEquals("Expected default intent to be returned "
                + "when all suppliers return null", defaultIntent, result);
    }

    @Test
    public void testResolveIgnoresSuppliersAfterFirstNonNull() {
        Intent intentA = new Intent().setAction("A");
        Intent intentB = new Intent().setAction("B");
        Intent defaultIntent = new Intent().setAction("default");

        List<Supplier<Intent>> suppliers = List.of(
                () -> intentA,
                () -> intentB,
                () -> null
        );

        StartupIntentResolver resolver = new StartupIntentResolver(suppliers, defaultIntent);
        Intent result = resolver.resolve();

        assertEquals("Expected only the first non-null intent to be returned, "
                + "ignoring the rest", intentA, result);
    }
}