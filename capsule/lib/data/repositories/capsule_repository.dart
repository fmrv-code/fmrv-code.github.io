import 'package:isar/isar.dart';

import '../models/capsule.dart';

abstract class CapsuleRepository {
  Future<List<Capsule>> getAll();
  Future<Capsule?> getById(int id);
  Future<int> save(Capsule capsule);
  Future<void> delete(int id);
  Stream<List<Capsule>> watchAll();
}

class IsarCapsuleRepository implements CapsuleRepository {
  const IsarCapsuleRepository(this._isar);

  final Isar _isar;

  @override
  Future<List<Capsule>> getAll() =>
      _isar.capsules.where().sortByName().findAll();

  @override
  Future<Capsule?> getById(int id) => _isar.capsules.get(id);

  @override
  Future<int> save(Capsule capsule) =>
      _isar.writeTxn(() => _isar.capsules.put(capsule));

  @override
  Future<void> delete(int id) =>
      _isar.writeTxn(() => _isar.capsules.delete(id));

  @override
  Stream<List<Capsule>> watchAll() =>
      _isar.capsules.where().sortByName().watch(fireImmediately: true);
}
