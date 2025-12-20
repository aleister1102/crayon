import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.settings.SettingsPanelWithData;
import burp.api.montoya.websocket.BinaryMessage;
import burp.api.montoya.websocket.BinaryMessageAction;
import burp.api.montoya.websocket.Direction;
import burp.api.montoya.websocket.MessageHandler;
import burp.api.montoya.websocket.TextMessage;
import burp.api.montoya.websocket.TextMessageAction;
import burp.api.montoya.websocket.WebSocketCreated;
import burp.api.montoya.websocket.WebSocketCreatedHandler;

public class WebSocketColorizer implements WebSocketCreatedHandler {
    private final SettingsPanelWithData settings;
    private final Logging logging;

    public WebSocketColorizer(MontoyaApi api, SettingsPanelWithData settings) {
        this.settings = settings;
        this.logging = api.logging();
    }

    @Override
    public void handleWebSocketCreated(WebSocketCreated webSocketCreated) {
        if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
            logging.logToOutput("Crayon: WebSocket created from " + 
                webSocketCreated.toolSource().toolType() + " for: " + 
                webSocketCreated.upgradeRequest().url());
        }

        webSocketCreated.webSocket().registerMessageHandler(new MessageHandler() {
            @Override
            public TextMessageAction handleTextMessage(TextMessage textMessage) {
                logMessage(textMessage.direction(), "text");
                return TextMessageAction.continueWith(textMessage);
            }

            @Override
            public BinaryMessageAction handleBinaryMessage(BinaryMessage binaryMessage) {
                logMessage(binaryMessage.direction(), "binary");
                return BinaryMessageAction.continueWith(binaryMessage);
            }

            private void logMessage(Direction direction, String type) {
                if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                    String colorName;
                    if (direction == Direction.SERVER_TO_CLIENT) {
                        colorName = settings.getString(Extension.WEBSOCKET_INCOMING_COLOR_SETTING);
                    } else {
                        colorName = settings.getString(Extension.WEBSOCKET_OUTGOING_COLOR_SETTING);
                    }
                    logging.logToOutput("Crayon WebSocket (" + type + "): " + direction + 
                        " -> would highlight with " + colorName);
                }
            }
        });
    }
}
