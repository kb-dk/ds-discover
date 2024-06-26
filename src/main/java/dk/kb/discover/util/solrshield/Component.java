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

import dk.kb.discover.util.solrshield.params.Param;
import dk.kb.util.yaml.NotFoundException;
import dk.kb.util.yaml.YAML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Representation of a Solr component, i.e. {@code search}, {@code facet}, {@code highlight}...
 */
public abstract class Component<T extends Component<T>> extends ProfileElement<T> {

    public boolean defaultEnabled = false;
    public boolean allowed = false;
    public double weightConstant = 0.0;
    public Map<String, Param<?, ?>> params = new HashMap<>(); // Not set in base Component

    public boolean enabled;

    public Component(Profile profile, String name, YAML config) {
        super(profile, name);
        if (!config.containsKey("params")) {
            throw new IllegalArgumentException("No configuration sub map 'params' for the " + name + " component");
        }
        defaultEnabled = config.getBoolean("defaultEnabled", defaultEnabled);
        allowed = config.getBoolean("allowed", allowed);
        weightConstant = config.getDouble("weightConstant", weightConstant);

        enabled = defaultEnabled;
    }

    @Override
    protected void deepCopyNonAtomicAttributes(T clone) {
        clone.params = params.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().deepCopy(clone.profile)));
        clone.alignParams();
    }

    /**
     * If the {@code config} contains a sub-config with key {@code name}, the config is extracted and send to
     * {@code constructor}. The {@link Param} returned by the {@code constructor} is added to {@link #params}.
     * If the key {@code name} is not present, a warning is logged.
     * @param config      parameter configurations.
     * @param name        name of the wanted sub-config for parameter construction.
     * @param constructor produces a {@link Param} from the sub-configuration.
     */
    protected void addParam(YAML config, String name, Function<YAML, Param<?, ?>> constructor) {
        if (!containsKey(config, name)) {
            throw new NullPointerException(
                    "Attempted to construct the Param '" + name + "' but no sub-configuration was found");
        }

        Param<?, ?> param = constructor.apply(getSubMap(config, name));
        param.name = name;
        params.put(name, param);
    }

    /**
     * Temporary hack to add support for null-value keys until kb-util gets up to speed.
     * @param config a YAML config.
     * @param name key used for lookup. Must only be a single level deep.
     * @return true if the key exists in the {@code config}.
     */
    // TODO: Remove this when kb-util has support for null values
    private boolean containsKey(YAML config, String name) {
        return config.keySet().stream().anyMatch(name::equals);
    }

    /**
     * Temporary hack to add support for null-value keys until kb-util gets up to speed.
     * @param config a YAML config.
     * @param key key used for lookup. Must only be a single level deep.
     * @return the submap for the given key or an empty map if the value is null.
     */
    // TODO: Remove this when kb-util has support for null values
    private YAML getSubMap(YAML config, String key) {
        if (!containsKey(config, key)) {
            throw new NullPointerException("The key '" + key + "' was not present in the config");
        }
        try {
            return config.getSubMap('"' + key + '"'); // We know this is level 1 so we can escape the key safely
        } catch (NotFoundException e) {
            return new YAML();
        }
    }

    /**
     * Apply the parameters in the request.
     * @param request a Solr request, represented as map of {@code key, values}.
     * @return keys for the parameters that were applied.
     */
    public Set<String> apply(Iterable<Map.Entry<String, String[]>> request) {
        return StreamSupport.stream(request.spliterator(), true)
                .filter(e -> params.containsKey(e.getKey()))
                .peek(e -> params.get(e.getKey()).apply(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Utilty method for fetching params with implicit casting.
     * @param name the name of a given param.
     * @return the {@code param}, cast to the expected type.
     * @param <C> a subclass of {@code Param}.
     */
    @SuppressWarnings("unchecked")
    protected <C extends Param<?, ?>> C getParam(String name) {
        if (!params.containsKey(name)) {
            throw new NullPointerException(
                    "No param with '" + name + "' exists im params. Available keys are " + params.keySet());
        }
        return (C)params.get(name);
    }

    /**
     * Ensure that the {@link Param}s in {@link #params} are the same as their first class attributes.
     * <p>
     * Important: The {@link Param}s in {@link #params} are authoritative and should override all other pointers.
     */
    abstract void alignParams();

    @Override
    public double getWeight() {
        return enabled ? weightConstant : 0.0;
    }

    @Override
    public boolean isAllowed(List<String> reasons) {
        if (!enabled) {
            return true;
        }
        boolean allowed = true;
        if (!this.allowed) {
            reasons.add("Component " + name + " not allowed as the component itself is not allowed");
            allowed = false;
        }
        return allowed & params.values().parallelStream() // Binary & as we want to collect all reasons for not allowed
                .allMatch(param -> param.isAllowed(reasons));
    }
}
