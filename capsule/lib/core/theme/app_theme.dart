import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'app_colors.dart';
import 'app_text_styles.dart';

abstract final class AppTheme {
  static const _radius = Radius.circular(24);
  static const _borderRadius = BorderRadius.all(_radius);
  static const _inputRadius = BorderRadius.all(Radius.circular(16));

  // ------------------------------------------------------------------ LIGHT
  static ThemeData get light => ThemeData(
        useMaterial3: true,
        brightness: Brightness.light,
        scaffoldBackgroundColor: AppColors.lightBackground,
        colorScheme: const ColorScheme.light(
          brightness: Brightness.light,
          primary: AppColors.accentLight,
          onPrimary: Colors.white,
          primaryContainer: Color(0xFFE5F0FF),
          onPrimaryContainer: Color(0xFF003E8A),
          secondary: AppColors.lightOnSurfaceVariant,
          onSecondary: Colors.white,
          surface: AppColors.lightSurface,
          onSurface: AppColors.lightOnSurface,
          surfaceContainerHighest: AppColors.lightSurfaceVariant,
          onSurfaceVariant: AppColors.lightOnSurfaceVariant,
          outline: AppColors.lightBorder,
          error: AppColors.destructive,
        ),
        appBarTheme: AppBarTheme(
          backgroundColor: AppColors.lightBackground,
          foregroundColor: AppColors.lightOnSurface,
          elevation: 0,
          scrolledUnderElevation: 0,
          centerTitle: false,
          titleTextStyle: AppTextStyles.largeTitle.copyWith(
            color: AppColors.lightOnSurface,
          ),
          systemOverlayStyle: const SystemUiOverlayStyle(
            statusBarBrightness: Brightness.light,
            statusBarIconBrightness: Brightness.dark,
          ),
        ),
        cardTheme: CardThemeData(
          color: AppColors.lightSurface,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: _borderRadius,
            side: const BorderSide(
              color: AppColors.lightBorder,
              width: 1,
            ),
          ),
          margin: EdgeInsets.zero,
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: AppColors.lightSurface,
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 20,
            vertical: 16,
          ),
          border: OutlineInputBorder(
            borderRadius: _inputRadius,
            borderSide: const BorderSide(color: AppColors.lightBorder, width: 1),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: _inputRadius,
            borderSide: const BorderSide(color: AppColors.lightBorder, width: 1),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: _inputRadius,
            borderSide: const BorderSide(color: AppColors.accentLight, width: 1.5),
          ),
          hintStyle: AppTextStyles.body.copyWith(
            color: AppColors.lightOnSurfaceVariant,
          ),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.accentLight,
            foregroundColor: Colors.white,
            elevation: 0,
            shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            textStyle: AppTextStyles.headline,
          ),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.accentLight,
            foregroundColor: Colors.white,
            shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            textStyle: AppTextStyles.headline,
          ),
        ),
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.accentLight,
            side: const BorderSide(color: AppColors.accentLight, width: 1.5),
            shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            textStyle: AppTextStyles.headline,
          ),
        ),
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            foregroundColor: AppColors.accentLight,
            textStyle: AppTextStyles.callout,
            shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
          ),
        ),
        listTileTheme: const ListTileThemeData(
          tileColor: AppColors.lightSurface,
          contentPadding: EdgeInsets.symmetric(horizontal: 24, vertical: 4),
          shape: RoundedRectangleBorder(borderRadius: _borderRadius),
        ),
        dividerTheme: const DividerThemeData(
          color: AppColors.lightBorder,
          thickness: 1,
          space: 0,
        ),
        chipTheme: ChipThemeData(
          backgroundColor: AppColors.lightSurfaceVariant,
          selectedColor: const Color(0xFFE5F0FF),
          labelStyle: AppTextStyles.caption.copyWith(
            color: AppColors.lightOnSurface,
          ),
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(12)),
          ),
          side: const BorderSide(color: AppColors.lightBorder, width: 1),
        ),
        bottomNavigationBarTheme: const BottomNavigationBarThemeData(
          backgroundColor: AppColors.lightSurface,
          selectedItemColor: AppColors.accentLight,
          unselectedItemColor: AppColors.lightOnSurfaceVariant,
          elevation: 0,
          type: BottomNavigationBarType.fixed,
        ),
        navigationBarTheme: NavigationBarThemeData(
          backgroundColor: AppColors.lightSurface,
          indicatorColor: const Color(0xFFE5F0FF),
          iconTheme: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.selected)) {
              return const IconThemeData(color: AppColors.accentLight, size: 24);
            }
            return const IconThemeData(
                color: AppColors.lightOnSurfaceVariant, size: 24);
          }),
          labelTextStyle: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.selected)) {
              return AppTextStyles.caption2.copyWith(
                color: AppColors.accentLight,
                fontWeight: FontWeight.w600,
              );
            }
            return AppTextStyles.caption2.copyWith(
              color: AppColors.lightOnSurfaceVariant,
            );
          }),
          elevation: 0,
          shadowColor: Colors.transparent,
        ),
        floatingActionButtonTheme: const FloatingActionButtonThemeData(
          backgroundColor: AppColors.accentLight,
          foregroundColor: Colors.white,
          elevation: 0,
          focusElevation: 0,
          hoverElevation: 0,
          shape: RoundedRectangleBorder(borderRadius: _borderRadius),
        ),
        textTheme: _buildTextTheme(AppColors.lightOnSurface),
        iconTheme: const IconThemeData(
          color: AppColors.lightOnSurface,
          size: 24,
        ),
        snackBarTheme: SnackBarThemeData(
          backgroundColor: AppColors.lightOnSurface,
          contentTextStyle: AppTextStyles.callout.copyWith(
            color: AppColors.lightSurface,
          ),
          behavior: SnackBarBehavior.floating,
          shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
        ),
        switchTheme: SwitchThemeData(
          thumbColor: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.selected)) return Colors.white;
            return AppColors.lightOnSurfaceVariant;
          }),
          trackColor: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.selected)) {
              return AppColors.accentLight;
            }
            return AppColors.lightSurfaceVariant;
          }),
        ),
      );

  // ------------------------------------------------------------------ DARK
  static ThemeData get dark => ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        scaffoldBackgroundColor: AppColors.darkBackground,
        colorScheme: const ColorScheme.dark(
          brightness: Brightness.dark,
          primary: AppColors.accentDark,
          onPrimary: Colors.white,
          primaryContainer: Color(0xFF003E8A),
          onPrimaryContainer: Color(0xFFD1E4FF),
          secondary: AppColors.darkOnSurfaceVariant,
          onSecondary: Colors.white,
          surface: AppColors.darkSurface,
          onSurface: AppColors.darkOnSurface,
          surfaceContainerHighest: AppColors.darkSurfaceVariant,
          onSurfaceVariant: AppColors.darkOnSurfaceVariant,
          outline: AppColors.darkBorder,
          error: AppColors.destructive,
        ),
        appBarTheme: AppBarTheme(
          backgroundColor: AppColors.darkBackground,
          foregroundColor: AppColors.darkOnSurface,
          elevation: 0,
          scrolledUnderElevation: 0,
          centerTitle: false,
          titleTextStyle: AppTextStyles.largeTitle.copyWith(
            color: AppColors.darkOnSurface,
          ),
          systemOverlayStyle: const SystemUiOverlayStyle(
            statusBarBrightness: Brightness.dark,
            statusBarIconBrightness: Brightness.light,
          ),
        ),
        cardTheme: CardThemeData(
          color: AppColors.darkSurface,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: _borderRadius,
            side: const BorderSide(
              color: AppColors.darkBorder,
              width: 1,
            ),
          ),
          margin: EdgeInsets.zero,
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: AppColors.darkSurface,
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 20,
            vertical: 16,
          ),
          border: OutlineInputBorder(
            borderRadius: _inputRadius,
            borderSide: const BorderSide(color: AppColors.darkBorder, width: 1),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: _inputRadius,
            borderSide: const BorderSide(color: AppColors.darkBorder, width: 1),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: _inputRadius,
            borderSide: const BorderSide(color: AppColors.accentDark, width: 1.5),
          ),
          hintStyle: AppTextStyles.body.copyWith(
            color: AppColors.darkOnSurfaceVariant,
          ),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.accentDark,
            foregroundColor: Colors.white,
            elevation: 0,
            shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            textStyle: AppTextStyles.headline,
          ),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.accentDark,
            foregroundColor: Colors.white,
            shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            textStyle: AppTextStyles.headline,
          ),
        ),
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.accentDark,
            side: const BorderSide(color: AppColors.accentDark, width: 1.5),
            shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            textStyle: AppTextStyles.headline,
          ),
        ),
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            foregroundColor: AppColors.accentDark,
            textStyle: AppTextStyles.callout,
            shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
          ),
        ),
        listTileTheme: const ListTileThemeData(
          tileColor: AppColors.darkSurface,
          contentPadding: EdgeInsets.symmetric(horizontal: 24, vertical: 4),
          shape: RoundedRectangleBorder(borderRadius: _borderRadius),
        ),
        dividerTheme: const DividerThemeData(
          color: AppColors.darkBorder,
          thickness: 1,
          space: 0,
        ),
        chipTheme: ChipThemeData(
          backgroundColor: AppColors.darkSurfaceVariant,
          selectedColor: const Color(0xFF003E8A),
          labelStyle: AppTextStyles.caption.copyWith(
            color: AppColors.darkOnSurface,
          ),
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(12)),
          ),
          side: const BorderSide(color: AppColors.darkBorder, width: 1),
        ),
        bottomNavigationBarTheme: const BottomNavigationBarThemeData(
          backgroundColor: AppColors.darkSurface,
          selectedItemColor: AppColors.accentDark,
          unselectedItemColor: AppColors.darkOnSurfaceVariant,
          elevation: 0,
          type: BottomNavigationBarType.fixed,
        ),
        navigationBarTheme: NavigationBarThemeData(
          backgroundColor: AppColors.darkSurface,
          indicatorColor: const Color(0xFF003E8A),
          iconTheme: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.selected)) {
              return const IconThemeData(color: AppColors.accentDark, size: 24);
            }
            return const IconThemeData(
                color: AppColors.darkOnSurfaceVariant, size: 24);
          }),
          labelTextStyle: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.selected)) {
              return AppTextStyles.caption2.copyWith(
                color: AppColors.accentDark,
                fontWeight: FontWeight.w600,
              );
            }
            return AppTextStyles.caption2.copyWith(
              color: AppColors.darkOnSurfaceVariant,
            );
          }),
          elevation: 0,
          shadowColor: Colors.transparent,
        ),
        floatingActionButtonTheme: const FloatingActionButtonThemeData(
          backgroundColor: AppColors.accentDark,
          foregroundColor: Colors.white,
          elevation: 0,
          focusElevation: 0,
          hoverElevation: 0,
          shape: RoundedRectangleBorder(borderRadius: _borderRadius),
        ),
        textTheme: _buildTextTheme(AppColors.darkOnSurface),
        iconTheme: const IconThemeData(
          color: AppColors.darkOnSurface,
          size: 24,
        ),
        snackBarTheme: SnackBarThemeData(
          backgroundColor: AppColors.darkSurfaceVariant,
          contentTextStyle: AppTextStyles.callout.copyWith(
            color: AppColors.darkOnSurface,
          ),
          behavior: SnackBarBehavior.floating,
          shape: const RoundedRectangleBorder(borderRadius: _borderRadius),
        ),
        switchTheme: SwitchThemeData(
          thumbColor: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.selected)) return Colors.white;
            return AppColors.darkOnSurfaceVariant;
          }),
          trackColor: WidgetStateProperty.resolveWith((states) {
            if (states.contains(WidgetState.selected)) {
              return AppColors.accentDark;
            }
            return AppColors.darkSurfaceVariant;
          }),
        ),
      );

  static TextTheme _buildTextTheme(Color baseColor) => TextTheme(
        displayLarge: AppTextStyles.largeTitle.copyWith(color: baseColor),
        displayMedium: AppTextStyles.title1.copyWith(color: baseColor),
        displaySmall: AppTextStyles.title2.copyWith(color: baseColor),
        headlineLarge: AppTextStyles.title2.copyWith(color: baseColor),
        headlineMedium: AppTextStyles.title3.copyWith(color: baseColor),
        headlineSmall: AppTextStyles.headline.copyWith(color: baseColor),
        titleLarge: AppTextStyles.headline.copyWith(color: baseColor),
        titleMedium: AppTextStyles.callout.copyWith(
          color: baseColor,
          fontWeight: FontWeight.w600,
        ),
        titleSmall: AppTextStyles.subheadline.copyWith(
          color: baseColor,
          fontWeight: FontWeight.w600,
        ),
        bodyLarge: AppTextStyles.body.copyWith(color: baseColor),
        bodyMedium: AppTextStyles.callout.copyWith(color: baseColor),
        bodySmall: AppTextStyles.subheadline.copyWith(color: baseColor),
        labelLarge: AppTextStyles.footnote.copyWith(
          color: baseColor,
          fontWeight: FontWeight.w600,
        ),
        labelMedium: AppTextStyles.caption.copyWith(color: baseColor),
        labelSmall: AppTextStyles.caption2.copyWith(color: baseColor),
      );
}
