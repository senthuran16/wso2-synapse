package org.apache.synapse.api;

import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ApiUtils {

    private static final Log log = LogFactory.getLog(ApiUtils.class);

    public static String getFullRequestPath(MessageContext synCtx) {
        Object obj = synCtx.getProperty(RESTConstants.REST_FULL_REQUEST_PATH);
        if (obj != null) {
            return (String) obj;
        }

        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).
                getAxis2MessageContext();
        String url = (String) msgCtx.getProperty(Constants.Configuration.TRANSPORT_IN_URL);
        if (url == null) {
            url = (String) synCtx.getProperty(NhttpConstants.SERVICE_PREFIX);
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                url = new URL(url).getFile();
            } catch (MalformedURLException e) {
                handleException("Request URL: " + url + " is malformed", e);
            }
        } else if (url.startsWith("ws://") || url.startsWith("wss://")) { // TODO converge this to the above case
            try {
                URI uri = new URI(url);
                url = uri.getPath() + "?" + uri.getQuery(); // TODO no ? if no queryParams
            } catch (URISyntaxException e) {
                handleException("Request URL: " + url + " is malformed", e);
            }
        }
        synCtx.setProperty(RESTConstants.REST_FULL_REQUEST_PATH, url);
        return url;
    }

    public static void main(String[] args) throws URISyntaxException {
        URI uri = new URI("http://localhost:9090/foo");
        Object o = null;
    }

    private static void handleException(String msg, Throwable t) {
        log.error(msg, t);
        throw new SynapseException(msg, t);
    }
}
