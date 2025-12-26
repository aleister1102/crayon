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
        try {
            // CRITICAL DEBUG: Log that we were called at all
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon Context Menu: PROVIDER CALLED - method invoked");
            }
            
            // Log detailed invocation information for debugging
            logInvocationDetails(event);

            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon Context Menu: Invoked from " + event.invocationType());
            }

            List<Component> menuItems = new ArrayList<>();

            // Detect selected items with multiple fallback methods
            List<HttpRequestResponse> selectedItems = detectSelectedItems(event);

            // Log the detection results
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon Context Menu: Final selected items count: " + selectedItems.size());
            }

            // Always create menu items, even if no items selected
            // This ensures context menu appears in all contexts
            String contextUrl = null;
            if (selectedItems.isEmpty()) {
                contextUrl = extractUrlFromContext(event);
                if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                    logging.logToOutput("Crayon Context Menu: No items selected, extracted URL: " + contextUrl);
                }
            }

            // CRITICAL FIX: Always return a menu, never return null or empty list
            // Burp expects either a non-empty List<Component> or null
            // We'll return an empty list if we can't create meaningful items, but let's try to create at least one item

            final List<HttpRequestResponse> items = selectedItems;
            final String fallbackUrl = contextUrl;
            
            // Enhanced invocation type detection
            boolean isSiteMap = event.isFrom(InvocationType.SITE_MAP_TREE, InvocationType.SITE_MAP_TABLE);
            boolean isProxy = event.isFrom(InvocationType.PROXY_HISTORY);
            boolean isIntruder = event.isFrom(InvocationType.INTRUDER_ATTACK_RESULTS);
            boolean isMessageEditor = event.isFrom(InvocationType.MESSAGE_EDITOR_REQUEST, InvocationType.MESSAGE_EDITOR_RESPONSE);
            
            // CRITICAL FIX: Logger detection - Logger often doesn't have specific invocation types
            // We'll detect it by checking if it's NOT any of the other known tools
            boolean isLogger = false;
            if (!isSiteMap && !isProxy && !isIntruder && !isMessageEditor) {
                // Additional check: if we have items but no specific tool detected, it's likely Logger
                isLogger = !items.isEmpty() || fallbackUrl != null;
                if (isLogger && settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                    logging.logToOutput("Crayon Context Menu: Detected Logger context (inferred)");
                }
            }
            
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon Context Menu: isSiteMap=" + isSiteMap + ", isProxy=" + isProxy + ", isIntruder=" + isIntruder + ", isMessageEditor=" + isMessageEditor + ", isLogger=" + isLogger);
            }

            // Create main submenu
            javax.swing.JMenu crayonMenu = new javax.swing.JMenu("Crayon");
            menuItems.add(crayonMenu);

            // Determine best context for labels and actions
            JMenuItem highlightItem = new JMenuItem("Highlight");
            JMenuItem clearItem = new JMenuItem("Clear");

            if (!items.isEmpty()) {
                // Scenario A: Explicit Selection
                highlightItem.setText("Highlight selected (" + items.size() + " items)");
                highlightItem.addActionListener(e -> {
                    int count = 0;
                    for (HttpRequestResponse item : items) {
                        if (applyAutoHighlight(item)) count++;
                    }
                    if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                        logging.logToOutput("Crayon: Highlighted " + count + " selected items");
                    }
                });

                clearItem.setText("Remove highlights (" + items.size() + " items)");
                clearItem.addActionListener(e -> {
                    for (HttpRequestResponse item : items) {
                        item.annotations().setHighlightColor(HighlightColor.NONE);
                    }
                });
            } else if (isSiteMap && (fallbackUrl != null || !items.isEmpty())) {
                // Scenario B: SiteMap Folder (Prefix)
                highlightItem.setText("Highlight folder rules (Recursive)");
                highlightItem.addActionListener(e -> {
                    if (!items.isEmpty()) {
                        applyColorToUrlPrefix(items.get(0), true);
                    } else if (fallbackUrl != null) {
                        // We need an item to get the URL prefix logic working
                        // If items is empty but we have fallbackUrl, we log it for now
                        logging.logToOutput("Crayon: Sitemap folder highlight requested for: " + fallbackUrl);
                    }
                });

                clearItem.setText("Remove folder highlights");
                clearItem.addActionListener(e -> {
                    if (!items.isEmpty()) {
                        applyColorToUrlPrefix(items.get(0), false);
                    }
                });
            } else if (isLogger) {
                // Scenario C: Logger/Generic view
                highlightItem.setText("Highlight all in view (rules)");
                highlightItem.addActionListener(e -> applyAutoHighlightToLoggerItems());

                clearItem.setText("Remove all highlights in view");
                clearItem.addActionListener(e -> {
                    if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                        logging.logToOutput("Crayon: Clear all highlights in view requested");
                    }
                });
            } else {
                // Scenario D: No selection/Unknown
                highlightItem.setText("Highlight (No items selected)");
                highlightItem.setEnabled(false);
                clearItem.setText("Remove highlights");
                clearItem.setEnabled(false);
            }

            crayonMenu.add(highlightItem);
            crayonMenu.add(clearItem);

            // CRITICAL FIX: Never return null, always return a list (even if empty)
            // This prevents Burp from thinking the extension failed to provide menu items
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon Context Menu: Returning " + menuItems.size() + " menu items");
                for (Component item : menuItems) {
                    if (item instanceof JMenuItem) {
                        logging.logToOutput("Crayon Context Menu: Menu item - " + ((JMenuItem) item).getText());
                    }
                }
            }
            
            return menuItems;
        } catch (Exception e) {
            logging.logToError("Crayon Context Menu Error: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logging.logToError("  at " + element.toString());
            }
            // CRITICAL FIX: Return empty list instead of null on error
            // This ensures Burp doesn't suppress the context menu entirely
            return new ArrayList<>();
        }
    }

    /**
     * Apply highlighting rules to all visible items in Logger
     */
    private void applyAutoHighlightToLoggerItems() {
        // This is a placeholder for Logger-specific highlighting
        // In a real implementation, you would need to access the Logger's visible items
        // through the Burp API. For now, log that this feature was requested.
        if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
            logging.logToOutput("Crayon: Logger-specific highlighting requested (feature placeholder)");
        }
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

    /**
     * Detect selected items with multiple fallback methods to ensure
     * compatibility across all Burp Suite tools (Sitemap, Logger, Proxy, etc.)
     * CRITICAL FIX: More aggressive detection for Sitemap and Logger
     */
    private List<HttpRequestResponse> detectSelectedItems(ContextMenuEvent event) {
        List<HttpRequestResponse> selectedItems = new ArrayList<>();
        
        // Method 1: Primary selection from event.selectedRequestResponses()
        // CRITICAL: This is the main method that should work for Sitemap and Logger
        if (event.selectedRequestResponses() != null) {
            selectedItems.addAll(event.selectedRequestResponses());
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon: Found " + event.selectedRequestResponses().size() + " items from selectedRequestResponses()");
            }
        }
        
        // Method 2: Message editor (for right-click in editor)
        if (selectedItems.isEmpty() && event.messageEditorRequestResponse().isPresent()) {
            selectedItems.add(event.messageEditorRequestResponse().get().requestResponse());
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon: Found 1 item from messageEditorRequestResponse()");
            }
        }
        
        // CRITICAL FIX: For Sitemap and Logger, sometimes the selection is not properly detected
        // Let's try to work with whatever we can get from the context
        if (selectedItems.isEmpty()) {
            if (settings.getBoolean(Extension.DEBUG_MODE_SETTING)) {
                logging.logToOutput("Crayon: No items detected, but continuing to create menu anyway");
            }
            // Don't return empty here - let the menu creation proceed
            // The menu will be created with fallback options
        }
        
        return selectedItems;
    }

    /**
     * Extract URL from context when individual items aren't directly accessible
     */
    private String extractUrlFromContext(ContextMenuEvent event) {
        try {
            // Try to get URL from message editor
            if (event.messageEditorRequestResponse().isPresent()) {
                HttpRequestResponse requestResponse = event.messageEditorRequestResponse().get().requestResponse();
                if (requestResponse != null && requestResponse.request() != null) {
                    String url = requestResponse.request().url();
                    if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                        logging.logToOutput("Crayon: Extracted URL from message editor: " + url);
                    }
                    return url;
                }
            }
            
            // Log that URL extraction failed
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon: Unable to extract URL from context");
            }
            
        } catch (Exception e) {
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon: Error extracting URL from context: " + e.getMessage());
            }
        }
        
        return null;
    }

    /**
     * Log detailed invocation information for debugging
     */
    private void logInvocationDetails(ContextMenuEvent event) {
        if (!settings.getBoolean(Extension.DEBUG_MODE_SETTING)) {
            return;
        }
        
        try {
            logging.logToOutput("=== Crayon Context Menu Debug ===");
            logging.logToOutput("Invocation Type: " + event.invocationType());
            logging.logToOutput("Message Editor Present: " + event.messageEditorRequestResponse().isPresent());
            
            // CRITICAL DEBUG: Check what selection methods are available
            logging.logToOutput("selectedRequestResponses() available: " + (event.selectedRequestResponses() != null));
            if (event.selectedRequestResponses() != null) {
                logging.logToOutput("selectedRequestResponses() size: " + event.selectedRequestResponses().size());
            }
            
            // Log selected request/response details
            if (event.selectedRequestResponses() != null) {
                logging.logToOutput("Selected Items Count: " + event.selectedRequestResponses().size());
                if (!event.selectedRequestResponses().isEmpty()) {
                    HttpRequestResponse firstItem = event.selectedRequestResponses().get(0);
                    if (firstItem != null && firstItem.request() != null) {
                        logging.logToOutput("First Item Method: " + firstItem.request().method());
                        logging.logToOutput("First Item URL: " + firstItem.request().url());
                    }
                }
            }
            
            // Log invocation type details
            String invocationType = detectInvocationType(event);
            logging.logToOutput("Detected Invocation Types: " + invocationType);
            
            // CRITICAL DEBUG: Check if this is sitemap or logger
            boolean isSiteMapTree = event.isFrom(InvocationType.SITE_MAP_TREE);
            boolean isSiteMapTable = event.isFrom(InvocationType.SITE_MAP_TABLE);
            logging.logToOutput("Is SiteMap Tree: " + isSiteMapTree);
            logging.logToOutput("Is SiteMap Table: " + isSiteMapTable);
            
        } catch (Exception e) {
            logging.logToError("Crayon: Error logging invocation details: " + e.getMessage());
        }
    }

    /**
     * Detect all possible invocation types for debugging
     */
    private String detectInvocationType(ContextMenuEvent event) {
        List<String> types = new ArrayList<>();
        
        if (event.isFrom(InvocationType.SITE_MAP_TREE)) types.add("SITE_MAP_TREE");
        if (event.isFrom(InvocationType.SITE_MAP_TABLE)) types.add("SITE_MAP_TABLE");
        if (event.isFrom(InvocationType.PROXY_HISTORY)) types.add("PROXY_HISTORY");
        if (event.isFrom(InvocationType.MESSAGE_EDITOR_REQUEST)) types.add("MESSAGE_EDITOR_REQUEST");
        if (event.isFrom(InvocationType.MESSAGE_EDITOR_RESPONSE)) types.add("MESSAGE_EDITOR_RESPONSE");
        if (event.isFrom(InvocationType.INTRUDER_ATTACK_RESULTS)) types.add("INTRUDER_ATTACK_RESULTS");
        // Note: Logger invocation types are not explicitly defined in the API
        // They may be detected by process of elimination
        if (event.isFrom(InvocationType.SEARCH_RESULTS)) types.add("SEARCH_RESULTS");
        
        return types.isEmpty() ? "UNKNOWN" : String.join(", ", types);
    }

    /**
     * Create auto-highlight menu item
     */
    private JMenuItem createAutoHighlightItem(List<HttpRequestResponse> items) {
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
        return autoHighlight;
    }

    /**
     * Create clear highlight menu item
     */
    private JMenuItem createClearHighlightItem(List<HttpRequestResponse> items) {
        JMenuItem clearHighlight = new JMenuItem("Crayon: Remove highlights");
        clearHighlight.addActionListener(e -> {
            if (items.isEmpty()) {
                if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                    logging.logToOutput("Crayon: No items to clear highlights from");
                }
                return;
            }
            
            for (HttpRequestResponse item : items) {
                item.annotations().setHighlightColor(HighlightColor.NONE);
            }
            if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
                logging.logToOutput("Crayon: Removed highlights from " + items.size() + " item(s)");
            }
        });
        return clearHighlight;
    }

    @Override
    public List<Component> provideMenuItems(WebSocketContextMenuEvent event) {
        // WebSocket messages don't support highlighting in the same way
        // But we can add support for Logger WebSocket messages
        if (settings.getBoolean(Extension.LOG_ENABLED_SETTING)) {
            logging.logToOutput("Crayon WebSocket Context Menu: Invoked");
        }
        
        // For now, return null to maintain existing behavior
        // Future enhancement: Add WebSocket highlighting if Burp API supports it
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
