import 'package:flutter/material.dart';

import '../datasources/app_database.dart';

extension TagX on Tag {
  Color get color => Color(colorValue);
}

Tag blankTag() => Tag(
      id: 0,
      name: '',
      colorValue: 0xFF007AFF,
      createdAt: DateTime.now(),
    );
