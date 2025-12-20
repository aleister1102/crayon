import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.sitemap.SiteMap;
import burp.api.montoya.sitemap.SiteMapFilter;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.InvocationType;
import burp.api.montoya.ui.contextmenu.WebSocketContextMenuEvent;
import burp.api.montoya.ui.settings.SettingsPanelWithData;

public class CrayonContextMenu implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final SettingsPanelWithData settings;
    private final Logging logging;

    public CrayonContextMenu(MontoyaApi api, SettingsPanelWithData settings) {
        this.api = api;
        this.settings = settings;
        this.logging = api.logging();
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        // Get selected request/responses from various sources
        List<HttpRequestResponse> selectedItems = new ArrayList<>(event.selectedRequestResponses());
        
        // Also check message editor if no items selected
        if (selectedItems.isEmpty() && event.messageEditorRequestResponse().isPresent()) {
            selectedItems.add(event.messageEditorRequestResponse().get().requestResponse());
        }

        if (selectedItems.isEmpty()) {
            return null;
        }

        final List<HttpRequestResponse> items = selectedItems;
        boolean isSiteMap = event.isFrom(InvocationType.SITE_MAP_TREE, InvocationType.SITE_MAP_TABLE);

        // Auto-highlight based on rules
        JMenuItem autoHighlight = new JMenuItem("Crayon: Highlight (apply rules)");
        autoHighlight.addActionListener(e -> {
            int count = 0;
            for (HttpRequestResponse item : items) {
                if (applyAutoHighlight(item)) {
                    count++;
                }
            }
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon: Auto-highlighted " + count + " of " + items.size() + " item(s)");
            }
        });
        menuItems.add(autoHighlight);

        // Add sitemap-specific option for URL prefix
        if (isSiteMap && !items.isEmpty()) {
            JMenuItem autoHighlightPrefix = new JMenuItem("Crayon: Highlight URL prefix (apply rules)");
            autoHighlightPrefix.addActionListener(e -> {
                applyColorToUrlPrefix(items.get(0), true);
            });
            menuItems.add(autoHighlightPrefix);
        }

        // Clear highlight
        JMenuItem clearHighlight = new JMenuItem("Crayon: Remove highlights");
        clearHighlight.addActionListener(e -> {
            for (HttpRequestResponse item : items) {
                item.annotations().setHighlightColor(HighlightColor.NONE);
            }
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon: Removed highlights from " + items.size() + " item(s)");
            }
        });
        menuItems.add(clearHighlight);

        // Add sitemap-specific clear option for URL prefix
        if (isSiteMap && !items.isEmpty()) {
            JMenuItem clearPrefixHighlight = new JMenuItem("Crayon: Remove highlights for URL prefix");
            clearPrefixHighlight.addActionListener(e -> {
                applyColorToUrlPrefix(items.get(0), false);
            });
            menuItems.add(clearPrefixHighlight);
        }

        return menuItems;
    }

    private void applyColorToUrlPrefix(HttpRequestResponse item, boolean applyRules) {
        String url = item.request().url();
        String prefix = getUrlPrefix(url);
        
        SiteMap siteMap = api.siteMap();
        List<HttpRequestResponse> matchingItems = siteMap.requestResponses(
            SiteMapFilter.prefixFilter(prefix)
        );

        int count = 0;
        for (HttpRequestResponse matchingItem : matchingItems) {
            if (applyRules) {
                if (applyAutoHighlight(matchingItem)) {
                    count++;
                }
            } else {
                matchingItem.annotations().setHighlightColor(HighlightColor.NONE);
                count++;
            }
        }

        if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
            String action = applyRules ? "Auto-highlighted" : "Removed highlights from";
            logging.logToOutput("Crayon: " + action + " " + count + " item(s) with prefix: " + prefix);
        }
    }

    private String getUrlPrefix(String url) {
        int queryStart = url.indexOf('?');
        String pathPart = queryStart > 0 ? url.substring(0, queryStart) : url;
        int lastSlash = pathPart.lastIndexOf('/');
        if (lastSlash > 0) {
            return pathPart.substring(0, lastSlash + 1);
        }
        return pathPart;
    }

    @Override
    public List<Component> provideMenuItems(WebSocketContextMenuEvent event) {
        // WebSocket messages don't support highlighting in the same way
        return null;
    }

    private boolean applyAutoHighlight(HttpRequestResponse requestResponse) {
        if (requestResponse.response() == null) {
            return false;
        }

        short statusCode = requestResponse.response().statusCode();
        HighlightColor color = null;

        // Apply same logic as RequestColorizer
        if (statusCode >= 500) {
            color = getColorFromSetting(Extension.STATUS_5XX_COLOR_SETTING);
        } else if (statusCode >= 400) {
            color = getColorFromSetting(Extension.STATUS_4XX_COLOR_SETTING);
        } else if (statusCode >= 300) {
            color = getColorFromSetting(Extension.STATUS_3XX_COLOR_SETTING);
        } else if (statusCode >= 200) {
            String method = requestResponse.request().method();
            String contentType = requestResponse.response().headerValue("Content-Type");
            if (contentType == null) contentType = "";

            if ("GET".equalsIgnoreCase(method)) {
                if (contentType.contains("json")) {
                    color = getColorFromSetting(Extension.STATUS_200_GET_JSON_COLOR_SETTING);
                } else if (contentType.contains("html")) {
                    color = getColorFromSetting(Extension.STATUS_200_GET_HTML_COLOR_SETTING);
                } else {
                    color = getColorFromSetting(Extension.STATUS_200_GET_COLOR_SETTING);
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                color = getColorFromSetting(Extension.STATUS_200_POST_COLOR_SETTING);
            } else {
                color = getColorFromSetting(Extension.STATUS_200_OTHER_COLOR_SETTING);
            }
        }

        // Fallback to content type colors
        if (color == null) {
            String contentType = requestResponse.response().headerValue("Content-Type");
            if (contentType == null) contentType = "";

            if (contentType.contains("json")) {
                color = getColorFromSetting(Extension.JSON_COLOR_SETTING);
            } else if (contentType.contains("xml")) {
                color = getColorFromSetting(Extension.XML_COLOR_SETTING);
            } else if (contentType.contains("html")) {
                color = getColorFromSetting(Extension.HTML_COLOR_SETTING);
            } else if (contentType.startsWith("text/plain")) {
                color = getColorFromSetting(Extension.TEXT_COLOR_SETTING);
            }
        }

        if (color != null) {
            requestResponse.annotations().setHighlightColor(color);
            return true;
        }
        return false;
    }

    private HighlightColor getColorFromSetting(String settingName) {
        String colorName = settings.getString(settingName);
        if (colorName != null) {
            try {
                return HighlightColor.valueOf(colorName);
            } catch (IllegalArgumentException e) {
                logging.logToError("Invalid color name in settings: " + colorName);
            }
        }
        return null;
    }
}
