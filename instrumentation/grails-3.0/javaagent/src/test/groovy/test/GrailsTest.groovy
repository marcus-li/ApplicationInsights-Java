/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package test

import static io.opentelemetry.api.trace.SpanKind.INTERNAL
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.ERROR
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.EXCEPTION
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.NOT_FOUND
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.PATH_PARAM
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.QUERY_PARAM
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.REDIRECT

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import io.opentelemetry.instrumentation.test.AgentTestTrait
import io.opentelemetry.instrumentation.test.asserts.TraceAssert
import io.opentelemetry.instrumentation.test.base.HttpServerTest
import io.opentelemetry.sdk.trace.data.SpanData
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.context.ConfigurableApplicationContext

class GrailsTest extends HttpServerTest<ConfigurableApplicationContext> implements AgentTestTrait {

  @CompileStatic
  @SpringBootApplication
  static class TestApplication extends GrailsAutoConfiguration {
    static ConfigurableApplicationContext start(int port, String contextPath) {
      GrailsApp grailsApp = new GrailsApp(TestApplication)
      // context path configuration property name changes between spring boot versions
      def contextPathKey = "server.context-path"
      try {
        ServerProperties.getDeclaredMethod("getServlet")
        contextPathKey = "server.servlet.contextPath"
      } catch (NoSuchMethodException ignore) {
      }
      Map<String, Object> properties = new HashMap<>()
      properties.put("server.port", port)
      properties.put(contextPathKey, contextPath)
      grailsApp.setDefaultProperties(properties)
      return grailsApp.run()
    }

    @Override
    Collection<Class> classes() {
      return Arrays.asList(TestController, ErrorController, UrlMappings)
    }
  }

  @Override
  String expectedServerSpanName(ServerEndpoint endpoint) {
    if (endpoint == PATH_PARAM) {
      return getContextPath() + "/test/path"
    } else if (endpoint == QUERY_PARAM) {
      return getContextPath() + "/test/query"
    } else if (endpoint == ERROR) {
      return getContextPath() + "/error/index"
    } else if (endpoint == EXCEPTION) {
      return getContextPath() + "/error"
    } else if (endpoint == NOT_FOUND) {
      return getContextPath() + "/**"
    }
    return getContextPath() + "/test" + endpoint.path
  }

  @Override
  ConfigurableApplicationContext startServer(int port) {
    return TestApplication.start(port, getContextPath())
  }

  @Override
  void stopServer(ConfigurableApplicationContext ctx) {
    ctx.close()
  }

  @Override
  String getContextPath() {
    return "/xyz"
  }

  @Override
  boolean hasHandlerSpan() {
    true
  }

  @Override
  boolean hasExceptionOnServerSpan() {
    true
  }

  @Override
  boolean hasResponseSpan(ServerEndpoint endpoint) {
    endpoint == REDIRECT || endpoint == ERROR || endpoint == NOT_FOUND
  }

  @Override
  boolean hasErrorPageSpans(ServerEndpoint endpoint) {
    endpoint == ERROR || endpoint == EXCEPTION || endpoint == NOT_FOUND
  }

  @Override
  int getErrorPageSpansCount(ServerEndpoint endpoint) {
    endpoint == NOT_FOUND ? 3 : 2
  }

  @Override
  boolean testPathParam() {
    true
  }

  @Override
  boolean sendsBackAiTargetAppId(ServerEndpoint endpoint) {
    true
  }

  @Override
  void errorPageSpans(TraceAssert trace, int index, Object parent, String method = "GET", ServerEndpoint endpoint) {
    forwardSpan(trace, index, trace.span(0))
    def errorSpanName = endpoint == NOT_FOUND ? "ErrorController.notFound" : "ErrorController.index"
    trace.span(index + 1) {
      name errorSpanName
      kind INTERNAL
      errored false
      attributes {
      }
    }
    if (endpoint == NOT_FOUND) {
      trace.span(index + 2) {
        name ~/\.sendError$/
        kind INTERNAL
        errored false
        attributes {
        }
      }
    }
  }

  @Override
  void responseSpan(TraceAssert trace, int index, Object parent, String method = "GET", ServerEndpoint endpoint) {
    trace.span(index) {
      name endpoint == REDIRECT ? ~/\.sendRedirect$/ : ~/\.sendError$/
      kind INTERNAL
      errored false
      attributes {
      }
    }
  }

  @Override
  void handlerSpan(TraceAssert trace, int index, Object parent, String method = "GET", ServerEndpoint endpoint) {
    trace.span(index) {
      if (endpoint == QUERY_PARAM) {
        name "TestController.query"
      } else if (endpoint == PATH_PARAM) {
        name "TestController.path"
      } else if (endpoint == NOT_FOUND) {
        name "ResourceHttpRequestHandler.handleRequest"
      } else {
        name "TestController.${endpoint.name().toLowerCase()}"
      }
      kind INTERNAL
      errored endpoint == EXCEPTION
      if (endpoint == EXCEPTION) {
        errorEvent(Exception, EXCEPTION.body)
      }
      childOf((SpanData) parent)
    }
  }
}
