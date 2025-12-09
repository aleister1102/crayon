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

    public RequestColorizer(MontoyaApi api, SettingsPanelWithData settings) {
        this.settings = settings;
        this.logging = api.logging();
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
            logging.logToOutput("Processing response for: " + responseReceived.initiatingRequest().url());
        }

        short statusCode = responseReceived.statusCode();
        HighlightColor color = null;

        // Prioritize status codes over content types
        if (statusCode >= 500) {
            String colorName = settings.getString(Extension.STATUS_5XX_COLOR_SETTING);
            if (colorName != null) {
                try {
                    color = HighlightColor.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    logging.logToError("Invalid color name in settings: " + colorName);
                }
            }
        } else if (statusCode >= 400) {
            String colorName = settings.getString(Extension.STATUS_4XX_COLOR_SETTING);
            if (colorName != null) {
                try {
                    color = HighlightColor.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    logging.logToError("Invalid color name in settings: " + colorName);
                }
            }
        } else if (statusCode >= 300) {
            String colorName = settings.getString(Extension.STATUS_3XX_COLOR_SETTING);
            if (colorName != null) {
                try {
                    color = HighlightColor.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    logging.logToError("Invalid color name in settings: " + colorName);
                }
            }
        } else if (statusCode >= 200) {
            String method = responseReceived.initiatingRequest().method();
            String colorName = null;

            if ("GET".equalsIgnoreCase(method)) {
                MimeType mimeType = responseReceived.inferredMimeType();
                String contentType = responseReceived.headerValue("Content-Type");
                if (contentType == null) {
                    contentType = "";
                }
                
                if (mimeType == MimeType.JSON || contentType.contains("json")) {
                    colorName = settings.getString(Extension.STATUS_200_GET_JSON_COLOR_SETTING);
                } else if (mimeType == MimeType.HTML || contentType.contains("html")) {
                    colorName = settings.getString(Extension.STATUS_200_GET_HTML_COLOR_SETTING);
                }
                
                // Fallback to general GET color if specific content type color is not set or not applicable
                if (colorName == null) {
                    colorName = settings.getString(Extension.STATUS_200_GET_COLOR_SETTING);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                colorName = settings.getString(Extension.STATUS_200_POST_COLOR_SETTING);
            } else {
                colorName = settings.getString(Extension.STATUS_200_OTHER_COLOR_SETTING);
            }

            if (colorName != null) {
                try {
                    color = HighlightColor.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    logging.logToError("Invalid color name in settings: " + colorName);
                }
            }
        }

        // If no status code color was set, check content type
        if (color == null) {
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
            } else if (contentType.startsWith("text/plain")) {
                colorName = settings.getString(Extension.TEXT_COLOR_SETTING);
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