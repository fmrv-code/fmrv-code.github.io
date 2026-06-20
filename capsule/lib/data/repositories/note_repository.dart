import 'package:drift/drift.dart';

import '../datasources/app_database.dart';

abstract class NoteRepository {
  Future<List<Note>> getAll({int? capsuleId, bool? isPinned});
  Future<Note?> getById(int id);
  Future<int> save(Note note);
  Future<void> delete(int id);
  Future<List<Note>> search(String query);
  Stream<List<Note>> watchAll({int? capsuleId});
  Future<int> count({int? capsuleId});
}

class DriftNoteRepository implements NoteRepository {
  const DriftNoteRepository(this._db);
  final AppDatabase _db;

  @override
  Future<List<Note>> getAll({int? capsuleId, bool? isPinned}) {
    final q = _db.select(_db.notes)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]);
    if (capsuleId != null) q.where((t) => t.capsuleId.equals(capsuleId));
    if (isPinned != null) q.where((t) => t.isPinned.equals(isPinned));
    return q.get();
  }

  @override
  Future<Note?> getById(int id) =>
      (_db.select(_db.notes)..where((t) => t.id.equals(id))).getSingleOrNull();

  @override
  Future<int> save(Note note) {
    if (note.id <= 0) {
      return _db.into(_db.notes).insert(NotesCompanion.insert(
            title: note.title,
            content: Value(note.content),
            capsuleId: Value(note.capsuleId),
            tagIds: Value(note.tagIds),
            isPinned: Value(note.isPinned),
            isFavorite: Value(note.isFavorite),
            createdAt: Value(note.createdAt),
            updatedAt: Value(note.updatedAt),
          ));
    } else {
      return (_db.update(_db.notes)..where((t) => t.id.equals(note.id)))
          .write(NotesCompanion(
            title: Value(note.title),
            content: Value(note.content),
            capsuleId: Value(note.capsuleId),
            tagIds: Value(note.tagIds),
            isPinned: Value(note.isPinned),
            isFavorite: Value(note.isFavorite),
            updatedAt: Value(DateTime.now()),
          ))
          .then((_) => note.id);
    }
  }

  @override
  Future<void> delete(int id) =>
      (_db.delete(_db.notes)..where((t) => t.id.equals(id))).go();

  @override
  Future<List<Note>> search(String query) {
    if (query.isEmpty) return Future.value([]);
    return (_db.select(_db.notes)
          ..where((t) => t.title.like('%$query%') | t.content.like('%$query%'))
          ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .get();
  }

  @override
  Stream<List<Note>> watchAll({int? capsuleId}) {
    final q = _db.select(_db.notes)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]);
    if (capsuleId != null) q.where((t) => t.capsuleId.equals(capsuleId));
    return q.watch();
  }

  @override
  Future<int> count({int? capsuleId}) async {
    final countCol = _db.notes.id.count();
    final q = _db.selectOnly(_db.notes)..addColumns([countCol]);
    if (capsuleId != null) {
      q.where(_db.notes.capsuleId.equals(capsuleId));
    }
    final row = await q.getSingle();
    return row.read(countCol) ?? 0;
  }
}
