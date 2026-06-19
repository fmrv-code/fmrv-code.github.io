import 'package:isar/isar.dart';

import '../models/tag.dart';

abstract class TagRepository {
  Future<List<Tag>> getAll();
  Future<List<Tag>> getByIds(List<int> ids);
  Future<int> save(Tag tag);
  Future<void> delete(int id);
  Stream<List<Tag>> watchAll();
}

class IsarTagRepository implements TagRepository {
  const IsarTagRepository(this._isar);

  final Isar _isar;

  @override
  Future<List<Tag>> getAll() =>
      _isar.tags.where().sortByName().findAll();

  @override
  Future<List<Tag>> getByIds(List<int> ids) =>
      _isar.tags.getAll(ids).then((list) => list.whereType<Tag>().toList());

  @override
  Future<int> save(Tag tag) =>
      _isar.writeTxn(() => _isar.tags.put(tag));

  @override
  Future<void> delete(int id) =>
      _isar.writeTxn(() => _isar.tags.delete(id));

  @override
  Stream<List<Tag>> watchAll() =>
      _isar.tags.where().sortByName().watch(fireImmediately: true);
}
