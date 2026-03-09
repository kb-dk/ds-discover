package dk.kb.discover.util.solrshield;

import dk.kb.discover.SolrManager;
import dk.kb.discover.config.ServiceConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.util.yaml.YAML;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
class SolrShieldTest {
    private static final Logger log = LoggerFactory.getLogger(SolrShieldTest.class);

    @BeforeAll
    static void setup() throws IOException {   
      ServiceConfig.getInstance().initialize("solrshield-test1.yaml");
      SolrShield.ensureConfig();
    }

    @Test
    void testDeepCopySearch() {
        SearchComponent search = SolrShield.getInstance().profile.search;
        assertSearchParamSame(search, "origo");
        assertFalse(search.isCopy, "Origo Search should initially not be a copy");

        SearchComponent searchCopy = search.deepCopy(null);

        assertSearchParamSame(searchCopy, "deep copy");

        assertTrue(searchCopy.isCopy, "The deep copy of Search should be marked as a copy");
        assertFalse(search.isCopy, "The origo Search should still not be a copy itself after deep copy");
    }

    // After deep copying, the first class attributes for search should match the ones in the params map
    void assertSearchParamSame(SearchComponent search, String searchDesignation) {
        assertSame(search.q, search.getParam("q"),
                "The 'q' param from " + searchDesignation + " search should be the same");
        assertSame(search.fq, search.getParam("fq"),
                        "The 'fq' param from " + searchDesignation + " search should be the same");
        assertSame(search.fl, search.getParam("fl"),
                        "The 'fl' param from " + searchDesignation + " search should be the same");
        // TODO: Add the rest of the search params
    }

    @Test
    void testDeepCopySearchApply() {
        SearchComponent search = SolrShield.getInstance().profile.search;
        assertFalse(search.isCopy, "Origo Search should initially not be a copy");

        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title", "text"}
        );
        SolrShield.evaluate(request.entrySet(), 1000.0);

