import 'package:drift/drift.dart';

import '../datasources/app_database.dart';

abstract class CapsuleRepository {
  Future<List<Capsule>> getAll();
  Future<Capsule?> getById(int id);
  Future<int> save(Capsule capsule);
  Future<void> delete(int id);
  Stream<List<Capsule>> watchAll();
}

class DriftCapsuleRepository implements CapsuleRepository {
  const DriftCapsuleRepository(this._db);
  final AppDatabase _db;

  @override
  Future<List<Capsule>> getAll() =>
      (_db.select(_db.capsules)..orderBy([(t) => OrderingTerm.asc(t.name)]))
          .get();

  @override
  Future<Capsule?> getById(int id) =>
      (_db.select(_db.capsules)..where((t) => t.id.equals(id)))
          .getSingleOrNull();

  @override
  Future<int> save(Capsule capsule) {
    if (capsule.id <= 0) {
      return _db.into(_db.capsules).insert(CapsulesCompanion.insert(
            name: capsule.name,
            iconCodePoint: Value(capsule.iconCodePoint),
            colorValue: Value(capsule.colorValue),
          ));
    } else {
      return (_db.update(_db.capsules)
            ..where((t) => t.id.equals(capsule.id)))
          .write(CapsulesCompanion(
            name: Value(capsule.name),
            iconCodePoint: Value(capsule.iconCodePoint),
            colorValue: Value(capsule.colorValue),
            noteCount: Value(capsule.noteCount),
            updatedAt: Value(DateTime.now()),
          ))
          .then((_) => capsule.id);
    }
  }

  @override
  Future<void> delete(int id) =>
      (_db.delete(_db.capsules)..where((t) => t.id.equals(id))).go();

  @override
  Stream<List<Capsule>> watchAll() =>
      (_db.select(_db.capsules)..orderBy([(t) => OrderingTerm.asc(t.name)]))
          .watch();
}
