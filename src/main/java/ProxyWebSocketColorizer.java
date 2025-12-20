import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.websocket.BinaryMessageReceivedAction;
import burp.api.montoya.proxy.websocket.BinaryMessageToBeSentAction;
import burp.api.montoya.proxy.websocket.InterceptedBinaryMessage;
import burp.api.montoya.proxy.websocket.InterceptedTextMessage;
import burp.api.montoya.proxy.websocket.ProxyMessageHandler;
import burp.api.montoya.proxy.websocket.ProxyWebSocketCreation;
import burp.api.montoya.proxy.websocket.ProxyWebSocketCreationHandler;
import burp.api.montoya.proxy.websocket.TextMessageReceivedAction;
import burp.api.montoya.proxy.websocket.TextMessageToBeSentAction;
import burp.api.montoya.ui.settings.SettingsPanelWithData;
import burp.api.montoya.websocket.Direction;

public class ProxyWebSocketColorizer implements ProxyWebSocketCreationHandler {
    private final SettingsPanelWithData settings;
    private final Logging logging;

    public ProxyWebSocketColorizer(MontoyaApi api, SettingsPanelWithData settings) {
        this.settings = settings;
        this.logging = api.logging();
    }

    @Override
    public void handleWebSocketCreation(ProxyWebSocketCreation webSocketCreation) {
        if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
            logging.logToOutput("Crayon: Proxy WebSocket created for: " + webSocketCreation.upgradeRequest().url());
        }

        webSocketCreation.proxyWebSocket().registerProxyMessageHandler(new ProxyMessageHandler() {
            @Override
            public TextMessageReceivedAction handleTextMessageReceived(InterceptedTextMessage interceptedTextMessage) {
                applyHighlight(interceptedTextMessage);
                return TextMessageReceivedAction.continueWith(interceptedTextMessage);
            }

            @Override
            public TextMessageToBeSentAction handleTextMessageToBeSent(InterceptedTextMessage interceptedTextMessage) {
                applyHighlight(interceptedTextMessage);
                return TextMessageToBeSentAction.continueWith(interceptedTextMessage);
            }

            @Override
            public BinaryMessageReceivedAction handleBinaryMessageReceived(InterceptedBinaryMessage interceptedBinaryMessage) {
                applyBinaryHighlight(interceptedBinaryMessage);
                return BinaryMessageReceivedAction.continueWith(interceptedBinaryMessage);
            }

            @Override
            public BinaryMessageToBeSentAction handleBinaryMessageToBeSent(InterceptedBinaryMessage interceptedBinaryMessage) {
                applyBinaryHighlight(interceptedBinaryMessage);
                return BinaryMessageToBeSentAction.continueWith(interceptedBinaryMessage);
            }

            private void applyHighlight(InterceptedTextMessage message) {
                HighlightColor color = getColorForDirection(message.direction());
                if (color != null) {
                    message.annotations().setHighlightColor(color);
                    if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                        logging.logToOutput("Crayon WebSocket: " + message.direction() + " -> " + color.name());
                    }
                }
            }

            private void applyBinaryHighlight(InterceptedBinaryMessage message) {
                HighlightColor color = getColorForDirection(message.direction());
                if (color != null) {
                    message.annotations().setHighlightColor(color);
                    if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                        logging.logToOutput("Crayon WebSocket Binary: " + message.direction() + " -> " + color.name());
                    }
                }
            }

            private HighlightColor getColorForDirection(Direction direction) {
                String colorName;
                if (direction == Direction.SERVER_TO_CLIENT) {
                    // Incoming (from server) - Green
                    colorName = settings.getString(Extension.WEBSOCKET_INCOMING_COLOR_SETTING);
                } else {
                    // Outgoing (to server) - Yellow
                    colorName = settings.getString(Extension.WEBSOCKET_OUTGOING_COLOR_SETTING);
                }

                if (colorName != null) {
                    try {
                        return HighlightColor.valueOf(colorName);
                    } catch (IllegalArgumentException e) {
                        logging.logToError("Invalid color name in settings: " + colorName);
                    }
                }
                return null;
            }
        });
    }
}