        assertSame(search, SolrShield.getInstance().profile.search, "The base search should not have been replaced");
        assertFalse(search.isCopy, "The origo Search should still not be a copy itself after test");
    }

    @Test
    void basicSearch() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title", "text"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 1000.0);
        log.debug("SolrShield response: " + response);
        assertTrue(response.allowed, "Request " + toString(request) + " should be allowed, but was not with reasons " +
                response.reasons);
        assertTrue(response.weight > 0.0, "Response should have weight > 0.0 but had " + response.weight);
    }

    @Test
    void searchFields() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title", "text"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        double firstWeight = response.weight;

        Map<String, String[]> request2 = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title"}
        );
        Response response2 = SolrShield.evaluate(request2.entrySet(), 100000.0);
        double secondWeight = response2.weight;

        assertNotEquals(firstWeight, secondWeight, "The weights for searches with different fields should not be equal");
    }

    @Test
    void searchFieldsStar() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"*, score"} // * expands to all fields, making the request too heavy
        );
        Response response = SolrShield.evaluate(request.entrySet(), 1000.0);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed, "Request " + toString(request) + " should be allowed. Response: " + response);
        assertTrue(response.weight > 1000.0, "Response should have weight > 1000.0 but had " + response.weight);
    }

    @Test
    void basicFacet() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"location", "genre"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 2000.0);
        log.debug("SolrShield response: " + response);
        assertTrue(response.allowed,
                "Request " + toString(request) + " should be allowed, but was not with reasons " + response.reasons);
        assertTrue(response.weight > 0.0,
                "Response should have weight > 0.0 but had " + response.weight);
    }

    @Test
    void facetTooHeavy() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"genre"},
                "facet.limit", new String[]{"50000"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"location", "genre"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 2000.0);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed,
                "Request " + toString(request) + " should not be allowed due to facet.limit. Response: " + response);
        assertTrue(response.weight > 2000.0,
                "The weight of the response should be > 2000 but was " + response.weight);
    }

    @Test
    void facetIllegal() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"genre"},
                "facet.limit", new String[]{"50000"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"location"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), Double.MAX_VALUE);
        log.debug("SolrShield response: " + response);
        assertFalse(response.allowed,
                "Request " + toString(request) + " should not be allowed. Response: " + response);
        assertTrue(response.reasons.toString().contains("is larger than maxValue"),
                "Response should contain clause stating that facet.limit is > maxValue. Response: " + response);
    }

    // --- Unlisted / unknown params ---

    @Test
    void unknownParamRejected() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "bogusParam", new String[]{"value"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertFalse(response.allowed,
                "Request with unknown param should be rejected (unlistedParams.allowed=false)");
        assertTrue(response.reasons.toString().contains("bogusParam"),
                "Reason should mention the unknown param 'bogusParam'");
    }

    @Test
    void extraAllowedParameterAccepted() {
        // queryUUID is in solr.extraAllowedParameters and should bypass shield
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "queryUUID", new String[]{"abc-123"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Request with extraAllowedParameter 'queryUUID' should be accepted but got reasons: " +
                        response.reasons);
    }

    // --- Explicitly denied param ---

    @Test
    void defTypeDenied() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "defType", new String[]{"edismax"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertFalse(response.allowed,
                "Request with defType should be rejected (allowed=false in config)");
        assertTrue(response.reasons.toString().contains("defType"),
                "Reason should mention 'defType'");
    }

    // --- Disabled shield ---

    @Test
    void disabledShieldAllowsEverything() {
        // Temporarily disable the shield by resetting config with enabled=false
        YAML disabledConfig = new YAML();
        // Build a minimal config that disables the shield
        YAML originalConf = ServiceConfig.getConfig().getSubMap("solr.shield");

        try {
            // Create a modified config with enabled=false
            YAML modifiedConf = new YAML(originalConf);
            modifiedConf.put("enabled", false);
            SolrShield.setConfig(modifiedConf);

            // A request that would normally be rejected (unknown param)
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"id"},
                    "bogusParam", new String[]{"value"}
            );
            Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
            assertTrue(response.allowed,
                    "Disabled shield should allow all requests, even with unknown params");
        } finally {
            // Restore original config — must explicitly set enabled=true since
            // the original config may not have the key, and setConfig defaults to current value
            YAML restoreConf = new YAML(originalConf);
            restoreConf.put("enabled", true);
            SolrShield.setConfig(restoreConf);
        }
    }

    // --- Filter query (fq) ---

    @Test
    void filterQueryAccepted() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "fq", new String[]{"genre:drama"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Request with fq should be accepted but got reasons: " + response.reasons);
    }

    @Test
    void filterQueryAddsWeight() {
        Map<String, String[]> requestWithoutFq = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"}
        );
        Response responseWithout = SolrShield.evaluate(requestWithoutFq.entrySet(), 100000.0);

        Map<String, String[]> requestWithFq = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "fq", new String[]{"genre:drama"}
        );
        Response responseWith = SolrShield.evaluate(requestWithFq.entrySet(), 100000.0);

        assertTrue(responseWith.weight > responseWithout.weight,
                "Adding fq should increase weight. Without: " + responseWithout.weight +
                        ", with: " + responseWith.weight);
    }

    // --- rows maxValue ---

    @Test
    void rowsExceedingMaxValueRejected() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "rows", new String[]{"6000"} // maxValue is 5000
        );
        Response response = SolrShield.evaluate(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Request with rows exceeding maxValue should be rejected");
        assertTrue(response.reasons.toString().contains("rows"),
                "Reason should mention 'rows'");
        assertTrue(response.reasons.toString().contains("maxValue"),
                "Reason should mention 'maxValue'");
    }

    @Test
    void rowsWithinMaxValueAccepted() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "rows", new String[]{"100"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Request with rows=100 should be accepted but got reasons: " + response.reasons);
    }

    // --- start maxValue ---

    @Test
    void startExceedingMaxValueRejected() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "start", new String[]{"1500"} // maxValue is 1000
        );
        Response response = SolrShield.evaluate(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Request with start exceeding maxValue should be rejected");
        assertTrue(response.reasons.toString().contains("start"),
                "Reason should mention 'start'");
    }

    // --- q maxChars ---

    @Test
    void queryExceedingMaxCharsRejected() {
        // maxChars for q is 1000
        String longQuery = "a".repeat(1001);
        Map<String, String[]> request = Map.of(
                "q", new String[]{longQuery},
                "fl", new String[]{"id"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Request with q exceeding maxChars should be rejected");
        assertTrue(response.reasons.toString().contains("maxChars"),
                "Reason should mention 'maxChars'");
    }

    @Test
    void queryWithinMaxCharsAccepted() {
        String query = "a".repeat(500);
        Map<String, String[]> request = Map.of(
                "q", new String[]{query},
                "fl", new String[]{"id"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Request with q within maxChars should be accepted but got reasons: " + response.reasons);
    }

    // --- Weight calculation sanity checks ---

    @Test
    void weightCalculationSanity() {
        // Minimal request: q + fl with a single lightweight field
        Map<String, String[]> request = Map.of(
                "q", new String[]{"test"},
                "fl", new String[]{"id"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertTrue(response.allowed, "Simple request should be allowed");

        // Weight should be: profile.weightConstant(100) + search.weightConstant(100) +
        //                   q.weightConstant(10) + fl(weightFactor*fieldWeight) + rows(default) + facet(default)
        // Exact value depends on defaults, but should be deterministic
        double expectedMinWeight = 100 + 100 + 10; // At minimum: profile + search + q constants
        assertTrue(response.weight >= expectedMinWeight,
                "Weight should be at least " + expectedMinWeight + " but was " + response.weight);

        // Run the same request again — weight must be identical (prototype not mutated)
        Response response2 = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertEquals(response.weight, response2.weight,
                "Identical requests should produce identical weights (prototype integrity)");
    }

    @Test
    void moreRowsIncreasesWeight() {
        Map<String, String[]> request10 = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title"},
                "rows", new String[]{"10"}
        );
        Map<String, String[]> request100 = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"title"},
                "rows", new String[]{"100"}
        );
        Response r10 = SolrShield.evaluate(request10.entrySet(), 100000.0);
        Response r100 = SolrShield.evaluate(request100.entrySet(), 100000.0);
        assertTrue(r100.weight > r10.weight,
                "More rows should increase weight. rows=10: " + r10.weight + ", rows=100: " + r100.weight);
    }

    @Test
    void heavierFieldsIncreaseWeight() {
        // id has weight 10, text has weight 200
        Map<String, String[]> requestLight = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"}
        );
        Map<String, String[]> requestHeavy = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"text"}
        );
        Response rLight = SolrShield.evaluate(requestLight.entrySet(), 100000.0);
        Response rHeavy = SolrShield.evaluate(requestHeavy.entrySet(), 100000.0);
        assertTrue(rHeavy.weight > rLight.weight,
                "Heavier field should increase weight. id: " + rLight.weight + ", text: " + rHeavy.weight);
    }

    // --- Facet field whitelist ---

    @Test
    void facetFieldNotInWhitelistRejected() {
        // facet.field has allowedFields whitelist; 'text' is NOT in it
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"text"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Facet on field not in allowedFields should be rejected");
        assertTrue(response.reasons.toString().contains("allowed list"),
                "Reason should mention the allowed list. Reasons: " + response.reasons);
    }

    @Test
    void facetFieldInWhitelistAccepted() {
        // 'catalog' is in the allowedFields list
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "facet", new String[]{"true"},
                "facet.field", new String[]{"catalog"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), 100000.0);
        assertTrue(response.allowed,
                "Facet on field in allowedFields should be accepted but got reasons: " + response.reasons);
    }

    // --- evaluate(Map) overload ---

    @Test
    void evaluateMapOverload() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"}
        );
        // evaluate(Map) uses defaultMaxWeight from config (1000)
        Response response = SolrShield.evaluate(request);
        assertTrue(response.allowed,
                "evaluate(Map) should work and accept simple request. Reasons: " + response.reasons);
        assertEquals(1000.0, response.maxWeight,
                "evaluate(Map) should use defaultMaxWeight from config");
    }

    // --- Default maxWeight enforcement ---

    @Test
    void defaultMaxWeightEnforced() {
        // defaultMaxWeight is 1000 in test config
        // Request with heavy fields + many rows should exceed it
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"text", "freetext", "abstract"},
                "rows", new String[]{"500"}
        );
        Response response = SolrShield.evaluate(request);
        assertFalse(response.allowed,
                "Heavy request should exceed defaultMaxWeight of 1000");
        assertTrue(response.reasons.toString().contains("Weight exceeded"),
                "Reason should mention weight exceeded");
    }

    // --- facet.query denied ---

    @Test
    void facetQueryDenied() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "facet", new String[]{"true"},
                "facet.query", new String[]{"genre:drama"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "facet.query should be rejected (allowed=false in config)");
        assertTrue(response.reasons.toString().contains("facet.query"),
                "Reason should mention 'facet.query'");
    }

    // --- fl denied field (blacklist) ---

    @Test
    void flDeniedField() {
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"text_shingles"}
        );
        Response response = SolrShield.evaluate(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed,
                "Requesting denied field 'text_shingles' in fl should be rejected");
        assertTrue(response.reasons.toString().contains("denied"),
                "Reason should mention denied list");
    }

    // --- Multiple reasons collected ---

    @Test
    void multipleReasonsCollected() {
        // Denied field in fl + facet.limit exceeds maxValue — should collect reasons from both components
        Map<String, String[]> request = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"text_shingles"},   // denied field in search component
                "facet", new String[]{"true"},
                "facet.field", new String[]{"catalog"},
                "facet.limit", new String[]{"50000"}   // exceeds maxValue in facet component
        );
        Response response = SolrShield.evaluate(request.entrySet(), Double.MAX_VALUE);
        assertFalse(response.allowed);
        assertTrue(response.reasons.size() >= 2,
                "Should collect multiple failure reasons but got: " + response.reasons);
    }

    // --- Debug weight ---

    @Test
    void debugAddsSignificantWeight() {
        Map<String, String[]> requestNoDebug = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "rows", new String[]{"1"}
        );
        Map<String, String[]> requestDebug = Map.of(
                "q", new String[]{"*:*"},
                "fl", new String[]{"id"},
                "rows", new String[]{"1"},
                "debug", new String[]{"true"}
        );
        Response rNoDebug = SolrShield.evaluate(requestNoDebug.entrySet(), 100000.0);
        Response rDebug = SolrShield.evaluate(requestDebug.entrySet(), 100000.0);
        assertTrue(rDebug.weight > rNoDebug.weight + 400,
                "Debug should add at least 500 weight (weightConstant=500). " +
                        "Without: " + rNoDebug.weight + ", with: " + rDebug.weight);
    }

    // --- Per-collection shield tests ---

    /**
     * Helper to build a SolrManager config YAML with collections pointing to shield paths.
     */
    private static YAML buildCollectionConfig(Map<String, String> collectionShields) {
        List<Map<String, Object>> collections = new ArrayList<>();
        for (Map.Entry<String, String> entry : collectionShields.entrySet()) {
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("server", "http://localhost:8983");
            inner.put("collection", entry.getKey());
            if (entry.getValue() != null) {
                inner.put("shield", entry.getValue());
            }
            Map<String, Object> collEntry = new LinkedHashMap<>();
            collEntry.put(entry.getKey(), inner);
            collections.add(collEntry);
        }
        Map<String, Object> solr = new LinkedHashMap<>();
        solr.put("collections", collections);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("solr", solr);
        return new YAML(root);
    }

    @Test
    void perCollectionShieldLoaded() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-coll", "solrshield-permissive.yaml",
                "restrictive-coll", "solrshield-restrictive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            Optional<SolrShield> permissive = SolrManager.getShield("permissive-coll");
            Optional<SolrShield> restrictive = SolrManager.getShield("restrictive-coll");

            assertTrue(permissive.isPresent(), "Permissive collection should have a shield");
            assertTrue(restrictive.isPresent(), "Restrictive collection should have a shield");
            assertNotSame(permissive.get(), restrictive.get(),
                    "Different collections should have different shield instances");
        } finally {
            // Restore empty state so other tests aren't affected
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void noShieldForUnconfiguredCollection() {
        // Use a LinkedHashMap so we can include a null value for the no-shield collection
        Map<String, String> collections = new LinkedHashMap<>();
        collections.put("permissive-coll", "solrshield-permissive.yaml");
        collections.put("noshield-coll", null);
        YAML config = buildCollectionConfig(collections);
        SolrManager.getInstance().setConfig(config);

        try {
            Optional<SolrShield> noShield = SolrManager.getShield("noshield-coll");
            assertTrue(noShield.isEmpty(), "Collection without shield config should return empty");
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void permissiveShieldAllowsBasicQuery() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-coll", "solrshield-permissive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield shield = SolrManager.getShield("permissive-coll").orElseThrow();
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"title"},
                    "rows", new String[]{"50"}
            );
            Response response = shield.evaluateRequest(request);
            assertTrue(response.allowed,
                    "Permissive shield should allow basic query. Reasons: " + response.reasons);
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void restrictiveShieldRejectsBasicQuery() {
        YAML config = buildCollectionConfig(Map.of(
                "restrictive-coll", "solrshield-restrictive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield shield = SolrManager.getShield("restrictive-coll").orElseThrow();
            // defaultMaxWeight=100, but weightConstant(100) + search.weightConstant(100) already exceeds it
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"id"}
            );
            Response response = shield.evaluateRequest(request);
            assertFalse(response.allowed,
                    "Restrictive shield (maxWeight=100) should reject even basic queries. " +
                            "Weight: " + response.weight);
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void sameQueryDifferentResultPerCollection() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-coll", "solrshield-permissive.yaml",
                "restrictive-coll", "solrshield-restrictive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield permissive = SolrManager.getShield("permissive-coll").orElseThrow();
            SolrShield restrictive = SolrManager.getShield("restrictive-coll").orElseThrow();

            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"id"}
            );

            Response permissiveResponse = permissive.evaluateRequest(request);
            Response restrictiveResponse = restrictive.evaluateRequest(request);

            assertTrue(permissiveResponse.allowed,
                    "Permissive shield should allow the query. Reasons: " + permissiveResponse.reasons);
            assertFalse(restrictiveResponse.allowed,
                    "Restrictive shield should reject the same query. Weight: " + restrictiveResponse.weight);
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void restrictiveShieldDeniesTextField() {
        YAML config = buildCollectionConfig(Map.of(
                "restrictive-coll", "solrshield-restrictive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield shield = SolrManager.getShield("restrictive-coll").orElseThrow();
            // 'text' is in deniedFields for the restrictive shield
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"text"}
            );
            Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
            assertFalse(response.allowed,
                    "Restrictive shield should deny field 'text'. Reasons: " + response.reasons);
            assertTrue(response.reasons.toString().contains("denied"),
                    "Reason should mention denied list");
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void permissiveShieldAllowsTextField() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-coll", "solrshield-permissive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            SolrShield shield = SolrManager.getShield("permissive-coll").orElseThrow();
            // 'text' is NOT denied in the permissive shield
            Map<String, String[]> request = Map.of(
                    "q", new String[]{"*:*"},
                    "fl", new String[]{"text"}
            );
            Response response = shield.evaluateRequest(request.entrySet(), Double.MAX_VALUE);
            assertTrue(response.allowed,
                    "Permissive shield should allow field 'text'. Reasons: " + response.reasons);
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    @Test
    void shieldCachedAcrossCalls() {
        YAML config = buildCollectionConfig(Map.of(
                "permissive-coll", "solrshield-permissive.yaml"
        ));
        SolrManager.getInstance().setConfig(config);

        try {
            Optional<SolrShield> first = SolrManager.getShield("permissive-coll");
            Optional<SolrShield> second = SolrManager.getShield("permissive-coll");

            assertTrue(first.isPresent());
            assertSame(first.get(), second.get(),
                    "getShield should return the same cached instance on repeated calls");
        } finally {
            SolrManager.getInstance().setConfig(buildCollectionConfig(Map.of()));
        }
    }

    private String toString(Map<String, String[]> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
                .collect(Collectors.joining("\", \"", "[\"", "\"]"));
    }
}
