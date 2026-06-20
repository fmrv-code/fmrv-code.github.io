import 'package:drift/drift.dart';

import '../datasources/app_database.dart';

abstract class LinkRepository {
  Future<List<CapsuleLink>> getAll({int? capsuleId, bool? isFavorite});
  Future<CapsuleLink?> getById(int id);
  Future<CapsuleLink?> getByUrl(String url);
  Future<int> save(CapsuleLink link);
  Future<void> delete(int id);
  Future<List<CapsuleLink>> search(String query);
  Stream<List<CapsuleLink>> watchAll({int? capsuleId});
}

class DriftLinkRepository implements LinkRepository {
  const DriftLinkRepository(this._db);
  final AppDatabase _db;

  @override
  Future<List<CapsuleLink>> getAll({int? capsuleId, bool? isFavorite}) {
    final q = _db.select(_db.capsuleLinks)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]);
    if (capsuleId != null) q.where((t) => t.capsuleId.equals(capsuleId));
    if (isFavorite != null) q.where((t) => t.isFavorite.equals(isFavorite));
    return q.get();
  }

  @override
  Future<CapsuleLink?> getById(int id) =>
      (_db.select(_db.capsuleLinks)..where((t) => t.id.equals(id)))
          .getSingleOrNull();

  @override
  Future<CapsuleLink?> getByUrl(String url) =>
      (_db.select(_db.capsuleLinks)..where((t) => t.url.equals(url)))
          .getSingleOrNull();

  @override
  Future<int> save(CapsuleLink link) {
    if (link.id <= 0) {
      return _db.into(_db.capsuleLinks).insert(CapsuleLinksCompanion.insert(
            url: link.url,
            title: Value(link.title),
            description: Value(link.description),
            faviconUrl: Value(link.faviconUrl),
            imageUrl: Value(link.imageUrl),
            siteName: Value(link.siteName),
            metadataFetched: Value(link.metadataFetched),
            capsuleId: Value(link.capsuleId),
            tagIds: Value(link.tagIds),
            isFavorite: Value(link.isFavorite),
          ));
    } else {
      return (_db.update(_db.capsuleLinks)
            ..where((t) => t.id.equals(link.id)))
          .write(CapsuleLinksCompanion(
            url: Value(link.url),
            title: Value(link.title),
            description: Value(link.description),
            faviconUrl: Value(link.faviconUrl),
            imageUrl: Value(link.imageUrl),
            siteName: Value(link.siteName),
            metadataFetched: Value(link.metadataFetched),
            capsuleId: Value(link.capsuleId),
            tagIds: Value(link.tagIds),
            isFavorite: Value(link.isFavorite),
            updatedAt: Value(DateTime.now()),
          ))
          .then((_) => link.id);
    }
  }

  @override
  Future<void> delete(int id) =>
      (_db.delete(_db.capsuleLinks)..where((t) => t.id.equals(id))).go();

  @override
  Future<List<CapsuleLink>> search(String query) {
    if (query.isEmpty) return Future.value([]);
    return (_db.select(_db.capsuleLinks)
          ..where((t) =>
              t.url.like('%$query%') |
              t.title.like('%$query%') |
              t.description.like('%$query%'))
          ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .get();
  }

  @override
  Stream<List<CapsuleLink>> watchAll({int? capsuleId}) {
    final q = _db.select(_db.capsuleLinks)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]);
    if (capsuleId != null) q.where((t) => t.capsuleId.equals(capsuleId));
    return q.watch();
  }
}
