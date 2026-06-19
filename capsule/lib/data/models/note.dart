import 'package:flutter/material.dart';

import '../datasources/app_database.dart';

extension NoteX on Note {
  String get excerpt {
    final plain = content.replaceAll(RegExp(r'[#*`>~\[\]!]|\*+|_+'), '').trim();
    return plain.length > 120 ? '${plain.substring(0, 120)}…' : plain;
  }
}

Note blankNote({int? capsuleId}) => Note(
      id: 0,
      title: '',
      content: '',
      capsuleId: capsuleId,
      tagIds: const [],
      isPinned: false,
      isFavorite: false,
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );

// Suppress unused import warning — Color is used by feature widgets
// that import this file for the blankNote factory.
const _unused = Colors.transparent;
