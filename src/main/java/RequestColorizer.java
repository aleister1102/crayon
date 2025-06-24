import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.MimeType;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.settings.SettingsPanelWithData;

public class RequestColorizer implements HttpHandler {
    private final SettingsPanelWithData settings;
    private final Logging logging;
    private final MontoyaApi api;

    public RequestColorizer(MontoyaApi api, SettingsPanelWithData settings) {
        this.api = api;
        this.settings = settings;
        this.logging = api.logging();
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if (requestToBeSent.annotations().highlightColor() != null) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        HighlightColor color = null;
        String method = requestToBeSent.method();

        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH")) {
            color = HighlightColor.YELLOW;
        } else if (method.equalsIgnoreCase("DELETE")) {
            color = HighlightColor.RED;
        }

        if (color != null) {
            requestToBeSent.annotations().setHighlightColor(color);
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon Request: " + requestToBeSent.method() + " " + requestToBeSent.url() + " -> "
                        + color.name());
            }
        }

        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
            logging.logToOutput("Processing response for: " + responseReceived.initiatingRequest().url());
        }

        short statusCode = responseReceived.statusCode();
        HighlightColor color = null;

        if (statusCode >= 500) {
            color = HighlightColor.RED;
        } else if (statusCode >= 400) {
            color = HighlightColor.ORANGE;
        } else if (statusCode >= 300) {
            color = HighlightColor.YELLOW;
        } else if (statusCode >= 200) {
            MimeType mimeType = responseReceived.inferredMimeType();
            String colorName = null;
            String contentType = responseReceived.headerValue("Content-Type");
            if (contentType == null) {
                contentType = "";
            }

            if (mimeType == MimeType.JSON || contentType.contains("json")) {
                colorName = settings.getString(Extension.JSON_COLOR_SETTING);
            } else if (mimeType == MimeType.XML || contentType.contains("xml")) {
                colorName = settings.getString(Extension.XML_COLOR_SETTING);
            } else if (mimeType == MimeType.HTML || contentType.contains("html")) {
                colorName = settings.getString(Extension.HTML_COLOR_SETTING);
            }

            if (colorName != null) {
                try {
                    color = HighlightColor.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    logging.logToError("Invalid color name in settings: " + colorName);
                }
            }
        }

        if (color != null) {
            responseReceived.annotations().setHighlightColor(color);
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput(
                        "Crayon Response: " + responseReceived.initiatingRequest().url() + " -> " + color.name());
            }
        }

        return ResponseReceivedAction.continueWith(responseReceived);
    }
}