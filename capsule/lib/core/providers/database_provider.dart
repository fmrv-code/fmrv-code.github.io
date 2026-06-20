import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/app_database.dart';
import '../../data/repositories/capsule_repository.dart';
import '../../data/repositories/link_repository.dart';
import '../../data/repositories/note_repository.dart';
import '../../data/repositories/tag_repository.dart';

// Overridden in main.dart before runApp.
final databaseProvider = Provider<AppDatabase>(
  (ref) => throw UnimplementedError('databaseProvider not initialized'),
);

final noteRepositoryProvider = Provider<NoteRepository>(
  (ref) => DriftNoteRepository(ref.watch(databaseProvider)),
);

final capsuleRepositoryProvider = Provider<CapsuleRepository>(
  (ref) => DriftCapsuleRepository(ref.watch(databaseProvider)),
);

final tagRepositoryProvider = Provider<TagRepository>(
  (ref) => DriftTagRepository(ref.watch(databaseProvider)),
);

final linkRepositoryProvider = Provider<LinkRepository>(
  (ref) => DriftLinkRepository(ref.watch(databaseProvider)),
);
