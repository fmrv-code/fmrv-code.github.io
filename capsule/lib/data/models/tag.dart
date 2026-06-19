import 'package:isar/isar.dart';

part 'tag.g.dart';

@collection
class Tag {
  Id id = Isar.autoIncrement;

  @Index(unique: true, replace: true)
  late String name;

  // Stored as ARGB int (e.g. 0xFF007AFF)
  int colorValue = 0xFF007AFF;

  DateTime createdAt = DateTime.now();
}
