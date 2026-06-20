import 'package:drift/drift.dart';

import '../datasources/app_database.dart';

abstract class TagRepository {
  Future<List<Tag>> getAll();
  Future<List<Tag>> getByIds(List<int> ids);
  Future<int> save(Tag tag);
  Future<void> delete(int id);
  Stream<List<Tag>> watchAll();
}

class DriftTagRepository implements TagRepository {
  const DriftTagRepository(this._db);
  final AppDatabase _db;

  @override
  Future<List<Tag>> getAll() =>
      (_db.select(_db.tags)..orderBy([(t) => OrderingTerm.asc(t.name)])).get();

  @override
  Future<List<Tag>> getByIds(List<int> ids) {
    if (ids.isEmpty) return Future.value([]);
    return (_db.select(_db.tags)..where((t) => t.id.isIn(ids))).get();
  }

  @override
  Future<int> save(Tag tag) {
    if (tag.id <= 0) {
      return _db.into(_db.tags).insert(TagsCompanion.insert(
            name: tag.name,
            colorValue: Value(tag.colorValue),
          ));
    } else {
      return (_db.update(_db.tags)..where((t) => t.id.equals(tag.id)))
          .write(TagsCompanion(
            name: Value(tag.name),
            colorValue: Value(tag.colorValue),
          ))
          .then((_) => tag.id);
    }
  }

  @override
  Future<void> delete(int id) =>
      (_db.delete(_db.tags)..where((t) => t.id.equals(id))).go();

  @override
  Stream<List<Tag>> watchAll() =>
      (_db.select(_db.tags)..orderBy([(t) => OrderingTerm.asc(t.name)]))
          .watch();
}
