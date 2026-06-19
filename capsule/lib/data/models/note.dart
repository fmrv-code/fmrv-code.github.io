import 'package:isar/isar.dart';

part 'note.g.dart';

@collection
class Note {
  Id id = Isar.autoIncrement;

  @Index()
  late String title;

  String content = '';

  /// null → note lives in the root (no capsule)
  @Index()
  int? capsuleId;

  List<int> tagIds = [];

  bool isPinned = false;
  bool isFavorite = false;

  @Index()
  DateTime createdAt = DateTime.now();

  DateTime updatedAt = DateTime.now();

  // Derived helper — not persisted by Isar
  @ignore
  String get excerpt {
    final plain = content.replaceAll(RegExp(r'[#*_`>~\[\]!]'), '').trim();
    return plain.length > 120 ? '${plain.substring(0, 120)}…' : plain;
  }
}
