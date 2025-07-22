package dk.kb.discover.api.v1.impl;

import dk.kb.discover.DocumentationExtractor;
import dk.kb.discover.SolrManager;
import dk.kb.discover.SolrService;
import dk.kb.discover.api.v1.DsDiscoverApiApi;
import dk.kb.discover.config.ServiceConfig;
import dk.kb.discover.util.LicenseUtil;
import dk.kb.discover.util.solrshield.Response;
import dk.kb.discover.util.solrshield.SolrShield;
import dk.kb.license.model.v1.GetUserQueryInputDto;
import dk.kb.license.model.v1.GetUsersFilterQueryOutputDto;
import dk.kb.license.util.DsLicenseClient;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Providers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ds-discover
 *
 * <p>ds-discover by the Royal Danish Library 
 *
 */
@InInterceptors(interceptors = "dk.kb.discover.webservice.KBAuthorizationInterceptor")
public class DsDiscoverApiServiceImpl extends ImplBase implements DsDiscoverApiApi {
    private static final Logger log = LoggerFactory.getLogger(DsDiscoverApiServiceImpl.class);

    /**
     * If the config has {@code config.solr.permissive: true}, all parameters are passed on Solr calls.
     * If it is false, only vetted parameters are allowed.
     */
    public static final String PERMISSIVE_KEY = "solr.permissive";
    public static final boolean PERMISSIVE_DEFAULT = false;

    /**
     * Signals that a filter should be cached in Solr (default is already to cache).
     * This prefix has two purposes:
     * <ol>
     *   <li>To signal caching, even though it probably changes nothing</li>
     *   <li>To act as a magic String, making it possible to remove the filter from the response using simple
     *   search/replace</li>
     * </ol>
     */
    public static final String FILTER_CACHE_PREFIX = "{!cache=true}";

    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

    @Context
    private transient UriInfo uriInfo;

    @Context
    private transient SecurityContext securityContext;

    @Context
    private transient HttpHeaders httpHeaders;

    @Context
    private transient Providers providers;

    @Context
    private transient Request request;

    // Disabled as it is always null? TODO: Investigate when it can be not-null, then re-enable with type
    //@Context
    //private transient ContextResolver contextResolver;

    @Context
    private transient HttpServletRequest httpServletRequest;

    @Context
    private transient HttpServletResponse httpServletResponse;

    @Context
    private transient ServletContext servletContext;

    @Context
    private transient ServletConfig servletConfig;

    @Context
    private transient MessageContext messageContext;


    private static DsLicenseClient licenseClient;  
    
