package com.newtoncodes.spellchecker.settings;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;


@SuppressWarnings({"unused", "WeakerAccess"})
public final class Bundle {
  public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, @NotNull Object... params) {
    return CommonBundle.message(BUNDLE, key, params);
  }

  @NonNls
  private static final String BUNDLE_NAME = "com.newtoncodes.spellchecker.settings.Bundle";
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  private Bundle() {}
}
