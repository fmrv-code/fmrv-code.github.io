import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers/database_provider.dart';
import '../../../data/datasources/app_database.dart';

// ─── Stream live de toutes les notes ─────────────────────────────────────────

final notesStreamProvider = StreamProvider<List<Note>>((ref) {
  return ref.watch(noteRepositoryProvider).watchAll();
});

// ─── Recherche ────────────────────────────────────────────────────────────────

final notesSearchQueryProvider = StateProvider<String>((ref) => '');

// Filtrage client-side (rapide pour V1)
final filteredNotesProvider = Provider<AsyncValue<List<Note>>>((ref) {
  final notesAsync = ref.watch(notesStreamProvider);
  final query = ref.watch(notesSearchQueryProvider).trim().toLowerCase();

  return notesAsync.whenData((notes) {
    if (query.isEmpty) return notes;
    return notes
        .where((n) =>
            n.title.toLowerCase().contains(query) ||
            n.content.toLowerCase().contains(query))
        .toList();
  });
});