    /**
     * Solr [Collection Management Commands](https://solr.apache.org/guide/8_10/collection-management.html)
     * 
     * @param action: Collection-action to perform. Note that some actions require high-level permissions: * [LIST](https://solr.apache.org/guide/8_10/collection-management.html#list) * [DELETE](https://solr.apache.org/guide/8_10/collection-management.html#delete) 
     * 
     * @param name: Mandatory for the action [DELETE](https://solr.apache.org/guide/8_10/collection-management.html#delete) The name of the collection to delete. 
     * 
     * @param async: Optional for the action [DELETE](https://solr.apache.org/guide/8_10/collection-management.html#delete) Request ID to track this action which will be processed asynchronously. 
     * 
     * @return <ul>
      *   <li>code = 200, message = "JSON structure with Solr collection action response", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String collectionAction(String action, String name, String async) throws ServiceException {
        try {
            log.debug("collectionAction(action='{}', name='{}', async='{}') called with call details: {}",
                      action, name, async, getCallDetails());
            String response = "MnOL42";
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Solr [Configsets Commands](https://solr.apache.org/guide/8_10/configsets-api.html)
     * 
     * @param action: Configset-action to perform. Note that some actions require high-level permissions: * [LIST](https://solr.apache.org/guide/8_10/configsets-api.html#configsets-list) * [DELETE](https://solr.apache.org/guide/8_10/configsets-api.html#configsets-delete) 
     * 
     * @param name: Mandatory for the action [DELETE](https://solr.apache.org/guide/8_10/configsets-api.html#configsets-delete) The name of the configset to delete. 
     * 
     * @return <ul>
      *   <li>code = 200, message = "JSON structure with Solr configset action response", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String configAction(String action, String name) throws ServiceException {
        // TODO: Implement...
    
        
        try { 
            log.debug("configAction() called with call details: {}", getCallDetails());
            String response = "BQBHQ5M0";
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }


    @Override
    public String solrMLT(String collection, String q, String mltFl, Integer mltMintf, Integer mltMindf, Integer mltMaxdf, Integer mltMaxdfpct, Integer mltMinwl, Integer mltMaxwl, Integer mltMaxqt, Boolean mltBoost, String mltInterestingTerms, List<String> fq, Integer rows, Integer start, String fl, String qOp, String wt) {
        try {

            log.debug("solrMLT(collection='{}', q='{}', ...) called with call details: {}",
                      collection, q, getCallDetails());
            Map<String, String[]> extra = getExtraParams();
            Map<String, String[]> notValidParameters=filterAdditionalAllowedParameters(extra);
            if (!notValidParameters.isEmpty()) {
                if (ServiceConfig.getConfig().getBoolean(PERMISSIVE_KEY, PERMISSIVE_DEFAULT)) {
                    log.warn("solrMLT: ds-discover is configured to permit all Solr parameters. " +
                            "Non-vetted parameters passed on: {}", toString(extra));
                } else {
                    throw new InvalidArgumentServiceException("Unsupported parameters: " + toString(notValidParameters));
                }
            }
            SolrService solr = SolrManager.getSolrService(collection);
            // TODO: Pass the map of request parameters instead of all parameters as first class
            httpServletResponse.setContentType(solr.getResponseMIMEType(wt)); // Needed by SolrJ

            //Add filter query from license module.
            fq = addAccessFilter("solrMLT", fq);

            // No removal of access filter as that is not part of MLT-responses
            return solr.mlt(q, fq, rows, start, fl, qOp, wt,
                    mltFl, mltMintf, mltMindf, mltMaxdf, mltMaxdfpct, mltMinwl, mltMaxwl, mltMaxqt,
                    mltBoost, mltInterestingTerms,
                    extra);
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Return the solr schema through the original solr-endpoint for schema retrieval. The result from this method does
     * not include documentation written in XML processing instructions.
     * To retrieve the documented schema use {@link #documentedSchema(String collection, String format)}.
     * @param collection to retrieve solr schema for.
     * @param wt the format for the schema.
     */
    @Override
    public String solrSchema(String collection, String wt) {
        try {
            SolrService solr = SolrManager.getSolrService(collection);
            httpServletResponse.setContentType(solr.getSchemaResponseMIMEType(wt)); // Needed by SolrJ
            return solr.schema(wt);
        } catch (Exception e){
            throw handleException(e);
        }
    }


