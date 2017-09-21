package ch.x42.at17.aggregator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.servlet.Servlet;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.query.SlingQuery;
import org.apache.sling.query.api.SearchStrategy;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** BindingsValuesProvider that provides the "content" variable,
 *  populated by running a content aggregation script.
 */
@Component(service = BindingsValuesProvider.class)
public class ContentBVP implements BindingsValuesProvider {

    public static final String RESOURCE_KEY = "resource";
    
    @Reference
    private ServletResolver servletResolver;
    
    @Reference(target="(names=javascript)")
    private ScriptEngineFactory javascriptEngineFactory;
    
    public class DollarAdapter {
        public SlingQuery $(Resource r) {
            return SlingQuery.$(r);
        }
        public SlingQuery $(ResourceResolver resolver) {
            return SlingQuery.$(resolver);
        }
    }
    
    @Override
    public void addBindings(Bindings b) {
        final Resource resource = (Resource)b.get("resource");
        final SlingHttpServletRequest request = (SlingHttpServletRequest)b.get("request");
        if(resource != null && request != null) {
            try {
                b.put("content", aggregateContent(resource, getAggregatorScript(request)));
            } catch(Exception e) {
                throw new RuntimeException("Content aggregation failed", e);
            }
        }
    }
    
    private Object aggregateContent(Resource r, SlingScript script) throws ScriptException {
        final String text = getText(script);
        final ScriptEngine e = javascriptEngineFactory.getScriptEngine();
        final Bindings b = e.createBindings();
        b.put("query", new DollarAdapter());
        b.put("SearchStrategyQUERY", SearchStrategy.QUERY);
        b.put("resource", r);
        b.put("resourceResolver", r.getResourceResolver());
        final Object result = e.eval(text, b);
        return result;
    }
    
    private SlingScript getAggregatorScript(SlingHttpServletRequest request) {
        final Servlet s = servletResolver.resolveServlet(new MethodWrapper(request, "SLING-CONTENT"));
        return s instanceof SlingScript ? (SlingScript)s : null;
    }
    
    private String getText(SlingScript aggregator) {
        try {
            final InputStream is = aggregator.getScriptResource().adaptTo(InputStream.class);
            try {
                if(is != null) {
                    return IOUtils.toString(is);
                }
            } finally {
                if(is != null) {
                    is.close();
                }
            }
        } catch(IOException ignore) {
        }
        return null;
    }
}

class MethodWrapper extends SlingHttpServletRequestWrapper {
    private final String method;
    
    MethodWrapper(SlingHttpServletRequest r, String method) {
        super(r);
        this.method = method;
    }
    
    @Override
    public String getMethod() {
        return method;
    }
}