import 'package:isar/isar.dart';

import '../models/note.dart';

abstract class NoteRepository {
  Future<List<Note>> getAll({int? capsuleId, bool? isPinned});
  Future<Note?> getById(int id);
  Future<int> save(Note note);
  Future<void> delete(int id);
  Future<List<Note>> search(String query);
  Stream<List<Note>> watchAll({int? capsuleId});
  Future<int> count({int? capsuleId});
}

class IsarNoteRepository implements NoteRepository {
  const IsarNoteRepository(this._isar);

  final Isar _isar;

  @override
  Future<List<Note>> getAll({int? capsuleId, bool? isPinned}) {
    return _isar.notes
        .filter()
        .optional(capsuleId != null, (q) => q.capsuleIdEqualTo(capsuleId))
        .optional(isPinned != null, (q) => q.isPinnedEqualTo(isPinned!))
        .sortByCreatedAtDesc()
        .findAll();
  }

  @override
  Future<Note?> getById(int id) => _isar.notes.get(id);

  @override
  Future<int> save(Note note) => _isar.writeTxn(() => _isar.notes.put(note));

  @override
  Future<void> delete(int id) =>
      _isar.writeTxn(() => _isar.notes.delete(id));

  @override
  Future<List<Note>> search(String query) {
    if (query.isEmpty) return Future.value([]);
    return _isar.notes
        .filter()
        .titleContains(query, caseSensitive: false)
        .or()
        .contentContains(query, caseSensitive: false)
        .sortByCreatedAtDesc()
        .findAll();
  }

  @override
  Stream<List<Note>> watchAll({int? capsuleId}) {
    return _isar.notes
        .filter()
        .optional(capsuleId != null, (q) => q.capsuleIdEqualTo(capsuleId))
        .sortByCreatedAtDesc()
        .watch(fireImmediately: true);
  }

  @override
  Future<int> count({int? capsuleId}) {
    return _isar.notes
        .filter()
        .optional(capsuleId != null, (q) => q.capsuleIdEqualTo(capsuleId))
        .count();
  }
}
