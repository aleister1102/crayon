import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.settings.SettingsPanelWithData;
import burp.api.montoya.ui.settings.SettingsPanelPersistence;
import burp.api.montoya.ui.settings.SettingsPanelSetting;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.ui.settings.SettingsPanelBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Extension implements BurpExtension {
        public static final String HTML_COLOR_SETTING = "HTML Color";
        public static final String JSON_COLOR_SETTING = "JSON Color";
        public static final String JS_COLOR_SETTING = "JavaScript Color";
        public static final String XML_COLOR_SETTING = "XML Color";
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
                                                                HighlightColor.MAGENTA.name()),
                                                SettingsPanelSetting.listSetting(JSON_COLOR_SETTING, colorNames,
                                                                HighlightColor.GREEN.name()),
                                                SettingsPanelSetting.listSetting(JS_COLOR_SETTING, colorNames,
                                                                HighlightColor.YELLOW.name()),
                                                SettingsPanelSetting.listSetting(XML_COLOR_SETTING, colorNames,
                                                                HighlightColor.BLUE.name()),
                                                SettingsPanelSetting.booleanSetting(LOG_ENABLED_SETTING, false))
                                .build();

                montoyaApi.userInterface().registerSettingsPanel(settingsPanel);

                montoyaApi.http().registerHttpHandler(new RequestColorizer(montoyaApi, settingsPanel));
        }
}