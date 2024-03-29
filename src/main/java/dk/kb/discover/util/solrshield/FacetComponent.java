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

import dk.kb.discover.util.solrshield.params.*;
import dk.kb.util.yaml.YAML;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Representation of a Solr Facet component.
 */
public class FacetComponent extends Component<FacetComponent> {

    protected StringParam facetQuery;
    protected FieldsParam facetField; // Multiple values, but the Solr param name is singular 'facet.field'
    protected IntegerParam facetLimit;
    protected StringParam facetSort;
    protected IntegerParam facetMincount;
    protected BooleanParam facetExists;

    // TODO: Handle field-specific tweaks (sort + limit)

    public FacetComponent(Profile profile, YAML config) {
        super(profile, "facet", config);

        YAML paramsConf = config.getSubMap("params");
        addParam(paramsConf, "facet.query", paramConf -> this.facetQuery = new StringParam(profile, paramConf, false));
        addParam(paramsConf, "facet.field", paramConf -> this.facetField = new FieldsParam(profile, paramConf));
        addParam(paramsConf, "facet.limit", paramConf -> this.facetLimit = new IntegerParam(profile, paramConf));
        addParam(paramsConf, "facet.sort", paramConf -> this.facetSort = new StringParam(profile, paramConf, false));
        addParam(paramsConf, "facet.mincount", paramConf -> this.facetMincount = new IntegerParam(profile, paramConf));
        addParam(paramsConf, "facet.exists", paramConf -> this.facetExists = new BooleanParam(profile, paramConf));
    }

    @Override
    void alignParams() {
        facetQuery = getParam("facet.query");
        facetField = getParam("facet.field");
        facetLimit = getParam("facet.limit");
        facetSort = getParam("facet.sort");
        facetMincount = getParam("facet.mincount");
        facetExists = getParam("facet.exists");
    }

    @Override
    public Set<String> apply(Iterable<Map.Entry<String, String[]>> request) {
        Optional<Boolean> enabled = StreamSupport.stream(request.spliterator(), true)
                .filter(e -> "facet".equals(e.getKey()))
                .filter(e -> e.getValue() != null && e.getValue().length > 0)
                .map(e -> Boolean.parseBoolean(e.getValue()[0]))
                .findAny();
        this.enabled = enabled.orElse(defaultEnabled);

        Set<String> applied = super.apply(request);
        if (enabled.isPresent()) {
            applied.add("facet");
        }
        return applied;
    }

    @Override
    public double getWeight() {
        return !enabled ? 0.0 :
                super.getWeight() +
                        facetQuery.getWeight() +
                        facetLimit.getWeight() + facetLimit.getValue() * facetLimit.weightFactor * facetField.getWeight() +
                        facetSort.getWeight() +
                        facetMincount.getWeight() +
                        facetExists.getWeight();
    }

}
