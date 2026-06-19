import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'shared_preferences_provider.dart';

const _kThemeModeKey = 'theme_mode';

class ThemeNotifier extends Notifier<ThemeMode> {
  static const _values = {
    'system': ThemeMode.system,
    'light': ThemeMode.light,
    'dark': ThemeMode.dark,
  };

  @override
  ThemeMode build() {
    final prefs = ref.read(sharedPreferencesProvider);
    final stored = prefs.getString(_kThemeModeKey);
    return _values[stored] ?? ThemeMode.system;
  }

  Future<void> setTheme(ThemeMode mode) async {
    final key = _values.entries.firstWhere((e) => e.value == mode).key;
    await ref.read(sharedPreferencesProvider).setString(_kThemeModeKey, key);
    state = mode;
  }
}

final themeProvider = NotifierProvider<ThemeNotifier, ThemeMode>(
  ThemeNotifier.new,
);
