import 'package:flutter/material.dart';

abstract final class AppColors {
  // --- Light palette ---
  static const lightBackground = Color(0xFFF5F5F7);
  static const lightSurface = Color(0xFFFFFFFF);
  static const lightSurfaceVariant = Color(0xFFEFEFF0);
  static const lightBorder = Color(0x0D000000); // 5% black
  static const lightOnSurface = Color(0xFF1C1C1E);
  static const lightOnSurfaceVariant = Color(0xFF6E6E73);

  // --- Dark palette ---
  static const darkBackground = Color(0xFF000000);
  static const darkSurface = Color(0xFF1C1C1E);
  static const darkSurfaceVariant = Color(0xFF2C2C2E);
  static const darkBorder = Color(0x0DFFFFFF); // 5% white
  static const darkOnSurface = Color(0xFFF5F5F7);
  static const darkOnSurfaceVariant = Color(0xFF8E8E93);

  // --- Accent (system-adaptive) ---
  static const accentLight = Color(0xFF007AFF); // iOS blue
  static const accentDark = Color(0xFF0A84FF);  // iOS dark-mode blue

  // --- Semantic ---
  static const destructive = Color(0xFFFF3B30);
  static const success = Color(0xFF34C759);
  static const warning = Color(0xFFFF9500);
}
