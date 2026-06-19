import 'package:isar/isar.dart';
import 'package:path_provider/path_provider.dart';

import '../models/capsule.dart';
import '../models/capsule_link.dart';
import '../models/note.dart';
import '../models/tag.dart';

abstract final class IsarService {
  static Future<Isar> open() async {
    final dir = await getApplicationDocumentsDirectory();
    return Isar.open(
      [
        NoteSchema,
        CapsuleSchema,
        CapsuleLinkSchema,
        TagSchema,
      ],
      directory: dir.path,
      name: 'capsule_db',
    );
  }
}
