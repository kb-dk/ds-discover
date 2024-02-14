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
package dk.kb.discover.util.solrshield;

import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Representation of a given profile for SolrShield.
 * <p>
 * The implementation uses the <a href="https://en.wikipedia.org/wiki/Prototype_pattern">Prototype Pattern</a> to
 * avoid maintaining 1:1 mapped configuration and instance objects.
 * <p>
 * The profile is used for calculating the weight of a query. Different components (search, facet, highlight...)
 * are dependent on each other and on the fields. To handle interdependency, all elements of the profile are aware
 * of each other.
 */
public class Profile extends ProfileElement<Profile> {
    private static final Logger log = LoggerFactory.getLogger(Profile.class);

    /*
    # Simply activating a call comes at a cost
     */
    public double weight_constant = 1000;

    /*
    # Ideally all fields are listed under 'fields'.
    # 'unlisted_fields' controls handling of requests for unlisted fields
    unlisted_fields:
      allowed: true
      weight: 100
     */
    public boolean unlistedFieldsAllowed = false;
    public double unlistedFieldsWeight = 100.0;

    /*
    # How to handle parameters not specified in SolrShield.
    # This is shared between all components
     */
    public boolean unlistedParamsAllowed = false;
    public double unlistedParamsWeight = 1000.0;

    /*
    # The fields section assign base weight to each field.
    # The scale goes from 1 to 1000, where
    # "An integer field" is 1
    # "A DocValued StrField, where the corpus holds a few thousand short unique values" is 5
    # "A DocValued StrField, where the values are mostly unique" is 10
    # "A TextField with a 5-10 words" is 50 (text fields are markedly heavier to process than StrFields)
    # "A TextField with a 100-1000 words" is 100
    # "A TextField with a full transcription of hours of speech" is 1000 (about 50 book pages)
    # "A TextField holding a full book of hundreds of pages" is 5000 (yes, it breaks the scale)
    fields:
      - name: id
        weight: 10
      - name: resource_id
        weight: 10
      - name: origin
        weight: 5
     */
    public Map<String, Field> fields;

    /**
     * First class search component. Always present, always enabled.
     */
    public SearchComponent search;

    /**
     * First class facet component. Always present, but might not be enabled.
     */
    public FacetComponent facet;

    /**
     * Create a base setup for SolrShield. and initialize based on the setup specified in {@code config}.
     * <p>
     * Note: This configuration must state the SolrShield properties directly.
     * @param config a SolrShield configuration.
     */
    public Profile(YAML config) {
        super(null, "config");
        weight_constant = config.getDouble("weight_constant", weight_constant);

        unlistedFieldsAllowed = config.getBoolean("unlisted_fields.allowed", unlistedFieldsAllowed);
        unlistedFieldsWeight = config.getDouble("unlisted_fields.weight", unlistedFieldsWeight);
        fields = config.containsKey("fields") ?
                config.getYAMLList("fields").stream()
                        .map(fieldMap -> new Field(this, fieldMap))
                        .collect(Collectors.toMap(k -> k.name, v -> v)) :
                Collections.emptyMap();

        unlistedParamsAllowed = config.getBoolean("unlisted_params.allowed", unlistedParamsAllowed);
        unlistedParamsWeight = config.getDouble("unlisted_params.weight", unlistedParamsWeight);

        search = new SearchComponent(this, config.getSubMap("components.search")); // Mandatory
        facet = new FacetComponent(this, config.getSubMap("components.facet"));    // Mandatory

        log.info("Created base SorShield config " + this);
    }

    /**
     * @return an independent/deep copy of this Profile.
     */
    public Profile deepCopy() {
        return super.deepCopy(null);
    }

    // TODO: Signal illegal request
    /**
     * Create a deep copy of this Profile, apply the parameters in the request and return the updated copy.
     * @param request a Solr request, represented as map of {@code key, values}.
     */
    public Profile apply(Iterable<Map.Entry<String, String[]>> request) {
        Profile clone = deepCopy();
        Set<String> processedKeys = new HashSet<>();
        processedKeys.addAll(clone.search.apply(request));
        processedKeys.addAll(clone.facet.apply(request));

        Map<String, String[]> unhandled =
                StreamSupport.stream(request.spliterator(), false)
                        .filter(e -> !processedKeys.contains(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        // TODO: Handle unhandled keys

        return clone;
    }

    @Override
    protected void deepCopyNonAtomicAttributes(Profile clone) {
        super.deepCopyNonAtomicAttributes(clone);
        clone.fields = fields.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().deepCopy(clone.profile)));
        search = search.deepCopy(clone.profile);
        facet = facet.deepCopy(clone.profile);
    }

    @Override
    public String toString() {
        return "Profile{" +
                "unlistedFieldsAllowed=" + unlistedFieldsAllowed +
                ", unlistedFieldsWeight=" + unlistedFieldsWeight +
                ", unlistedParamsAllowed=" + unlistedParamsAllowed +
                ", unlistedParamsWeight=" + unlistedParamsWeight +
                ", fields=" + fields +
                ", components.search=" + search +
                ", components.facet=" + facet +
                '}';
    }

    @Override
    double getWeight() {
        return weight_constant + search.getWeight() + facet.getWeight();
    }

    /**
     * Calculate the sum of weights for the given {@code fields}.
     * @param fields a list of Solr fields.
     * @return the sum of field weights.
     */
    public double getFieldsWeight(List<String> fields) {
        return fields.stream()
                .map(this::getFieldWeight)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    /**
     * Resolve the field weight. If the field is not in {@link #fields}, the weight {@link #unlistedFieldsWeight}
     * will be used.
     * @param field a Solr field.
     * @return the weight of the field.
     */
    public double getFieldWeight(String field) {
        return this.fields.containsKey(field) ?
                this.fields.get(field).getWeight() :
                unlistedFieldsWeight;
    }
}