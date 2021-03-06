package com.ost.walletsdk.ui.uicomponents.uiutils.theme;

import org.json.JSONObject;

public interface Theme {

    UIConfig H1();

    UIConfig H2();

    UIConfig H3();

    UIConfig H4();

    UIConfig C1();

    UIConfig C2();

    UIConfig B1();

    UIConfig B2();

    UIConfig B3();

    UIConfig B4();

    DrawableConfig getDrawableConfig(String imageName);

    DrawableConfig getIconConfig(String imageName);

    String getFontRelativePath(String font);

    NavigationConfig getNavigationBar();

    PinViewConfig getPinViewConfig();

    EditTextUIConfig getEditText();

    JSONObject getThemeObject();
}
