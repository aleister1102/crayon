
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.ui.settings.SettingsPanelBuilder;
import burp.api.montoya.ui.settings.SettingsPanelPersistence;
import burp.api.montoya.ui.settings.SettingsPanelSetting;
import burp.api.montoya.ui.settings.SettingsPanelWithData;

public class Extension implements BurpExtension {

    public static final String HTML_COLOR_SETTING = "HTML Color";
    public static final String JSON_COLOR_SETTING = "JSON Color";
    public static final String XML_COLOR_SETTING = "XML Color";
    public static final String TEXT_COLOR_SETTING = "Text Color";
    public static final String STATUS_5XX_COLOR_SETTING = "5xx Status Color";
    public static final String STATUS_4XX_COLOR_SETTING = "4xx Status Color";
    public static final String STATUS_3XX_COLOR_SETTING = "3xx Status Color";
    public static final String STATUS_200_GET_COLOR_SETTING = "200 GET Color";
    public static final String STATUS_200_GET_JSON_COLOR_SETTING = "200 GET JSON Color";
    public static final String STATUS_200_GET_HTML_COLOR_SETTING = "200 GET HTML Color";
    public static final String STATUS_200_POST_COLOR_SETTING = "200 POST Color";
    public static final String STATUS_200_OTHER_COLOR_SETTING = "200 Other Methods Color";
    public static final String LOG_ENABLED_SETTING = "Enable logging";

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        montoyaApi.extension().setName("Crayon");

        List<String> colorNames = Arrays.stream(HighlightColor.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        SettingsPanelWithData settingsPanel = SettingsPanelBuilder.settingsPanel()
                .withPersistence(SettingsPanelPersistence.USER_SETTINGS)
                .withTitle("Crayon Settings")
                .withSettings(
                        SettingsPanelSetting.listSetting(HTML_COLOR_SETTING, colorNames,
                                HighlightColor.BLUE.name()),
                        SettingsPanelSetting.listSetting(JSON_COLOR_SETTING, colorNames,
                                HighlightColor.GREEN.name()),
                        SettingsPanelSetting.listSetting(XML_COLOR_SETTING, colorNames,
                                HighlightColor.BLUE.name()),
                        SettingsPanelSetting.listSetting(TEXT_COLOR_SETTING, colorNames,
                                HighlightColor.GRAY.name()),
                        SettingsPanelSetting.listSetting(STATUS_5XX_COLOR_SETTING, colorNames,
                                HighlightColor.RED.name()),
                        SettingsPanelSetting.listSetting(STATUS_4XX_COLOR_SETTING, colorNames,
                                HighlightColor.ORANGE.name()),
                        SettingsPanelSetting.listSetting(STATUS_3XX_COLOR_SETTING, colorNames,
                                HighlightColor.YELLOW.name()),
                        SettingsPanelSetting.listSetting(STATUS_200_GET_COLOR_SETTING, colorNames,
                                HighlightColor.GREEN.name()),
                        SettingsPanelSetting.listSetting(STATUS_200_GET_JSON_COLOR_SETTING, colorNames,
                                HighlightColor.GREEN.name()),
                        SettingsPanelSetting.listSetting(STATUS_200_GET_HTML_COLOR_SETTING, colorNames,
                                HighlightColor.CYAN.name()),
                        SettingsPanelSetting.listSetting(STATUS_200_POST_COLOR_SETTING, colorNames,
                                HighlightColor.BLUE.name()),
                        SettingsPanelSetting.listSetting(STATUS_200_OTHER_COLOR_SETTING, colorNames,
                                HighlightColor.PINK.name()),
                        SettingsPanelSetting.booleanSetting(LOG_ENABLED_SETTING, false))
                .build();

        montoyaApi.userInterface().registerSettingsPanel(settingsPanel);

        montoyaApi.http().registerHttpHandler(new RequestColorizer(montoyaApi, settingsPanel));
    }
}
