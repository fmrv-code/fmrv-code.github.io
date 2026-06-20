import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../data/datasources/app_database.dart';

class BackupService {
  const BackupService(this._db);
  final AppDatabase _db;

  // ── Dossier de sync automatique ───────────────────────────────────────────

  /// Dossier dans lequel Capsule écrit la sauvegarde automatiquement.
  /// Android : /sdcard/Android/data/<pkg>/files/
  /// Linux   : ~/Documents/Capsule/
  Future<String?> get syncFolderPath async {
    try {
      if (Platform.isAndroid) {
        final dir = await getExternalStorageDirectory();
        return dir?.path;
      } else {
        final home = Platform.environment['HOME'];
        if (home != null) return p.join(home, 'Documents', 'Capsule');
        final docs = await getApplicationDocumentsDirectory();
        return p.join(docs.path, 'Capsule');
      }
    } catch (_) {
      return null;
    }
  }

  Future<String?> get syncFilePath async {
    final folder = await syncFolderPath;
    return folder != null ? p.join(folder, 'capsule_backup.json') : null;
  }

  /// Sauvegarde silencieuse déclenchée à chaque enregistrement de note.
  Future<void> autoBackup() async {
    try {
      final path = await syncFilePath;
      if (path == null) return;
      final dir = Directory(p.dirname(path));
      if (!dir.existsSync()) dir.createSync(recursive: true);
      final json = await _buildJson();
      await File(path).writeAsString(json, flush: true);
    } catch (_) {
      // Silencieux : l'export manuel reste disponible
    }
  }

  // ── Export ─────────────────────────────────────────────────────────────────

  Future<void> exportAndShare() async {
    final json = await _buildJson();
    final dir = await getTemporaryDirectory();
    final file = File(p.join(dir.path, 'capsule_backup.json'));
    await file.writeAsString(json, flush: true);
    await Share.shareXFiles(
      [XFile(file.path, mimeType: 'application/json')],
      subject: 'Sauvegarde Capsule',
    );
  }

  Future<String> _buildJson() async {
    final notes = await _db.select(_db.notes).get();
    final capsules = await _db.select(_db.capsules).get();
    final links = await _db.select(_db.capsuleLinks).get();
    final tags = await _db.select(_db.tags).get();

    return jsonEncode({
      'version': 1,
      'exportedAt': DateTime.now().toIso8601String(),
      'notes': notes
          .map((n) => {
                'id': n.id,
                'title': n.title,
                'content': n.content,
                'capsuleId': n.capsuleId,
                'tagIds': n.tagIds,
                'isPinned': n.isPinned,
                'isFavorite': n.isFavorite,
                'createdAt': n.createdAt.toIso8601String(),
                'updatedAt': n.updatedAt.toIso8601String(),
              })
          .toList(),
      'capsules': capsules
          .map((c) => {
                'id': c.id,
                'name': c.name,
                'iconCodePoint': c.iconCodePoint,
                'colorValue': c.colorValue,
                'createdAt': c.createdAt.toIso8601String(),
                'updatedAt': c.updatedAt.toIso8601String(),
              })
          .toList(),
      'links': links
          .map((l) => {
                'id': l.id,
                'url': l.url,
                'title': l.title,
                'description': l.description,
                'faviconUrl': l.faviconUrl,
                'imageUrl': l.imageUrl,
                'siteName': l.siteName,
                'capsuleId': l.capsuleId,
                'tagIds': l.tagIds,
                'isFavorite': l.isFavorite,
                'createdAt': l.createdAt.toIso8601String(),
                'updatedAt': l.updatedAt.toIso8601String(),
              })
          .toList(),
      'tags': tags
          .map((t) => {
                'id': t.id,
                'name': t.name,
                'colorValue': t.colorValue,
                'createdAt': t.createdAt.toIso8601String(),
              })
          .toList(),
    });
  }

  // ── Import ─────────────────────────────────────────────────────────────────

  /// Retourne le nombre de notes importées.
  Future<int> importFromJson(String jsonString) async {
    final Map<String, dynamic> data = jsonDecode(jsonString);
    int count = 0;

    // Tags d'abord (référencés par notes/liens)
    final tags = (data['tags'] as List? ?? []);
    for (final t in tags) {
      await _db.into(_db.tags).insertOnConflictUpdate(TagsCompanion.insert(
            name: t['name'] as String,
            colorValue: Value(t['colorValue'] as int),
            createdAt: Value(DateTime.parse(t['createdAt'] as String)),
          ));
    }

    // Capsules
    final capsules = (data['capsules'] as List? ?? []);
    for (final c in capsules) {
      await _db
          .into(_db.capsules)
          .insertOnConflictUpdate(CapsulesCompanion.insert(
            name: c['name'] as String,
            iconCodePoint: Value(c['iconCodePoint'] as int),
            colorValue: Value(c['colorValue'] as int),
            createdAt: Value(DateTime.parse(c['createdAt'] as String)),
            updatedAt: Value(DateTime.parse(c['updatedAt'] as String)),
          ));
    }

    // Notes
    final notes = (data['notes'] as List? ?? []);
    for (final n in notes) {
      final tagIds = (n['tagIds'] as List).cast<int>();
      await _db.into(_db.notes).insertOnConflictUpdate(NotesCompanion.insert(
            title: n['title'] as String,
            content: Value(n['content'] as String),
            capsuleId: Value(n['capsuleId'] as int?),
            tagIds: Value(tagIds),
            isPinned: Value(n['isPinned'] as bool),
            isFavorite: Value(n['isFavorite'] as bool),
            createdAt: Value(DateTime.parse(n['createdAt'] as String)),
            updatedAt: Value(DateTime.parse(n['updatedAt'] as String)),
          ));
      count++;
    }

    // Liens
    final links = (data['links'] as List? ?? []);
    for (final l in links) {
      final tagIds = (l['tagIds'] as List).cast<int>();
      await _db
          .into(_db.capsuleLinks)
          .insertOnConflictUpdate(CapsuleLinksCompanion.insert(
            url: l['url'] as String,
            title: Value(l['title'] as String?),
            description: Value(l['description'] as String?),
            faviconUrl: Value(l['faviconUrl'] as String?),
            imageUrl: Value(l['imageUrl'] as String?),
            siteName: Value(l['siteName'] as String?),
            capsuleId: Value(l['capsuleId'] as int?),
            tagIds: Value(tagIds),
            isFavorite: Value(l['isFavorite'] as bool),
            createdAt: Value(DateTime.parse(l['createdAt'] as String)),
            updatedAt: Value(DateTime.parse(l['updatedAt'] as String)),
          ));
    }

    return count;
  }
}