    /**
     * Return the documented solr schema. This endpoint retrieves the raw solr schema and then transforms it to the
     * specified format using an XSLT. This transformation retrieves processing instructions and includes these in the
     * retrieved solr schema.
     * @param collection the name of the solr collection to retrieve.
     * @param format     the format which the schema gets transformed to
     * @return the transformed schema.
     */
    @Override
    public String documentedSchema(String collection, String format){
        try {
            String filename = DocumentationExtractor.getSchemaFileName(format);

            // Formats are applied correctly when calling the endpoint.
            // However, the OpenAPI interface does not use the Content-Disposition header when downloaded manually.
            httpServletResponse.setContentType("text/" + format);
            httpServletResponse.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + filename + "\"");

            return DocumentationExtractor.transformSchema(collection, format);

        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Perform a Solr-compatible search in the stated collection
     * 
     * @param collection: The ID of the Solr collection to search. Available collections can be requested from /solr/admin/collections
     * 
     * @param q: Solr query param [https://solr.apache.org/guide/8_10/the-standard-query-parser.html#standard-query-parser-parameters](q)
     * 
     * @return <ul>
      *   <li>code = 200, message = "JSON structure with Solr response", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String solrSearch(String collection,
    		                 String q, 
    		                 List<String> fq,
    		                 Integer rows,
    		                 Integer start,
    		                 String fl,
    		                 String facet,
    		                 List<String> facetField,
    		                 String spellcheck,
    		                 String spellcheckBuild,
    		                 String spellcheckReload,
    		                 String spellcheckQuery,
    		                 String spellcheckDictionary,    		                 
    		                 Integer spellcheckCount,
    		                 String spellchecKOnlyMorePopular,
    		                 String spellcheckExtendedResults,
    		                 String spellcheckCollate,
    		                 Integer spellcheckMaxCollations,
    		                 Integer spellcheckMaxCollationTries,
    		                 Double spellcheckAccuracy,
    		                 String qOp,
    		                 String wt,
    		                 String version,
    		                 String indent,
    		                 String debug,
    		                 String debugExplainStructured) {
    
    	
    
        try {
                      
            log.debug("solrSearch(collection='{}', q='{}', ...) called with call details: {}",
                    collection, q, getCallDetails());
            Map<String, String[]> extra = getExtraParams();
            Map<String, String[]> notValidParamters=filterAdditionalAllowedParameters(extra);
            
            if (!notValidParamters.isEmpty()) {
                if (ServiceConfig.getConfig().getBoolean(PERMISSIVE_KEY, PERMISSIVE_DEFAULT)) {
                    log.warn("solrSearch: ds-discover is configured to permit all Solr parameters. " +
                            "Non-vetted parameters passed on: {}", toString(extra));
                } else {
                    throw new InvalidArgumentServiceException("Unsupported parameters: " + toString(notValidParamters));
                }
            }

            Response shieldResponse = SolrShield.evaluate(httpServletRequest.getParameterMap());
            log.debug("solrSearch(collection='{}', has weight={} with maximum weight allowed={} ",collection, shieldResponse.getWeight(), shieldResponse.getMaxWeight());
            if (!shieldResponse.isAllowed()) {
                throw new ServiceException("Call blocked by SolrShield: " + shieldResponse.getReasons(),
                        javax.ws.rs.core.Response.Status.FORBIDDEN);
            }

            SolrService solr = SolrManager.getSolrService(collection);
            // TODO: Pass the map of request parameters instead of all parameters as first class
            httpServletResponse.setContentType(solr.getResponseMIMEType(wt)); // Needed by SolrJ

            //Add filter query from license module.
            fq = addAccessFilter("solrSearch", fq);

            String rawResponse = solr.query(q, fq, rows, start, fl, facet, facetField,
            		spellcheck,spellcheckBuild,spellcheckReload,spellcheckQuery,spellcheckDictionary,spellcheckCount,spellchecKOnlyMorePopular,spellcheckExtendedResults,spellcheckCollate,spellcheckMaxCollations,spellcheckMaxCollationTries,spellcheckAccuracy,
            		qOp, wt, version, indent, debug, debugExplainStructured, extra);
            
            return SolrService.removePrefixedFilters(rawResponse, FILTER_CACHE_PREFIX, wt);
        } catch (Exception e){
            throw handleException(e);
        }
    }

    
    /**
     * Perform a Solr-suggest search in the stated collection
     * 
     * @param collection: The ID of the Solr collection to search. Available collections can be requested from /solr/admin/collections
     * @param suggestDictionary A suggest dictionary defined in the solr configuration.
     * @param suggestQuery The prefix text that the suggest compontent will try to autocomplete. 
     * @param suggestCount Number of results to return. 10 is the default value.
     * @param wt The return format from solr. (json, xml etc.)
     * 
     * @return <ul>
      *   <li>code = 200, message = "JSON structure with Solr response", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override   
    public String solrSuggest(String collection, String suggestDictionary, String suggestQuery, Integer suggestCount, String wt) {

        log.debug("solrsuggest(collection='{}', q='{}', ...) called with call details: {}", collection, suggestQuery, getCallDetails());
        
        SolrService solr = SolrManager.getSolrService(collection);
        httpServletResponse.setContentType(solr.getResponseMIMEType(wt)); // Needed by SolrJ

        String rawResponse = solr.suggest(suggestDictionary, suggestQuery, suggestCount, wt);
                       
        return rawResponse;
    }
    
    
    
    /**
     * Request a filter query from ds-license and append it to {@code fq}.
     * @param designation describes the caller, used for logging only.
     * @param fq a list of existing filter queries or null.
     * @return {@code fq} extended with an access filter from ds-license.
     */
    private List<String> addAccessFilter(String designation, List<String> fq) {
        //Add filter query from license module.
        DsLicenseClient licenseClient = getDsLicenseApiClient();
        GetUserQueryInputDto licenseQueryDto = getLicenseQueryDto();
        GetUsersFilterQueryOutputDto filterQuery;
        try {
            filterQuery = licenseClient.getUserLicenseQuery(licenseQueryDto);
        } catch (Exception e) {
            log.warn("Unable to get response from ds-license at URL '" +
                    ServiceConfig.getConfig().getString("licensemodule.url") + "'", e);
            throw new InternalServiceException("Unable to contact license server");
        }

        log.debug("{}: Using filter query='{}' for user attributes='{}'",
                designation, filterQuery.getFilterQuery(), getLicenseQueryDto());
        if (fq == null) {
            fq = new ArrayList<>();
        }
        if (filterQuery.getFilterQuery() != null && !filterQuery.getFilterQuery().isEmpty()) {
            fq.add(FILTER_CACHE_PREFIX + filterQuery.getFilterQuery()); //Add the additional filter query
        }
        return fq;
    }

    /**
     * Tiny helper for creating a proper toString for maps with String arrays as values.
     * @param map a {@code String -> String[]} map.
     * @return human readable representation of {@code map}.
     */
    private String toString(Map<String, String[]> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=[" + String.join(", ", e.getValue()) + "]")
                .collect(Collectors.joining(", ", "{", "}"));
    }


    private static GetUserQueryInputDto getLicenseQueryDto() {
       return LicenseUtil.getLicenseQueryDto();
    }
    
    private static DsLicenseClient getDsLicenseApiClient() {
        return LicenseUtil.getDsLicenseApiClient();
    }
    
    
    /**
     * Subtracts parameters defined for the called endpoint from the total set of parameters in the called URI,
     * resulting in a map of unhandled parameters. These parameters has to be removed since all are set again in the solr call method.
     * @return a map of unhandled parameters.
     */
    private Map<String, String[]> getExtraParams() {
        // All query params from the client call
        Map<String, String[]> extras = new HashMap<>(httpServletRequest.getParameterMap());

        // Iterate defined params and remove them from the client params, leaving unhandled params
        // JAXRSUtils.getCurrentMessage() uses ThreadLocal, so it must be called from the caller Thread
        List<Parameter> parameters = JAXRSUtils.getCurrentMessage().getExchange().get(OperationResourceInfo.class).getParameters();

        parameters.stream()
                .filter(param -> param.getType() == ParameterType.QUERY)
                .map(Parameter::getName)
                .forEach(extras::remove);
     
        return extras;
    }

    /**
     * Will remove the additional allowed parameters in a new map.  
     * The remaining parameters in the map is values that are not allowed.  
     *   
     * @param The map to test if it has parameters not allowed. The input map object will not be modified.
     */   
    @SuppressWarnings("unchecked")
    private Map<String , String[]> filterAdditionalAllowedParameters (Map<String , String[]> params) {
        //Remove allowed parameters from the configuration.    
        
        HashMap<String, String[]>  hashMap =(HashMap<String, String[]>) params;  //Must cast to HashMap to clone      
        HashMap<String, String[]>  cloned= (HashMap<String, String[]>) hashMap.clone();
               
        List<String> extraAllowedParameters = dk.kb.discover.config.ServiceConfig.getConfig().getList("solr.extraAllowedParameters");
        extraAllowedParameters.stream().forEach(cloned::remove);
        return cloned;              
    }

}
