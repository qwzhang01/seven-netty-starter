package com.iwj.netty.pojo;

import com.iwj.netty.standard.ServerEndpointConfig;
import com.iwj.netty.support.*;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.TypeMismatchException;

import java.lang.reflect.Method;
import java.util.*;


public class PojoEndpointServer {

    public static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("WEBSOCKET_SESSION");
    public static final AttributeKey<Map<String, String>> URI_TEMPLATE = AttributeKey.valueOf("WEBSOCKET_URI_TEMPLATE");
    public static final AttributeKey<Map<String, List<String>>> REQUEST_PARAM = AttributeKey.valueOf("WEBSOCKET_REQUEST_PARAM");
    private static final AttributeKey<Object> POJO_KEY = AttributeKey.valueOf("WEBSOCKET_IMPLEMENT");
    private static final AttributeKey<String> PATH_KEY = AttributeKey.valueOf("WEBSOCKET_PATH");
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PojoEndpointServer.class);
    private final Map<String, PojoMethodMapping> pathMethodMappingMap = new HashMap<>();
    private final ServerEndpointConfig config;
    private Set<WsPathMatcher> pathMatchers = new HashSet<>();

    public PojoEndpointServer(PojoMethodMapping methodMapping, ServerEndpointConfig config, String path) {
        addPathPojoMethodMapping(path, methodMapping);
        this.config = config;
    }

    public boolean hasBeforeHandshake(Channel channel, String path) {
        PojoMethodMapping methodMapping = getPojoMethodMapping(path, channel);
        return methodMapping.getBeforeHandshake() != null;
    }

    public void doBeforeHandshake(Channel channel, FullHttpRequest req, String path) {
        PojoMethodMapping methodMapping = null;
        methodMapping = getPojoMethodMapping(path, channel);

        Object implement = null;
        try {
            implement = methodMapping.getEndpointInstance();
        } catch (Exception e) {
            logger.error(e);
            return;
        }
        channel.attr(POJO_KEY).set(implement);
        Session session = new Session(channel);
        channel.attr(SESSION_KEY).set(session);
        Method beforeHandshake = methodMapping.getBeforeHandshake();
        if (beforeHandshake != null) {
            try {
                beforeHandshake.invoke(implement, methodMapping.getBeforeHandshakeArgs(channel, req));
            } catch (TypeMismatchException e) {
                throw e;
            } catch (Throwable t) {
                logger.error(t);
            }
        }
    }

    public void doOnOpen(Channel channel, FullHttpRequest req, String path) {
        PojoMethodMapping methodMapping = getPojoMethodMapping(path, channel);

        Object implement = channel.attr(POJO_KEY).get();
        if (implement == null) {
            try {
                implement = methodMapping.getEndpointInstance();
                channel.attr(POJO_KEY).set(implement);
            } catch (Exception e) {
                logger.error(e);
                return;
            }
            Session session = new Session(channel);
            channel.attr(SESSION_KEY).set(session);
        }

        Method onOpenMethod = methodMapping.getOnOpen();
        if (onOpenMethod != null) {
            try {
                onOpenMethod.invoke(implement, methodMapping.getOnOpenArgs(channel, req));
            } catch (TypeMismatchException e) {
                throw e;
            } catch (Throwable t) {
                logger.error(t);
            }
        }
    }

    public void doOnClose(Channel channel) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        PojoMethodMapping methodMapping = null;
        if (pathMethodMappingMap.size() == 1) {
            methodMapping = pathMethodMappingMap.values().iterator().next();
        } else {
            String path = attrPath.get();
            methodMapping = pathMethodMappingMap.get(path);
            if (methodMapping == null) {
                return;
            }
        }
        if (methodMapping.getOnClose() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(POJO_KEY).get();
            try {
                methodMapping.getOnClose().invoke(implement,
                        methodMapping.getOnCloseArgs(channel));
            } catch (Throwable t) {
                logger.error(t);
            }
        }
    }


    public void doOnError(Channel channel, Throwable throwable) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        PojoMethodMapping methodMapping = null;
        if (pathMethodMappingMap.size() == 1) {
            methodMapping = pathMethodMappingMap.values().iterator().next();
        } else {
            String path = attrPath.get();
            methodMapping = pathMethodMappingMap.get(path);
        }
        if (methodMapping.getOnError() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(POJO_KEY).get();
            try {
                Method method = methodMapping.getOnError();
                Object[] args = methodMapping.getOnErrorArgs(channel, throwable);
                method.invoke(implement, args);
            } catch (Throwable t) {
                logger.error(t);
            }
        }
    }

    public void doOnMessage(Channel channel, WebSocketFrame frame) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        PojoMethodMapping methodMapping = null;
        if (pathMethodMappingMap.size() == 1) {
            methodMapping = pathMethodMappingMap.values().iterator().next();
        } else {
            String path = attrPath.get();
            methodMapping = pathMethodMappingMap.get(path);
        }
        if (methodMapping.getOnMessage() != null) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            Object implement = channel.attr(POJO_KEY).get();
            try {
                methodMapping.getOnMessage().invoke(implement, methodMapping.getOnMessageArgs(channel, textFrame));
            } catch (Throwable t) {
                logger.error(t);
            }
        }
    }

    public void doOnBinary(Channel channel, WebSocketFrame frame) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        PojoMethodMapping methodMapping = null;
        if (pathMethodMappingMap.size() == 1) {
            methodMapping = pathMethodMappingMap.values().iterator().next();
        } else {
            String path = attrPath.get();
            methodMapping = pathMethodMappingMap.get(path);
        }
        if (methodMapping.getOnBinary() != null) {
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) frame;
            Object implement = channel.attr(POJO_KEY).get();
            try {
                methodMapping.getOnBinary().invoke(implement, methodMapping.getOnBinaryArgs(channel, binaryWebSocketFrame));
            } catch (Throwable t) {
                logger.error(t);
            }
        }
    }

    public void doOnEvent(Channel channel, Object evt) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        PojoMethodMapping methodMapping = null;
        if (pathMethodMappingMap.size() == 1) {
            methodMapping = pathMethodMappingMap.values().iterator().next();
        } else {
            String path = attrPath.get();
            methodMapping = pathMethodMappingMap.get(path);
        }
        if (methodMapping.getOnEvent() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(POJO_KEY).get();
            try {
                methodMapping.getOnEvent().invoke(implement, methodMapping.getOnEventArgs(channel, evt));
            } catch (Throwable t) {
                logger.error(t);
            }
        }
    }

    public String getHost() {
        return config.getHost();
    }

    public int getPort() {
        return config.getPort();
    }

    public Set<WsPathMatcher> getPathMatcherSet() {
        return pathMatchers;
    }

    public void addPathPojoMethodMapping(String path, PojoMethodMapping pojoMethodMapping) {
        pathMethodMappingMap.put(path, pojoMethodMapping);
        for (MethodArgumentResolver onOpenArgResolver : pojoMethodMapping.getOnOpenArgResolvers()) {
            if (onOpenArgResolver instanceof PathVariableMethodArgumentResolver || onOpenArgResolver instanceof PathVariableMapMethodArgumentResolver) {
                pathMatchers.add(new AntPathMatcherWrapper(path));
                return;
            }
        }
        pathMatchers.add(new DefaultPathMatcher(path));
    }

    private PojoMethodMapping getPojoMethodMapping(String path, Channel channel) {
        PojoMethodMapping methodMapping;
        if (pathMethodMappingMap.size() == 1) {
            methodMapping = pathMethodMappingMap.values().iterator().next();
        } else {
            Attribute<String> attrPath = channel.attr(PATH_KEY);
            attrPath.set(path);
            methodMapping = pathMethodMappingMap.get(path);
            if (methodMapping == null) {
                throw new RuntimeException("path " + path + " is not in pathMethodMappingMap ");
            }
        }
        return methodMapping;
    }
}
