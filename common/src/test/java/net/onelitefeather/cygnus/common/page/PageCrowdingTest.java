package net.onelitefeather.cygnus.common.page;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageCrowdingTest {

    @Test
    void testALonelySpotWeighsMoreThanACrowdedOne() {
        Map<PageResource, Double> weights = PageCrowding.weights(PageProviderTest.map());

        assertTrue(weights.get(PageProviderTest.LONELY.getFirst()) > weights.get(PageProviderTest.CROWDED.getFirst()),
                "a spot among many others must come up less often than one on its own");
    }

    @Test
    void testTheWeightsAreCapped() {
        for (double weight : PageCrowding.weights(PageProviderTest.map()).values()) {
            assertTrue(weight >= PageCrowding.LEAST_WEIGHT && weight <= PageCrowding.MOST_WEIGHT,
                    "weight " + weight + " must stay between the caps");
        }
    }

    @Test
    void testOnlyACrowdedSpotCountsAsCrowded() {
        Map<PageResource, Double> weights = PageCrowding.weights(PageProviderTest.map());

        assertTrue(PageCrowding.isCrowded(weights.get(PageProviderTest.CROWDED.getFirst())));
        assertFalse(PageCrowding.isCrowded(weights.get(PageProviderTest.LONELY.getFirst())));
    }
}
