import 'package:flutter/material.dart';

import '../datasources/app_database.dart';

extension CapsuleX on Capsule {
  IconData get icon => IconData(iconCodePoint, fontFamily: 'MaterialIcons');
  Color get color => Color(colorValue);
}

Capsule blankCapsule() => Capsule(
      id: 0,
      name: '',
      iconCodePoint: Icons.folder.codePoint,
      colorValue: 0xFF007AFF,
      noteCount: 0,
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );
