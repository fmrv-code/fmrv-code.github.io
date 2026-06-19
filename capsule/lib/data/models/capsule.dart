import 'package:isar/isar.dart';

part 'capsule.g.dart';

@collection
class Capsule {
  Id id = Isar.autoIncrement;

  @Index()
  late String name;

  /// Material icon codepoint (e.g. Icons.folder.codePoint)
  int iconCodePoint = 0xe2c7; // Icons.folder

  /// Accent color stored as ARGB int
  int colorValue = 0xFF007AFF;

  int noteCount = 0;

  DateTime createdAt = DateTime.now();
  DateTime updatedAt = DateTime.now();
}
