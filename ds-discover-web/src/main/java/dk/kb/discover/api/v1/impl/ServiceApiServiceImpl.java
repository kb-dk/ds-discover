package dk.kb.discover.api.v1.impl;

import dk.kb.discover.api.v1.ServiceApi;
import dk.kb.discover.model.v1.StatusDto;
import dk.kb.discover.model.v1.WhoamiDto;
import dk.kb.discover.model.v1.WhoamiTokenDto;
import dk.kb.util.BuildInfoManager;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.discover.webservice.KBAuthorizationInterceptor;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
/**
 * ds-discover
 *
 * <p>ds-discover by the Royal Danish Library 
 *
 */
public class ServiceApiServiceImpl extends ImplBase implements ServiceApi {
    private static final Logger log = LoggerFactory.getLogger(ServiceApiServiceImpl.class);

    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        try {
            log.debug("ping() called with call details: {}", getCallDetails());
            return "Pong";
        } catch (Exception e){
            throw handleException(e);
        }
    }
    
    /**
     * Extract info from OAUth2 accessTokens.
     * @return OAUth2 roles from the caller's accessToken, if present.
     */
    @SuppressWarnings("unchecked")
    @Override
    public WhoamiDto probeWhoami() {
        WhoamiDto whoami = new WhoamiDto();
        WhoamiTokenDto token = new WhoamiTokenDto();
        whoami.setToken(token);

        Message message = JAXRSUtils.getCurrentMessage();

        token.setPresent(message.containsKey(KBAuthorizationInterceptor.ACCESS_TOKEN_STRING));
        token.setValid(Boolean.TRUE.equals(message.get(KBAuthorizationInterceptor.VALID_TOKEN)));
        if (message.containsKey(KBAuthorizationInterceptor.FAILED_REASON)) {
            token.setError(message.get(KBAuthorizationInterceptor.FAILED_REASON).toString());
        }
        Object roles = message.get(KBAuthorizationInterceptor.TOKEN_ROLES);
        if (roles != null) {
            token.setRoles(new ArrayList<>((Set<String>)roles));
        }
        return whoami;
    }
    
    /**
     * Detailed status / health check for the service
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = StatusDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public StatusDto status() throws ServiceException {
        try {
            log.debug("status() called with call details: {}", getCallDetails());
            String host = "N/A";
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.warn("Exception resolving hostname", e);
            }
            return new StatusDto()
                    .application(BuildInfoManager.getName())
                    .version(BuildInfoManager.getVersion())
                    .build(BuildInfoManager.getBuildTime())
                    .java(System.getProperty("java.version"))
                    .heap(Runtime.getRuntime().maxMemory()/1048576L)
                    .server(host)
                    .gitCommitChecksum(BuildInfoManager.getGitCommitChecksum())
                    .gitBranch(BuildInfoManager.getGitBranch())
                    .gitClosestTag(BuildInfoManager.getGitClosestTag())
                    .gitCommitTime(BuildInfoManager.getGitCommitTime())
                    .health("ok");
        } catch (Exception e){
            throw handleException(e);
        }
    }

}
