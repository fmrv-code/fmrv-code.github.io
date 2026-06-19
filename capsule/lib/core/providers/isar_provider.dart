import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:isar/isar.dart';

import '../../data/repositories/capsule_repository.dart';
import '../../data/repositories/link_repository.dart';
import '../../data/repositories/note_repository.dart';
import '../../data/repositories/tag_repository.dart';

// Overridden in main.dart before runApp.
final isarProvider = Provider<Isar>(
  (ref) => throw UnimplementedError('isarProvider not initialized'),
);

final noteRepositoryProvider = Provider<NoteRepository>(
  (ref) => IsarNoteRepository(ref.watch(isarProvider)),
);

final capsuleRepositoryProvider = Provider<CapsuleRepository>(
  (ref) => IsarCapsuleRepository(ref.watch(isarProvider)),
);

final tagRepositoryProvider = Provider<TagRepository>(
  (ref) => IsarTagRepository(ref.watch(isarProvider)),
);

final linkRepositoryProvider = Provider<LinkRepository>(
  (ref) => IsarLinkRepository(ref.watch(isarProvider)),
);
