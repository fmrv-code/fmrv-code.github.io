import 'package:isar/isar.dart';

import '../models/capsule_link.dart';

abstract class LinkRepository {
  Future<List<CapsuleLink>> getAll({int? capsuleId, bool? isFavorite});
  Future<CapsuleLink?> getById(int id);
  Future<CapsuleLink?> getByUrl(String url);
  Future<int> save(CapsuleLink link);
  Future<void> delete(int id);
  Future<List<CapsuleLink>> search(String query);
  Stream<List<CapsuleLink>> watchAll({int? capsuleId});
}

class IsarLinkRepository implements LinkRepository {
  const IsarLinkRepository(this._isar);

  final Isar _isar;

  @override
  Future<List<CapsuleLink>> getAll({int? capsuleId, bool? isFavorite}) {
    return _isar.capsuleLinks
        .filter()
        .optional(capsuleId != null, (q) => q.capsuleIdEqualTo(capsuleId))
        .optional(isFavorite != null, (q) => q.isFavoriteEqualTo(isFavorite!))
        .sortByCreatedAtDesc()
        .findAll();
  }

  @override
  Future<CapsuleLink?> getById(int id) => _isar.capsuleLinks.get(id);

  @override
  Future<CapsuleLink?> getByUrl(String url) =>
      _isar.capsuleLinks.filter().urlEqualTo(url).findFirst();

  @override
  Future<int> save(CapsuleLink link) =>
      _isar.writeTxn(() => _isar.capsuleLinks.put(link));

  @override
  Future<void> delete(int id) =>
      _isar.writeTxn(() => _isar.capsuleLinks.delete(id));

  @override
  Future<List<CapsuleLink>> search(String query) {
    if (query.isEmpty) return Future.value([]);
    return _isar.capsuleLinks
        .filter()
        .urlContains(query, caseSensitive: false)
        .or()
        .titleContains(query, caseSensitive: false)
        .or()
        .descriptionContains(query, caseSensitive: false)
        .sortByCreatedAtDesc()
        .findAll();
  }

  @override
  Stream<List<CapsuleLink>> watchAll({int? capsuleId}) {
    return _isar.capsuleLinks
        .filter()
        .optional(capsuleId != null, (q) => q.capsuleIdEqualTo(capsuleId))
        .sortByCreatedAtDesc()
        .watch(fireImmediately: true);
  }
}
